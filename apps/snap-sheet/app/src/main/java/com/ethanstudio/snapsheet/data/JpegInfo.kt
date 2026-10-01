package com.ethanstudio.snapsheet.data

/** Kích thước và số kênh màu (1 = xám, 3 = màu, 4 = CMYK) đọc từ header JPEG. */
data class JpegInfo(val width: Int, val height: Int, val components: Int)

/**
 * Đọc marker SOF (C0–CF trừ C4, C8, CC) của file JPEG.
 * Không phải JPEG, file cụt, gặp SOS hoặc EOI trước SOF, hoặc cao/rộng = 0 → null.
 */
fun readJpegInfo(bytes: ByteArray): JpegInfo? {
    fun u8(i: Int): Int = bytes[i].toInt() and 0xFF
    fun u16(i: Int): Int = (u8(i) shl 8) or u8(i + 1)

    if (bytes.size < 4 || u8(0) != 0xFF || u8(1) != 0xD8) return null
    var i = 2
    while (i < bytes.size) {
        if (u8(i) != 0xFF) return null
        // Bỏ các byte đệm 0xFF trước marker.
        while (i < bytes.size && u8(i) == 0xFF) i++
        if (i >= bytes.size) return null
        val marker = u8(i)
        i++
        when {
            marker == 0xD9 || marker == 0xDA -> return null
            // Marker không có phần dữ liệu (RSTn, TEM).
            marker in 0xD0..0xD7 || marker == 0x01 -> continue
        }
        if (i + 1 >= bytes.size) return null
        val length = u16(i)
        if (length < 2 || i + length > bytes.size) return null
        val isSof = marker in 0xC0..0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC
        if (isSof) {
            if (length < 8) return null
            val height = u16(i + 3)
            val width = u16(i + 5)
            val components = u8(i + 7)
            if (width == 0 || height == 0) return null
            return JpegInfo(width, height, components)
        }
        i += length
    }
    return null
}
