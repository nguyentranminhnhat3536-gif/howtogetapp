package com.ethanstudio.lunartasks.ui.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethanstudio.lunartasks.data.Repeat
import com.ethanstudio.lunartasks.data.Task
import com.ethanstudio.lunartasks.data.TaskRepository
import com.ethanstudio.lunartasks.lunar.LunarCalendar
import com.ethanstudio.lunartasks.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class EditTaskUiState(
    val loaded: Boolean = false,
    val isNew: Boolean = true,
    val title: String = "",
    val note: String = "",
    val date: LocalDate? = null,
    val minute: Int? = null,
    val repeat: Repeat = Repeat.NONE,
    val remind: Boolean = false,
    val important: Boolean = false,
    /** Ngày âm người dùng chọn trực tiếp (giữ ngày 30 kể cả khi tháng thiếu). */
    val pickedLunarDay: Int? = null,
    val pickedLunarMonth: Int? = null,
    val finished: Boolean = false,
) {
    val canSave: Boolean get() = title.isNotBlank()
}

class EditTaskViewModel(
    private val repository: TaskRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val taskId: Long = savedStateHandle.get<Long>(ARG_TASK_ID) ?: 0L
    private var original: Task? = null

    private val _uiState = MutableStateFlow(EditTaskUiState())
    val uiState: StateFlow<EditTaskUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val task = if (taskId != 0L) repository.get(taskId) else null
            original = task
            _uiState.value = if (task == null) {
                EditTaskUiState(loaded = true)
            } else {
                EditTaskUiState(
                    loaded = true,
                    isNew = false,
                    title = task.title,
                    note = task.note,
                    date = task.dueDate,
                    minute = task.dueMinute,
                    repeat = task.repeat,
                    remind = task.remind,
                    important = task.important,
                    pickedLunarDay = task.lunarDay,
                    pickedLunarMonth = task.lunarMonth,
                )
            }
        }
    }

    fun setTitle(value: String) = _uiState.update { it.copy(title = value) }
    fun setNote(value: String) = _uiState.update { it.copy(note = value) }
    fun setImportant(value: Boolean) = _uiState.update { it.copy(important = value) }

    fun setDate(date: LocalDate) = _uiState.update { it.copy(date = date, pickedLunarDay = null, pickedLunarMonth = null) }

    fun clearDate() = _uiState.update {
        it.copy(date = null, minute = null, repeat = Repeat.NONE, remind = false, pickedLunarDay = null, pickedLunarMonth = null)
    }

    fun setTime(minute: Int) = _uiState.update { it.copy(minute = minute, date = it.date ?: LocalDate.now()) }
    fun clearTime() = _uiState.update { it.copy(minute = null) }

    fun setRepeat(repeat: Repeat) = _uiState.update { it.copy(repeat = repeat, date = it.date ?: LocalDate.now()) }

    fun setRemind(remind: Boolean) = _uiState.update {
        it.copy(
            remind = remind,
            date = it.date ?: LocalDate.now(),
            minute = it.minute ?: if (remind) ReminderScheduler.DEFAULT_REMINDER_MINUTE else null,
        )
    }

    /** Chọn "mùng 1" hoặc "rằm": đặt ngày đến hạn là lần gần nhất (kể cả hôm nay). */
    fun pickLunarDayOfMonth(day: Int) {
        val date = LunarCalendar.nextLunarMonthly(LocalDate.now().minusDays(1), day)
        _uiState.update { it.copy(date = date, pickedLunarDay = day, pickedLunarMonth = null) }
    }

    /** Chọn một ngày âm lịch (ví dụ ngày giỗ): đặt ngày đến hạn là lần gần nhất (kể cả hôm nay). */
    fun pickLunarDate(day: Int, month: Int) {
        val date = LunarCalendar.nextLunarYearly(LocalDate.now().minusDays(1), day, month)
        _uiState.update { it.copy(date = date, pickedLunarDay = day, pickedLunarMonth = month) }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        val date = state.date ?: if (state.repeat != Repeat.NONE || state.remind) LocalDate.now() else null
        val lunar = date?.let(LunarCalendar::fromSolar)
        val base = original ?: Task(title = "")
        val task = base.copy(
            title = state.title.trim(),
            note = state.note.trim(),
            dueEpochDay = date?.toEpochDay(),
            dueMinute = state.minute,
            repeat = state.repeat,
            lunarDay = if (state.repeat.isLunar) state.pickedLunarDay ?: lunar?.day else null,
            lunarMonth = if (state.repeat == Repeat.LUNAR_YEARLY) state.pickedLunarMonth ?: lunar?.month else null,
            remind = state.remind && date != null,
            important = state.important,
        )
        viewModelScope.launch {
            repository.save(task)
            _uiState.update { it.copy(finished = true) }
        }
    }

    fun delete() {
        val task = original ?: return
        viewModelScope.launch {
            repository.delete(task)
            _uiState.update { it.copy(finished = true) }
        }
    }

    companion object {
        const val ARG_TASK_ID = "taskId"
    }
}
