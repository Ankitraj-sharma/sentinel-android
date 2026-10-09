package com.phishshield.sentinel.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.phishshield.sentinel.MainActivity
import com.phishshield.sentinel.R

object AlertNotifier {
    private var pendingToken: String? = null
    fun retryPending(context: Context) {
        val current = MonitorRuntime.state.value
        val detection = current.latest ?: return
        if (pendingToken == detection.token && current.alertsEnabled && current.accessGranted &&
            current.connected && current.policyEnabled && post(context, detection)) {
            MonitorRuntime.update { it.copy(lastEvent = "Pending warning posted after delivery became available.") }
        }
    }
    const val CHANNEL_ID = "sentinel_risk_alerts"
    const val ALERT_ID = 4100
    const val TOKEN_EXTRA = "notification_detection_token"
    fun createChannel(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Scam warning alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Local rule warnings for selected notification previews"
                lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            })
    }
    fun post(context: Context, detection: NotificationDetection): Boolean {
        createChannel(context)
        MonitorRuntime.refresh(context)
        val current = MonitorRuntime.state.value
        if (!current.alertsEnabled || !current.accessGranted || !current.connected ||
            !current.policyEnabled || current.latest?.token != detection.token) {
            if (current.latest?.token == detection.token) pendingToken = detection.token
            return false
        }
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            .putExtra(TOKEN_EXTRA, detection.token)
        val pending = PendingIntent.getActivity(context, ALERT_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_sentinel)
            .setContentTitle("Sentinel · ${detection.result.level.label}")
            .setContentText("A selected notification matched warning rules. Tap to review on this device.")
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(Notification.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_sentinel)
                .setContentTitle("Sentinel warning").setContentText("Unlock to review.").build())
            .setContentIntent(pending).setAutoCancel(true).build()
        return try {
            context.getSystemService(NotificationManager::class.java).notify(ALERT_ID, notification)
            pendingToken = null
            true
        } catch (_: SecurityException) { pendingToken = detection.token; false }
    }
}
