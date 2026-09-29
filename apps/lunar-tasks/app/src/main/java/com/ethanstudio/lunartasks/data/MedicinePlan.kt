package com.ethanstudio.lunartasks.data

import java.time.LocalDate
import java.time.LocalTime

/** Biến một đơn thuốc (tên + các giờ uống) thành các việc lặp hằng ngày có nhắc. Logic thuần, có unit test. */
object MedicinePlan {
    val PRESET_MINUTES = listOf(7 * 60, 12 * 60, 19 * 60)

    fun toTasks(name: String, note: String, minutes: Collection<Int>, now: LocalDate, nowTime: LocalTime): List<Task> =
        minutes.distinct().sorted().map { minute ->
            val firstDay = if (minute > nowTime.hour * 60 + nowTime.minute) now else now.plusDays(1)
            Task(
                title = name.trim(),
                note = note.trim(),
                dueEpochDay = firstDay.toEpochDay(),
                dueMinute = minute,
                repeat = Repeat.DAILY,
                remind = true,
                isMedicine = true,
            )
        }
}
