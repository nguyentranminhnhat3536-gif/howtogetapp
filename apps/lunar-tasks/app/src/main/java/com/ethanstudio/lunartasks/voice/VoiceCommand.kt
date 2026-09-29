package com.ethanstudio.lunartasks.voice

import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime

/** Một lệnh nói đã hiểu được. `on = null` nghĩa là đảo trạng thái hiện tại. */
sealed interface VoiceCommand {
    data class AddTask(val title: String, val date: LocalDate?, val minute: Int?) : VoiceCommand
    data object OpenAddTask : VoiceCommand
    data object ReadToday : VoiceCommand
    data class SetHighContrast(val on: Boolean?) : VoiceCommand
    data class SetSpeakReminders(val on: Boolean?) : VoiceCommand
    data class SetHaptics(val on: Boolean?) : VoiceCommand
    data class ChangeTextSize(val step: Int) : VoiceCommand
    data object CallContact : VoiceCommand
    data object AddMedicine : VoiceCommand
    data object OpenSettings : VoiceCommand
    data object Help : VoiceCommand
    data object Unknown : VoiceCommand
}

/**
 * Hiểu câu nói tiếng Việt hoặc tiếng Anh thành [VoiceCommand]. Chạy hoàn toàn trên máy.
 * So khớp trên chữ đã bỏ dấu (giữ nguyên độ dài) để vẫn hiểu khi bộ nhận giọng bỏ sót dấu.
 */
object VoiceCommandParser {

    fun parse(spoken: String, now: LocalDateTime): VoiceCommand {
        val text = spoken.trim().replace(Regex("\\s+"), " ")
        if (text.isEmpty()) return VoiceCommand.Unknown
        val folded = fold(text)

        if (MEDICINE_PREFIXES.any { folded.startsWith(it) }) return VoiceCommand.AddMedicine
        ADD_PREFIXES.firstOrNull { folded == it || folded.startsWith("$it ") }?.let { prefix ->
            return parseAddTask(text, folded, prefix.length, now)
        }

        val words = folded.split(' ').toSet()
        return when {
            has(folded, "tuong phan", "contrast", "den vang") -> VoiceCommand.SetHighContrast(onOff(words))
            has(folded, "doc nhac", "doc loi nhac", "doc to loi nhac", "doc thong bao", "read reminder", "speak reminder") ->
                VoiceCommand.SetSpeakReminders(onOff(words))
            "rung" in words || has(folded, "vibrat", "haptic") -> VoiceCommand.SetHaptics(onOff(words))
            has(folded, "chu to", "chu lon", "phong to", "to hon", "lon hon", "bigger", "larger", "zoom in") ->
                VoiceCommand.ChangeTextSize(+1)
            has(folded, "chu nho", "thu nho", "nho hon", "smaller", "zoom out") -> VoiceCommand.ChangeTextSize(-1)
            has(folded, "uong thuoc", "them thuoc", "nhac thuoc", "medicine", "medication", "pill") -> VoiceCommand.AddMedicine
            has(folded, "doc viec", "doc lich", "hom nay co gi", "doc to", "read today", "read my", "what's today", "whats today") ->
                VoiceCommand.ReadToday
            words.contains("goi") || words.contains("call") -> VoiceCommand.CallContact
            has(folded, "cai dat", "setting") -> VoiceCommand.OpenSettings
            has(folded, "giup", "huong dan", "noi gi", "help", "what can") -> VoiceCommand.Help
            else -> VoiceCommand.Unknown
        }
    }

    private fun parseAddTask(text: String, folded: String, start: Int, now: LocalDateTime): VoiceCommand {
        val removed = BooleanArray(text.length)
        for (i in 0 until start) removed[i] = true

        var date: LocalDate? = null
        for ((pattern, offset) in DATE_WORDS) {
            val match = pattern.find(folded, start) ?: continue
            date = now.toLocalDate().plusDays(offset)
            match.range.forEach { removed[it] = true }
            break
        }

        var minute: Int? = null
        for (pattern in TIME_PATTERNS) {
            val match = pattern.find(folded, start) ?: continue
            val value = toMinute(match) ?: continue
            minute = value
            match.range.forEach { removed[it] = true }
            break
        }

        val title = cleanTitle(text.filterIndexed { i, _ -> !removed[i] })
        if (title.isEmpty()) return VoiceCommand.OpenAddTask

        if (minute != null && date == null) {
            val today = now.toLocalDate()
            date = if (minute <= now.hour * 60 + now.minute) today.plusDays(1) else today
        }
        return VoiceCommand.AddTask(title, date, minute)
    }

    /** Đổi kết quả của TIME_PATTERNS ra số phút trong ngày. */
    private fun toMinute(match: MatchResult): Int? {
        var hour = match.groups["h"]?.value?.toIntOrNull() ?: return null
        var minute = match.groups["m"]?.value?.toIntOrNull() ?: 0
        if (match.groups["half"] != null) minute = 30
        when (match.groups["part"]?.value?.replace(".", "")) {
            "chieu", "toi", "pm" -> if (hour in 1..11) hour += 12
            "dem" -> if (hour in 6..11) hour += 12
            "trua" -> if (hour in 1..4) hour += 12
            "sang", "am" -> if (hour == 12) hour = 0
        }
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    private fun cleanTitle(raw: String): String {
        var words = raw.replace(Regex("\\s+"), " ").trim(' ', ',', '.', '!', '?').split(' ').filter { it.isNotEmpty() }
        while (words.isNotEmpty() && fold(words.last()) in DANGLING) words = words.dropLast(1)
        while (words.isNotEmpty() && fold(words.first()) in LEADING) words = words.drop(1)
        return words.joinToString(" ").trim(' ', ',', '.').replaceFirstChar { it.uppercase() }
    }

    private fun onOff(words: Set<String>): Boolean? = when {
        words.any { it in OFF_WORDS } -> false
        words.any { it in ON_WORDS } -> true
        else -> null
    }

    private fun has(text: String, vararg keys: String) = keys.any { text.contains(it) }

    /** Chữ thường, bỏ dấu tiếng Việt, đ → d. Giữ nguyên số ký tự để vị trí khớp với câu gốc. */
    fun fold(text: String): String = buildString(text.length) {
        for (c in text.lowercase()) {
            append(
                when (c) {
                    'đ' -> 'd'
                    else -> Normalizer.normalize(c.toString(), Normalizer.Form.NFD)[0]
                },
            )
        }
    }

    private val MEDICINE_PREFIXES = listOf("them thuoc", "nhac thuoc", "nhac uong thuoc", "add medicine", "add medication", "add pill")

    // Dài trước ngắn để "them viec" thắng "them".
    private val ADD_PREFIXES = listOf(
        "them cong viec", "them viec", "tao viec", "nhac toi", "nhac em", "nhac minh", "ghi nho", "ghi chu",
        "remind me to", "remind me", "add task", "new task", "them", "nhac", "ghi", "add",
    )

    private val DATE_WORDS = listOf(
        Regex("\\b(ngay kia|ngay mot|day after tomorrow)\\b") to 2L,
        Regex("\\b(ngay mai|sang mai|chieu mai|toi mai|tomorrow)\\b") to 1L,
        Regex("\\bmai\\b") to 1L,
        Regex("\\b(hom nay|toi nay|chieu nay|sang nay|today|tonight)\\b") to 0L,
    )

    // Kiểu tiếng Anh có am/pm đứng trước để "3:30 pm" không bị hiểu thành 3:30 sáng.
    private val TIME_PATTERNS = listOf(
        Regex("(?:\\bat\\s+)?\\b(?<h>\\d{1,2})(?::(?<m>\\d{2}))?\\s*(?<part>am|pm|a\\.m\\.|p\\.m\\.)(?:\\s|$)"),
        Regex("(?:\\b(?:luc|vao luc|vao)\\s+)?\\b(?<h>\\d{1,2})\\s*(?:gio|h|:)\\s*(?:(?<m>\\d{1,2})\\s*(?:phut)?)?\\s*(?<half>ruoi)?\\s*(?<part>sang|trua|chieu|toi|dem)?\\b"),
        Regex("\\bat\\s+(?<h>\\d{1,2})(?::(?<m>\\d{2}))?\\b"),
    )

    private val DANGLING = setOf("vao", "luc", "at", "on", "nhe", "nha", "ngay", "nhe.", "please")
    private val LEADING = setOf("la", "rang", "to", "de")
    private val OFF_WORDS = setOf("tat", "off", "disable", "ngung", "dung")
    private val ON_WORDS = setOf("bat", "mo", "on", "enable")
}
