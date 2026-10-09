package com.phishshield.sentinel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phishshield.sentinel.engine.ThreatEngine
import com.phishshield.sentinel.ui.ResultScreen
import com.phishshield.sentinel.ui.SentinelTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ResultUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun partialHighRiskShowsCoverageEvidenceHostAndExpandableGuidance() {
        val input = "Send OTP immediately https://bit.ly/demo"
        compose.setContent { SentinelTheme(true) { ResultScreen(ThreatEngine().analyse(input), input, {}, {}) } }
        compose.onNodeWithText("High risk").assertIsDisplayed()
        compose.onNodeWithText("Partial coverage · Some content remains unverified").assertIsDisplayed()
        compose.onNodeWithText("Verification checklist").performScrollTo().performClick()
        compose.onNodeWithText("1. Open the organisation's official app yourself, or use a contact you already know. Avoid contact details supplied by this message.").assertExists()
        compose.onNodeWithText("Send OTP", substring = false).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("bit.ly").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Analysed message").performScrollTo().performClick()
        compose.onNodeWithText(input).performScrollTo().assertIsDisplayed()
    }

    @Test fun rejectedInputShowsNoScoreAndClearActionWorks() {
        var cleared = false
        compose.setContent { SentinelTheme(false) {
            ResultScreen(ThreatEngine().analyse(""), "", {}, { cleared = true })
        } }
        compose.onNodeWithText("Not analysed · Correct the input and try again").assertIsDisplayed()
        compose.onAllNodesWithText("Rule score:", substring = true).assertCountEquals(0)
        compose.onNodeWithText("No rules were evaluated.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Clear scan").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(cleared) }
    }
}
