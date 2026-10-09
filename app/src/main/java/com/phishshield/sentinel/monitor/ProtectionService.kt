package com.phishshield.sentinel.monitor

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.phishshield.sentinel.MainActivity
import com.phishshield.sentinel.R
import com.phishshield.sentinel.data.SettingsRepository
import com.phishshield.sentinel.data.Setting
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/** User-started, visible and stoppable protection. No inbox access or periodic message polling. */
class ProtectionService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Background protection", NotificationManager.IMPORTANCE_LOW))
        val open = PendingIntent.getActivity(this, ID, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val pause = PendingIntent.getService(this, ID, Intent(this, ProtectionService::class.java).setAction(PAUSE), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_sentinel)
            .setContentTitle("Sentinel protection active")
            .setContentText("Checking selected notification previews on this device.")
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(Notification.Action.Builder(null, "Pause protection", pause).build()).build()
        if (Build.VERSION.SDK_INT >= 34) startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        else startForeground(ID, notification)
        MonitorRuntime.update { it.copy(backgroundActive = true) }
        scope.launch {
            SettingsRepository(this@ProtectionService).settings.collect { settings ->
                if (!settings.monitoring || !settings.onboardingComplete) stopProtection()
            }
        }
        scope.launch {
            while (isActive) {
                MonitorRuntime.ensureConnected(this@ProtectionService)
                if (!MonitorRuntime.state.value.accessGranted) { stopProtection(); break }
                delay(30_000)
            }
        }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == PAUSE) scope.launch {
            SettingsRepository(this@ProtectionService).set(Setting.MONITORING, false)
            MonitorRuntime.clear(this@ProtectionService)
            stopProtection()
        }
        return START_STICKY
    }
    private fun stopProtection() { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    override fun onDestroy() {
        scope.cancel()
        MonitorRuntime.update { it.copy(backgroundActive = false) }
        super.onDestroy()
    }
    companion object {
        const val CHANNEL = "sentinel_background_protection"
        const val ID = 4101
        const val PAUSE = "com.phishshield.sentinel.PAUSE_PROTECTION"
        /** Call from a visible activity or its user action; never bypass Android background restrictions. */
        suspend fun syncFromVisibleActivity(context: Context) {
            val settings = SettingsRepository(context).settings.first()
            MonitorRuntime.refresh(context)
            if (settings.monitoring && settings.onboardingComplete && MonitorRuntime.state.value.accessGranted) {
                try { context.startForegroundService(Intent(context, ProtectionService::class.java)) }
                catch (_: RuntimeException) {
                    MonitorRuntime.update { it.copy(backgroundActive = false, lastEvent = "Background protection could not start. Check app battery settings, then reopen Sentinel.") }
                }
            } else context.stopService(Intent(context, ProtectionService::class.java))
        }
    }
}
