package com.ethanstudio.lunartasks.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class Repeat { NONE, DAILY, WEEKLY, MONTHLY, YEARLY, LUNAR_MONTHLY, LUNAR_YEARLY;

    val isLunar: Boolean get() = this == LUNAR_MONTHLY || this == LUNAR_YEARLY
}

/**
 * Một việc cần làm. Ngày lưu dạng epochDay, giờ lưu dạng số phút tính từ 0h.
 * Với việc lặp theo âm lịch, [lunarDay]/[lunarMonth] giữ ngày gốc (ví dụ ngày 30 vẫn là 30
 * dù tháng này thiếu và việc đang rơi vào ngày 29).
 */
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val note: String = "",
    val dueEpochDay: Long? = null,
    val dueMinute: Int? = null,
    val repeat: Repeat = Repeat.NONE,
    val lunarDay: Int? = null,
    val lunarMonth: Int? = null,
    val remind: Boolean = false,
    val important: Boolean = false,
    val done: Boolean = false,
    val completedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
) {
    val dueDate: LocalDate? get() = dueEpochDay?.let(LocalDate::ofEpochDay)
}
