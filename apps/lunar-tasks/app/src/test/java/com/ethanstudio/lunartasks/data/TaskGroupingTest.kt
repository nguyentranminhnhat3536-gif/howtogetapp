package com.ethanstudio.lunartasks.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TaskGroupingTest {
    private val today = LocalDate.of(2026, 9, 29)

    private fun task(id: Long, due: LocalDate?, done: Boolean = false, important: Boolean = false, minute: Int? = null) =
        Task(id = id, title = "t$id", dueEpochDay = due?.toEpochDay(), done = done, important = important, dueMinute = minute)

    @Test
    fun groupsIntoSectionsInOrder() {
        val tasks = listOf(
            task(1, null),
            task(2, today.plusDays(5)),
            task(3, today),
            task(4, today.minusDays(2)),
            task(5, today.plusDays(1)),
            task(6, today, done = true),
        )
        val grouped = TaskGrouping.group(tasks, today)
        assertEquals(
            listOf(Section.OVERDUE, Section.TODAY, Section.TOMORROW, Section.UPCOMING, Section.NO_DATE),
            grouped.open.map { it.first },
        )
        assertEquals(listOf(6L), grouped.done.map { it.id })
    }

    @Test
    fun importantFirstThenByTime() {
        val tasks = listOf(
            task(1, today, minute = 600),
            task(2, today, minute = 480),
            task(3, today, important = true, minute = 900),
            task(4, today),
        )
        val todayTasks = TaskGrouping.group(tasks, today).open.single().second
        assertEquals(listOf(3L, 2L, 1L, 4L), todayTasks.map { it.id })
    }
}
