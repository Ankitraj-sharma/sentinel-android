package com.phishshield.sentinel.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.phishshield.sentinel.engine.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class HistorySnapshot(val enabled: Boolean = false, val entries: List<HistoryRecord> = emptyList())
interface HistoryStorage {
    fun read(): HistorySnapshot
    fun write(snapshot: HistorySnapshot)
    fun reset()
}

/** Authenticated ciphertext in private storage; Keystore key material never leaves Android Keystore. */
class EncryptedHistoryStorage(context: Context, fileName: String = "history-v1.bin", private val alias: String = "sentinel.history.v1") : HistoryStorage {
    private val file = File(context.noBackupFilesDir, fileName)
    private val atomic = AtomicFile(file)
    private val magic = byteArrayOf(83, 72, 54, 1)
    private val maxBytes = 128 * 1024
    private fun store() = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private fun key(create: Boolean): SecretKey {
        (store().getKey(alias, null) as? SecretKey)?.let { return it }
        if (!create) throw IOException("History key unavailable")
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    override fun read(): HistorySnapshot {
        if (!file.exists() && !File(file.path + ".bak").exists()) return HistorySnapshot()
        val bytes = atomic.openRead().use { input ->
            val output = ByteArrayOutputStream(); val chunk = ByteArray(4096)
            while (output.size() <= maxBytes) {
                val count = input.read(chunk, 0, minOf(chunk.size, maxBytes + 1 - output.size()))
                if (count < 0) break
                output.write(chunk, 0, count)
            }
            output.toByteArray()
        }
        if (bytes.size !in 33..maxBytes || !bytes.copyOfRange(0, 4).contentEquals(magic)) throw IOException("Invalid history envelope")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(false), GCMParameterSpec(128, bytes.copyOfRange(4, 16)))
        cipher.updateAAD(magic)
        val json = JSONObject(String(cipher.doFinal(bytes.copyOfRange(16, bytes.size)), Charsets.UTF_8))
        require(json.getInt("v") == 1)
        val enabled = json.getBoolean("enabled")
        val items = json.getJSONArray("items")
        require(items.length() <= HistoryPolicy.MAX_ENTRIES)
        val records = (0 until items.length()).map { index ->
            val item = items.getJSONObject(index)
            val id = item.getString("id"); UUID.fromString(id)
            val time = item.getLong("time"); require(time >= 0)
            val score = if (item.isNull("score")) null else item.getInt("score").also { require(it in 0..100) }
            val codes = item.getJSONArray("rules"); require(codes.length() <= HistoryRule.entries.size)
            val status = AnalysisStatus.valueOf(item.getString("status")); require(status != AnalysisStatus.NOT_ANALYSED)
            HistoryRecord(id, time, HistorySource.valueOf(item.getString("source")), RiskLevel.valueOf(item.getString("level")), status, score,
                (0 until codes.length()).map { HistoryRule.valueOf(codes.getString(it)) }.distinct())
        }
        require(enabled || records.isEmpty())
        return HistorySnapshot(enabled, records)
    }
    override fun write(snapshot: HistorySnapshot) {
        require(snapshot.entries.size <= HistoryPolicy.MAX_ENTRIES)
        val items = JSONArray()
        snapshot.entries.forEach { item ->
            items.put(JSONObject().put("id", item.id).put("time", item.timeMillis).put("source", item.source.name)
                .put("level", item.level.name).put("status", item.status.name).put("score", item.score ?: JSONObject.NULL)
                .put("rules", JSONArray(item.rules.map { it.name })))
        }
        val plain = JSONObject().put("v", 1).put("enabled", snapshot.enabled).put("items", items).toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(true)); cipher.updateAAD(magic)
        require(cipher.iv.size == 12)
        val packet = magic + cipher.iv + cipher.doFinal(plain)
        require(packet.size <= maxBytes)
        val stream = atomic.startWrite()
        try { stream.write(packet); atomic.finishWrite(stream) }
        catch (failure: Exception) { atomic.failWrite(stream); throw failure }
        if (read() != snapshot) throw IOException("History write did not commit")
    }
    override fun reset() {
        // Key removal makes any remaining old envelope unreadable if filesystem deletion fails.
        store().deleteEntry(alias)
        atomic.delete()
        if (file.exists() || File(file.path + ".bak").exists() || File(file.path + ".new").exists()) throw IOException("History deletion failed")
    }
}
