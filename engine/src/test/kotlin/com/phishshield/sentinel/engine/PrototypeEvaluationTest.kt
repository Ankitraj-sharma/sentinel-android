package com.phishshield.sentinel.engine

import org.junit.Assert.*
import org.junit.Test

class PrototypeEvaluationTest {
    @Test fun labelledFixturesAreUniqueBoundedAndDiagnosticsExposeMisses() {
        val cases = PrototypeEvaluation.cases()
        assertEquals(40, cases.size)
        assertEquals(cases.size, cases.map { it.id }.distinct().size)
        assertEquals(16, cases.count { it.label == "SCAM" })
        assertEquals(16, cases.count { it.label == "BENIGN" })
        assertTrue(cases.all { it.text.length in 1..ThreatEngine.MAX_INPUT })
        val observed = PrototypeEvaluation.observe()
        assertEquals(40, observed.size)
        // Diagnostics must report actual outcomes; improved coverage may eliminate these authored misses.
        assertEquals(16, observed.count { it.case.label == "SCAM" })
        assertTrue(observed.any { it.case.label == "BENIGN" && it.warned })
        val report = PrototypeEvaluation.report(observed)
        assertTrue(report.contains("not real user data"))
        assertTrue(report.contains("Recall includes abstained"))
        assertTrue(report.contains("| s15 | SCAM |"))
    }
}
