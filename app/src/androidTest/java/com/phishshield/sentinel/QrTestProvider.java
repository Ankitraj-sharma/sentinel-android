package com.phishshield.sentinel;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

/** Test APK provider runs separately: Android/Java only, no target-app Kotlin/ZXing dependencies. */
public class QrTestProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "image/png"; }
    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] args, String order) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] args) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { return 0; }
    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        final String kind = uri.getLastPathSegment();
        if (!"r".equals(mode) || !("valid".equals(kind) || "invalid".equals(kind) || "large".equals(kind) || "dimensions".equals(kind))) throw new FileNotFoundException("Synthetic missing image");
        final ParcelFileDescriptor[] pipe;
        try { pipe = ParcelFileDescriptor.createPipe(); }
        catch (IOException failure) { throw new FileNotFoundException("Could not create synthetic pipe"); }
        new Thread(() -> {
            try (ParcelFileDescriptor.AutoCloseOutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])) {
                if ("large".equals(kind)) { byte[] chunk = new byte[8192]; for (int i = 0; i < 1600; i++) output.write(chunk); return; }
                if ("invalid".equals(kind)) { output.write(new byte[] {1, 2, 3, 4}); return; }
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                try (InputStream image = getContext().getAssets().open("qr-synthetic.png")) {
                    byte[] chunk = new byte[4096]; int count;
                    while ((count = image.read(chunk)) != -1) bytes.write(chunk, 0, count);
                }
                byte[] data = bytes.toByteArray();
                if ("dimensions".equals(kind)) {
                    ByteBuffer.wrap(data).putInt(16, 100000).putInt(20, 100000);
                    CRC32 crc = new CRC32(); crc.update(data, 12, 17);
                    ByteBuffer.wrap(data).putInt(29, (int) crc.getValue());
                }
                output.write(data);
            } catch (IOException ignored) { /* Size rejection intentionally closes the reader early. */ }
        }, "synthetic-qr-provider").start();
        return pipe[0];
    }
}
