package com.ethanstudio.lunartasks.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class MedicinePlanTest {
    private val today = LocalDate.of(2026, 9, 29)

    @Test
    fun oneDailyTaskPerTimeSortedAndDeduplicated() {
        val tasks = MedicinePlan.toTasks("  Thuốc huyết áp ", "1 viên", listOf(19 * 60, 7 * 60, 7 * 60), today, LocalTime.of(6, 0))
        assertEquals(listOf(7 * 60, 19 * 60), tasks.map { it.dueMinute })
        assertTrue(tasks.all { it.isMedicine && it.remind && it.repeat == Repeat.DAILY && it.title == "Thuốc huyết áp" })
    }

    @Test
    fun timesAlreadyPassedStartTomorrow() {
        val tasks = MedicinePlan.toTasks("A", "", listOf(7 * 60, 12 * 60), today, LocalTime.of(9, 0))
        assertEquals(today.plusDays(1), tasks[0].dueDate)
        assertEquals(today, tasks[1].dueDate)
    }
}
