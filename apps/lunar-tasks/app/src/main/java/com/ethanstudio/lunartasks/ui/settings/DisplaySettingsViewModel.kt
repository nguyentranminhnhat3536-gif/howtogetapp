package com.ethanstudio.lunartasks.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethanstudio.lunartasks.data.AppSettings
import com.ethanstudio.lunartasks.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DisplaySettingsViewModel(private val settings: SettingsRepository) : ViewModel() {
    val uiState: StateFlow<AppSettings> =
        settings.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    /** step = +1 (A+) hoặc -1 (A−). */
    fun changeTextSize(step: Int) {
        val next = AppSettings.nextScale(uiState.value.textScale, step)
        viewModelScope.launch { settings.setTextScale(next) }
    }

    fun setSpeakReminders(value: Boolean) {
        viewModelScope.launch { settings.setSpeakReminders(value) }
    }

    fun setHaptics(value: Boolean) {
        viewModelScope.launch { settings.setHaptics(value) }
    }

    fun setHighContrast(value: Boolean) {
        viewModelScope.launch { settings.setHighContrast(value) }
    }

    fun setContact(name: String, phone: String) {
        viewModelScope.launch { settings.setContact(name, phone) }
    }
}
