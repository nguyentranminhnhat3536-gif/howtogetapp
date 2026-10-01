package com.ethanstudio.snapsheet.data

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Mỗi trang PDF nhập vào được dựng thành ảnh ở độ phân giải này (điểm ảnh mỗi inch). */
const val IMPORT_DPI = 200

/** Cạnh dài tối đa (điểm ảnh) của ảnh trang khi nhập PDF. */
const val IMPORT_MAX_SIDE = 2000

/** File PDF lớn hơn mức này (MB) thì từ chối nhập. */
const val IMPORT_MAX_MB = 100
const val IMPORT_MAX_BYTES: Long = IMPORT_MAX_MB * 1024L * 1024L

/** Lỗi khi nhập PDF, kèm lý do để báo đúng cho người dùng. */
class PdfImportException(val reason: Reason) : IOException(reason.name) {
    enum class Reason { LOCKED, TOO_LARGE, UNREADABLE }
}

/** [total] trang trong file, nhập [take] trang đầu. */
data class ImportPlan(val total: Int, val take: Int) {
    val truncated: Boolean get() = take < total
}

/** Số trang sẽ nhập: take = min(total, limit), không âm. */
fun importPlan(totalPages: Int, limit: Int): ImportPlan {
    val total = totalPages.coerceAtLeast(0)
    return ImportPlan(total, minOf(total, limit).coerceAtLeast(0))
}

/**
 * Cỡ ảnh (điểm ảnh) để dựng một trang PDF khổ [widthPt] × [heightPt] (điểm, 1/72 inch):
 * đủ [dpi] nhưng cạnh dài không quá [maxSide]; mỗi cạnh ≥ 1.
 */
fun renderSize(widthPt: Int, heightPt: Int, dpi: Int = IMPORT_DPI, maxSide: Int = IMPORT_MAX_SIDE): Pair<Int, Int> {
    require(widthPt > 0 && heightPt > 0) { "Invalid page size" }
    val scale = minOf(dpi / 72.0, maxSide.toDouble() / maxOf(widthPt, heightPt))
    val width = Math.round(widthPt * scale).toInt().coerceAtLeast(1)
    val height = Math.round(heightPt * scale).toInt().coerceAtLeast(1)
    return width to height
}

/** Tên tài liệu lấy từ tên file: bỏ đuôi ".pdf" (không phân biệt hoa thường) rồi làm sạch. Rỗng → null. */
fun docNameFromFileName(fileName: String?): String? {
    if (fileName.isNullOrEmpty()) return null
    val base = if (fileName.endsWith(PDF_SUFFIX, ignoreCase = true)) fileName.dropLast(PDF_SUFFIX.length) else fileName
    return cleanDocName(base)
}

/**
 * Chép [input] sang [output] (bộ đệm 64 KB). Chép vượt [maxBytes] thì dừng ngay và trả false;
 * hết nguồn thì trả true.
 */
fun copyAtMost(input: InputStream, output: OutputStream, maxBytes: Long): Boolean {
    val buffer = ByteArray(COPY_BUFFER_BYTES)
    var total = 0L
    while (true) {
        val read = input.read(buffer)
        if (read < 0) return true
        total += read
        if (total > maxBytes) return false
        output.write(buffer, 0, read)
    }
}

private const val PDF_SUFFIX = ".pdf"
private const val COPY_BUFFER_BYTES = 64 * 1024
