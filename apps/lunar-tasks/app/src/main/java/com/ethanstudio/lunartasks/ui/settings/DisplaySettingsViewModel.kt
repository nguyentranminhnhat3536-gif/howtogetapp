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

    fun setLargeText(value: Boolean) {
        viewModelScope.launch { settings.setLargeText(value) }
    }

    fun setHighContrast(value: Boolean) {
        viewModelScope.launch { settings.setHighContrast(value) }
    }

    fun setContact(name: String, phone: String) {
        viewModelScope.launch { settings.setContact(name, phone) }
    }
}
