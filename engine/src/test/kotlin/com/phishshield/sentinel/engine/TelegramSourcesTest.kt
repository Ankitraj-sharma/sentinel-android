package com.phishshield.sentinel.engine

import org.junit.Test
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class TelegramSourcesTest {
    @Test fun officialClientsRespectNotificationConsentAndFilters() {
        val policy = NotificationPolicy(true, TelegramSources.packages)
        for (owner in TelegramSources.packages) {
            assertTrue(policy.accepts(owner, "sentinel", false, false))
            assertFalse(policy.copy(enabled = false).accepts(owner, "sentinel", false, false))
            assertFalse(policy.copy(packages = emptySet()).accepts(owner, "sentinel", false, false))
            assertFalse(policy.accepts(owner, "sentinel", true, false))
            assertFalse(policy.accepts(owner, "sentinel", false, true))
            assertFalse(policy.accepts(owner, owner, false, false))
        }
        for (owner in listOf("org.telegram.messenger.fake", "org.telegram.messenger.beta", "org.thunderdog.challegram")) {
            assertFalse(policy.accepts(owner, "sentinel", false, false))
        }
    }
}
