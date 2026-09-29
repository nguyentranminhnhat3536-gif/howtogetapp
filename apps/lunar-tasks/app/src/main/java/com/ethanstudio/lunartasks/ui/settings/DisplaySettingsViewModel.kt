package com.ethanstudio.lunartasks.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethanstudio.lunartasks.data.DisplaySettings
import com.ethanstudio.lunartasks.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DisplaySettingsViewModel(private val settings: SettingsRepository) : ViewModel() {
    val uiState: StateFlow<DisplaySettings> =
        settings.display.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DisplaySettings())

    fun setLargeText(value: Boolean) {
        viewModelScope.launch { settings.setLargeText(value) }
    }

    fun setHighContrast(value: Boolean) {
        viewModelScope.launch { settings.setHighContrast(value) }
    }
}
