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

data class ScanState(val input: String = "", val result: AnalysisResult? = null,
    val busy: Boolean = false, val error: String? = null,
    val source: InputSource = InputSource.MANUAL, val importId: Long = 0)

class SentinelViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    val settings = repository.settings
    private val mutableSaveError = MutableStateFlow(false)
    val saveError = mutableSaveError.asStateFlow()
    private val engine = ThreatEngine()
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
        mutableScan.value = current.copy(busy = true, result = null)
        analysisJob = viewModelScope.launch {
            try {
                val result = withContext(Dispatchers.Default) { engine.analyse(input) }
                mutableScan.value = current.copy(result = result, busy = false)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                mutableScan.value = current.copy(error = "Analysis could not finish. Please try again.", busy = false)
            }
        }
    }

    fun clearScan() { analysisJob?.cancel(); mutableScan.value = ScanState(importId = mutableScan.value.importId) }

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
