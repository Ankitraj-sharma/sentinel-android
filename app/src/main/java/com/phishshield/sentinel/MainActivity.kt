package com.phishshield.sentinel

import android.os.Bundle
import androidx.activity.ComponentActivity
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: SentinelViewModel = viewModel()
            val settings by model.settings.collectAsStateWithLifecycle(initialValue = null)
            val saveError by model.saveError.collectAsStateWithLifecycle()
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
                        onSetting = model::set)
                }
            }
        }
    }
}
