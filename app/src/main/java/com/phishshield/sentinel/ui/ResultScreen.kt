package com.phishshield.sentinel.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phishshield.sentinel.engine.*

@Composable
fun ResultScreen(result: AnalysisResult, input: String, onEdit: () -> Unit, onClear: () -> Unit,
    notificationSource: String? = null, inputSource: String? = null, historyNotice: String? = null) {
    val explanation = remember(result) { result.explain() }
    val color = when (result.level) {
        RiskLevel.HIGH -> MaterialTheme.colorScheme.error
        RiskLevel.REVIEW, RiskLevel.UNKNOWN -> MaterialTheme.colorScheme.secondary
        RiskLevel.NO_OBVIOUS_THREAT -> MaterialTheme.colorScheme.primary
    }
    val icon = when (result.level) {
        RiskLevel.HIGH, RiskLevel.REVIEW -> Icons.Outlined.WarningAmber
        RiskLevel.UNKNOWN -> Icons.Outlined.HelpOutline
        RiskLevel.NO_OBVIOUS_THREAT -> Icons.Outlined.Search
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(notificationSource?.let { "NOTIFICATION · $it" } ?: inputSource?.let { "$it · ON DEVICE" } ?: "SCAN RESULT · ON DEVICE", style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(40.dp))
                Text(result.level.label, style = MaterialTheme.typography.headlineMedium, color = color,
                    modifier = Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite })
                Text(explanation.summary)
                Text(explanation.coverage, style = MaterialTheme.typography.labelLarge)
                result.ruleScore?.let { Text("Rule score: $it / 100 · Not a probability") }
            }
        }
        OutlinedButton(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text(if (notificationSource == null) "Edit message" else "Back to protection") }
        SectionTitle("What to do next")
        Text(result.nextAction, style = MaterialTheme.typography.bodyLarge)
        Expandable("Verification checklist") {
            explanation.verificationSteps.forEachIndexed { index, step -> Text("${index + 1}. $step") }
        }
        SectionTitle("Why this result")
        Text(if (result.signals.isEmpty()) {
            if (result.status == AnalysisStatus.NOT_ANALYSED) "No rules were evaluated."
            else "No configured warning signals matched. Scams can still use wording or destinations these rules do not recognise."
        } else "${result.signals.size} distinct warning rules contributed. Repeated matches do not add extra points.")
        result.signals.forEach { signal ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(signal.title, style = MaterialTheme.typography.titleMedium)
                    Text("Rule contribution: ${signal.weight} points", style = MaterialTheme.typography.labelMedium)
                    Text("Matched evidence", style = MaterialTheme.typography.labelLarge)
                    SelectionContainer { Text(visibleEvidence(signal.evidence), color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyLarge) }
                    Text(signal.explanation)
                }
            }
        }
        Expandable("How rule points work") { Text(explanation.scoreMeaning) }
        SectionTitle("Link destinations")
        if (result.urls.isEmpty()) Text("No supported link was extracted. This does not verify the sender or message.")
        result.urls.forEachIndexed { index, url ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Link ${index + 1} · Parsed host", style = MaterialTheme.typography.labelLarge)
                    SelectionContainer { Text(url.host ?: "Destination not parsed",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                    Text("The host is the destination, not the brand name shown elsewhere in a link.")
                    Text("Submitted link", style = MaterialTheme.typography.labelLarge)
                    SelectionContainer { Text(visibleEvidence(url.original)) }
                    Text(url.limitation ?: "Host parsed locally. Website content, reputation and sender remain unverified.")
                }
            }
        }
        SectionTitle("Coverage and limitations")
        result.limitations.forEach { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Expandable("Analysed message") {
            Text("Highlights show exact evidence matches only. Normalised evidence can differ from the submitted text. Invisible controls are displayed as U+ codes.",
                style = MaterialTheme.typography.bodySmall)
            val raw = remember(input) { visibleEvidence(input) }
            val highlight = MaterialTheme.colorScheme.secondaryContainer
            val foreground = MaterialTheme.colorScheme.onSecondaryContainer
            val annotated = remember(raw, result, highlight, foreground) {
                buildAnnotatedString {
                    append(raw)
                    result.signals.map { visibleEvidence(it.evidence) }.filter { it.isNotEmpty() }.distinct().forEach { evidence ->
                        var start = raw.indexOf(evidence)
                        while (start >= 0) {
                            addStyle(SpanStyle(background = highlight, color = foreground, fontWeight = FontWeight.Bold),
                                start, start + evidence.length)
                            start = raw.indexOf(evidence, start + evidence.length)
                        }
                    }
                }
            }
            SelectionContainer { Text(annotated) }
        }
        Text(historyNotice ?: "Full scan content is held in memory. Clear removes the current content; any opted-in metadata is managed separately in History.",
            style = MaterialTheme.typography.bodySmall)
        OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) { Text(if (notificationSource == null) "Clear scan" else "Clear warning") }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

@Composable
private fun Expandable(title: String, content: @Composable ColumnScope.() -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    OutlinedCard(Modifier.fillMaxWidth()) {
        TextButton(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth().semantics {
            stateDescription = if (expanded) "Expanded" else "Collapsed"
        }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, modifier = Modifier.weight(1f))
                Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
            }
        }
        if (expanded) Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
