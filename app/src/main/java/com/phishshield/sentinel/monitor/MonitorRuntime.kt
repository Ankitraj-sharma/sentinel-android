package com.phishshield.sentinel.monitor

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.phishshield.sentinel.engine.AnalysisResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NotificationDetection(val token: String, val source: String, val text: String,
    val result: AnalysisResult, val receivedAt: Long)
data class MonitorState(val connected: Boolean = false, val accessGranted: Boolean = false,
    val alertsEnabled: Boolean = false, val scanned: Int = 0, val skipped: Int = 0,
    val lastEvent: String = "No selected notification received in this session.",
    val latest: NotificationDetection? = null)

/** One transient result, never a persistent inbox or history. Mutations run on the main thread. */
object MonitorRuntime {
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
}
