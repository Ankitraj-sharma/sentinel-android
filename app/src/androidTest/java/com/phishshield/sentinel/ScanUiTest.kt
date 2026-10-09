package com.phishshield.sentinel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScanUiTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun realEngineShowsResultAndEditingClearsStaleVerdict() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Explore Sentinel").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("Protection not active").fetchSemanticsNodes().isNotEmpty()
        }
        if (compose.onAllNodesWithText("Explore Sentinel").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithText("Explore Sentinel").performScrollTo().performClick()
        }
        compose.onNodeWithText("Scan", useUnmergedTree = true).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Send OTP immediately https://paypal.com.evil.test")
        compose.onNodeWithText("Analyse locally").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("High risk").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("High risk").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Supported rules checked · Website and sender unverified").assertExists()
        compose.activity.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Analyse a message").assertIsDisplayed()
        compose.onNodeWithText("View last result").performScrollTo().performClick()
        compose.onNodeWithText("Edit message").performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("Your appointment is tomorrow.")
        compose.onNodeWithText("High risk").assertDoesNotExist()
        compose.onNodeWithText("Analyse locally").performScrollTo().performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("No obvious threat detected").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("No obvious threat detected").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Clear scan").performScrollTo().performClick()
        compose.onNodeWithText("No obvious threat detected").assertDoesNotExist()
        compose.onNodeWithText("0 / 20000 characters · Not saved to history").assertExists()
    }
}
