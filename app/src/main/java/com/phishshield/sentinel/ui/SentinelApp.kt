package com.phishshield.sentinel.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.Role
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import com.phishshield.sentinel.data.Setting
import com.phishshield.sentinel.data.UserSettings
import com.phishshield.sentinel.ScanState
import com.phishshield.sentinel.monitor.*
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import com.phishshield.sentinel.data.HistoryState

private enum class Destination(val label: String, val icon: ImageVector) {
    Protection("Protection", Icons.Outlined.Shield),
    Scan("Scan", Icons.Outlined.QrCodeScanner),
    History("History", Icons.Outlined.History),
    Settings("Settings", Icons.Outlined.Settings),
}

@Composable
fun SentinelApp(settings: UserSettings, saveError: Boolean = false,
    onDismissError: () -> Unit = {}, scan: ScanState = ScanState(),
    onInput: (String) -> Unit = {}, onAnalyse: () -> Unit = {}, onClear: () -> Unit = {},
    onQr: (String) -> Unit = {}, onQrImage: (Uri) -> Unit = {},
    history: HistoryState = HistoryState(loaded = true), onHistoryEnabled: (Boolean) -> Unit = {},
    onDeleteHistory: (String) -> Unit = {}, onDeleteAllHistory: () -> Unit = {},
    onRefreshHistory: () -> Unit = {}, onPauseClear: () -> Unit = {},
    monitor: MonitorState = MonitorState(), alertToken: String? = null, onAlertHandled: () -> Unit = {},
    onSetting: (Setting, Boolean) -> Unit) {
    if (!settings.onboardingComplete) {
        Welcome(saveError) { onSetting(Setting.ONBOARDING, true) }
        return
    }
    val nav = rememberNavController()
    val context = LocalContext.current
    var expiredAlert by remember { mutableStateOf(false) }
    LaunchedEffect(scan.importId) {
        if (scan.importId > 0) nav.navigate(Destination.Scan.name) {
            popUpTo(nav.graph.findStartDestination().id)
            launchSingleTop = true
        }
    }
    LaunchedEffect(alertToken) {
        if (alertToken != null) {
            if (monitor.latest?.token == alertToken) nav.navigate("Warning/$alertToken") { launchSingleTop = true }
            else { expiredAlert = true; nav.navigate(Destination.Protection.name) { launchSingleTop = true } }
            onAlertHandled()
        }
    }
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: Destination.Protection.name
    Scaffold(bottomBar = {
        NavigationBar {
            Destination.entries.forEach { destination ->
                NavigationBarItem(
                    selected = route == destination.name || route.startsWith("Warning/") && destination == Destination.Protection,
                    onClick = { nav.navigate(destination.name) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    } },
                    icon = { Icon(destination.icon, contentDescription = null) },
                    label = { Text(destination.label) },
                )
            }
        }
    }) { padding ->
        Column(Modifier.padding(padding)) {
            if (expiredAlert) {
                Text("That warning is no longer available. Notification results are held only in memory.", Modifier.padding(16.dp))
                TextButton(onClick = { expiredAlert = false }) { Text("Dismiss warning notice") }
            }
            if (saveError) {
                Text("Settings could not be saved. Please try again.", Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onDismissError) { Text("Dismiss") }
            }
            NavHost(nav, startDestination = Destination.Protection.name) {
                composable(Destination.Protection.name) {
                    ProtectionScreen(settings, monitor, onSetting) {
                        monitor.latest?.let { nav.navigate("Warning/${it.token}") { launchSingleTop = true } }
                    }
                }
                composable(Destination.Scan.name) { ScanScreen(scan, onInput, onAnalyse, onClear, onQr, onQrImage, history.enabled) }
                composable(Destination.History.name) { HistoryScreen(history, onHistoryEnabled, onDeleteHistory, onDeleteAllHistory, onRefreshHistory) }
                composable(Destination.Settings.name) { Settings(settings, onSetting, history, onHistoryEnabled, onPauseClear) }
                composable("Warning/{token}") { entry ->
                    val detection = monitor.latest?.takeIf { it.token == entry.arguments?.getString("token") }
                    if (detection != null) ResultScreen(detection.result, detection.text,
                        onEdit = { nav.popBackStack() },
                        onClear = { MonitorRuntime.clear(context); nav.popBackStack() }, notificationSource = detection.source,
                        historyNotice = "Full notification content stays in memory. If history was enabled when received, only metadata may be saved. Manage saved entries in History.")
                    else Page("Warning unavailable", "It was cleared, replaced or lost when the app process restarted.") {
                        Button(onClick = { nav.popBackStack() }) { Text("Back to protection") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Page(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text("SENTINEL", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary)
        Text(title, style = MaterialTheme.typography.headlineLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        content()
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Welcome(saveError: Boolean, onContinue: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.safeDrawingPadding().fillMaxSize().verticalScroll(rememberScrollState())
            .padding(28.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Spacer(Modifier.height(24.dp))
            Icon(Icons.Outlined.Shield, contentDescription = null,
                modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Sentinel", style = MaterialTheme.typography.displaySmall)
            Text("Understand suspicious messages.", style = MaterialTheme.typography.headlineMedium)
            Text("Designed for on-device scam analysis with clear explanations and access you control.",
                style = MaterialTheme.typography.bodyLarge)
            Info("Local protection", "Analyse messages and links, scan a web QR code, receive shared text, or opt in to supported notification preview analysis.")
            Text("Notification access is optional and set up separately. Selected previews are read locally; nothing is uploaded.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (saveError) Text("Could not save setup. Please try again.",
                color = MaterialTheme.colorScheme.error)
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Explore Sentinel") }
            Text("Team PhishShield · Code Carnival 3.0", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Settings(settings: UserSettings, onSetting: (Setting, Boolean) -> Unit,
    history: HistoryState, onHistoryEnabled: (Boolean) -> Unit, onPauseClear: () -> Unit) {
    Page("Settings", "Choose your preferences. They are saved on this device.") {
        Text("Appearance", style = MaterialTheme.typography.titleLarge)
        Preference("Dark theme", "Switch between dark and light appearance.", settings.darkTheme) {
            onSetting(Setting.DARK_THEME, it)
        }
        HorizontalDivider()
        Text("Notification sources", style = MaterialTheme.typography.titleLarge)
        Text("Enable monitoring and manage notification access in Protection. Coverage depends on available preview content.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Preference("WhatsApp", "Available notification text", settings.whatsapp) { onSetting(Setting.WHATSAPP, it) }
        Preference("Gmail", "Notification previews; inbox connection is separate", settings.gmail) { onSetting(Setting.GMAIL, it) }
        Preference("SMS", "Available notifications; no direct inbox access", settings.sms) { onSetting(Setting.SMS, it) }
        HorizontalDivider()
        Text("Privacy", style = MaterialTheme.typography.titleLarge)
        HistoryControl(history, onHistoryEnabled)
        history.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        OutlinedButton(onClick = onPauseClear, modifier = Modifier.fillMaxWidth()) { Text("Pause monitoring and clear session") }
        Text("Clears current scan and notification warning from memory. Saved metadata is deleted separately in History. Pausing does not revoke Android notification access.", style = MaterialTheme.typography.bodySmall)
        Info("No network access", "No internet permission, cloud analysis or analytics. Camera access is used only for QR scanning. A chosen QR image is read locally without saving the image. Preferences are saved locally. History is off by default; only opted-in metadata is encrypted and stored. Full scan text and the latest notification warning stay in memory.")
        Text("Sentinel ${com.phishshield.sentinel.BuildConfig.VERSION_NAME}\nTeam PhishShield", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Preference(title: String, summary: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch,
        onValueChange = onChange).semantics(mergeDescendants = true) { }
        .padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(summary, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun Info(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(body, style = MaterialTheme.typography.bodyLarge)
    }
}
