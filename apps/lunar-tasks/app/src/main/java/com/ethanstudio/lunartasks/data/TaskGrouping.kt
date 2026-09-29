package com.ethanstudio.lunartasks.data

import java.time.LocalDate

enum class Section { OVERDUE, TODAY, TOMORROW, UPCOMING, NO_DATE }

data class GroupedTasks(
    val open: List<Pair<Section, List<Task>>>,
    val done: List<Task>,
)

/** Chia việc thành các nhóm Quá hạn / Hôm nay / Ngày mai / Sắp tới / Không có ngày và Đã xong. */
object TaskGrouping {
    private val openOrder = compareBy<Task>(
        { it.dueEpochDay ?: Long.MAX_VALUE },
        { !it.important },
        { it.dueMinute ?: Int.MAX_VALUE },
        { it.createdAt },
    )

    fun group(tasks: List<Task>, today: LocalDate): GroupedTasks {
        val (done, open) = tasks.partition { it.done }
        val sections = open.sortedWith(openOrder)
            .groupBy { sectionOf(it, today) }
            .toList()
            .sortedBy { it.first.ordinal }
        return GroupedTasks(open = sections, done = done.sortedByDescending { it.completedAt ?: 0L })
    }

    fun sectionOf(task: Task, today: LocalDate): Section {
        val due = task.dueDate ?: return Section.NO_DATE
        return when {
            due.isBefore(today) -> Section.OVERDUE
            due == today -> Section.TODAY
            due == today.plusDays(1) -> Section.TOMORROW
            else -> Section.UPCOMING
        }
    }
}
