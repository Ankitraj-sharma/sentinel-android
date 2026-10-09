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
    val sms: Boolean = true,
)

enum class Setting { ONBOARDING, DARK_THEME, SAVE_HISTORY, WHATSAPP, GMAIL, SMS }

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
            sms = values[key(Setting.SMS)] ?: true,
        )
    }

    suspend fun set(setting: Setting, enabled: Boolean) {
        context.settingsStore.edit { it[key(setting)] = enabled }
    }
}
