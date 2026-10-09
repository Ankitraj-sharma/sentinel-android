package com.phishshield.sentinel

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
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
    private fun shell(command: String) {
        ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use { it.readBytes() }
    }
    private fun waitFor(message: String, condition: () -> Boolean) {
        val until = SystemClock.elapsedRealtime() + 15_000
        while (!condition() && SystemClock.elapsedRealtime() < until) Thread.sleep(100)
        assertTrue(message, condition())
    }
    private fun post(kind: String) = shell("am start -W -n com.phishshield.sentinel.fixture/.FixtureActivity --es kind $kind")
    private fun main(action: () -> Unit) = instrumentation.runOnMainSync(action)

    @Test fun realDeliveryRespectsConsentDedupPauseSelectionAndRevocation() = runBlocking {
        val repository = SettingsRepository(context)
        val before = repository.settings.first()
        val manager = context.getSystemService(NotificationManager::class.java)
        val accessBefore = if (android.os.Build.VERSION.SDK_INT >= 27) manager.isNotificationListenerAccessGranted(ComponentName(context, SentinelNotificationListener::class.java)) else false
        assertTrue("Grant app and fixture POST_NOTIFICATIONS with adb before this physical test", manager.areNotificationsEnabled())
        try {
            repository.set(Setting.MONITORING, false)
            repository.set(Setting.WHATSAPP, false); repository.set(Setting.GMAIL, false); repository.set(Setting.SMS, false)
            repository.set(Setting.ONBOARDING, true); repository.set(Setting.DEMO_SOURCE, true)
            shell("cmd notification allow_listener $component")
            waitFor("listener connected") { MonitorRuntime.state.value.connected }
            repository.set(Setting.MONITORING, true)
            waitFor("policy enabled") { MonitorRuntime.state.value.policyEnabled }
            post("scam")
            waitFor("high risk warning delivered") { MonitorRuntime.state.value.latest?.result?.level == RiskLevel.HIGH }
            val first = MonitorRuntime.state.value.latest!!
            val scanned = MonitorRuntime.state.value.scanned
            waitFor("system alert posted") { manager.activeNotifications.any { it.id == AlertNotifier.ALERT_ID } }
            val alert = manager.activeNotifications.first { it.id == AlertNotifier.ALERT_ID }.notification
            assertFalse(alert.extras.getCharSequence(android.app.Notification.EXTRA_TEXT).toString().contains("OTP"))
            // Tap the actual System UI notification; direct background send() can be blocked by OEM policy.
            shell("cmd statusbar expand-notifications")
            waitFor("warning visible in system notification drawer") {
                instrumentation.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("Sentinel · High risk")?.isNotEmpty() == true
            }
            var node: AccessibilityNodeInfo? = instrumentation.uiAutomation.rootInActiveWindow
                .findAccessibilityNodeInfosByText("Sentinel · High risk").first()
            var clicked = false
            while (node != null && !clicked) {
                clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                node = node.parent
            }
            assertTrue("system notification clicked", clicked)
            waitFor("alert tap opens notification evidence") {
                instrumentation.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText("NOTIFICATION · Synthetic demo")?.isNotEmpty() == true
            }
            post("scam"); Thread.sleep(600)
            assertEquals("identical update not rescanned", scanned, MonitorRuntime.state.value.scanned)
            post("updated")
            waitFor("changed update reanalysed") { MonitorRuntime.state.value.latest?.token != first.token }
            assertNotNull(MonitorRuntime.state.value.latest)
            post("benign")
            waitFor("benign preview scanned") { MonitorRuntime.state.value.lastEvent.contains("No obvious threat") }
            post("shortener")
            waitFor("short link inconclusive") { MonitorRuntime.state.value.lastEvent.contains("Unable to assess fully") }
            val skipped = MonitorRuntime.state.value.skipped
            post("hidden")
            waitFor("hidden body counted unassessed") { MonitorRuntime.state.value.skipped > skipped }
            repository.set(Setting.MONITORING, false)
            waitFor("pause clears warning and disables policy") { !MonitorRuntime.state.value.policyEnabled && MonitorRuntime.state.value.latest == null }
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
            repository.set(Setting.MONITORING, false)
            shell("cmd notification disallow_listener $component")
            repository.set(Setting.WHATSAPP, before.whatsapp); repository.set(Setting.GMAIL, before.gmail); repository.set(Setting.SMS, before.sms)
            repository.set(Setting.DEMO_SOURCE, before.demoSource); repository.set(Setting.ONBOARDING, before.onboardingComplete)
            repository.set(Setting.MONITORING, before.monitoring)
            if (accessBefore) shell("cmd notification allow_listener $component")
            main { MonitorRuntime.clear(context); MonitorRuntime.refresh(context) }
            shell("am force-stop com.phishshield.sentinel.fixture")
        }
    }
}
