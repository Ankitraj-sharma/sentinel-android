package com.phishshield.sentinel.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.phishshield.sentinel.ScanState
import com.phishshield.sentinel.engine.AnalysisResult
import com.phishshield.sentinel.engine.RiskLevel
import com.phishshield.sentinel.engine.ThreatEngine

@Composable
fun ScanScreen(state: ScanState, onInput: (String) -> Unit, onAnalyse: () -> Unit, onClear: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("SENTINEL", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Text("Analyse a message", style = MaterialTheme.typography.headlineLarge)
        Text("Paste English message text or an HTTP(S) URL. Analysis runs on this device.")
        OutlinedTextField(value = state.input, onValueChange = onInput,
            label = { Text("Message or URL") }, modifier = Modifier.fillMaxWidth(),
            minLines = 3, maxLines = 6, isError = state.error != null,
            supportingText = { Text(state.error ?: "${state.input.length} / ${ThreatEngine.MAX_INPUT} characters · Not saved to history") })
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { keyboard?.hide(); onAnalyse() }, enabled = state.input.isNotBlank() && !state.busy && state.error == null) {
                Text(if (state.busy) "Analysing…" else "Analyse locally")
            }
            TextButton(onClick = { keyboard?.hide(); onClear() }, enabled = state.input.isNotEmpty() || state.result != null) { Text("Clear") }
        }
        if (state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.result?.let { Result(it) }
        HorizontalDivider()
        Text("QR scanning, shared input and automatic notification protection will follow. This prototype uses local rules, not a trained ML model.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Result(result: AnalysisResult) {
    val color = when (result.level) {
        RiskLevel.HIGH -> MaterialTheme.colorScheme.error
        RiskLevel.REVIEW, RiskLevel.UNKNOWN -> MaterialTheme.colorScheme.secondary
        RiskLevel.NO_OBVIOUS_THREAT -> MaterialTheme.colorScheme.primary
    }
    HorizontalDivider()
    Text(result.level.label, style = MaterialTheme.typography.headlineSmall, color = color)
    result.ruleScore?.let { Text("Rule score: $it / 100 · Not a probability", style = MaterialTheme.typography.bodyMedium) }
    Text(result.nextAction, style = MaterialTheme.typography.bodyLarge)
    if (result.signals.isNotEmpty()) {
        Text("Evidence", style = MaterialTheme.typography.titleLarge)
        result.signals.forEach { signal ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(signal.title, style = MaterialTheme.typography.titleMedium)
                SelectionContainer { Text("“${signal.evidence}”", color = MaterialTheme.colorScheme.primary) }
                Text(signal.explanation)
            }
        }
    }
    if (result.urls.isNotEmpty()) {
        Text("Destinations", style = MaterialTheme.typography.titleLarge)
        result.urls.forEach { url ->
            SelectionContainer { Text(url.host ?: "Destination not parsed", style = MaterialTheme.typography.titleMedium) }
            SelectionContainer { Text(url.original, style = MaterialTheme.typography.bodySmall) }
            url.limitation?.let { Text(it) }
        }
    }
    Text("Coverage and limitations", style = MaterialTheme.typography.titleLarge)
    result.limitations.forEach { Text(it, style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant) }
}
