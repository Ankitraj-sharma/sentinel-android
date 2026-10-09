package com.phishshield.sentinel.engine

import org.junit.Assert.*
import org.junit.Test

class ScanInputTest {
    @Test fun completeSharedTextIsPreservedWithoutSilentTruncation() {
        val value = "Send OTP immediately https://paypal.com.evil.test"
        assertEquals(value, validateImportedInput(value, InputSource.SHARED_TEXT).text)
        assertNotNull(validateImportedInput("x".repeat(ThreatEngine.MAX_INPUT + 1), InputSource.SHARED_TEXT).error)
        assertEquals("", validateImportedInput("x".repeat(ThreatEngine.MAX_INPUT + 1), InputSource.SHARED_TEXT).text)
        assertNotNull(validateImportedInput(null, InputSource.SHARED_TEXT).error)
    }
    @Test fun qrWebDestinationsReachTheExistingEngine() {
        for (source in listOf(InputSource.QR_CAMERA, InputSource.QR_IMAGE)) {
            val imported = validateImportedInput("https://paypal.com.evil.test", source)
            assertNull(imported.error)
            assertTrue(ThreatEngine().analyse(imported.text).signals.any { it.id == "brand-domain-mismatch" })
            assertEquals("HTTPS://example.org", validateImportedInput("HTTPS://example.org", source).text)
        }
    }
    @Test fun unsupportedQrCommandsAndMultilineDestinationsAreNotAssessed() {
        for (value in listOf("upi://pay?pa=demo", "WIFI:T:WPA;S:demo;;", "tel:123", "javascript:alert(1)", "BEGIN:VCARD", "https://example.org\nhttps://evil.test", "https://example.org\u0000")) {
            assertNotNull(value, validateImportedInput(value, InputSource.QR_CAMERA).error)
        }
    }
}
