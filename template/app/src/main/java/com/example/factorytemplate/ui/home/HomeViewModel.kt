package com.example.factorytemplate.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Trạng thái màn hình: một data class bất biến, UI chỉ đọc. */
data class HomeUiState(
    val taps: Int = 0,
)

/** ViewModel giữ trạng thái qua xoay màn hình; logic nằm ở đây, không nằm trong Composable. */
class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun onTap() {
        _uiState.update { it.copy(taps = it.taps + 1) }
    }
}
