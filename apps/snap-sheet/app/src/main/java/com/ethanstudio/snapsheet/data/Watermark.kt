package com.ethanstudio.snapsheet.data

import kotlin.math.atan2
import kotlin.math.hypot

/** Chữ mờ dài tối đa bao nhiêu ký tự. */
const val WATERMARK_MAX_CHARS = 40

/** Màu chữ mờ (RGB, chưa có độ trong). Đây là màu in lên giấy nên cố ý giữ cố định ở mọi giao diện. */
enum class WatermarkColor(val rgb: Int) { GRAY(0x5A6275), RED(0xC62828), BLUE(0x1747C9) }

/** Độ đậm của chữ mờ (độ đục 0..1). */
enum class WatermarkStrength(val alpha: Float) { LIGHT(0.15f), MEDIUM(0.3f), STRONG(0.45f) }

data class WatermarkSpec(val text: String, val color: WatermarkColor, val strength: WatermarkStrength)

/** Cắt còn tối đa [max] ký tự UTF-16; nếu ký tự cuối là nửa đầu của emoji (cặp surrogate) thì bỏ nó. */
fun takeSafe(text: String, max: Int): String {
    if (text.length <= max) return text
    val cut = text.take(max)
    return if (cut.isNotEmpty() && cut.last().isHighSurrogate()) cut.dropLast(1) else cut
}

/** Gộp mọi khoảng trắng (cả xuống dòng) thành một dấu cách, bỏ hai đầu, tối đa 40 ký tự. Rỗng → null. */
fun cleanWatermarkText(input: String): String? =
    takeSafe(input.replace(WHITESPACE_RUN, " ").trim(), WATERMARK_MAX_CHARS).trim().takeIf { it.isNotEmpty() }

/** Góc xoay (độ, theo chiều Canvas Android) để chữ chạy từ góc dưới trái lên góc trên phải. */
fun watermarkAngle(width: Int, height: Int): Float {
    require(width > 0 && height > 0) { "Invalid size" }
    return (-Math.toDegrees(atan2(height.toDouble(), width.toDouble()))).toFloat()
}

/**
 * Cỡ chữ (điểm ảnh) để chữ dài khoảng 70% đường chéo trang, không quá 20% cạnh ngắn, không dưới 8.
 * [widthAt100] = bề rộng chữ đo ở cỡ 100 điểm ảnh.
 */
fun watermarkTextSize(width: Int, height: Int, widthAt100: Float): Float {
    require(width > 0 && height > 0 && widthAt100 > 0f) { "Invalid size" }
    val size = 100.0 * DIAGONAL_SHARE * hypot(width.toDouble(), height.toDouble()) / widthAt100
    return size.toFloat().coerceAtMost(MAX_SHORT_SIDE_SHARE * minOf(width, height)).coerceAtLeast(MIN_TEXT_PX)
}

private val WHITESPACE_RUN = Regex("\\s+")
private const val DIAGONAL_SHARE = 0.7
private const val MAX_SHORT_SIDE_SHARE = 0.2f
private const val MIN_TEXT_PX = 8f
