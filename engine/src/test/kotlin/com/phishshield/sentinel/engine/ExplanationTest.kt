package com.phishshield.sentinel.engine

import org.junit.Assert.*
import org.junit.Test

class ExplanationTest {
    private val engine = ThreatEngine()

    @Test fun partialCoverageIsVisibleEvenWhenPositiveEvidenceIsHighRisk() {
        val result = engine.analyse("Send OTP immediately https://bit.ly/demo")
        assertEquals(RiskLevel.HIGH, result.level)
        assertEquals(AnalysisStatus.LIMITED, result.status)
        assertTrue(result.explain().coverage.startsWith("Partial coverage"))
        assertTrue(result.explain().scoreMeaning.contains("not a probability"))
    }

    @Test fun benignAndUnknownResultsNeverClaimVerifiedSafety() {
        val benign = engine.analyse("Appointment tomorrow.").explain()
        assertTrue(benign.summary.contains("does not establish"))
        val unknown = engine.analyse("https://bit.ly/demo").explain()
        assertTrue(unknown.summary.contains("could not be fully assessed"))
        assertTrue(unknown.coverage.startsWith("Partial coverage"))
    }

    @Test fun rejectedContentHasNoScoreOrInventedRulesAndGivesRecoveryStep() {
        val result = engine.analyse("x".repeat(ThreatEngine.MAX_INPUT + 1))
        val explanation = result.explain()
        assertNull(result.ruleScore)
        assertTrue(explanation.coverage.startsWith("Not analysed"))
        assertTrue(explanation.scoreMeaning.startsWith("No score was produced"))
        assertEquals(1, explanation.verificationSteps.size)
        assertTrue(result.signals.isEmpty())
    }

    @Test fun guidanceUsesIndependentContactsAndNeverInventsDestination() {
        val result = engine.analyse("Give password immediately")
        assertEquals(2, result.explain().verificationSteps.size)
        assertTrue(result.explain().verificationSteps.first().contains("contact you already know"))
        assertEquals(3, engine.analyse("https://example.com").explain().verificationSteps.size)
    }

    @Test fun invisibleAndBidirectionalControlsCannotDisguiseEvidence() {
        assertEquals("pay⟦U+200B⟧pal⟦U+202E⟧.com", visibleEvidence("pay\u200Bpal\u202E.com"))
        assertEquals("a⟦U+0000⟧b\n\tc", visibleEvidence("a\u0000b\n\tc"))
        assertEquals("Normal text 🔎", visibleEvidence("Normal text 🔎"))
    }
}
