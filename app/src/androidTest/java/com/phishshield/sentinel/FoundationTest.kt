package com.phishshield.sentinel

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.phishshield.sentinel.data.Setting
import com.phishshield.sentinel.data.SettingsRepository
import com.phishshield.sentinel.data.UserSettings
import com.phishshield.sentinel.ui.SentinelApp
import com.phishshield.sentinel.ui.SentinelTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoundationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun navigationDoesNotMisrepresentProtection() {
        compose.setContent {
            SentinelTheme(true) {
                SentinelApp(UserSettings(onboardingComplete = true)) { _, _ -> }
            }
        }
        compose.onNodeWithText("Protection not active").assertIsDisplayed()
        compose.onNodeWithText("Scan", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Analyse a message").assertIsDisplayed()
        compose.onNodeWithText("History", useUnmergedTree = true).performClick()
        compose.onNodeWithText("No detections yet").assertIsDisplayed()
        compose.onNodeWithText("Settings", useUnmergedTree = true).performClick()
        compose.onNodeWithText("Dark theme").assertIsDisplayed()
    }

    @Test fun preferencesSurviveRepositoryRecreation() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = SettingsRepository(context)
        val previous = first.settings.first().darkTheme
        try {
            first.set(Setting.DARK_THEME, false)
            assertFalse(SettingsRepository(context).settings.first().darkTheme)
        } finally {
            first.set(Setting.DARK_THEME, previous)
        }
    }
}
