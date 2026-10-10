package com.phishshield.sentinel

import androidx.test.platform.app.InstrumentationRegistry
import com.phishshield.sentinel.data.Setting
import com.phishshield.sentinel.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class TelegramSettingsTest {
    @Test fun telegramConsentPersistsIndependentlyOfExistingSources() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        check(context.packageName.endsWith(".telegramqa")) { "Run only in isolated telegramqa package" }
        val repository = SettingsRepository(context)
        val before = repository.settings.first()
        try {
            assertFalse(before.telegram)
            repository.set(Setting.TELEGRAM, true)
            val enabled = SettingsRepository(context).settings.first()
            assertTrue(enabled.telegram)
            assertEquals(before.gmail, enabled.gmail)
            assertEquals(before.whatsapp, enabled.whatsapp)
            assertEquals(before.sms, enabled.sms)
            assertEquals(before.monitoring, enabled.monitoring)
            repository.set(Setting.TELEGRAM, false)
            assertFalse(repository.settings.first().telegram)
        } finally { repository.set(Setting.TELEGRAM, before.telegram) }
    }
}
