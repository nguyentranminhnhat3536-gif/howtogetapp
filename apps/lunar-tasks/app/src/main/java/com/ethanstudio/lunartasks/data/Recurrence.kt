package com.ethanstudio.lunartasks.data

import com.ethanstudio.lunartasks.lunar.LunarCalendar
import java.time.LocalDate

/** Tính ngày đến hạn tiếp theo của việc lặp lại. Logic thuần, có unit test. */
object Recurrence {

    /** Lần lặp ngay sau [from]. Trả về null nếu [repeat] là NONE. */
    fun next(repeat: Repeat, from: LocalDate, lunarDay: Int?, lunarMonth: Int?): LocalDate? {
        val lunar by lazy { LunarCalendar.fromSolar(from) }
        return when (repeat) {
            Repeat.NONE -> null
            Repeat.DAILY -> from.plusDays(1)
            Repeat.WEEKLY -> from.plusWeeks(1)
            Repeat.MONTHLY -> from.plusMonths(1)
            Repeat.YEARLY -> from.plusYears(1)
            Repeat.LUNAR_MONTHLY -> LunarCalendar.nextLunarMonthly(from, lunarDay ?: lunar.day)
            Repeat.LUNAR_YEARLY ->
                LunarCalendar.nextLunarYearly(from, lunarDay ?: lunar.day, lunarMonth ?: lunar.month)
        }
    }

    /** Lần lặp đầu tiên sau cả ngày đến hạn hiện tại lẫn [today], để việc quá hạn không hiện lại ngay. */
    fun nextAfter(task: Task, today: LocalDate): LocalDate? {
        val due = task.dueDate ?: return null
        var next = next(task.repeat, due, task.lunarDay, task.lunarMonth) ?: return null
        var guard = 0
        while (!next.isAfter(today) && guard++ < 5000) {
            next = next(task.repeat, next, task.lunarDay, task.lunarMonth) ?: return null
        }
        return next
    }
}
