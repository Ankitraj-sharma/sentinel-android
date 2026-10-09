package com.phishshield.sentinel

import android.app.Notification
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.phishshield.sentinel.monitor.extractNotificationText
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NotificationTextTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun defaultSmsOwnerMatchesSystemRoleWithoutInboxPermission() {
        if (android.os.Build.VERSION.SDK_INT < 29) return
        val ui = InstrumentationRegistry.getInstrumentation().uiAutomation
        val expected = android.os.ParcelFileDescriptor.AutoCloseInputStream(
            ui.executeShellCommand("cmd role get-role-holders android.app.role.SMS")
        ).use { it.bufferedReader().readText().trim().ifEmpty { null } }
        assertEquals(expected, android.provider.Telephony.Sms.getDefaultSmsPackage(context))
    }
    @Test fun bigTextAndMessagingPreviewsAreExtracted() {
        val expanded = Notification.Builder(context, "test").setContentTitle("Subject").setContentText("Short")
            .setStyle(Notification.BigTextStyle().bigText("Expanded warning text")).build()
        assertTrue(extractNotificationText(expanded).text.contains("Expanded warning text"))
        assertFalse(extractNotificationText(expanded).text.contains("Short"))
        val messaging = Notification.Builder(context, "test").setStyle(Notification.MessagingStyle("Demo")
            .addMessage("Send OTP immediately", 1L, "Demo sender")).build()
        assertTrue(extractNotificationText(messaging).text.contains("Send OTP immediately"))
    }
    @Test fun titleOnlyAndOversizedContentRemainUnassessed() {
        val hidden = Notification.Builder(context, "test").setContentTitle("New message").build()
        assertNotNull(extractNotificationText(hidden).unavailable)
        val oversized = Notification.Builder(context, "test").setContentTitle("x").build().apply {
            extras.putCharSequence(Notification.EXTRA_TEXT, "x".repeat(20_001))
        }
        val result = extractNotificationText(oversized)
        assertNotNull(result.unavailable)
        assertEquals("", result.text)
    }

    @Test fun messagingOnlyBundleIsReadWithoutFallbackText() {
        val messaging = Notification.Builder(context, "test").setStyle(Notification.MessagingStyle("Demo")
            .addMessage("Buy gift cards and send the codes", 1L, "Sender")).build().apply {
            extras.remove(Notification.EXTRA_TEXT)
            extras.remove(Notification.EXTRA_BIG_TEXT)
            extras.remove(Notification.EXTRA_TEXT_LINES)
        }
        val preview = extractNotificationText(messaging)
        assertNull(preview.unavailable)
        assertTrue(preview.text.contains("send the codes"))
    }
}
