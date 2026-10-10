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
    fun sourceRemoved(key: String) {
        val detection = MonitorRuntime.state.value.latest ?: return
        // Only undelivered warnings expire. Our own replacement must retain its posted assessment.
        if (detection.origin?.key == key && pendingToken == detection.token) {
            pendingToken = null
            MonitorRuntime.update { it.copy(latest = null, lastEvent = "Source notification removed before warning delivery.") }
        }
    }
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
            .setContentTitle(if (detection.result.level == com.phishshield.sentinel.engine.RiskLevel.HIGH)
                "Potential scam detected" else "Suspicious message detected")
            .setContentText("${detection.source} · Avoid links, payments or sharing personal details. Tap to review.")
            .setStyle(Notification.BigTextStyle().bigText("${detection.source} · This notification contains signs of a possible scam. Avoid opening links, making payments or sharing personal details until you verify the sender. Tap to review Sentinel’s assessment."))
            .setCategory(Notification.CATEGORY_ERROR)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(Notification.Builder(context, CHANNEL_ID).setSmallIcon(R.drawable.ic_sentinel)
                .setContentTitle("Sentinel security alert").setContentText("A suspicious message was detected. Unlock to review.").build())
            .setContentIntent(pending).setAutoCancel(true).build()
        return try {
            context.getSystemService(NotificationManager::class.java).notify(ALERT_ID, notification)
            pendingToken = null
            SentinelNotificationListener.dismissAfterAlert(detection)
            true
        } catch (_: SecurityException) { pendingToken = detection.token; false }
    }
}
