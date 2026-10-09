package com.phishshield.sentinel.engine

import org.junit.Assert.*
import org.junit.Test

class ThreatEngineTest {
    private val engine = ThreatEngine()

    @Test fun credentialDisclosureAndPressureAreHighRisk() {
        val result = engine.analyse("Send your OTP to our agent immediately.")
        assertEquals(RiskLevel.HIGH, result.level)
        assertEquals(setOf("credential-request", "urgency"), result.signals.map { it.id }.toSet())
        assertTrue(result.signals.all { it.evidence.isNotBlank() && it.explanation.isNotBlank() })
    }

    @Test fun securityAdviceDoesNotBecomeDisclosureRequest() {
        for (text in listOf("Do not share your OTP with anyone.", "Never share your password.",
            "Your bank will never ask you to share your OTP.", "Don't give your PIN to anyone.")) {
            assertEquals(text, RiskLevel.NO_OBVIOUS_THREAT, engine.analyse(text).level)
        }
    }

    @Test fun laterMaliciousInstructionIsNotHiddenByEarlierAdvice() {
        assertEquals(RiskLevel.HIGH, engine.analyse("Do not share your OTP. Send your OTP to me immediately.").level)
    }

    @Test fun normalMessagesAndWeakKeywordsDoNotTriggerDanger() {
        for (text in listOf("Your appointment is tomorrow at 10.", "Urgent: project deadline tomorrow.",
            "We won the football game!", "Enter your OTP in the official app.",
            "Please pay your invoice at https://paypal.com/invoice/42", "https://example.com/path")) {
            assertEquals(text, RiskLevel.NO_OBVIOUS_THREAT, engine.analyse(text).level)
        }
    }

    @Test fun configuredDomainMatchingHonoursBoundaries() {
        assertFalse(engine.analyse("https://login.paypal.com/").signals.any { it.id == "brand-domain-mismatch" })
        for (url in listOf("https://paypal.com.evil.test/", "https://notpaypal.com/", "https://paypa1.com/")) {
            assertTrue(url, engine.analyse(url).signals.any { it.id == "brand-domain-mismatch" })
        }
    }

    @Test fun configuredDomainsCannotSuppressTextEvidence() {
        assertEquals(RiskLevel.HIGH, engine.analyse("Send password immediately: https://paypal.com").level)
    }

    @Test fun anotherKnownBrandDomainDoesNotGrantTrust() {
        val result = engine.analyse("PayPal account suspended. Act now: https://google.com")
        assertEquals(RiskLevel.HIGH, result.level)
        assertTrue(result.signals.any { it.title.contains("PayPal") })
    }

    @Test fun userInfoDoesNotHideActualDestination() {
        val result = engine.analyse("https://paypal.com@evil.test/login")
        assertEquals("evil.test", result.urls.single().host)
        assertEquals(RiskLevel.HIGH, result.level)
    }

    @Test fun urlsAreNormalisedWithoutFollowingThem() {
        assertEquals("paypal.com", engine.analyse("HTTPS://PAYPAL.COM:443/path").urls.single().host)
        assertEquals("example.com", engine.analyse("www.example.com/path").urls.single().host)
        assertEquals("example.com", engine.analyse("example.com").urls.single().host)
        assertEquals("xn--bcher-kva.de", engine.analyse("https://bücher.de/").urls.single().host)
    }

    @Test fun numericUrlsDoNotRequireDnsAndIpv6BracketsAreKept() {
        assertEquals("[::1]", engine.analyse("https://[::1]").urls.single().host)
        assertEquals("192.168.1.1", engine.analyse("http://192.168.1.1").urls.single().host)
    }

    @Test fun unsupportedMalformedAndUnresolvedUrlsAreInconclusive() {
        for (url in listOf("javascript:alert(1)", "https://", "https://example.com:65536", "https://example.com:",
            "https://exa%6dple.com/", "https://bit.ly/example", "https://example.com\\evil")) {
            assertEquals(url, RiskLevel.UNKNOWN, engine.analyse(url).level)
        }
    }

    @Test fun maliciousSignalsSurvivePartialCoverage() {
        val result = engine.analyse("Send OTP immediately https://bit.ly/example")
        assertEquals(RiskLevel.HIGH, result.level)
        assertEquals(AnalysisStatus.LIMITED, result.status)
    }

    @Test fun repeatedLinksAndPhrasesCannotInflateScores() {
        val input = "PayPal account suspended. Act now https://paypal.com.evil.test"
        assertEquals(engine.analyse(input).ruleScore, engine.analyse("$input $input $input").ruleScore)
    }

    @Test fun contextualPrizePaymentIsFlaggedButPrizeAloneIsNot() {
        assertEquals(RiskLevel.HIGH, engine.analyse("You won a prize. Pay the processing fee immediately.").level)
        assertEquals(RiskLevel.NO_OBVIOUS_THREAT, engine.analyse("Congratulations, you won a prize!").level)
    }

    @Test fun inputLimitsRejectInsteadOfSilentlyTruncating() {
        for (input in listOf("", " ", "a".repeat(ThreatEngine.MAX_INPUT + 1),
            (1..17).joinToString(" ") { "https://example$it.com" })) {
            val result = engine.analyse(input)
            assertEquals(AnalysisStatus.NOT_ANALYSED, result.status)
            assertEquals(RiskLevel.UNKNOWN, result.level)
            assertNull(result.ruleScore)
        }
    }

    @Test fun unsupportedLanguageIsNotDeclaredSafe() {
        assertEquals(RiskLevel.UNKNOWN, engine.analyse("आपका खाता बंद हो जाएगा कृपया जानकारी भेजें").level)
    }

    @Test fun configurableBrandDirectorySupportsExactOfficialDomains() {
        val custom = ThreatEngine(listOf(Brand("Acme", setOf("acme.example"))))
        assertEquals(RiskLevel.NO_OBVIOUS_THREAT, custom.analyse("Acme urgent notice https://login.acme.example").level)
        assertEquals(RiskLevel.HIGH, custom.analyse("Acme account suspended. Act now https://acme.example.evil.test").level)
    }
}
