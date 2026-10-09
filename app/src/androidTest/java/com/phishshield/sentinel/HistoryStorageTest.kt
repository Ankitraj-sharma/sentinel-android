package com.phishshield.sentinel

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.phishshield.sentinel.data.*
import com.phishshield.sentinel.engine.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

@RunWith(AndroidJUnit4::class)
class HistoryStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val result = ThreatEngine().analyse("Send OTP 123456 immediately https://paypal.com.evil.test/private")
    private fun isolated(): Pair<EncryptedHistoryStorage, File> {
        val suffix = UUID.randomUUID().toString()
        val name = "history-test-$suffix.bin"
        return EncryptedHistoryStorage(context, name, "sentinel.test.history.$suffix") to File(context.noBackupFilesDir, name)
    }
    @Test fun keystoreCiphertextPersistsMetadataWithoutRawContent() = runBlocking {
        val (storage, file) = isolated()
        try {
            val repository = HistoryRepository(storage); repository.awaitReady()
            assertFalse(repository.state.value.enabled); assertFalse(file.exists())
            assertFalse(repository.record(repository.ticket(), HistorySource.MANUAL, result))
            assertTrue(repository.setEnabled(true))
            assertTrue(repository.record(repository.ticket(), HistorySource.SHARED_TEXT, result))
            val entry = repository.state.value.entries.single()
            val bytes = file.readBytes().toString(Charsets.ISO_8859_1)
            for (secret in listOf("123456", "evil.test", "credential-request", "SHARED_TEXT")) assertFalse(secret, bytes.contains(secret))
            val recreated = HistoryRepository(storage); recreated.awaitReady()
            assertTrue(recreated.state.value.enabled)
            assertEquals(entry, recreated.state.value.entries.single())
            assertEquals(listOf(HistorySource.SHARED_TEXT), storage.read().entries.map { it.source })
        } finally { storage.reset() }
    }
    @Test fun consentAndDeletionInvalidateQueuedTickets() = runBlocking {
        val (storage, file) = isolated()
        try {
            val repository = HistoryRepository(storage); repository.awaitReady()
            val beforeConsent = repository.ticket()
            repository.setEnabled(true)
            assertFalse(repository.record(beforeConsent, HistorySource.QR_CAMERA, result))
            val beforeDelete = repository.ticket()
            repository.deleteAll()
            assertFalse(repository.record(beforeDelete, HistorySource.MANUAL, result))
            assertTrue(repository.record(repository.ticket(), HistorySource.QR_IMAGE, result))
            val id = repository.state.value.entries.single().id
            val beforeIndividualDelete = repository.ticket()
            repository.delete(id)
            assertFalse(repository.record(beforeIndividualDelete, HistorySource.MANUAL, result))
            val beforeOff = repository.ticket()
            val queued = (0..15).map { async(Dispatchers.Default) { repository.record(beforeOff, HistorySource.SYNTHETIC_DEMO, result) } }
            repository.setEnabled(false); queued.awaitAll()
            assertFalse(repository.state.value.enabled); assertTrue(repository.state.value.entries.isEmpty())
            assertFalse(file.exists()); assertFalse(storage.read().enabled)
            assertFalse(repository.record(beforeOff, HistorySource.MANUAL, result))
        } finally { storage.reset() }
    }
    @Test fun capacityAndExpiryAreEnforcedInPersistedStore() = runBlocking {
        val (storage, _) = isolated()
        val now = AtomicLong(HistoryPolicy.RETENTION_MILLIS * 2)
        try {
            val repository = HistoryRepository(storage, now::get); repository.awaitReady(); repository.setEnabled(true)
            val ticket = repository.ticket()
            repeat(105) { assertTrue(repository.record(ticket, HistorySource.MANUAL, result)) }
            assertEquals(100, storage.read().entries.size)
            now.addAndGet(HistoryPolicy.RETENTION_MILLIS + 1)
            repository.refresh()
            assertTrue(repository.state.value.entries.isEmpty())
            assertTrue(storage.read().entries.isEmpty())
        } finally { storage.reset() }
    }
    @Test fun corruptionFailsClosedAndExplicitResetRecovers() = runBlocking {
        val (storage, file) = isolated()
        try {
            val original = HistoryRepository(storage); original.awaitReady(); original.setEnabled(true)
            original.record(original.ticket(), HistorySource.MANUAL, result)
            val tampered = file.readBytes().apply { this[lastIndex] = (this[lastIndex].toInt() xor 1).toByte() }
            file.writeBytes(tampered)
            val repository = HistoryRepository(storage); repository.awaitReady()
            assertFalse(repository.state.value.enabled); assertNotNull(repository.state.value.error)
            assertNull(repository.ticket()); assertTrue(repository.state.value.entries.isEmpty())
            assertFalse(repository.setEnabled(true)); assertArrayEquals(tampered, file.readBytes())
            assertTrue(repository.setEnabled(false)); assertNull(repository.state.value.error); assertFalse(file.exists())
            assertTrue(repository.setEnabled(true)); assertTrue(repository.record(repository.ticket(), HistorySource.MANUAL, result))
        } finally { storage.reset() }
    }
}
