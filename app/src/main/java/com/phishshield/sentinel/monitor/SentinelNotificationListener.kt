package com.phishshield.sentinel.monitor

import android.app.Notification
import android.provider.Telephony
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.phishshield.sentinel.BuildConfig
import com.phishshield.sentinel.LocalThreatEngine
import com.phishshield.sentinel.data.SettingsRepository
import com.phishshield.sentinel.data.UserSettings
import com.phishshield.sentinel.data.HistoryRepository
import com.phishshield.sentinel.data.HistoryTicket
import com.phishshield.sentinel.engine.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.UUID
import java.lang.ref.WeakReference

class SentinelNotificationListener : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val queue = Channel<Preview>(16)
    private var settings: UserSettings? = null
    private var policy = NotificationPolicy(false, emptySet())
    private var generation = 0L
    private val deduplicator = NotificationDeduplicator()
    private val settingsReady = CompletableDeferred<Unit>()
    private var startingEvents = 0
    private var sequence = 0L
    private val revisions = LinkedHashMap<String, Long>()
    private data class Preview(val generation: Long, val owner: String, val key: String,
        val revision: Long, val text: NotificationText, val receivedAt: Long, val historyTicket: HistoryTicket?)

    override fun onCreate() {
        super.onCreate()
        active = WeakReference(this)
        AlertNotifier.createChannel(this)
        scope.launch {
            SettingsRepository(applicationContext).settings.collect { next ->
                settings = next
                refreshPolicy(next)
                settingsReady.complete(Unit)
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
                if (deduplicator.duplicate(preview.key, preview.text.text)) {
                    val previous = MonitorRuntime.state.value.latest
                    if (previous?.origin?.key == preview.key && previous.text == preview.text.text) {
                        val refreshed = previous.copy(origin = NotificationOrigin(preview.key, preview.owner, preview.revision))
                        MonitorRuntime.update { it.copy(latest = refreshed) }
                        AlertNotifier.post(this@SentinelNotificationListener, refreshed)
                    }
                    continue
                }
                val result = try {
                    val engine = LocalThreatEngine.get(applicationContext)
                    withContext(Dispatchers.Default) { engine.analyse(preview.text.text) }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    MonitorRuntime.update { it.copy(skipped = it.skipped + 1, lastEvent = "Analysis could not finish. No verdict produced.") }
                    continue
                }
                if (!allowed(preview)) continue
                val limited = result.copy(status = if (result.status == AnalysisStatus.NOT_ANALYSED) result.status else AnalysisStatus.LIMITED,
                    limitations = result.limitations + "Notification preview only: hidden, truncated, bundled or redacted content and the sender's identity cannot be verified." +
                        if (preview.text.visiblyCutOff) listOf("Visible preview cut-off: only the displayed fragment was analysed. Share the full message for more context.") else emptyList())
                MonitorRuntime.update { it.copy(scanned = it.scanned + 1, lastEvent = "Selected preview analysed locally · ${limited.level.label}") }
                val historySource = when (preview.owner) {
                    "com.whatsapp", "com.whatsapp.w4b" -> HistorySource.WHATSAPP
                    "com.google.android.gm" -> HistorySource.GMAIL
                    "org.telegram.messenger", "org.telegram.messenger.web" -> HistorySource.TELEGRAM
                    demoPackage(this@SentinelNotificationListener) -> HistorySource.SYNTHETIC_DEMO
                    else -> HistorySource.SMS
                }
                if (limited.level == RiskLevel.HIGH || limited.level == RiskLevel.REVIEW) {
                    val source = when (preview.owner) {
                        "com.whatsapp", "com.whatsapp.w4b" -> "WhatsApp"
                        "com.google.android.gm" -> "Gmail"
                        "org.telegram.messenger", "org.telegram.messenger.web" -> "Telegram"
                        demoPackage(this@SentinelNotificationListener) -> "Synthetic demo"
                        else -> "SMS"
                    }
                    val detection = NotificationDetection(UUID.randomUUID().toString(), source, preview.text.text, limited, preview.receivedAt,
                        NotificationOrigin(preview.key, preview.owner, preview.revision))
                    MonitorRuntime.update { it.copy(latest = detection) }
                    if (!AlertNotifier.post(this@SentinelNotificationListener, detection)) {
                        MonitorRuntime.update { it.copy(lastEvent = "Warning available in Protection · System alert delivery is disabled.") }
                    }
                }
                HistoryRepository.get(applicationContext).record(preview.historyTicket, historySource, limited)
            }
        }
    }

    private fun allowed(preview: Preview): Boolean {
        settings?.let(::refreshPolicy)
        return generation == preview.generation && revisions[preview.key] == preview.revision && MonitorRuntime.state.value.connected &&
            policy.accepts(preview.owner, packageName, false, false)
    }

    private fun drain() { while (queue.tryReceive().isSuccess) { }; revisions.clear() }
    private fun refreshPolicy(next: UserSettings) {
        val packages = buildSet {
            if (next.whatsapp) { add("com.whatsapp"); add("com.whatsapp.w4b") }
            if (next.gmail) add("com.google.android.gm")
            if (next.telegram) addAll(TelegramSources.packages)
            if (next.sms) Telephony.Sms.getDefaultSmsPackage(applicationContext)?.let(::add)
            if (BuildConfig.DEBUG && next.demoSource) add(demoPackage(this@SentinelNotificationListener))
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
        if (settings?.monitoring == true) scope.launch {
            delay(1_000)
            if (settings?.monitoring == true) MonitorRuntime.ensureConnected(this@SentinelNotificationListener)
        }
        super.onListenerDisconnected()
    }
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (settings == null) {
            if (startingEvents >= 16) return
            startingEvents++
            // Do not silently drop the first delivered event while DataStore initialises.
            val eventGeneration = generation
            scope.launch {
                try {
                    settingsReady.await()
                    if (generation == eventGeneration || generation == eventGeneration + 1) acceptNotification(sbn)
                } finally { startingEvents-- }
            }
            return
        }
        acceptNotification(sbn)
    }

    private fun acceptNotification(sbn: StatusBarNotification) {
        if (settings == null || !MonitorRuntime.state.value.connected) return
        // Re-resolve default SMS owner before accepting any event; never inspect a previous default's fields.
        refreshPolicy(settings!!)
        val flags = sbn.notification.flags
        if (!policy.accepts(sbn.packageName, packageName, flags and Notification.FLAG_GROUP_SUMMARY != 0,
                flags and Notification.FLAG_ONGOING_EVENT != 0)) return
        val revision = ++sequence
        revisions.remove(sbn.key); revisions[sbn.key] = revision
        if (revisions.size > 128) revisions.remove(revisions.keys.first())
        val preview = Preview(generation, sbn.packageName, sbn.key, revision, extractNotificationText(sbn.notification), System.currentTimeMillis(),
            HistoryRepository.get(applicationContext).ticket())
        if (!queue.trySend(preview).isSuccess) MonitorRuntime.update {
            it.copy(skipped = it.skipped + 1, lastEvent = "Preview skipped: local processing queue is full.")
        }
    }
    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Invalidate queued work for a removed event; a later repost deserves fresh analysis.
        revisions.remove(sbn.key)
        deduplicator.forget(sbn.key)
        AlertNotifier.sourceRemoved(sbn.key)
        super.onNotificationRemoved(sbn)
    }
    override fun onDestroy() {
        if (active?.get() === this) active = null
        scope.cancel(); queue.close()
        MonitorRuntime.update { it.copy(connected = false, policyEnabled = false) }
        MonitorRuntime.clear(this)
        super.onDestroy()
    }
    private fun dismissDetectedSource(detection: NotificationDetection) {
        val origin = detection.origin ?: return
        if (MonitorRuntime.state.value.latest?.token != detection.token || !MonitorRuntime.state.value.connected ||
            revisions[origin.key] != origin.revision || !policy.accepts(origin.owner, packageName, false, false)) return
        try { cancelNotification(origin.key) }
        catch (_: RuntimeException) {
            MonitorRuntime.update { it.copy(lastEvent = "Sentinel warning posted. Android could not dismiss the source notification.") }
        }
    }
    companion object {
        private var active: WeakReference<SentinelNotificationListener>? = null
        fun dismissAfterAlert(detection: NotificationDetection) { active?.get()?.dismissDetectedSource(detection) }
    }
}
