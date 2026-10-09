package com.phishshield.sentinel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phishshield.sentinel.data.HistoryRepository
import com.phishshield.sentinel.engine.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HistoryUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    @Test fun explicitConsentPersistenceAndDeletionControlsWorkWithoutSavingEvidence() {
        val repository = HistoryRepository.get(compose.activity)
        runBlocking { repository.awaitReady(); repository.setEnabled(false) }
        try {
            compose.waitUntil(10_000) { compose.onAllNodesWithText("Explore Sentinel").fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithText("Protection not active").fetchSemanticsNodes().isNotEmpty() }
            if (compose.onAllNodesWithText("Explore Sentinel").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithText("Explore Sentinel").performScrollTo().performClick()
            compose.onNodeWithText("History", useUnmergedTree = true).performClick()
            compose.onNodeWithText("Save detection history").performClick()
            compose.onNodeWithText("Cancel").performClick()
            assertFalse(repository.state.value.enabled)
            compose.onNodeWithText("Save detection history").performClick()
            compose.onNodeWithText("Enable history").performClick()
            compose.waitUntil(10_000) { repository.state.value.enabled }
            compose.onNodeWithText("Scan", useUnmergedTree = true).performClick()
            compose.onNode(hasSetTextAction()).performTextInput("Send OTP 123456 immediately https://paypal.com.evil.test/private")
            compose.onNodeWithText("Analyse locally").performScrollTo().performClick()
            compose.waitUntil(10_000) { repository.state.value.entries.size == 1 }
            compose.onNodeWithText("High risk").assertExists()
            compose.onNodeWithText("Clear scan").performScrollTo().performClick()
            compose.onNodeWithText("History", useUnmergedTree = true).performClick()
            compose.onNodeWithText("1 / 100 entries · 7-day retention").assertExists()
            compose.activityRule.scenario.recreate()
            compose.onNodeWithText("1 / 100 entries · 7-day retention").assertExists()
            compose.onNodeWithText("View saved metadata").performScrollTo().performClick()
            compose.onNodeWithText("Request to disclose a secret").performScrollTo().assertExists()
            compose.onAllNodesWithText("123456", substring = true).assertCountEquals(0)
            compose.onAllNodesWithText("evil.test", substring = true).assertCountEquals(0)
            compose.onNodeWithText("Delete entry").performScrollTo().performClick()
            compose.onNodeWithText("Cancel").performClick()
            assertEquals(1, repository.state.value.entries.size)
            compose.onNodeWithText("Delete entry").performScrollTo().performClick()
            compose.onNodeWithText("Delete", substring = false).performClick()
            compose.waitUntil(10_000) { repository.state.value.entries.isEmpty() }
            runBlocking { repository.record(repository.ticket(), HistorySource.QR_IMAGE, ThreatEngine().analyse("https://paypal.com@evil.test")) }
            compose.onNodeWithText("Delete all history").performScrollTo().performClick()
            compose.onNodeWithText("Delete all", substring = false).performClick()
            compose.waitUntil(10_000) { repository.state.value.entries.isEmpty() }
            assertTrue(repository.state.value.enabled)
            compose.onNodeWithText("Save detection history").performScrollTo().performClick()
            compose.onNodeWithText("Turn off and delete").performClick()
            compose.waitUntil(10_000) { !repository.state.value.enabled }
            compose.onNodeWithText("No detections yet").assertExists()
        } finally { runBlocking { repository.setEnabled(false) } }
    }
}
