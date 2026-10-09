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
    onSetting: (Setting, Boolean) -> Unit) {
    if (!settings.onboardingComplete) {
        Welcome(saveError) { onSetting(Setting.ONBOARDING, true) }
        return
    }
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route ?: Destination.Protection.name
    Scaffold(bottomBar = {
        NavigationBar {
            Destination.entries.forEach { destination ->
                NavigationBarItem(
                    selected = route == destination.name,
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
            if (saveError) {
                Text("Settings could not be saved. Please try again.", Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onDismissError) { Text("Dismiss") }
            }
            NavHost(nav, startDestination = Destination.Protection.name) {
                composable(Destination.Protection.name) {
                    Protection { nav.navigate(Destination.Settings.name) { launchSingleTop = true } }
                }
                composable(Destination.Scan.name) { ScanScreen(scan, onInput, onAnalyse, onClear) }
                composable(Destination.History.name) { History(settings.saveHistory) }
                composable(Destination.Settings.name) { Settings(settings, onSetting) }
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
            Info("Local analysis preview", "Manually analyse messages and links with explained local rules. Automatic monitoring and QR scanning are not available yet.")
            Text("No message access is requested in this build. Settings stay on this device.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (saveError) Text("Could not save setup. Please try again.",
                color = MaterialTheme.colorScheme.error)
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) { Text("Explore Sentinel") }
            Text("Team PhishShield · Code Carnival 3.0", style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun Protection(onSettings: () -> Unit) {
    Page("Your protection", "On-device analysis. Evidence you can understand.") {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Shield, contentDescription = null, Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.primary)
                Text("Protection not active", style = MaterialTheme.typography.headlineSmall)
                Text("Local message and URL analysis is available in Scan. Automatic notification monitoring is not active in this build.")
                OutlinedButton(onClick = onSettings) { Text("Configure preferences") }
            }
        }
        Text("How protection will work", style = MaterialTheme.typography.titleLarge)
        Info("01 · Detect", "Analyse available text and links from supported notifications after you grant access.")
        Info("02 · Explain", "Show suspicious signals and the evidence behind a warning.")
        Info("03 · Act", "Help you verify a request before opening a link or sharing information.")
        HorizontalDivider()
        Text("Notification access will not provide complete access to private chats or inboxes.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun History(enabled: Boolean) {
    Page("Detection history", "A private record, when you choose to keep one.") {
        Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary)
        Text("No detections yet", style = MaterialTheme.typography.titleLarge)
        Text("Manual scans are not saved in this build. No messages or scan results have been stored in history.")
        Info(if (enabled) "History preference enabled" else "History preference disabled",
            "Your preference is saved. Detection history and deletion controls will be available when scanning is enabled.")
    }
}

@Composable
private fun Settings(settings: UserSettings, onSetting: (Setting, Boolean) -> Unit) {
    Page("Settings", "Choose your preferences. They are saved on this device.") {
        Text("Appearance", style = MaterialTheme.typography.titleLarge)
        Preference("Dark theme", "Switch between dark and light appearance.", settings.darkTheme) {
            onSetting(Setting.DARK_THEME, it)
        }
        HorizontalDivider()
        Text("Future notification sources", style = MaterialTheme.typography.titleLarge)
        Text("These selections do not grant access or activate monitoring. Source coverage depends on available notification content.",
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Preference("WhatsApp", "Available notification text", settings.whatsapp) { onSetting(Setting.WHATSAPP, it) }
        Preference("Gmail", "Notification previews; inbox connection is separate", settings.gmail) { onSetting(Setting.GMAIL, it) }
        Preference("SMS", "Available notifications; no direct inbox access", settings.sms) { onSetting(Setting.SMS, it) }
        HorizontalDivider()
        Text("Privacy", style = MaterialTheme.typography.titleLarge)
        Preference("Save detection history", "Off by default. Applies when history is implemented.", settings.saveHistory) {
            onSetting(Setting.SAVE_HISTORY, it)
        }
        Info("No network access", "This build has no internet permission, cloud analysis or analytics. Only your preferences are saved locally.")
        Text("Sentinel 0.3.0 · Explained local analysis\nTeam PhishShield", style = MaterialTheme.typography.bodySmall,
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
