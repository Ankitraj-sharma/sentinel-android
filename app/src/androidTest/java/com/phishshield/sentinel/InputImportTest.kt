package com.phishshield.sentinel

import android.content.Intent
import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.phishshield.sentinel.input.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class InputImportTest {
    private fun qr(text: String, size: Int = 512): Bitmap {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
            val pixels = IntArray(size * size) { index -> if (matrix[index % size, index / size]) android.graphics.Color.BLACK else android.graphics.Color.WHITE }
            setPixels(pixels, 0, size, 0, 0, size, size)
        }
    }
    @Test fun shareParserRejectsUnsupportedMissingAndOversizedContent() {
        assertNull(readSharedInput(Intent(Intent.ACTION_VIEW)))
        assertNotNull(readSharedInput(Intent(Intent.ACTION_SEND).setType("image/png"))!!.error)
        assertNotNull(readSharedInput(Intent(Intent.ACTION_SEND).setType("text/plain"))!!.error)
        val value = "Send OTP immediately https://paypal.com.evil.test"
        assertEquals(value, readSharedInput(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, value))!!.text)
        assertNotNull(readSharedInput(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "x".repeat(20_001)))!!.error)
    }
    @Test fun actualQrPixelsDecodeAndDemoImagesAreExportable() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for ((name, value) in listOf("suspicious" to "https://paypal.com@evil.test/login", "shortener" to "https://bit.ly/demo", "unsupported" to "upi://pay?pa=synthetic")) {
            val bitmap = qr(value)
            try {
                assertEquals(value, QrImageDecoder.decode(bitmap))
                File(context.cacheDir, "stage5-$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } finally { bitmap.recycle() }
        }
    }
    @Test fun blankAndAmbiguousQrImagesAreRejected() {
        val blank = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
        try { QrImageDecoder.decode(blank); fail("No QR must not produce a payload") }
        catch (expected: QrImportException) { assertTrue(expected.message!!.contains("No readable")) }
        finally { blank.recycle() }
        val first = qr("https://example.org", 256); val second = qr("https://evil.test", 256)
        val pair = Bitmap.createBitmap(640, 320, Bitmap.Config.ARGB_8888).apply {
            eraseColor(android.graphics.Color.WHITE)
            android.graphics.Canvas(this).apply { drawBitmap(first, 20f, 32f, null); drawBitmap(second, 360f, 32f, null) }
        }
        try { QrImageDecoder.decode(pair); fail("Multiple codes must not silently select one") }
        catch (expected: QrImportException) { assertTrue(expected.message!!.contains("Multiple")) }
        finally { first.recycle(); second.recycle(); pair.recycle() }
    }
    @Test fun mergedManifestHasCameraButNoNetworkOrInboxPermissions() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val permissions = context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        assertTrue(permissions.contains(android.Manifest.permission.CAMERA))
        for (forbidden in listOf(android.Manifest.permission.INTERNET, android.Manifest.permission.READ_SMS, android.Manifest.permission.READ_EXTERNAL_STORAGE, "android.permission.READ_MEDIA_IMAGES")) assertFalse(forbidden, permissions.contains(forbidden))
    }
}
