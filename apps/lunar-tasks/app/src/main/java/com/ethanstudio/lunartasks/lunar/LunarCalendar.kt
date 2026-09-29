package com.ethanstudio.lunartasks.lunar

import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

/** Một ngày âm lịch. [leap] = tháng nhuận. */
data class LunarDate(val day: Int, val month: Int, val year: Int, val leap: Boolean = false)

/**
 * Âm lịch Việt Nam (múi giờ UTC+7), theo thuật toán thiên văn của Hồ Ngọc Đức
 * (https://www.informatik.uni-leipzig.de/~duc/amlich/). Chạy offline, không cần mạng.
 */
object LunarCalendar {
    private const val TIME_ZONE = 7.0
    private const val JD_OF_EPOCH_DAY_0 = 2440588L // Số ngày Julius của 1970-01-01
    private const val SYNODIC_MONTH = 29.530588853
    private const val NEW_MOON_EPOCH = 2415021.076998695

    private val CAN = listOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
    private val CHI = listOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")

    fun fromSolar(date: LocalDate): LunarDate {
        val dayNumber = julianDay(date)
        val k = floor((dayNumber - NEW_MOON_EPOCH) / SYNODIC_MONTH).toInt()
        var monthStart = newMoonDay(k + 1)
        if (monthStart > dayNumber) monthStart = newMoonDay(k)

        var a11 = lunarMonth11(date.year)
        var b11 = a11
        var lunarYear: Int
        if (a11 >= monthStart) {
            lunarYear = date.year
            a11 = lunarMonth11(date.year - 1)
        } else {
            lunarYear = date.year + 1
            b11 = lunarMonth11(date.year + 1)
        }

        val lunarDay = dayNumber - monthStart + 1
        val diff = (monthStart - a11) / 29
        var leap = false
        var lunarMonth = diff + 11
        if (b11 - a11 > 365) {
            val leapMonthDiff = leapMonthOffset(a11)
            if (diff >= leapMonthDiff) {
                lunarMonth = diff + 10
                if (diff == leapMonthDiff) leap = true
            }
        }
        if (lunarMonth > 12) lunarMonth -= 12
        if (lunarMonth >= 11 && diff < 4) lunarYear -= 1
        return LunarDate(lunarDay, lunarMonth, lunarYear, leap)
    }

    /** Đổi âm lịch sang dương lịch. Trả về null nếu tháng nhuận không tồn tại trong năm đó. */
    fun toSolar(day: Int, month: Int, year: Int, leap: Boolean = false): LocalDate? {
        val a11: Int
        val b11: Int
        if (month < 11) {
            a11 = lunarMonth11(year - 1)
            b11 = lunarMonth11(year)
        } else {
            a11 = lunarMonth11(year)
            b11 = lunarMonth11(year + 1)
        }
        val k = floor(0.5 + (a11 - NEW_MOON_EPOCH) / SYNODIC_MONTH).toInt()
        var off = month - 11
        if (off < 0) off += 12
        if (b11 - a11 > 365) {
            val leapOff = leapMonthOffset(a11)
            var leapMonth = leapOff - 2
            if (leapMonth < 0) leapMonth += 12
            if (leap && month != leapMonth) return null
            if (leap || off >= leapOff) off += 1
        } else if (leap) {
            return null
        }
        val monthStart = newMoonDay(k + off)
        return dateOf(monthStart + day - 1)
    }

    /**
     * Ngày dương đầu tiên sau [after] có ngày âm là [day] (mọi tháng, kể cả tháng nhuận).
     * Tháng thiếu (29 ngày) mà cần ngày 30 thì lấy ngày cuối tháng.
     */
    fun nextLunarMonthly(after: LocalDate, day: Int): LocalDate {
        var date = after.plusDays(1)
        repeat(62) {
            val lunar = fromSolar(date)
            if (lunar.day == day) return date
            if (day == 30 && lunar.day == 29 && fromSolar(date.plusDays(1)).day == 1) return date
            date = date.plusDays(1)
        }
        return date
    }

    /** Ngày dương đầu tiên sau [after] ứng với ngày [day] tháng [month] âm lịch (không tính tháng nhuận). */
    fun nextLunarYearly(after: LocalDate, day: Int, month: Int): LocalDate {
        val startYear = fromSolar(after).year
        for (year in startYear..startYear + 2) {
            val candidate = toSolarClamped(day, month, year)
            if (candidate.isAfter(after)) return candidate
        }
        return after.plusYears(1)
    }

    /** Như [toSolar] nhưng ngày 30 của tháng thiếu được lùi về ngày 29. */
    fun toSolarClamped(day: Int, month: Int, year: Int): LocalDate {
        val date = toSolar(day, month, year)!!
        val back = fromSolar(date)
        return if (back.day == day && back.month == month) date else toSolar(day - 1, month, year)!!
    }

    /** Tên năm theo Can Chi, ví dụ 2026 → "Bính Ngọ". */
    fun canChi(lunarYear: Int): String =
        CAN[Math.floorMod(lunarYear + 6, 10)] + " " + CHI[Math.floorMod(lunarYear + 8, 12)]

    private fun julianDay(date: LocalDate): Int = (date.toEpochDay() + JD_OF_EPOCH_DAY_0).toInt()

    private fun dateOf(julianDay: Int): LocalDate = LocalDate.ofEpochDay(julianDay - JD_OF_EPOCH_DAY_0)

    private fun newMoon(k: Int): Double {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val dr = PI / 180
        var jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd1 += 0.00033 * sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)
        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3
        var c1 = (0.1734 - 0.000393 * t) * sin(m * dr) + 0.0021 * sin(2 * dr * m)
        c1 = c1 - 0.4068 * sin(mpr * dr) + 0.0161 * sin(dr * 2 * mpr)
        c1 = c1 - 0.0004 * sin(dr * 3 * mpr)
        c1 = c1 + 0.0104 * sin(dr * 2 * f) - 0.0051 * sin(dr * (m + mpr))
        c1 = c1 - 0.0074 * sin(dr * (m - mpr)) + 0.0004 * sin(dr * (2 * f + m))
        c1 = c1 - 0.0004 * sin(dr * (2 * f - m)) - 0.0006 * sin(dr * (2 * f + mpr))
        c1 = c1 + 0.0010 * sin(dr * (2 * f - mpr)) + 0.0005 * sin(dr * (2 * mpr + m))
        val deltaT = if (t < -11) {
            0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
        } else {
            -0.000278 + 0.000265 * t + 0.000262 * t2
        }
        return jd1 + c1 - deltaT
    }

    private fun newMoonDay(k: Int): Int = floor(newMoon(k) + 0.5 + TIME_ZONE / 24).toInt()

    private fun sunLongitude(jdn: Double): Double {
        val t = (jdn - 2451545.0) / 36525
        val t2 = t * t
        val dr = PI / 180
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(dr * m)
        dl = dl + (0.019993 - 0.000101 * t) * sin(dr * 2 * m) + 0.000290 * sin(dr * 3 * m)
        var l = (l0 + dl) * dr
        l -= PI * 2 * floor(l / (PI * 2))
        return l
    }

    /** Cung hoàng đạo (0..11) của Mặt Trời lúc bắt đầu ngày [dayNumber]. */
    private fun sunSector(dayNumber: Int): Int =
        floor(sunLongitude(dayNumber - 0.5 - TIME_ZONE / 24) / PI * 6).toInt()

    /** Ngày bắt đầu tháng 11 âm lịch (tháng chứa Đông chí) của năm [year]. */
    private fun lunarMonth11(year: Int): Int {
        val off = julianDay(LocalDate.of(year, 12, 31)) - 2415021
        val k = floor(off / SYNODIC_MONTH).toInt()
        var nm = newMoonDay(k)
        if (sunSector(nm) >= 9) nm = newMoonDay(k - 1)
        return nm
    }

    private fun leapMonthOffset(a11: Int): Int {
        val k = floor((a11 - NEW_MOON_EPOCH) / SYNODIC_MONTH + 0.5).toInt()
        var i = 1
        var arc = sunSector(newMoonDay(k + i))
        var last: Int
        do {
            last = arc
            i++
            arc = sunSector(newMoonDay(k + i))
        } while (arc != last && i < 14)
        return i - 1
    }
}
