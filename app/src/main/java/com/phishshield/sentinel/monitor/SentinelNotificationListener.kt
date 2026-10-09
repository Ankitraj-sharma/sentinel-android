package com.phishshield.sentinel.monitor

import android.app.Notification
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.phishshield.sentinel.BuildConfig
import com.phishshield.sentinel.data.SettingsRepository
import com.phishshield.sentinel.data.UserSettings
import com.phishshield.sentinel.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.UUID

class SentinelNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val queue = Channel<Preview>(16)
    private var settings: UserSettings? = null
    private var policy = NotificationPolicy(false, emptySet())
    private var generation = 0L
    private val deduplicator = NotificationDeduplicator()
    private val engine = ThreatEngine()
    private data class Preview(val generation: Long, val owner: String, val key: String,
        val text: NotificationText, val receivedAt: Long)

    override fun onCreate() {
        super.onCreate()
        AlertNotifier.createChannel(this)
        scope.launch {
            SettingsRepository(applicationContext).settings.collect { next ->
                settings = next
                refreshPolicy(next)
            }
        }
        scope.launch {
            for (preview in queue) {
                if (!allowed(preview)) continue
                val unavailable = preview.text.unavailable
                if (unavailable != null) {
                    MonitorRuntime.update { it.copy(skipped = it.skipped + 1, lastEvent = unavailable) }
                    continue
                }
                if (deduplicator.duplicate(preview.key, preview.text.text)) continue
                val result = try {
                    withContext(Dispatchers.Default) { engine.analyse(preview.text.text) }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    MonitorRuntime.update { it.copy(skipped = it.skipped + 1, lastEvent = "Analysis could not finish. No verdict produced.") }
                    continue
                }
                if (!allowed(preview)) continue
                val limited = result.copy(status = if (result.status == AnalysisStatus.NOT_ANALYSED) result.status else AnalysisStatus.LIMITED,
                    limitations = result.limitations + "Notification preview only: hidden, truncated, bundled or redacted content and the sender's identity cannot be verified.")
                MonitorRuntime.update { it.copy(scanned = it.scanned + 1, lastEvent = "Selected preview analysed locally · ${limited.level.label}") }
                if (limited.level == RiskLevel.HIGH || limited.level == RiskLevel.REVIEW) {
                    val source = when (preview.owner) {
                        "com.whatsapp", "com.whatsapp.w4b" -> "WhatsApp"
                        "com.google.android.gm" -> "Gmail"
                        "com.phishshield.sentinel.fixture" -> "Synthetic demo"
                        else -> "Default SMS app"
                    }
                    val detection = NotificationDetection(UUID.randomUUID().toString(), source, preview.text.text, limited, preview.receivedAt)
                    MonitorRuntime.update { it.copy(latest = detection) }
                    if (!AlertNotifier.post(this@SentinelNotificationListener, detection)) {
                        MonitorRuntime.update { it.copy(lastEvent = "Warning available in Protection · System alert delivery is disabled.") }
                    }
                }
            }
        }
    }

    private fun allowed(preview: Preview): Boolean {
        settings?.let(::refreshPolicy)
        return generation == preview.generation && MonitorRuntime.state.value.connected &&
            policy.accepts(preview.owner, packageName, false, false)
    }

    private fun drain() { while (queue.tryReceive().isSuccess) { } }
    private fun refreshPolicy(next: UserSettings) {
        val packages = buildSet {
            if (next.whatsapp) { add("com.whatsapp"); add("com.whatsapp.w4b") }
            if (next.gmail) add("com.google.android.gm")
            if (next.sms) Telephony.Sms.getDefaultSmsPackage(applicationContext)?.let(::add)
            if (BuildConfig.DEBUG && next.demoSource) add("com.phishshield.sentinel.fixture")
        }
        val updated = NotificationPolicy(next.monitoring && next.onboardingComplete, packages)
        if (updated != policy) {
            policy = updated; generation++; deduplicator.clear(); drain()
            MonitorRuntime.clear(this)
        }
        MonitorRuntime.update { it.copy(policyEnabled = updated.enabled) }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        generation++; deduplicator.clear(); drain()
        MonitorRuntime.update { it.copy(connected = true) }
        MonitorRuntime.refresh(this)
        // Do not replay pre-consent or previously active notifications.
    }
    override fun onListenerDisconnected() {
        generation++
        deduplicator.clear(); drain()
        MonitorRuntime.update { it.copy(connected = false) }
        MonitorRuntime.clear(this)
        super.onListenerDisconnected()
    }
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (settings == null || !MonitorRuntime.state.value.connected) return
        // Re-resolve default SMS owner before accepting any event; never inspect a previous default's fields.
        refreshPolicy(settings!!)
        val flags = sbn.notification.flags
        if (!policy.accepts(sbn.packageName, packageName, flags and Notification.FLAG_GROUP_SUMMARY != 0,
                flags and Notification.FLAG_ONGOING_EVENT != 0)) return
        val preview = Preview(generation, sbn.packageName, sbn.key, extractNotificationText(sbn.notification), System.currentTimeMillis())
        if (!queue.trySend(preview).isSuccess) MonitorRuntime.update {
            it.copy(skipped = it.skipped + 1, lastEvent = "Preview skipped: local processing queue is full.")
        }
    }
    override fun onDestroy() {
        scope.cancel(); queue.close()
        MonitorRuntime.update { it.copy(connected = false, policyEnabled = false) }
        MonitorRuntime.clear(this)
        super.onDestroy()
    }
}
