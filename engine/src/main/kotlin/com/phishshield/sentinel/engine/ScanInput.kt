package com.phishshield.sentinel.engine

enum class InputSource(val label: String) {
    MANUAL("Manual input"), SHARED_TEXT("Shared text"), QR_CAMERA("QR camera"), QR_IMAGE("QR image")
}

data class ImportedInput(val text: String = "", val error: String? = null)

/** Reject complete unsupported payloads; never truncate or execute QR instructions. */
fun validateImportedInput(text: CharSequence?, source: InputSource): ImportedInput {
    if (text == null || text.isBlank()) return ImportedInput(error = "No readable content received. Paste a message or try another QR code.")
    if (text.length > ThreatEngine.MAX_INPUT) return ImportedInput(error = "Content exceeds ${ThreatEngine.MAX_INPUT} characters. Nothing was imported or scanned.")
    val value = text.toString().trim()
    if (source == InputSource.QR_CAMERA || source == InputSource.QR_IMAGE) {
        if (!Regex("(?i)^https?://").containsMatchIn(value)) return ImportedInput(error = "This QR payload is unsupported. Only HTTP(S) web destinations can be analysed; payment, Wi-Fi, contact and app commands are not assessed.")
        if (value.any { it.isWhitespace() || it.isISOControl() }) return ImportedInput(error = "The QR destination contains whitespace or control characters. Nothing was scanned.")
    }
    return ImportedInput(text = value)
}
