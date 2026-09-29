package com.ethanstudio.lunartasks.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ethanstudio.lunartasks.R
import com.ethanstudio.lunartasks.data.Repeat
import com.ethanstudio.lunartasks.lunar.LunarCalendar
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

fun formatTime(minute: Int): String = "%02d:%02d".format(Locale.ROOT, minute / 60, minute % 60)

/** "Hôm nay", "Ngày mai" hoặc ngày dạng ngắn theo ngôn ngữ máy. */
@Composable
fun relativeDate(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(R.string.date_today)
    today.plusDays(1) -> stringResource(R.string.date_tomorrow)
    today.minusDays(1) -> stringResource(R.string.date_yesterday)
    else -> date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "EEE, dd/MM" else "dd/MM/yyyy", Locale.getDefault()))
}

fun fullDate(date: LocalDate): String =
    date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(Locale.getDefault()))

/** Ngày âm ngắn gọn, ví dụ "ÂL 15/8" hoặc "ÂL 1/6N" (tháng nhuận). */
@Composable
fun lunarShort(date: LocalDate): String {
    val lunar = LunarCalendar.fromSolar(date)
    val month = if (lunar.leap) stringResource(R.string.lunar_leap_month, lunar.month) else lunar.month.toString()
    return stringResource(R.string.lunar_short, lunar.day, month)
}

/** Ngày âm đầy đủ, ví dụ "15/8 năm Bính Ngọ". */
@Composable
fun lunarLong(date: LocalDate): String {
    val lunar = LunarCalendar.fromSolar(date)
    val month = if (lunar.leap) stringResource(R.string.lunar_leap_month, lunar.month) else lunar.month.toString()
    return stringResource(R.string.lunar_long, lunar.day, month, LunarCalendar.canChi(lunar.year))
}

@Composable
fun repeatLabel(repeat: Repeat): String = stringResource(
    when (repeat) {
        Repeat.NONE -> R.string.repeat_none
        Repeat.DAILY -> R.string.repeat_daily
        Repeat.WEEKLY -> R.string.repeat_weekly
        Repeat.MONTHLY -> R.string.repeat_monthly
        Repeat.YEARLY -> R.string.repeat_yearly
        Repeat.LUNAR_MONTHLY -> R.string.repeat_lunar_monthly
        Repeat.LUNAR_YEARLY -> R.string.repeat_lunar_yearly
    },
)
