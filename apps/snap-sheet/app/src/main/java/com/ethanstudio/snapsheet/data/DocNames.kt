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
