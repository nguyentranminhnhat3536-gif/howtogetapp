package com.ethanstudio.snapsheet.ocr

/** Ngắt dòng đơn (không phải ngắt đoạn), kèm khoảng trắng dính hai bên. */
private val SingleBreak = Regex("(?<=[^\\n])[ \\t]*\\n[ \\t]*(?=[^\\n])")

/** Ký tự không được dùng trong tên file trên đa số hệ thống. */
private val BadFileChars = Regex("[\\\\/:*?\"<>|\\r\\n\\t]")

private const val MAX_FILE_NAME = 60

/**
 * Chữ của trang đang chọn. 0 = mọi trang: bỏ trang trống, nối bằng một dòng trống.
 * n (1..size) = đúng trang n (có thể là ""). n ngoài khoảng thì coi như 0.
 */
fun pageText(pages: List<String>, selected: Int): String =
    if (selected in 1..pages.size) {
        pages[selected - 1]
    } else {
        pages.filter { it.isNotBlank() }.joinToString("\n\n")
    }

/** Nối các dòng trong một đoạn thành một dòng (mỗi chỗ ngắt thành đúng 1 dấu cách). Giữ nguyên ngắt đoạn "\n\n". */
fun joinLines(text: String): String = text.replace(SingleBreak, " ")

/** Chữ sẽ hiển thị, copy, share hoặc lưu. */
fun displayText(pages: List<String>, selected: Int, joined: Boolean): String =
    pageText(pages, selected).let { if (joined) joinLines(it) else it }

/** Vị trí các chỗ khớp với [query] (đã trim), không phân biệt hoa thường, không chồng nhau, từ trái sang phải. */
fun findMatches(text: String, query: String): List<IntRange> {
    val q = query.trim()
    if (q.isEmpty()) return emptyList()
    val matches = mutableListOf<IntRange>()
    var from = 0
    while (from <= text.length - q.length) {
        val index = text.indexOf(q, from, ignoreCase = true)
        if (index < 0) break
        matches += index until index + q.length
        from = index + q.length
    }
    return matches
}

/** Số từ = số cụm ký tự không phải khoảng trắng. */
fun wordCount(text: String): Int = text.split(Regex("\\s+")).count { it.isNotEmpty() }

/** Số ký tự theo code point (emoji tính 1). */
fun charCount(text: String): Int = text.codePointCount(0, text.length)

/** Tên file gợi ý khi lưu: bỏ ký tự cấm, tối đa 60 ký tự, rỗng thì dùng [fallback]; luôn có đuôi ".txt". */
fun txtFileName(docName: String, fallback: String = "SnapSheet"): String {
    val clean = docName.replace(BadFileChars, "_").trim().take(MAX_FILE_NAME).trim()
    return clean.ifEmpty { fallback } + ".txt"
}
