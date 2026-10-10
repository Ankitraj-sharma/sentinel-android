package com.phishshield.sentinel.engine

import org.junit.Assert.*
import org.junit.Test

class Stage7RegressionTest {
    @Test fun taskWithdrawalPaymentsDifferFromNormalPayrollUpdates() {
        assertTrue(ThreatEngine().analyse("Complete tasks then pay a deposit to unlock your earnings.").signals.any { it.id == "job-deposit" })
        assertFalse(ThreatEngine().analyse("Your salary and commission will arrive on Friday.").signals.any { it.id == "job-deposit" })
    }
    @Test fun visiblePreviewCutOffDoesNotTreatNormalPeriodsAsCutOff() {
        assertTrue(PreviewCompleteness.hasCutOffMarker("Sender\nYour account requires…"))
        assertTrue(PreviewCompleteness.hasCutOffMarker("Pay now...  "))
        assertFalse(PreviewCompleteness.hasCutOffMarker("Message received."))
    }
    @Test fun walletSecretsAreCredentialsButSafetyAdviceIsNotARequest() {
        assertTrue(ThreatEngine().analyse("Share your recovery phrase with our support agent.").signals.any { it.id == "credential-request" })
        assertFalse(ThreatEngine().analyse("Never share your recovery phrase with anyone.").signals.any { it.id == "credential-request" })
    }
    @Test fun indirectNegationDoesNotTurnSafetyAdviceIntoGiftCardScam() {
        for (text in listOf("Never ask customers to buy gift cards and send their codes.",
            "Do not ever buy gift cards and send the codes to strangers.")) {
            assertFalse(ThreatEngine().analyse(text).signals.any { it.id == "gift-card-request" })
        }
        assertTrue(ThreatEngine().analyse("Please buy gift cards and send their codes to me.").signals.any { it.id == "gift-card-request" })
    }
    @Test fun removedNotificationCanBeAnalysedAgainWithoutForgettingOthers() {
        val dedup = NotificationDeduplicator()
        dedup.duplicate("removed", "same"); dedup.duplicate("kept", "same")
        dedup.forget("removed")
        assertFalse(dedup.duplicate("removed", "same"))
        assertTrue(dedup.duplicate("kept", "same"))
    }
}
