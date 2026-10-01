package com.ethanstudio.lunartasks.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecurrenceTest {
    private fun task(due: LocalDate, repeat: Repeat, lunarDay: Int? = null, lunarMonth: Int? = null) =
        Task(title = "t", dueEpochDay = due.toEpochDay(), repeat = repeat, lunarDay = lunarDay, lunarMonth = lunarMonth)

    @Test
    fun solarRepeats() {
        val d = LocalDate.of(2026, 1, 31)
        assertEquals(LocalDate.of(2026, 2, 1), Recurrence.next(Repeat.DAILY, d, null, null))
        assertEquals(LocalDate.of(2026, 2, 7), Recurrence.next(Repeat.WEEKLY, d, null, null))
        assertEquals(LocalDate.of(2026, 2, 28), Recurrence.next(Repeat.MONTHLY, d, null, null))
        assertEquals(LocalDate.of(2027, 1, 31), Recurrence.next(Repeat.YEARLY, d, null, null))
        assertNull(Recurrence.next(Repeat.NONE, d, null, null))
    }

    @Test
    fun overdueDailyTaskMovesPastToday() {
        val today = LocalDate.of(2026, 3, 10)
        assertEquals(LocalDate.of(2026, 3, 11), Recurrence.nextAfter(task(LocalDate.of(2026, 3, 1), Repeat.DAILY), today))
        assertEquals(LocalDate.of(2026, 3, 11), Recurrence.nextAfter(task(today, Repeat.DAILY), today))
    }

    @Test
    fun futureTaskAdvancesOnePeriod() {
        val today = LocalDate.of(2026, 3, 10)
        assertEquals(LocalDate.of(2026, 3, 27), Recurrence.nextAfter(task(LocalDate.of(2026, 3, 20), Repeat.WEEKLY), today))
    }

    @Test
    fun lunarMonthlyFullMoon() {
        // Rằm tháng Giêng 2025 (12/2) → rằm tháng Hai (14/3/2025)
        val today = LocalDate.of(2025, 2, 12)
        assertEquals(
            LocalDate.of(2025, 3, 14),
            Recurrence.nextAfter(task(today, Repeat.LUNAR_MONTHLY, lunarDay = 15), today),
        )
    }

    @Test
    fun lunarYearlyMemorial() {
        val tet2025 = LocalDate.of(2025, 1, 29)
        assertEquals(
            LocalDate.of(2026, 2, 17),
            Recurrence.nextAfter(task(tet2025, Repeat.LUNAR_YEARLY, lunarDay = 1, lunarMonth = 1), tet2025),
        )
    }

    @Test
    fun noDateMeansNoNext() {
        assertNull(Recurrence.nextAfter(Task(title = "t", repeat = Repeat.DAILY), LocalDate.of(2026, 1, 1)))
    }
}
