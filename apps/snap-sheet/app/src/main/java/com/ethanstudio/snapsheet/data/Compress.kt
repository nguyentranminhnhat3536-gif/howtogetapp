package com.ethanstudio.snapsheet.data

/** Mức nén PDF khi chia sẻ: cạnh dài tối đa (điểm ảnh) và chất lượng JPEG. ORIGINAL = gửi nguyên file. */
enum class CompressLevel(val maxSide: Int, val quality: Int) {
    SMALL(1200, 60),
    MEDIUM(1800, 75),
    ORIGINAL(0, 100),
}

/** Thu nhỏ giữ tỉ lệ để cạnh dài ≤ [maxSide]; không phóng to; mỗi cạnh ≥ 1. */
fun scaledSize(width: Int, height: Int, maxSide: Int): Pair<Int, Int> {
    require(width > 0 && height > 0 && maxSide > 0) { "Invalid size" }
    val longSide = maxOf(width, height)
    if (longSide <= maxSide) return width to height
    val w = (width.toLong() * maxSide / longSide).toInt().coerceAtLeast(1)
    val h = (height.toLong() * maxSide / longSide).toInt().coerceAtLeast(1)
    return w to h
}

/** Bản nén không nhỏ hơn bản gốc thì gửi bản gốc. */
fun shouldShareOriginal(originalBytes: Long, compressedBytes: Long): Boolean = compressedBytes >= originalBytes
