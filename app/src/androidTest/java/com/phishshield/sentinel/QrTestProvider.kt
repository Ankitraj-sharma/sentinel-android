package com.phishshield.sentinel

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.util.zip.CRC32

/** Installed only in the instrumentation APK; supplies fixed synthetic bytes through real content URIs. */
class QrTestProvider : ContentProvider() {
    override fun onCreate() = true
    override fun getType(uri: Uri) = "image/png"
    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val kind = uri.lastPathSegment
        if (mode != "r" || kind !in listOf("valid", "invalid", "large", "dimensions")) throw FileNotFoundException("Synthetic missing image")
        val pipe = ParcelFileDescriptor.createPipe()
        Thread {
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(pipe[1]).use { output ->
                    if (kind == "large") { repeat(1600) { output.write(ByteArray(8192)) }; return@use }
                    if (kind == "invalid") { output.write("Not an image".toByteArray()); return@use }
                    val size = 256
                    val matrix = QRCodeWriter().encode("https://example.org", BarcodeFormat.QR_CODE, size, size)
                    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
                    val pixels = IntArray(size * size) { index -> if (matrix[index % size, index / size]) -16777216 else -1 }
                    bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
                    val data = ByteArrayOutputStream().apply { bitmap.compress(Bitmap.CompressFormat.PNG, 100, this) }.toByteArray()
                    bitmap.recycle()
                    if (kind == "dimensions") {
                        ByteBuffer.wrap(data).putInt(16, 100_000).putInt(20, 100_000)
                        val crc = CRC32().apply { update(data, 12, 17) }
                        ByteBuffer.wrap(data).putInt(29, crc.value.toInt())
                    }
                    output.write(data)
                }
            } catch (_: Exception) { /* Reader deliberately closes early on size rejection. */ }
        }.start()
        return pipe[0]
    }
}
