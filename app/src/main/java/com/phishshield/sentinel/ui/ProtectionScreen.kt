package com.phishshield.sentinel.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.phishshield.sentinel.BuildConfig
import com.phishshield.sentinel.data.Setting
import com.phishshield.sentinel.data.UserSettings
import com.phishshield.sentinel.monitor.*

@Composable
fun ProtectionScreen(settings: UserSettings, state: MonitorState,
    onSetting: (Setting, Boolean) -> Unit, onDetails: () -> Unit) {
    val context = LocalContext.current
    var setupError by remember { mutableStateOf<String?>(null) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        MonitorRuntime.refresh(context)
    }
    fun open(intent: Intent) {
        try { context.startActivity(intent) } catch (_: Exception) { setupError = "This device could not open that settings page. Use Android Settings to manage Sentinel permissions." }
    }
    val selected = settings.whatsapp || settings.gmail || settings.sms || BuildConfig.DEBUG && settings.demoSource
    val title = when {
        !settings.monitoring -> "Protection not active"
        !state.accessGranted -> "Notification access needed"
        !state.connected -> "Listener disconnected"
        !selected -> "Choose a source"
        else -> "Monitoring selected notifications"
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("SENTINEL", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text("Your protection", style = MaterialTheme.typography.headlineLarge)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(if (settings.monitoring) "Only newly delivered, selected notification previews are analysed locally. Hidden content and full chats/inboxes are unavailable."
                    else "Monitoring is paused. Manual analysis remains available in Scan.")
                Text("Notification access: ${if (state.accessGranted) "Granted" else "Not granted"}")
                Text("Listener: ${if (state.connected) "Connected" else "Disconnected"}")
                Text("System alerts: ${if (state.alertsEnabled) "Enabled" else "Disabled"} · Delivery can also be suppressed by Android settings or Do Not Disturb.")
            }
        }
        Text("Access and privacy", style = MaterialTheme.typography.titleLarge)
        Text("Android notification access can deliver notifications from all apps to Sentinel. It filters unselected sources before reading preview fields. Selected previews are read on this phone; nothing is uploaded. Pause stops analysis but does not revoke Android access.")
        Text("Android may hide, truncate or redact sensitive content, including OTPs on newer versions. A missing warning never proves safety.")
        Row(Modifier.fillMaxWidth().toggleable(settings.monitoring, role = Role.Switch,
            onValueChange = { onSetting(Setting.MONITORING, it) }).semantics(mergeDescendants = true) { }
            .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Enable local monitoring", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(settings.monitoring, onCheckedChange = null)
        }
        OutlinedButton(onClick = { open(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }, modifier = Modifier.fillMaxWidth()) {
            Text("Manage notification access")
        }
        OutlinedButton(onClick = {
            AlertNotifier.createChannel(context)
            if (Build.VERSION.SDK_INT >= 33 && !context.getSystemService(android.app.NotificationManager::class.java).areNotificationsEnabled())
                permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }, modifier = Modifier.fillMaxWidth()) { Text("Enable or manage alerts") }
        if (state.accessGranted && !state.connected) OutlinedButton(onClick = {
            NotificationListenerService.requestRebind(ComponentName(context, SentinelNotificationListener::class.java))
        }, modifier = Modifier.fillMaxWidth()) { Text("Reconnect listener") }
        setupError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("Sources", style = MaterialTheme.typography.titleLarge)
        Source("WhatsApp", "Personal and Business notification previews", settings.whatsapp) { onSetting(Setting.WHATSAPP, it) }
        Source("Gmail", "Notification previews, not inbox access", settings.gmail) { onSetting(Setting.GMAIL, it) }
        Source("SMS", "Current default SMS app notifications, not inbox access", settings.sms) { onSetting(Setting.SMS, it) }
        if (BuildConfig.DEBUG) Source("Synthetic demo source", "Separate fixture app; debug builds only", settings.demoSource) { onSetting(Setting.DEMO_SOURCE, it) }
        HorizontalDivider()
        Text("This session", style = MaterialTheme.typography.titleLarge)
        Text("Previews analysed: ${state.scanned} · Unassessed/skipped: ${state.skipped}")
        Text(state.lastEvent)
        state.latest?.let {
            Text("Latest warning · ${it.source} · ${it.result.level.label}", style = MaterialTheme.typography.titleMedium)
            Button(onClick = onDetails, modifier = Modifier.fillMaxWidth()) { Text("Review latest warning") }
            TextButton(onClick = { MonitorRuntime.clear(context) }, modifier = Modifier.fillMaxWidth()) { Text("Clear latest warning") }
        }
        Text("Only the latest warning is kept in memory. No notification history is saved. Policy changes, disconnection or process death clear it.",
            style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Source(title: String, summary: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange)
        .semantics(mergeDescendants = true) { }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.titleMedium); Text(summary) }
        Switch(checked, onCheckedChange = null)
    }
}
