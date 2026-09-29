package com.ethanstudio.lunartasks.reminder

import com.ethanstudio.lunartasks.data.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class ReminderTimesTest {
    private val zone = ZoneId.of("Asia/Ho_Chi_Minh")
    private val due = LocalDate.of(2026, 10, 25)

    private fun millis(dateTime: LocalDateTime) = dateTime.atZone(zone).toInstant().toEpochMilli()

    @Test
    fun remindsAtDueTimeOrSevenAm() {
        val timed = Task(title = "t", dueEpochDay = due.toEpochDay(), dueMinute = 9 * 60 + 30, remind = true)
        assertEquals(millis(due.atTime(9, 30)), ReminderScheduler.triggerAtMillis(timed, zone))
        val untimed = timed.copy(dueMinute = null)
        assertEquals(millis(due.atTime(7, 0)), ReminderScheduler.triggerAtMillis(untimed, zone))
    }

    @Test
    fun dayBeforeReminderIsAtSevenPmPreviousDay() {
        val task = Task(title = "t", dueEpochDay = due.toEpochDay(), remindDayBefore = true)
        assertEquals(millis(due.minusDays(1).atTime(19, 0)), ReminderScheduler.dayBeforeTriggerAtMillis(task, zone))
        assertNull(ReminderScheduler.triggerAtMillis(task, zone))
    }

    @Test
    fun noReminderWhenDoneOrNoDate() {
        val done = Task(title = "t", dueEpochDay = due.toEpochDay(), remind = true, remindDayBefore = true, done = true)
        assertNull(ReminderScheduler.triggerAtMillis(done, zone))
        assertNull(ReminderScheduler.dayBeforeTriggerAtMillis(done, zone))
        val noDate = Task(title = "t", remind = true, remindDayBefore = true)
        assertNull(ReminderScheduler.triggerAtMillis(noDate, zone))
        assertNull(ReminderScheduler.dayBeforeTriggerAtMillis(noDate, zone))
    }
}
