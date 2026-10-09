package com.phishshield.sentinel.engine

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.File
import java.security.MessageDigest

class DetectionRegressionTest {
    private val engine = ThreatEngine()

    @Test fun scamsWithoutOtpRequestsProduceWarnings() {
        for (message in listOf(
            "Your parcel is delayed. Visit https://delivery-support.test to confirm",
            "Invest with us and double your money tomorrow",
            "We are your boss. Buy gift cards and send the numbers",
            "Your tax refund is ready https://refund-centre.test",
            "Bank support: install AnyDesk to resolve your account problem",
            "Work from home job: pay the registration fee to start")) {
            assertTrue(message, engine.analyse(message).level in setOf(RiskLevel.REVIEW, RiskLevel.HIGH))
        }
    }

    @Test fun routineTopicsAndLegitimateLocalUrlsDoNotBecomeWarnings() {
        for (message in listOf(
            "Your parcel has arrived. Collect it at reception",
            "Delivery delayed. We will try again tomorrow",
            "Investments can lose value. Read the documentation",
            "Buy a gift card for your friend's birthday",
            "Never buy gift cards and send the codes",
            "Your tax refund was deposited into your account",
            "Never install AnyDesk for bank support",
            "Job interview is tomorrow. There is no registration fee",
            "http://192.168.1.1", "http://localhost:8080", "https://example.com",
            "https://fonts.googleapis.com/css", "https://www.paypalobjects.com/image.png",
            "Your OTP is 123456. Never share it", "Enter your OTP in the official app")) {
            assertEquals(message, RiskLevel.NO_OBVIOUS_THREAT, engine.analyse(message).level)
        }
    }

    @Test fun exactHistoricalMatchDoesNotContaminateOtherPagesOnTheHost() {
        val canonical = UrlThreatIndex.canonicalUrl("https://example.com/Bad?x=A")!!
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
        val bytes = ByteArrayOutputStream().also { buffer ->
            DataOutputStream(buffer).use { it.writeInt(0x53554931); it.writeInt(1); it.write(digest); it.writeByte(0) }
        }.toByteArray()
        val index = UrlThreatIndex.read(ByteArrayInputStream(bytes))
        val indexed = ThreatEngine(threatLookup = index)
        assertEquals(RiskLevel.HIGH, indexed.analyse("http://EXAMPLE.COM/Bad?x=A#section").level)
        for (url in listOf("https://example.com", "https://example.com/bad?x=A", "https://example.com/Bad?x=B")) {
            assertEquals(url, RiskLevel.NO_OBVIOUS_THREAT, indexed.analyse(url).level)
        }
        assertTrue(indexed.analyse("https://example.com/Bad?x=A").signals.single().explanation.contains("historical"))
    }

    @Test fun bundledIndexLoadsAndHandlesSuppliedDatasetExamples() {
        val index = UrlThreatIndex.read(File("../app/src/main/assets/url-threats-v1.bin").inputStream())
        val indexed = ThreatEngine(threatLookup = index)
        assertEquals(RiskLevel.HIGH, indexed.analyse("br-icloud.com.br").level)
        assertEquals(RiskLevel.HIGH, indexed.analyse("Sender\nVisit br-icloud.com.br to sign in").level)
        assertEquals(RiskLevel.HIGH, indexed.analyse("http://www.pashminaonline.com/pure-pashminas").level)
        assertEquals(RiskLevel.NO_OBVIOUS_THREAT, indexed.analyse("bopsecrets.org/rexroth/cr/1.htm").level)
        assertNull(index.find("https://example.com/unknown-page"))
    }

    @Test fun newSignalsPersistAsTypedMetadataOnly() {
        val result = engine.analyse("Invest with us and double your money tomorrow")
        val metadata = HistoryPolicy.metadata("test", 1L, HistorySource.MANUAL, result)!!
        assertEquals(listOf(HistoryRule.INVESTMENT), metadata.rules)
        assertFalse(metadata.toString().contains("Invest with us"))
    }

    @Test fun truncatedAndCorruptedIndexesAreRejected() {
        for (bytes in listOf(byteArrayOf(), ByteArray(8), byteArrayOf(83, 85, 73, 49, 0, 0, 0, 1))) {
            assertThrows(Exception::class.java) { UrlThreatIndex.read(ByteArrayInputStream(bytes)) }
        }
    }

    @Test fun pastedUrlPathAndQueryPunctuationAreNotSilentlyRemoved() {
        for (url in listOf("https://example.com/a;b;", "https://example.com/?name=O'Reilly", "example.com/file.",
            "example.com?key=Value", "192.168.1.1/login")) {
            assertEquals(url, engine.analyse(url).urls.single().original)
        }
        assertEquals("example.com?key=Value", engine.analyse("Visit example.com?key=Value for details").urls.single().original)
    }
}
