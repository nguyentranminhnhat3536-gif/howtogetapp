package com.ethanstudio.snapsheet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Chèn chữ mờ: làm sạch chữ, cắt không vỡ emoji, góc xoay và cỡ chữ theo khổ trang. */
class WatermarkTest {

    // ---- cleanWatermarkText ----

    @Test
    fun spacesAroundTextAreTrimmed() {
        assertEquals("Mật", cleanWatermarkText("  Mật  "))
    }

    @Test
    fun newlinesAndTabsBecomeOneSpace() {
        assertEquals("a b", cleanWatermarkText("a\n\tb"))
        assertEquals("BẢN SAO", cleanWatermarkText("BẢN     SAO"))
    }

    @Test
    fun blankTextGivesNull() {
        assertNull(cleanWatermarkText(""))
        assertNull(cleanWatermarkText("   "))
        assertNull(cleanWatermarkText("\n\t "))
    }

    @Test
    fun longTextStopsAt40Chars() {
        assertEquals(40, cleanWatermarkText("x".repeat(60))?.length)
        assertEquals(40, WATERMARK_MAX_CHARS)
    }

    @Test
    fun spaceLeftAtCutPointIsTrimmed() {
        // Ký tự thứ 40 là dấu cách → bỏ đi, còn 39 chữ x.
        assertEquals("x".repeat(39), cleanWatermarkText("x".repeat(39) + " yz"))
    }

    @Test
    fun cleanTextNeverSplitsEmoji() {
        val text = cleanWatermarkText("a" + "😀".repeat(25))
        assertEquals(39, text?.length)
        assertFalse(text!!.last().isHighSurrogate())
    }

    // ---- takeSafe ----

    @Test
    fun takeSafeDropsHalfEmojiAtCut() {
        // "a" + 25 emoji = 51 ký tự UTF-16; cắt ở 40 rơi vào giữa emoji thứ 20 → còn 39.
        assertEquals(39, takeSafe("a" + "😀".repeat(25), 40).length)
        assertEquals("ab", takeSafe("ab😀", 3))
        assertEquals("", takeSafe("😀b", 1))
    }

    @Test
    fun takeSafeKeepsWholeEmojiAtCut() {
        assertEquals("a😀", takeSafe("a😀b", 3))
        assertEquals("😀".repeat(20), takeSafe("😀".repeat(20), 40))
    }

    @Test
    fun takeSafeKeepsShortTextAsIs() {
        assertEquals("abc", takeSafe("abc", 40))
        assertEquals("abc", takeSafe("abcdef", 3))
    }

    // ---- watermarkAngle: chữ chạy từ góc dưới trái lên góc trên phải ----

    @Test
    fun squarePageRotatesMinus45() {
        assertEquals(-45f, watermarkAngle(1000, 1000), 0.01f)
    }

    @Test
    fun landscapePageFollowsDiagonal() {
        assertEquals(-26.565f, watermarkAngle(2000, 1000), 0.01f)
    }

    @Test
    fun portraitPageFollowsDiagonal() {
        assertEquals(-63.435f, watermarkAngle(1000, 2000), 0.01f)
    }

    @Test
    fun emptyPageSizeIsRejectedForAngle() {
        assertThrows(IllegalArgumentException::class.java) { watermarkAngle(0, 5) }
        assertThrows(IllegalArgumentException::class.java) { watermarkAngle(5, -1) }
    }

    // ---- watermarkTextSize: chữ dài khoảng 70% đường chéo ----

    @Test
    fun textSpans70PercentOfDiagonal() {
        // 100 × 0,7 × 1414,21 / 1000 = 98,995
        assertEquals(98.995f, watermarkTextSize(1000, 1000, 1000f), 0.01f)
    }

    @Test
    fun shortTextIsCappedAt20PercentOfShortSide() {
        assertEquals(200f, watermarkTextSize(1000, 1000, 100f), 0.001f)
    }

    @Test
    fun longerTextGetsSmaller() {
        // Đường chéo 2000 × 1000 = 2236,07.
        assertEquals(78.262f, watermarkTextSize(2000, 1000, 2000f), 0.01f)
        assertEquals(39.131f, watermarkTextSize(2000, 1000, 4000f), 0.01f)
    }

    @Test
    fun textNeverSmallerThan8Px() {
        assertEquals(8f, watermarkTextSize(100, 100, 100_000f), 0.001f)
        // Trang tí hon: trần 20% (2 px) thấp hơn sàn 8 px → vẫn 8.
        assertEquals(8f, watermarkTextSize(10, 10, 1f), 0.001f)
    }

    @Test
    fun invalidSizeIsRejectedForTextSize() {
        assertThrows(IllegalArgumentException::class.java) { watermarkTextSize(0, 10, 100f) }
        assertThrows(IllegalArgumentException::class.java) { watermarkTextSize(10, 0, 100f) }
        assertThrows(IllegalArgumentException::class.java) { watermarkTextSize(10, 10, 0f) }
        assertThrows(IllegalArgumentException::class.java) { watermarkTextSize(10, 10, -5f) }
    }

    // ---- màu và độ đậm ----

    @Test
    fun strengthGrowsFromLightToStrong() {
        assertEquals(0.15f, WatermarkStrength.LIGHT.alpha, 0.0001f)
        assertEquals(0.3f, WatermarkStrength.MEDIUM.alpha, 0.0001f)
        assertEquals(0.45f, WatermarkStrength.STRONG.alpha, 0.0001f)
        assertTrue(WatermarkStrength.LIGHT.alpha < WatermarkStrength.MEDIUM.alpha)
        assertTrue(WatermarkStrength.MEDIUM.alpha < WatermarkStrength.STRONG.alpha)
    }

    @Test
    fun colorsAreDistinctPlainRgb() {
        // Độ trong được ghép vào 8 bit cao lúc vẽ, nên mã màu không được có sẵn bit đó.
        for (color in WatermarkColor.entries) {
            assertTrue(color.name, color.rgb in 0..0xFFFFFF)
        }
        assertEquals(3, WatermarkColor.entries.map { it.rgb }.toSet().size)
        assertEquals(0xC62828, WatermarkColor.RED.rgb)
    }
}
