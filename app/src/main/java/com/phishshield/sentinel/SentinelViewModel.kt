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
import com.phishshield.sentinel.engine.*
import com.phishshield.sentinel.input.*
import android.net.Uri
import com.phishshield.sentinel.data.HistoryRepository
import kotlinx.coroutines.flow.combine
import com.phishshield.sentinel.monitor.MonitorRuntime

data class ScanState(val input: String = "", val result: AnalysisResult? = null,
    val busy: Boolean = false, val error: String? = null,
    val source: InputSource = InputSource.MANUAL, val importId: Long = 0, val historyNotice: String? = null)

class SentinelViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    private val historyRepository = HistoryRepository.get(application)
    val history = historyRepository.state
    val settings = combine(repository.settings, history) { settings, history -> settings.copy(saveHistory = history.enabled) }
    private var historyAction: Job? = null
    private val mutableSaveError = MutableStateFlow(false)
    val saveError = mutableSaveError.asStateFlow()
    private val mutableScan = MutableStateFlow(ScanState())
    val scan = mutableScan.asStateFlow()
    private var analysisJob: Job? = null
    private var importSequence = 0L

    fun importContent(imported: ImportedInput, source: InputSource) {
        analysisJob?.cancel()
        mutableScan.value = ScanState(input = imported.text, error = imported.error,
            source = source, importId = ++importSequence)
    }

    fun importQr(text: String) = importContent(validateImportedInput(text, InputSource.QR_CAMERA), InputSource.QR_CAMERA)

    fun importQrImage(uri: Uri) {
        analysisJob?.cancel()
        val id = ++importSequence
        mutableScan.value = ScanState(busy = true, source = InputSource.QR_IMAGE, importId = id)
        analysisJob = viewModelScope.launch {
            val imported = try {
                withContext(Dispatchers.IO) {
                    validateImportedInput(QrImageDecoder.read(getApplication<Application>().contentResolver, uri), InputSource.QR_IMAGE)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: QrImportException) { ImportedInput(error = failure.message) }
            catch (_: Exception) { ImportedInput(error = "Image could not be read. Choose it again or use the camera.") }
            mutableScan.value = ScanState(input = imported.text, error = imported.error,
                source = InputSource.QR_IMAGE, importId = id)
        }
    }

    fun updateInput(input: String) {
        analysisJob?.cancel()
        mutableScan.value = if (input.length > ThreatEngine.MAX_INPUT) {
            mutableScan.value.copy(result = null, busy = false,
                error = "Input exceeds ${ThreatEngine.MAX_INPUT} characters. The new content was not accepted or scanned.")
        } else ScanState(input = input, importId = mutableScan.value.importId)
    }

    fun analyse() {
        analysisJob?.cancel()
        val input = mutableScan.value.input
        if (mutableScan.value.error != null || mutableScan.value.busy) return
        val current = mutableScan.value
        val historyTicket = historyRepository.ticket()
        mutableScan.value = current.copy(busy = true, result = null)
        analysisJob = viewModelScope.launch {
            try {
                val engine = LocalThreatEngine.get(getApplication())
                val result = withContext(Dispatchers.Default) { engine.analyse(input) }
                mutableScan.value = current.copy(result = result, busy = false)
                val saved = historyRepository.record(historyTicket, HistorySource.valueOf(current.source.name), result)
                mutableScan.value = mutableScan.value.copy(historyNotice = when {
                    saved -> "Encrypted metadata saved. Message text and URLs were not saved."
                    history.value.error != null -> "Metadata could not be saved. Check History for recovery."
                    else -> "No metadata saved for this scan."
                })
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableScan.value = current.copy(error = "Analysis could not finish. Please try again.", busy = false)
            }
        }
    }

    fun clearScan() { analysisJob?.cancel(); mutableScan.value = ScanState(importId = mutableScan.value.importId) }

    fun set(key: Setting, enabled: Boolean) {
        if (key == Setting.SAVE_HISTORY) { setHistoryEnabled(enabled); return }
        viewModelScope.launch {
            try {
                repository.set(key, enabled)
                if (key == Setting.MONITORING && enabled) MonitorRuntime.ensureConnected(getApplication())
                mutableSaveError.value = false
            } catch (_: IOException) {
                mutableSaveError.value = true
            }
        }
    }

    fun dismissError() { mutableSaveError.value = false }
    private fun historyCommand(action: suspend () -> Unit) {
        if (historyAction?.isActive == true) return
        historyAction = viewModelScope.launch { action() }
    }
    fun setHistoryEnabled(enabled: Boolean) = historyCommand { historyRepository.setEnabled(enabled) }
    fun deleteHistory(id: String) = historyCommand { historyRepository.delete(id) }
    fun deleteAllHistory() = historyCommand { historyRepository.deleteAll() }
    fun refreshHistory() { viewModelScope.launch { historyRepository.refresh() } }
    fun pauseAndClearSession() {
        viewModelScope.launch {
            try {
                repository.set(Setting.MONITORING, false)
                MonitorRuntime.clear(getApplication())
                clearScan()
            } catch (_: IOException) { mutableSaveError.value = true }
        }
    }
}
