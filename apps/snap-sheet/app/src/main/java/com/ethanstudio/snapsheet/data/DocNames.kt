package com.ethanstudio.snapsheet.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Tên mặc định của tài liệu mới, ví dụ "Scan 2026-10-01 09.37". */
fun defaultDocName(prefix: String, millis: Long, zone: ZoneId = ZoneId.systemDefault()): String {
    val text = DateTimeFormatter.ofPattern("yyyy-MM-dd HH.mm").format(Instant.ofEpochMilli(millis).atZone(zone))
    return "$prefix $text"
}

/** Làm sạch tên người dùng nhập: bỏ khoảng trắng thừa, tối đa 80 ký tự. Trống thì trả về null. */
fun cleanDocName(input: String): String? = input.trim().take(80).takeIf { it.isNotEmpty() }

/**
 * Tên bản sao theo [template] có "%1$s" (ví dụ "%1$s (signed)"): cắt bớt [base] để cả tên dài tối đa [max] ký tự.
 */
fun copyName(base: String, template: String, max: Int = 80): String {
    require(template.contains(NAME_SLOT)) { "Template has no name slot" }
    val room = (max - (template.length - NAME_SLOT.length)).coerceAtLeast(1)
    return template.replace(NAME_SLOT, base.trim().take(room).trimEnd())
}

private const val NAME_SLOT = "%1\$s"
