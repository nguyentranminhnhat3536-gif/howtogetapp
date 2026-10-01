package com.ethanstudio.snapsheet.i18n

/** Các ngôn ngữ app có bản dịch. Thuần Kotlin để test được trên JVM. */
object AppLanguages {
    /** Thứ tự hiện trong hộp chọn. "" (không có trong list) = theo hệ thống. */
    val TAGS: List<String> = listOf("en", "vi", "es", "pt-BR", "fr", "de", "id", "ru", "tr", "ja", "ko", "hi")

    /** Ngôn ngữ có đủ chữ trong font DM Serif Display (chỉ Latin, không có dấu tiếng Việt). */
    private val SERIF_LANGUAGES = setOf("en", "es", "pt", "fr", "de", "id", "in", "tr")

    /**
     * Chuẩn hóa mã ngôn ngữ: bỏ khoảng trắng, "_" thành "-", "in" thành "id", không phân biệt hoa thường.
     * Khớp đúng một mã trong [TAGS] thì trả mã đó; không thì thử phần ngôn ngữ ("en-US" → "en").
     * Không khớp ("", "xx", "pt-PT") → "" (theo hệ thống).
     */
    fun normalize(tag: String): String {
        val clean = tag.trim().replace('_', '-')
        if (clean.isEmpty()) return ""
        val parts = clean.split('-')
        val language = parts[0].lowercase().let { if (it == "in") "id" else it }
        val full = (listOf(language) + parts.drop(1)).joinToString("-")
        TAGS.firstOrNull { it.equals(full, ignoreCase = true) }?.let { return it }
        return TAGS.firstOrNull { it == language } ?: ""
    }

    /** true nếu tiêu đề ở ngôn ngữ này dùng được font DM Serif nhúng sẵn. */
    fun usesBundledSerif(tag: String): Boolean {
        val language = tag.trim().replace('_', '-').substringBefore('-').lowercase()
        return language in SERIF_LANGUAGES
    }
}
