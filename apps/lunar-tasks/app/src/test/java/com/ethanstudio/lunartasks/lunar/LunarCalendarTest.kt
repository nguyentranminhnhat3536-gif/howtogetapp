package com.ethanstudio.lunartasks.lunar

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class LunarCalendarTest {
    @Test
    fun lunarNewYearDates() {
        assertEquals(LunarDate(1, 1, 2023), LunarCalendar.fromSolar(LocalDate.of(2023, 1, 22)))
        assertEquals(LunarDate(1, 1, 2024), LunarCalendar.fromSolar(LocalDate.of(2024, 2, 10)))
        assertEquals(LunarDate(1, 1, 2025), LunarCalendar.fromSolar(LocalDate.of(2025, 1, 29)))
        assertEquals(LunarDate(1, 1, 2026), LunarCalendar.fromSolar(LocalDate.of(2026, 2, 17)))
        assertEquals(LunarDate(1, 1, 2027), LunarCalendar.fromSolar(LocalDate.of(2027, 2, 6)))
    }

    @Test
    fun leapMonths() {
        // 2025 nhuận tháng 6, 2023 nhuận tháng 2
        assertEquals(LunarDate(1, 6, 2025, leap = true), LunarCalendar.fromSolar(LocalDate.of(2025, 7, 25)))
        assertEquals(LunarDate(15, 7, 2025), LunarCalendar.fromSolar(LocalDate.of(2025, 9, 6)))
        assertEquals(LunarDate(1, 2, 2023, leap = true), LunarCalendar.fromSolar(LocalDate.of(2023, 3, 22)))
        assertEquals(LocalDate.of(2025, 7, 25), LunarCalendar.toSolar(1, 6, 2025, leap = true))
        assertNull(LunarCalendar.toSolar(1, 5, 2025, leap = true))
        assertNull(LunarCalendar.toSolar(1, 6, 2026, leap = true))
    }

    @Test
    fun roundTripEveryDayFrom2000To2050() {
        var date = LocalDate.of(2000, 1, 1)
        val end = LocalDate.of(2050, 12, 31)
        while (!date.isAfter(end)) {
            val lunar = LunarCalendar.fromSolar(date)
            assertEquals("$date → $lunar", date, LunarCalendar.toSolar(lunar.day, lunar.month, lunar.year, lunar.leap))
            date = date.plusDays(1)
        }
    }

    @Test
    fun nextFullMoonAndFirstDay() {
        // Rằm tháng Giêng 2025 là 12/2/2025
        assertEquals(LocalDate.of(2025, 2, 12), LunarCalendar.nextLunarMonthly(LocalDate.of(2025, 1, 29), 15))
        assertEquals(LocalDate.of(2025, 2, 28), LunarCalendar.nextLunarMonthly(LocalDate.of(2025, 2, 12), 1))
    }

    @Test
    fun nextYearlyLunarDate() {
        assertEquals(LocalDate.of(2026, 2, 17), LunarCalendar.nextLunarYearly(LocalDate.of(2025, 1, 29), 1, 1))
        assertEquals(LocalDate.of(2025, 1, 29), LunarCalendar.nextLunarYearly(LocalDate.of(2025, 1, 28), 1, 1))
    }

    @Test
    fun day30InShortMonthFallsBackToLastDay() {
        var date = LocalDate.of(2025, 1, 1)
        repeat(40) {
            val next = LunarCalendar.nextLunarMonthly(date, 30)
            val lunar = LunarCalendar.fromSolar(next)
            val tomorrow = LunarCalendar.fromSolar(next.plusDays(1))
            assertTrue("$next → $lunar", lunar.day == 30 || (lunar.day == 29 && tomorrow.day == 1))
            date = next
        }
    }

    @Test
    fun canChiNames() {
        assertEquals("Giáp Thìn", LunarCalendar.canChi(2024))
        assertEquals("Ất Tỵ", LunarCalendar.canChi(2025))
        assertEquals("Bính Ngọ", LunarCalendar.canChi(2026))
    }
}
