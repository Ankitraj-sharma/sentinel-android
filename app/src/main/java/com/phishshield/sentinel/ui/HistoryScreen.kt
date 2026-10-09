package com.phishshield.sentinel.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.phishshield.sentinel.data.HistoryState
import com.phishshield.sentinel.engine.*
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryControl(state: HistoryState, onEnabled: (Boolean) -> Unit) {
    var confirm by remember { mutableStateOf<Boolean?>(null) }
    Row(Modifier.fillMaxWidth().toggleable(state.enabled, enabled = state.loaded && !state.working && state.error == null,
        role = Role.Switch, onValueChange = { confirm = it }).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Save detection history", style = MaterialTheme.typography.titleMedium)
            Text(if (state.enabled) "On · Encrypted metadata only · 7 days / 100 entries" else "Off · No future scan metadata is saved",
                style = MaterialTheme.typography.bodyMedium)
        }
        Switch(state.enabled, onCheckedChange = null)
    }
    confirm?.let { enabled ->
        AlertDialog(onDismissRequest = { confirm = null },
            title = { Text(if (enabled) "Enable private history?" else "Turn off and delete history?") },
            text = { Text(if (enabled) "Save the time, source, risk level, score and matched rule names for new analyses. Message text, OTPs, URLs and images are never saved. Metadata is encrypted on this device, kept for up to 7 days and capped at 100 entries. Existing scans are not imported."
                else "Delete all saved metadata and its encryption key, and stop future saves. Current scan content is separate; use Clear scan to remove it from memory.") },
            confirmButton = { TextButton(onClick = { confirm = null; onEnabled(enabled) }) { Text(if (enabled) "Enable history" else "Turn off and delete") } },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text("Cancel") } })
    }
}

@Composable
fun HistoryScreen(state: HistoryState, onEnabled: (Boolean) -> Unit, onDelete: (String) -> Unit,
    onDeleteAll: () -> Unit, onRefresh: () -> Unit) {
    var deleteId by rememberSaveable { mutableStateOf<String?>(null) }
    var clearAll by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { onRefresh() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("SENTINEL", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(12.dp))
            Text("Detection history", style = MaterialTheme.typography.headlineLarge)
            Text("Private metadata, with deletion you control.")
        }
        item { HistoryControl(state, onEnabled) }
        if (!state.loaded || state.working) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        state.error?.let { error -> item {
            Text(error, color = MaterialTheme.colorScheme.error)
            Button(onClick = { clearAll = true }, enabled = !state.working) { Text("Reset local history") }
        } }
        item {
            Text("${state.entries.size} / 100 entries · Expires after 7 days", style = MaterialTheme.typography.titleMedium)
            Text("Messages, destinations and original evidence are not stored. A history entry cannot reproduce the full scan. No obvious threat does not prove safety.", style = MaterialTheme.typography.bodySmall)
        }
        if (state.entries.isEmpty() && state.loaded) item {
            Text("No detections yet", style = MaterialTheme.typography.titleLarge)
            Text(if (state.enabled) "New completed analyses can save metadata. Rejected input is not recorded." else "Enable history to save metadata from new analyses. Existing scans stay in memory only.")
        }
        if (state.entries.isNotEmpty()) item {
            OutlinedButton(onClick = { clearAll = true }, enabled = !state.working, modifier = Modifier.fillMaxWidth()) { Text("Delete all history") }
        }
        items(state.entries, key = { it.id }) { entry ->
            var expanded by rememberSaveable(entry.id) { mutableStateOf(false) }
            val time = remember(entry.timeMillis) { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.timeMillis)) }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(entry.level.label, style = MaterialTheme.typography.titleLarge,
                        color = if (entry.level == RiskLevel.HIGH) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    Text("${entry.source.label} · $time")
                    Text(if (entry.status == AnalysisStatus.LIMITED) "Partial coverage · Original content unavailable" else "Supported rules checked · Website and sender unverified", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Hide saved metadata" else "View saved metadata") }
                    if (expanded) {
                        entry.score?.let { Text("Rule score: $it / 100 · Not a probability") }
                        Text("Rule set: prototype v1")
                        if (entry.rules.isEmpty()) Text("No configured warning rules matched.")
                        entry.rules.forEach { Text("• ${it.label}") }
                        Text("Matched message excerpts and full result details were not saved.", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { deleteId = entry.id }, enabled = !state.working,
                        modifier = Modifier.semantics { contentDescription = "Delete ${entry.source.label} entry at $time" }) { Text("Delete entry") }
                }
            }
        }
    }
    deleteId?.let { id -> AlertDialog(onDismissRequest = { deleteId = null }, title = { Text("Delete this entry?") },
        text = { Text("Remove this saved metadata. Other entries and current scan content stay available.") },
        confirmButton = { TextButton(onClick = { deleteId = null; onDelete(id) }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleteId = null }) { Text("Cancel") } }) }
    if (clearAll) AlertDialog(onDismissRequest = { clearAll = false }, title = { Text(if (state.error == null) "Delete all history?" else "Reset local history?") },
        text = { Text(if (state.error == null) "Remove all saved metadata. History stays enabled if it is on; future analyses may create new entries."
            else "Delete the local encrypted history and its key. History will be off until you enable it again.") },
        confirmButton = { TextButton(onClick = { clearAll = false; if (state.error == null) onDeleteAll() else onEnabled(false) }) { Text("Delete all") } },
        dismissButton = { TextButton(onClick = { clearAll = false }) { Text("Cancel") } })
}
