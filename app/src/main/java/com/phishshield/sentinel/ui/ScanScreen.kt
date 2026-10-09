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
import com.phishshield.sentinel.engine.InputSource
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.phishshield.sentinel.input.QrCaptureActivity

@Composable
fun ScanScreen(state: ScanState, onInput: (String) -> Unit, onAnalyse: () -> Unit, onClear: () -> Unit,
    onQr: (String) -> Unit = {}, onQrImage: (Uri) -> Unit = {}) {
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    var cameraDenied by rememberSaveable { mutableStateOf(false) }
    val camera = rememberLauncherForActivityResult(ScanContract()) { result ->
        if (result.contents != null) { notice = null; onQr(result.contents) }
        else notice = "QR scan ended without a readable code. Your existing input was kept."
    }
    val options = remember { ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        .setCaptureActivity(QrCaptureActivity::class.java).setOrientationLocked(false)
        .setBeepEnabled(false).setBarcodeImageEnabled(false)
        .setPrompt("Scan one web QR code. Destination will be reviewed in Sentinel; nothing opens automatically.") }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraDenied = !granted
        if (granted) camera.launch(options)
        else notice = "Camera access denied. Choose a QR image or paste the destination instead."
    }
    val image = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { notice = null; onQrImage(uri) }
        else notice = "Image selection cancelled. Your existing input was kept."
    }
    var showingResult by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.importId) { if (state.importId > 0) { showingResult = false; notice = null } }
    val result = state.result
    if (showingResult && result != null) {
        BackHandler { showingResult = false }
        ResultScreen(result, state.input, onEdit = { showingResult = false },
            onClear = { showingResult = false; onClear() }, inputSource = state.source.label.uppercase())
        return
    }
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("SENTINEL", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        Text("Analyse a message", style = MaterialTheme.typography.headlineLarge)
        Text("Paste English message text or an HTTP(S) URL. Analysis runs on this device.")
        OutlinedButton(onClick = {
            keyboard?.hide()
            if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                notice = "No camera available. Choose a QR image or paste the destination."
            } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) camera.launch(options)
            else permission.launch(Manifest.permission.CAMERA)
        }, modifier = Modifier.fillMaxWidth(), enabled = !state.busy) { Text("Scan QR with camera") }
        OutlinedButton(onClick = { keyboard?.hide(); image.launch(arrayOf("image/png", "image/jpeg", "image/webp")) },
            modifier = Modifier.fillMaxWidth(), enabled = !state.busy) { Text("Choose QR image") }
        if (cameraDenied) TextButton(onClick = {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
        }) { Text("Manage camera permission") }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (state.source != InputSource.MANUAL) {
            Text("${state.source.label} · Review before analysis", style = MaterialTheme.typography.titleMedium)
            Text("Nothing has been opened, sent or saved to history. Check the received content, then analyse locally.")
        }
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
        if (state.busy) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
            Text(if (state.source == InputSource.QR_IMAGE && state.input.isEmpty()) "Reading QR image…" else "Analysing locally…")
        }
        HorizontalDivider()
        Text("Share plain text or a link from another app and choose Sentinel. QR scanning supports HTTP(S) destinations only; payment, Wi-Fi and app commands are not assessed. No links open automatically. Optional notification protection is configured in Protection. This prototype uses local rules, not a trained ML model.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

