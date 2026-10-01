package com.ethanstudio.snapsheet.data

import java.text.Collator
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

/** Cách sắp xếp danh sách trong tab Files. */
enum class DocSort { NEWEST, NAME }

/**
 * Sắp xếp tài liệu.
 *  - NEWEST: mới tạo trước; cùng thời điểm thì id lớn trước.
 *  - NAME: theo bảng chữ của [locale], không phân biệt hoa thường; trùng tên thì mới tạo trước.
 */
fun sortDocs(docs: List<Doc>, sort: DocSort, locale: Locale = Locale.getDefault()): List<Doc> = when (sort) {
    DocSort.NEWEST -> docs.sortedWith(compareByDescending<Doc> { it.createdAt }.thenByDescending { it.id })
    DocSort.NAME -> {
        val collator = Collator.getInstance(locale).apply { strength = Collator.SECONDARY }
        docs.sortedWith(Comparator<Doc> { a, b -> collator.compare(a.name, b.name) }.thenByDescending { it.createdAt })
    }
}

/**
 * Tách danh sách thành (hôm nay, trước đó), giữ nguyên thứ tự.
 * Tài liệu có ngày (theo [zone]) từ hôm nay trở đi tính là "hôm nay" (kể cả ngày tương lai do máy đổi giờ).
 */
fun splitToday(docs: List<Doc>, nowMillis: Long, zone: ZoneId = ZoneId.systemDefault()): Pair<List<Doc>, List<Doc>> {
    val today = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
    return docs.partition { !localDate(it.createdAt, zone).isBefore(today) }
}

private fun localDate(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()
