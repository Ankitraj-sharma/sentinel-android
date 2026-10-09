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

class SentinelViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SettingsRepository(application)
    val settings = repository.settings
    private val mutableSaveError = MutableStateFlow(false)
    val saveError = mutableSaveError.asStateFlow()

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
