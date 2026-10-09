package com.phishshield.sentinel.monitor

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import com.phishshield.sentinel.engine.AnalysisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Resolve from the installed package rather than an inlined build-variant constant. */
fun demoPackage(context: Context): String = "com.phishshield.sentinel.fixture" +
    context.packageName.removePrefix("com.phishshield.sentinel")

data class NotificationDetection(val token: String, val source: String, val text: String,
    val result: AnalysisResult, val receivedAt: Long)
data class MonitorState(val connected: Boolean = false, val accessGranted: Boolean = false,
    val policyEnabled: Boolean = false,
    val alertsEnabled: Boolean = false, val scanned: Int = 0, val skipped: Int = 0,
    val lastEvent: String = "No selected notification received in this session.",
    val latest: NotificationDetection? = null)

/** One transient result, never a persistent inbox or history. Mutations run on the main thread. */
object MonitorRuntime {
    private var lastRebindAt = -30_000L
    private val mutable = MutableStateFlow(MonitorState())
    val state = mutable.asStateFlow()
    fun update(change: (MonitorState) -> MonitorState) { mutable.value = change(mutable.value) }
    fun clear(context: Context) {
        update { it.copy(latest = null) }
        context.getSystemService(NotificationManager::class.java).cancel(AlertNotifier.ALERT_ID)
    }
    fun refresh(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val component = ComponentName(context, SentinelNotificationListener::class.java)
        val access = if (Build.VERSION.SDK_INT >= 27) manager.isNotificationListenerAccessGranted(component)
            else Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
                ?.split(':')?.any { ComponentName.unflattenFromString(it) == component } == true
        val channel = manager.getNotificationChannel(AlertNotifier.CHANNEL_ID)
        update { it.copy(accessGranted = access,
            connected = it.connected && access,
            alertsEnabled = manager.areNotificationsEnabled() && (channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE)) }
        if (!access) clear(context)
    }

    /** Recover an already-authorised listener; never grants notification access. */
    fun ensureConnected(context: Context) {
        refresh(context)
        AlertNotifier.retryPending(context)
        val now = SystemClock.elapsedRealtime()
        if (!state.value.accessGranted || state.value.connected || now - lastRebindAt < 30_000L) return
        lastRebindAt = now
        try {
            NotificationListenerService.requestRebind(ComponentName(context, SentinelNotificationListener::class.java))
        } catch (_: RuntimeException) {
            update { it.copy(lastEvent = "Listener reconnect failed. Open notification access settings and reconnect.") }
        }
    }
}
