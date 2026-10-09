package com.phishshield.sentinel

import android.content.Intent
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SharedInputUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun ready() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Explore Sentinel").fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithText("Protection not active").fetchSemanticsNodes().isNotEmpty() || compose.onAllNodesWithText("Local monitoring active").fetchSemanticsNodes().isNotEmpty() }
        if (compose.onAllNodesWithText("Explore Sentinel").fetchSemanticsNodes().isNotEmpty()) compose.onNodeWithText("Explore Sentinel").performScrollTo().performClick()
    }
    private fun share(text: String) {
        compose.activity.runOnUiThread { compose.activity.onNewIntent(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)) }
    }
    @Test fun warmShareRequiresReviewAndReplacesStaleVerdictWithoutRotationReplay() {
        ready()
        share("Send OTP immediately https://paypal.com.evil.test")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Shared text · Review before analysis").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("High risk").assertDoesNotExist()
        compose.onNodeWithText("Analyse locally").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("High risk").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("SHARED TEXT · ON DEVICE").assertExists()
        share("Your appointment is tomorrow.")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Shared text · Review before analysis").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("High risk").assertDoesNotExist()
        compose.onNode(hasSetTextAction()).assertTextContains("Your appointment is tomorrow.")
        compose.onNode(hasSetTextAction()).performTextReplacement("Edited content")
        compose.activityRule.scenario.recreate()
        compose.onNode(hasSetTextAction()).assertTextContains("Edited content")
    }
    @Test fun oversizedShareClearsPriorContentAndCannotBeAnalysed() {
        ready(); share("Initial synthetic text")
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Shared text · Review before analysis").fetchSemanticsNodes().isNotEmpty() }
        share("x".repeat(20_001))
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Content exceeds 20000 characters. Nothing was imported or scanned.").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Analyse locally").performScrollTo().assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).assertTextContains("")
    }
}
