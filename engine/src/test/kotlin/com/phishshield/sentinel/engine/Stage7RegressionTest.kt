package com.phishshield.sentinel.engine

import org.junit.Assert.*
import org.junit.Test

class Stage7RegressionTest {
    @Test fun taskWithdrawalPaymentsDifferFromNormalPayrollUpdates() {
        assertTrue(ThreatEngine().analyse("Complete tasks then pay a deposit to unlock your earnings.").signals.any { it.id == "task-earnings-deposit" })
        assertFalse(ThreatEngine().analyse("Your salary and commission will arrive on Friday.").signals.any { it.id == "task-earnings-deposit" })
    }
    @Test fun taskWithdrawalAndJobDepositRulesDoNotCollide() {
        val message = "Work from home job: pay the registration fee to start. Complete tasks then pay a deposit to unlock your earnings."
        val result = ThreatEngine().analyse(message)
        val ids = result.signals.map { it.id }.toSet()
        assertTrue(ids.contains("job-deposit"))
        assertTrue(ids.contains("task-earnings-deposit"))
        assertEquals(80, result.ruleScore)
        val metadata = HistoryPolicy.metadata("test", System.currentTimeMillis(), HistorySource.MANUAL, result)!!
        assertTrue(metadata.rules.contains(HistoryRule.JOB_DEPOSIT))
        assertTrue(metadata.rules.contains(HistoryRule.TASK_EARNINGS))
    }
    @Test fun legitimateSbiOnlineBankingDomainIsNotFlaggedAsImpersonation() {
        val result = ThreatEngine().analyse("Log in to your account at https://www.onlinesbi.com/personal/")
        assertFalse(result.signals.any { it.id == "brand-domain-mismatch" })
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
