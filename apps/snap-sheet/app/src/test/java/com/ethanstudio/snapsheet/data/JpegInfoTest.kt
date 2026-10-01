package com.ethanstudio.snapsheet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Mảng byte JPEG tự dựng tay: chỉ có phần header, đủ để đọc kích thước. */
class JpegInfoTest {
    private fun bytes(vararg values: Int): ByteArray = ByteArray(values.size) { values[it].toByte() }

    private val soi = bytes(0xFF, 0xD8)
    private val eoi = bytes(0xFF, 0xD9)

    /** APP0 "JFIF", dài 16 byte (kể cả 2 byte độ dài). */
    private val app0 = bytes(
        0xFF, 0xE0, 0x00, 0x10,
        0x4A, 0x46, 0x49, 0x46, 0x00, 0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00,
    )

    /** SOS (bắt đầu dữ liệu ảnh), dài 8 byte. */
    private val sos = bytes(0xFF, 0xDA, 0x00, 0x08, 0x01, 0x01, 0x00, 0x00, 0x3F, 0x00)

    /** Đoạn SOF: [marker] (C0, C2…), độ dài 8 + 3 × số kênh, rồi cao, rộng, số kênh. */
    private fun sof(marker: Int, width: Int, height: Int, components: Int): ByteArray {
        val length = 8 + 3 * components
        val head = bytes(
            0xFF, marker, length shr 8, length and 0xFF, 8,
            height shr 8, height and 0xFF, width shr 8, width and 0xFF, components,
        )
        val perComponent = ByteArray(3 * components) { if (it % 3 == 0) (it / 3 + 1).toByte() else 0x11.toByte() }
        return head + perComponent
    }

    @Test
    fun readsBaselineColorJpeg() {
        // SOI + APP0 + SOF0 (640 × 480, 3 kênh) + EOI, viết hẳn từng byte.
        val jpeg = soi + app0 + bytes(
            0xFF, 0xC0, 0x00, 0x11, 0x08, 0x01, 0xE0, 0x02, 0x80, 0x03,
            0x01, 0x22, 0x00, 0x02, 0x11, 0x01, 0x03, 0x11, 0x01,
        ) + eoi
        assertEquals(JpegInfo(640, 480, 3), readJpegInfo(jpeg))
    }

    @Test
    fun readsProgressiveGrayJpeg() {
        val jpeg = soi + app0 + sof(0xC2, 200, 100, 1) + eoi
        assertEquals(JpegInfo(200, 100, 1), readJpegInfo(jpeg))
    }

    @Test
    fun readsBigSizesAndCmykComponents() {
        assertEquals(JpegInfo(4000, 3000, 3), readJpegInfo(soi + sof(0xC0, 4000, 3000, 3)))
        assertEquals(JpegInfo(800, 600, 4), readJpegInfo(soi + sof(0xC0, 800, 600, 4)))
    }

    @Test
    fun skipsFillBytesBeforeMarker() {
        val jpeg = soi + app0 + bytes(0xFF, 0xFF, 0xFF) + sof(0xC0, 640, 480, 3) + eoi
        assertEquals(JpegInfo(640, 480, 3), readJpegInfo(jpeg))
    }

    @Test
    fun huffmanTableMarkerIsNotMistakenForSof() {
        // DHT (C4) nằm trong khoảng C0–CF nhưng không phải SOF. Nếu đọc nhầm sẽ ra 7 × 5, 1 kênh.
        val dht = bytes(0xFF, 0xC4, 0x00, 0x0B, 0x10, 0x00, 0x05, 0x00, 0x07, 0x01, 0x00, 0x00, 0x00)
        val jpeg = soi + dht + sof(0xC0, 640, 480, 3) + eoi
        assertEquals(JpegInfo(640, 480, 3), readJpegInfo(jpeg))
    }

    @Test
    fun notStartingWithSoiIsNull() {
        val png = bytes(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)
        assertNull(readJpegInfo(png))
        assertNull(readJpegInfo(sof(0xC0, 640, 480, 3)))
        assertNull(readJpegInfo(ByteArray(0)))
        assertNull(readJpegInfo(soi))
    }

    @Test
    fun truncatedInsideSofIsNull() {
        val full = soi + app0 + sof(0xC0, 640, 480, 3)
        assertNull(readJpegInfo(full.copyOfRange(0, full.size - 4)))
        // Cụt ngay sau marker SOF (thiếu cả độ dài).
        assertNull(readJpegInfo(soi + app0 + bytes(0xFF, 0xC0, 0x00)))
    }

    @Test
    fun scanOrEndBeforeSofIsNull() {
        assertNull(readJpegInfo(soi + app0 + sos + sof(0xC0, 640, 480, 3) + eoi))
        assertNull(readJpegInfo(soi + eoi + sof(0xC0, 640, 480, 3)))
    }

    @Test
    fun zeroWidthOrHeightIsNull() {
        assertNull(readJpegInfo(soi + sof(0xC0, 640, 0, 3) + eoi))
        assertNull(readJpegInfo(soi + sof(0xC0, 0, 480, 3) + eoi))
    }

    @Test
    fun garbageBetweenSegmentsIsNull() {
        assertNull(readJpegInfo(soi + bytes(0x00, 0x12, 0x34) + sof(0xC0, 640, 480, 3)))
    }
}
