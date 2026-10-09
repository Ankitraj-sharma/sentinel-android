package com.phishshield.sentinel.monitor

import android.app.Notification
import android.os.Build
import com.phishshield.sentinel.engine.ThreatEngine

data class NotificationText(val text: String, val unavailable: String? = null)

fun extractNotificationText(notification: Notification): NotificationText {
    val pieces = linkedSetOf<String>()
    var size = 0
    var hasBody = false
    fun add(value: CharSequence?, body: Boolean = true) {
        if (value == null || value.isBlank()) return
        if (value.length > ThreatEngine.MAX_INPUT || size + value.length + 1 > ThreatEngine.MAX_INPUT)
            throw IllegalArgumentException("Oversized preview")
        val text = value.toString()
        if (body) hasBody = true
        if (pieces.add(text)) size += text.length + 1
    }
    return try {
        val extras = notification.extras
        add(extras.getCharSequence(Notification.EXTRA_TITLE), body = false)
        // Prefer expanded text to its shortened duplicate. MessagingStyle is read separately.
        add(extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))
        val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (lines != null) {
            if (lines.size > 64) throw IllegalArgumentException("Too many preview lines")
            lines.forEach { add(it) }
        }
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        if (messages != null) {
            if (messages.size > 64) throw IllegalArgumentException("Too many preview messages")
            if (Build.VERSION.SDK_INT >= 30) {
                Notification.MessagingStyle.Message.getMessagesFromBundleArray(messages).forEach { add(it.text) }
            }
            // Older Android falls back to public text/big-text/text-lines fields.
        }
        val text = pieces.joinToString("\n")
        if (!hasBody || text.isBlank()) NotificationText("", "No readable preview body was provided. Hidden content was not assessed.")
        else NotificationText(text)
    } catch (_: Exception) {
        NotificationText("", "Preview could not be read within local input limits. Nothing was analysed.")
    }
}
