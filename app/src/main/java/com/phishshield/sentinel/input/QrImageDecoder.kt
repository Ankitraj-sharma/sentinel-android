package com.phishshield.sentinel.input

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.multi.qrcode.QRCodeMultiReader
import java.io.ByteArrayOutputStream

class QrImportException(message: String) : Exception(message)

/** Bounded, local single-image decoding. No filesystem paths, remote URLs or URI permissions retained. */
object QrImageDecoder {
    private const val MAX_BYTES = 12 * 1024 * 1024
    private const val MAX_PIXELS = 40_000_000L
    fun read(resolver: ContentResolver, uri: Uri): String {
        if (uri.scheme != "content") throw QrImportException("Choose an image through the Android picker.")
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (output.size() + count > MAX_BYTES) throw QrImportException("Image exceeds 12 MB. Choose a smaller image.")
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        } ?: throw QrImportException("Image could not be opened. Choose it again.")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0 || bounds.outWidth.toLong() * bounds.outHeight > MAX_PIXELS)
            throw QrImportException("Image format or dimensions are unsupported. Choose a smaller PNG or JPEG.")
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 2048) sample *= 2
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: throw QrImportException("Image could not be decoded.")
        return try { decode(bitmap) } finally { bitmap.recycle() }
    }
    fun decode(bitmap: Bitmap): String {
        val width = bitmap.width; val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        val source = RGBLuminanceSource(width, height, pixels)
        val hints = mapOf(DecodeHintType.TRY_HARDER to true, DecodeHintType.CHARACTER_SET to "UTF-8")
        val results = try { QRCodeMultiReader().decodeMultiple(BinaryBitmap(HybridBinarizer(source)), hints) }
        catch (_: NotFoundException) { throw QrImportException("No readable QR code found. Use a clear image with one complete QR code.") }
        if (results.size != 1) throw QrImportException("Multiple QR codes found. Crop the image to one code and try again.")
        return results.single().text
    }
}
