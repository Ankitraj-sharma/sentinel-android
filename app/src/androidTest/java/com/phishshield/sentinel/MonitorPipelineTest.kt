package com.phishshield.sentinel

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.phishshield.sentinel.data.*
import com.phishshield.sentinel.engine.RiskLevel
import com.phishshield.sentinel.monitor.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Requires the separate fixture APK installed; real source analysis stays disabled throughout. */
@RunWith(AndroidJUnit4::class)
class MonitorPipelineTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val component = ComponentName(context, SentinelNotificationListener::class.java).flattenToString()
    private fun shell(command: String): String =
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { String(it.readBytes()) }
    private fun sourceVisible(): Boolean = shell("cmd notification list").contains("|${demoPackage(context)}|")
    private fun waitFor(message: String, condition: () -> Boolean) {
        val until = SystemClock.elapsedRealtime() + 15_000
        while (!condition() && SystemClock.elapsedRealtime() < until) Thread.sleep(100)
        val state = MonitorRuntime.state.value
        assertTrue("$message (connected=${state.connected}, policy=${state.policyEnabled}, scanned=${state.scanned}, skipped=${state.skipped}, event=${state.lastEvent})", condition())
    }
    private fun post(kind: String) {
        Thread.sleep(350) // Keep synthetic traffic below Android's notification update rate limits.
        shell("am start -W --activity-single-top -n ${demoPackage(context)}/com.phishshield.sentinel.fixture.FixtureActivity --es kind $kind")
    }
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)

    @Test fun realDeliveryRespectsConsentDedupPauseSelectionAndRevocation() = runBlocking {
        val repository = SettingsRepository(context)
        val history = HistoryRepository.get(context)
        history.awaitReady()
        assertFalse("Synthetic instrumentation requires history off initially", history.state.value.enabled)
        val before = repository.settings.first()
        val manager = context.getSystemService(NotificationManager::class.java)
        val accessBefore = if (android.os.Build.VERSION.SDK_INT >= 27) manager.isNotificationListenerAccessGranted(ComponentName(context, SentinelNotificationListener::class.java)) else false
        assertTrue("Grant app and fixture POST_NOTIFICATIONS with adb before this physical test", manager.areNotificationsEnabled())
        try {
            repository.set(Setting.MONITORING, false)
            repository.set(Setting.WHATSAPP, false); repository.set(Setting.GMAIL, false); repository.set(Setting.SMS, false)
            repository.set(Setting.ONBOARDING, true); repository.set(Setting.DEMO_SOURCE, true)
            assertTrue(history.setEnabled(true))
            shell("cmd notification allow_listener $component")
            waitFor("listener connected") { MonitorRuntime.state.value.connected }
            repository.set(Setting.MONITORING, true)
            waitFor("policy enabled") { MonitorRuntime.state.value.policyEnabled }
            shell("am start -W -n ${context.packageName}/com.phishshield.sentinel.MainActivity")
            waitFor("background protection service running") { MonitorRuntime.state.value.backgroundActive }
            shell("input keyevent KEYCODE_HOME")
            main {
                androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance()
                    .getActivitiesInStage(androidx.test.runner.lifecycle.Stage.STOPPED)
                    .filterIsInstance<MainActivity>().forEach { it.finishAndRemoveTask() }
            }
            assertTrue("closing the app task keeps protection running", MonitorRuntime.state.value.backgroundActive)
            post("scam")
            waitFor("high risk warning delivered") { MonitorRuntime.state.value.latest?.result?.level == RiskLevel.HIGH }
            val first = MonitorRuntime.state.value.latest!!
            waitFor("notification metadata saved with partial coverage") {
                history.state.value.entries.any { it.source == com.phishshield.sentinel.engine.HistorySource.SYNTHETIC_DEMO && it.status == com.phishshield.sentinel.engine.AnalysisStatus.LIMITED }
            }
            assertFalse(history.state.value.entries.toString().contains("paypal.com.evil.test"))
            val firstHistoryCount = history.state.value.entries.size
            val scanned = MonitorRuntime.state.value.scanned
            waitFor("system alert posted") { manager.activeNotifications.any { it.id == AlertNotifier.ALERT_ID } }
            waitFor("detected source notification removed after warning") { !sourceVisible() }
            assertTrue(manager.activeNotifications.any { it.id == ProtectionService.ID })
            val alert = manager.activeNotifications.first { it.id == AlertNotifier.ALERT_ID }.notification
            assertEquals("Potential scam detected", alert.extras.getCharSequence(android.app.Notification.EXTRA_TITLE).toString())
            assertFalse(alert.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString().contains("OTP"))
            assertNotNull(alert.contentIntent)
            // Actual tray tap is verified separately on-device; OEM System UI node access is not portable.
            post("scam"); Thread.sleep(600)
            assertEquals("identical update not rescanned", scanned, MonitorRuntime.state.value.scanned)
            assertEquals("identical update not saved twice", firstHistoryCount, history.state.value.entries.size)
            waitFor("identical repost replaced without rescanning") { !sourceVisible() }
            // Simulate a transient runtime delivery gate; the recovery posts a real OS alert.
            // AppOps denial does not change areNotificationsEnabled in this instrumented Vivo process.
            main {
                manager.cancel(AlertNotifier.ALERT_ID)
                MonitorRuntime.update { it.copy(connected = false) }
                assertFalse(AlertNotifier.post(context, first))
                MonitorRuntime.update { it.copy(connected = true) }
                MonitorRuntime.ensureConnected(context)
            }
            waitFor("pending warning posts after delivery gate recovers") {
                manager.activeNotifications.any { it.id == AlertNotifier.ALERT_ID } &&
                    MonitorRuntime.state.value.lastEvent == "Pending warning posted after delivery became available."
            }
            assertEquals(first.token, MonitorRuntime.state.value.latest?.token)
            main { manager.cancel(AlertNotifier.ALERT_ID); MonitorRuntime.ensureConnected(context) }
            assertFalse("successful recovery is not replayed", manager.activeNotifications.any { it.id == AlertNotifier.ALERT_ID })
            post("updated")
            waitFor("changed update reanalysed") { MonitorRuntime.state.value.latest?.token != first.token }
            assertNotNull(MonitorRuntime.state.value.latest)
            post("giftcard")
            waitFor("non-OTP scam produces automatic warning") {
                MonitorRuntime.state.value.latest?.result?.signals?.any { it.id == "gift-card-request" } == true
            }
            post("known-url")
            waitFor("bundled URL evidence reaches automatic warning") {
                MonitorRuntime.state.value.latest?.result?.signals?.any { it.id == "listed-url" } == true
            }
            val beforeLocal = MonitorRuntime.state.value.scanned
            val priorWarning = MonitorRuntime.state.value.latest?.token
            post("local-url")
            waitFor("legitimate local URL still analysed") { MonitorRuntime.state.value.scanned > beforeLocal }
            assertTrue("benign source notification retained", sourceVisible())
            assertEquals("weak URL traits do not issue a new warning", priorWarning, MonitorRuntime.state.value.latest?.token)
            assertTrue(MonitorRuntime.state.value.lastEvent.contains("No obvious threat"))
            post("benign")
            waitFor("benign preview scanned") { MonitorRuntime.state.value.lastEvent.contains("No obvious threat") }
            post("shortener")
            waitFor("short link inconclusive") { MonitorRuntime.state.value.lastEvent.contains("Unable to assess fully") }
            val skipped = MonitorRuntime.state.value.skipped
            post("hidden")
            waitFor("hidden body counted unassessed") { MonitorRuntime.state.value.skipped > skipped }
            history.setEnabled(false)
            val priorToken = MonitorRuntime.state.value.latest?.token
            post("updated")
            waitFor("notification still analysed with history off") { MonitorRuntime.state.value.latest?.token != priorToken }
            Thread.sleep(300)
            assertTrue("history off stores no metadata", history.state.value.entries.isEmpty())
            repository.set(Setting.MONITORING, false)
            waitFor("pause clears warning and disables policy") { !MonitorRuntime.state.value.policyEnabled && MonitorRuntime.state.value.latest == null }
            waitFor("pause stops background protection") { !MonitorRuntime.state.value.backgroundActive }
            assertFalse(manager.activeNotifications.any { it.id == AlertNotifier.ALERT_ID })
            val pausedCount = MonitorRuntime.state.value.scanned
            post("scam"); Thread.sleep(600)
            assertEquals(pausedCount, MonitorRuntime.state.value.scanned)
            repository.set(Setting.DEMO_SOURCE, false); repository.set(Setting.MONITORING, true)
            waitFor("monitoring resumed") { MonitorRuntime.state.value.policyEnabled }
            post("updated"); Thread.sleep(600)
            assertEquals("unselected publisher ignored", pausedCount, MonitorRuntime.state.value.scanned)
            repository.set(Setting.DEMO_SOURCE, true)
            Thread.sleep(300)
            post("scam")
            waitFor("source restored") { MonitorRuntime.state.value.latest != null }
            val beforeIgnored = MonitorRuntime.state.value.scanned
            post("group"); post("ongoing"); Thread.sleep(600)
            assertEquals("summary and ongoing ignored", beforeIgnored, MonitorRuntime.state.value.scanned)
            shell("cmd notification disallow_listener $component")
            waitFor("revoked connection clears warning") { !MonitorRuntime.state.value.connected && MonitorRuntime.state.value.latest == null }
            shell("cmd notification allow_listener $component")
            waitFor("listener reconnects") { MonitorRuntime.state.value.connected }
            post("scam")
            waitFor("same preview can warn again after reconnect") { MonitorRuntime.state.value.latest != null }
            main { MonitorRuntime.clear(context) }
            assertNull(MonitorRuntime.state.value.latest)
        } finally {
            history.setEnabled(false)
            repository.set(Setting.MONITORING, false)
            shell("cmd notification disallow_listener $component")
            repository.set(Setting.WHATSAPP, before.whatsapp); repository.set(Setting.GMAIL, before.gmail); repository.set(Setting.SMS, before.sms)
            repository.set(Setting.DEMO_SOURCE, before.demoSource); repository.set(Setting.ONBOARDING, before.onboardingComplete)
            repository.set(Setting.MONITORING, before.monitoring)
            if (accessBefore) shell("cmd notification allow_listener $component")
            main { MonitorRuntime.clear(context); MonitorRuntime.refresh(context) }
            shell("am force-stop ${demoPackage(context)}")
        }
    }
}
