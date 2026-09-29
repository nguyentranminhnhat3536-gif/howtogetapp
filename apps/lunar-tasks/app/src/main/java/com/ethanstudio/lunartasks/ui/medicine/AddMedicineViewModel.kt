package com.ethanstudio.lunartasks.ui.medicine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethanstudio.lunartasks.data.MedicinePlan
import com.ethanstudio.lunartasks.data.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

data class AddMedicineUiState(
    val name: String = "",
    val note: String = "",
    val minutes: Set<Int> = setOf(7 * 60),
    val finished: Boolean = false,
) {
    val canSave: Boolean get() = name.isNotBlank() && minutes.isNotEmpty()
}

class AddMedicineViewModel(private val repository: TaskRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AddMedicineUiState())
    val uiState: StateFlow<AddMedicineUiState> = _uiState.asStateFlow()

    fun setName(value: String) = _uiState.update { it.copy(name = value) }
    fun setNote(value: String) = _uiState.update { it.copy(note = value) }

    fun toggleMinute(minute: Int) = _uiState.update {
        it.copy(minutes = if (minute in it.minutes) it.minutes - minute else it.minutes + minute)
    }

    fun addMinute(minute: Int) = _uiState.update { it.copy(minutes = it.minutes + minute) }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val tasks = MedicinePlan.toTasks(state.name, state.note, state.minutes, LocalDate.now(), LocalTime.now())
        viewModelScope.launch {
            repository.saveAll(tasks)
            _uiState.update { it.copy(finished = true) }
        }
    }
}
