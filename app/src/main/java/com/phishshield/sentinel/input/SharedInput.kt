package com.phishshield.sentinel.input

import android.content.Intent
import com.phishshield.sentinel.engine.ImportedInput
import com.phishshield.sentinel.engine.InputSource
import com.phishshield.sentinel.engine.validateImportedInput

/** Only plain text, never streams/HTML/URI fetching; exported activity input is untrusted. */
fun readSharedInput(intent: Intent): ImportedInput? {
    if (intent.action != Intent.ACTION_SEND) return null
    if (intent.type != "text/plain") return ImportedInput(error = "Unsupported share type. Share plain text or a link, or use Choose QR image.")
    return try {
        validateImportedInput(intent.getCharSequenceExtra(Intent.EXTRA_TEXT), InputSource.SHARED_TEXT)
    } catch (_: RuntimeException) {
        ImportedInput(error = "Shared content could not be read. Paste the message instead.")
    }
}
