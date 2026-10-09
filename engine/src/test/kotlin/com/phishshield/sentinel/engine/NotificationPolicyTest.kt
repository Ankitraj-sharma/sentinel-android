package com.phishshield.sentinel.engine

import org.junit.Assert.*
import org.junit.Test

class NotificationPolicyTest {
    private val policy = NotificationPolicy(true, setOf("com.whatsapp", "com.google.android.gm"))
    @Test fun onlyExactSelectedOwnersAreAccepted() {
        assertTrue(policy.accepts("com.whatsapp", "sentinel", false, false))
        assertFalse(policy.accepts("com.whatsapp.fake", "sentinel", false, false))
        assertFalse(policy.accepts("other", "sentinel", false, false))
        assertFalse(policy.copy(enabled = false).accepts("com.whatsapp", "sentinel", false, false))
        assertFalse(policy.copy(packages = emptySet()).accepts("com.whatsapp", "sentinel", false, false))
    }
    @Test fun selfSummaryAndOngoingEventsAreIgnored() {
        assertFalse(policy.accepts("com.whatsapp", "com.whatsapp", false, false))
        assertFalse(policy.accepts("com.whatsapp", "sentinel", true, false))
        assertFalse(policy.accepts("com.whatsapp", "sentinel", false, true))
    }
    @Test fun repeatedEventsAreSuppressedButUpdatesAndDifferentKeysAreNot() {
        val dedup = NotificationDeduplicator()
        assertFalse(dedup.duplicate("a", "first"))
        assertTrue(dedup.duplicate("a", "first"))
        assertFalse(dedup.duplicate("a", "changed"))
        assertFalse(dedup.duplicate("b", "changed"))
        dedup.clear()
        assertFalse(dedup.duplicate("a", "changed"))
    }
    @Test fun boundedDedupEvictsOldKeys() {
        val dedup = NotificationDeduplicator(2)
        dedup.duplicate("a", "1"); dedup.duplicate("b", "2"); dedup.duplicate("c", "3")
        assertFalse(dedup.duplicate("a", "1"))
        assertTrue(dedup.duplicate("c", "3"))
    }
}
