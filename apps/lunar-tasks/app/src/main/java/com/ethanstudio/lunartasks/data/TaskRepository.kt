package com.ethanstudio.lunartasks.data

import com.ethanstudio.lunartasks.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Kết quả khi bấm hoàn thành: [previous] để hoàn tác, [nextDue] khác null nếu việc lặp đã chuyển sang lần sau. */
data class CompletionResult(val previous: Task, val nextDue: LocalDate?)

class TaskRepository(
    private val dao: TaskDao,
    private val scheduler: ReminderScheduler,
) {
    val tasks: Flow<List<Task>> = dao.observeAll()

    suspend fun get(id: Long): Task? = dao.get(id)

    suspend fun save(task: Task): Long {
        val id = if (task.id == 0L) dao.insert(task) else task.id.also { dao.update(task) }
        scheduler.schedule(task.copy(id = id))
        return id
    }

    suspend fun saveAll(tasks: List<Task>) {
        tasks.forEach { save(it) }
    }

    suspend fun delete(task: Task) {
        scheduler.cancel(task.id)
        dao.delete(task)
    }

    suspend fun toggleDone(task: Task, today: LocalDate): CompletionResult {
        val nextDue = if (!task.done) Recurrence.nextAfter(task, today) else null
        val updated = when {
            nextDue != null -> task.copy(dueEpochDay = nextDue.toEpochDay())
            task.done -> task.copy(done = false, completedAt = null)
            else -> task.copy(done = true, completedAt = System.currentTimeMillis())
        }
        save(updated)
        return CompletionResult(previous = task, nextDue = nextDue)
    }

    suspend fun rescheduleAll() {
        dao.pendingReminders().forEach { scheduler.schedule(it) }
    }
}
