package com.ethanstudio.lunartasks.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ethanstudio.lunartasks.data.CompletionResult
import com.ethanstudio.lunartasks.data.GroupedTasks
import com.ethanstudio.lunartasks.data.Task
import com.ethanstudio.lunartasks.data.TaskGrouping
import com.ethanstudio.lunartasks.data.TaskRepository
import com.ethanstudio.lunartasks.lunar.LunarCalendar
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TaskListUiState(
    val loading: Boolean = true,
    val today: LocalDate = LocalDate.now(),
    val groups: GroupedTasks = GroupedTasks(emptyList(), emptyList()),
    val showDone: Boolean = false,
    val nextFirstDay: LocalDate = today,
    val nextFullMoon: LocalDate = today,
) {
    val isEmpty: Boolean get() = groups.open.isEmpty() && groups.done.isEmpty()
}

class TaskListViewModel(private val repository: TaskRepository) : ViewModel() {
    private val today = MutableStateFlow(LocalDate.now())
    private val showDone = MutableStateFlow(false)
    private val events = Channel<CompletionResult>(Channel.BUFFERED)

    /** Mỗi lần hoàn thành một việc, UI hiện snackbar có nút Hoàn tác. */
    val completions: Flow<CompletionResult> = events.receiveAsFlow()

    val uiState: StateFlow<TaskListUiState> =
        combine(repository.tasks, today, showDone) { tasks, day, show ->
            TaskListUiState(
                loading = false,
                today = day,
                groups = TaskGrouping.group(tasks, day),
                showDone = show,
                nextFirstDay = LunarCalendar.nextLunarMonthly(day.minusDays(1), 1),
                nextFullMoon = LunarCalendar.nextLunarMonthly(day.minusDays(1), 15),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TaskListUiState())

    /** Gọi khi mở lại app, phòng trường hợp đã qua ngày mới. */
    fun refreshToday() {
        today.value = LocalDate.now()
    }

    fun toggleShowDone() = showDone.update { !it }

    fun toggleDone(task: Task) {
        viewModelScope.launch {
            val result = repository.toggleDone(task, LocalDate.now())
            if (!task.done) events.send(result)
        }
    }

    fun toggleImportant(task: Task) {
        viewModelScope.launch { repository.save(task.copy(important = !task.important)) }
    }

    fun undo(result: CompletionResult) {
        viewModelScope.launch { repository.save(result.previous) }
    }
}
