package com.phishshield.sentinel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.phishshield.sentinel.data.SettingsRepository
import com.phishshield.sentinel.data.Setting
import com.phishshield.sentinel.ui.SentinelApp
import com.phishshield.sentinel.ui.SentinelTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = SettingsRepository(applicationContext)
        setContent {
            val settings by repository.settings.collectAsStateWithLifecycle(initialValue = null)
            val scope = rememberCoroutineScope()
            var saveError by remember { mutableStateOf(false) }
            SentinelTheme(dark = settings?.darkTheme ?: true) {
                val loaded = settings
                if (loaded == null) {
                    Surface(Modifier.fillMaxSize()) {
                        Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }
                } else {
                    SentinelApp(loaded, saveError, onDismissError = { saveError = false }) { key, value ->
                        scope.launch {
                            try { repository.set(key, value) }
                            catch (_: java.io.IOException) { saveError = true }
                        }
                    }
                }
            }
        }
    }
}
