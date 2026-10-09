package com.phishshield.sentinel

import android.os.Bundle
import android.content.Intent
import com.phishshield.sentinel.monitor.AlertNotifier
import com.phishshield.sentinel.monitor.MonitorRuntime
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import com.phishshield.sentinel.input.readSharedInput
import com.phishshield.sentinel.engine.InputSource
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phishshield.sentinel.ui.SentinelApp
import com.phishshield.sentinel.ui.SentinelTheme

class MainActivity : ComponentActivity() {
    private fun readAlertToken(intent: Intent): String? = try { intent.getStringExtra(AlertNotifier.TOKEN_EXTRA) } catch (_: RuntimeException) { null }
    private val model: SentinelViewModel by viewModels()
    private var alertToken by mutableStateOf<String?>(null)
    override fun onResume() { super.onResume(); MonitorRuntime.refresh(this); model.refreshHistory() }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent)
        alertToken = readAlertToken(intent)
        readSharedInput(intent)?.let { model.importContent(it, InputSource.SHARED_TEXT) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        alertToken = readAlertToken(intent)
        if (savedInstanceState == null) readSharedInput(intent)?.let { model.importContent(it, InputSource.SHARED_TEXT) }
        enableEdgeToEdge()
        setContent {
            val settings by model.settings.collectAsStateWithLifecycle(initialValue = null)
            val saveError by model.saveError.collectAsStateWithLifecycle()
            val scan by model.scan.collectAsStateWithLifecycle()
            val monitor by MonitorRuntime.state.collectAsStateWithLifecycle()
            val history by model.history.collectAsStateWithLifecycle()
            SideEffect {
                val dark = settings?.darkTheme ?: true
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT) { dark },
                )
            }
            SentinelTheme(dark = settings?.darkTheme ?: true) {
                val loaded = settings
                if (loaded == null) {
                    Surface(Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                } else {
                    SentinelApp(loaded, saveError, onDismissError = model::dismissError,
                        scan = scan, onInput = model::updateInput, onAnalyse = model::analyse,
                        onClear = model::clearScan, onSetting = model::set, monitor = monitor,
                        onQr = model::importQr, onQrImage = model::importQrImage,
                        history = history, onHistoryEnabled = model::setHistoryEnabled,
                        onDeleteHistory = model::deleteHistory, onDeleteAllHistory = model::deleteAllHistory,
                        onRefreshHistory = model::refreshHistory, onPauseClear = model::pauseAndClearSession,
                        alertToken = alertToken, onAlertHandled = { alertToken = null; intent.removeExtra(AlertNotifier.TOKEN_EXTRA) })
                }
            }
        }
    }
}
