package com.phishshield.sentinel.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.phishshield.sentinel.ScanState
import com.phishshield.sentinel.engine.ThreatEngine

@Composable
fun ScanScreen(state: ScanState, onInput: (String) -> Unit, onAnalyse: () -> Unit, onClear: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    var showingResult by rememberSaveable { mutableStateOf(false) }
    val result = state.result
    if (showingResult && result != null) {
        BackHandler { showingResult = false }
        ResultScreen(result, state.input, onEdit = { showingResult = false },
            onClear = { showingResult = false; onClear() })
        return
    }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("SENTINEL", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Text("Analyse a message", style = MaterialTheme.typography.headlineLarge)
        Text("Paste English message text or an HTTP(S) URL. Analysis runs on this device.")
        OutlinedTextField(value = state.input, onValueChange = onInput,
            label = { Text("Message or URL") }, modifier = Modifier.fillMaxWidth(),
            minLines = 3, maxLines = 6, isError = state.error != null,
            supportingText = { Text(state.error ?: "${state.input.length} / ${ThreatEngine.MAX_INPUT} characters · Not saved to history") })
            Button(onClick = { keyboard?.hide(); showingResult = true; onAnalyse() }, modifier = Modifier.fillMaxWidth(), enabled = state.input.isNotBlank() && !state.busy && state.error == null) {
                Text(if (state.busy) "Analysing…" else "Analyse locally")
            }
        if (result != null) OutlinedButton(onClick = { showingResult = true }, modifier = Modifier.fillMaxWidth()) {
            Text("View last result")
        }
        TextButton(onClick = { keyboard?.hide(); onClear() }, modifier = Modifier.fillMaxWidth(), enabled = state.input.isNotEmpty() || state.result != null) { Text("Clear") }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        HorizontalDivider()
        Text("QR scanning and shared input will follow. Optional notification protection is configured in Protection. This prototype uses local rules, not a trained ML model.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

