package com.phishshield.sentinel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.phishshield.sentinel.data.Setting
import com.phishshield.sentinel.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import com.phishshield.sentinel.engine.AnalysisResult
import com.phishshield.sentinel.engine.ThreatEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

data class ScanState(val input: String = "", val result: AnalysisResult? = null,
    val busy: Boolean = false, val error: String? = null)

class SentinelViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    val settings = repository.settings
    private val mutableSaveError = MutableStateFlow(false)
    val saveError = mutableSaveError.asStateFlow()
    private val engine = ThreatEngine()
    private val mutableScan = MutableStateFlow(ScanState())
    val scan = mutableScan.asStateFlow()
    private var analysisJob: Job? = null

    fun updateInput(input: String) {
        analysisJob?.cancel()
        mutableScan.value = if (input.length > ThreatEngine.MAX_INPUT) {
            mutableScan.value.copy(result = null, busy = false,
                error = "Input exceeds ${ThreatEngine.MAX_INPUT} characters. The new content was not accepted or scanned.")
        } else ScanState(input = input)
    }

    fun analyse() {
        analysisJob?.cancel()
        val input = mutableScan.value.input
        mutableScan.value = ScanState(input = input, busy = true)
        analysisJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) { engine.analyse(input) }
                mutableScan.value = ScanState(input = input, result = result)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableScan.value = ScanState(input = input, error = "Analysis could not finish. Please try again.")
            }
        }
    }

    fun clearScan() { analysisJob?.cancel(); mutableScan.value = ScanState() }

    fun set(key: Setting, enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.set(key, enabled)
                mutableSaveError.value = false
            } catch (_: IOException) {
                mutableSaveError.value = true
            }
        }
    }

    fun dismissError() { mutableSaveError.value = false }
}
