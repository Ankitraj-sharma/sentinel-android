package com.phishshield.sentinel.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException

private val Context.settingsStore by preferencesDataStore(name = "sentinel_settings")

data class UserSettings(
    val onboardingComplete: Boolean = false,
    val darkTheme: Boolean = true,
    val saveHistory: Boolean = false,
    val whatsapp: Boolean = true,
    val gmail: Boolean = true,
    val telegram: Boolean = false,
    val sms: Boolean = true,
    val monitoring: Boolean = false,
    val demoSource: Boolean = false,
)

enum class Setting { ONBOARDING, DARK_THEME, SAVE_HISTORY, WHATSAPP, GMAIL, TELEGRAM, SMS, MONITORING, DEMO_SOURCE }

class SettingsRepository(private val context: Context) {
    private fun key(setting: Setting) = booleanPreferencesKey(setting.name.lowercase())
    val settings = context.settingsStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { values ->
        UserSettings(
            onboardingComplete = values[key(Setting.ONBOARDING)] ?: false,
            darkTheme = values[key(Setting.DARK_THEME)] ?: true,
            saveHistory = values[key(Setting.SAVE_HISTORY)] ?: false,
            whatsapp = values[key(Setting.WHATSAPP)] ?: true,
            gmail = values[key(Setting.GMAIL)] ?: true,
            telegram = values[key(Setting.TELEGRAM)] ?: false,
            sms = values[key(Setting.SMS)] ?: true,
            monitoring = values[key(Setting.MONITORING)] ?: false,
            demoSource = values[key(Setting.DEMO_SOURCE)] ?: false,
        )
    }

    suspend fun set(setting: Setting, enabled: Boolean) {
        context.settingsStore.edit { it[key(setting)] = enabled }
    }
}
