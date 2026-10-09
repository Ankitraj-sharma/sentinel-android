package com.phishshield.sentinel.engine

import org.junit.Assert.*
import org.junit.Test

class HistoryPolicyTest {
    @Test fun metadataOmitsAllMessageUrlAndEvidenceFields() {
        val result = ThreatEngine().analyse("Send OTP immediately https://paypal.com.evil.test/private?secret=123456")
        val record = HistoryPolicy.metadata("synthetic", 100, HistorySource.SHARED_TEXT, result)!!
        assertTrue(record.rules.contains(HistoryRule.SECRET))
        assertFalse(record.toString().contains("123456"))
        assertFalse(record.toString().contains("evil.test"))
        assertFalse(record.toString().contains("Send OTP"))
        assertNull(HistoryPolicy.metadata("rejected", 100, HistorySource.MANUAL, ThreatEngine().analyse("")))
    }
    @Test fun retentionRemovesExpiredFutureAndDuplicateEntriesAndCapsCount() {
        val now = HistoryPolicy.RETENTION_MILLIS + 10_000
        fun entry(id: String, time: Long) = HistoryRecord(id, time, HistorySource.MANUAL, RiskLevel.HIGH, AnalysisStatus.COMPLETE, 70, emptyList())
        val input = (0..120).map { entry("$it", now - it) } + entry("expired", now - HistoryPolicy.RETENTION_MILLIS) + entry("future", now + 1) + entry("0", now)
        val kept = HistoryPolicy.retain(input, now)
        assertEquals(100, kept.size)
        assertEquals("0", kept.first().id)
        assertFalse(kept.any { it.id == "expired" || it.id == "future" })
        assertEquals(100, kept.map { it.id }.distinct().size)
    }
}
