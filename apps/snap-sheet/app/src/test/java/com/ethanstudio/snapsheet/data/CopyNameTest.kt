package com.ethanstudio.snapsheet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tên bản sao khi ký tên / chèn chữ mờ: "<tên gốc> (signed)", tối đa 80 ký tự. */
class CopyNameTest {
    private val signed = "%1\$s (signed)"

    @Test
    fun englishSignedCopy() {
        assertEquals("Invoice (signed)", copyName("Invoice", signed))
    }

    @Test
    fun vietnameseSignedCopy() {
        assertEquals("Hợp đồng (đã ký)", copyName("Hợp đồng", "%1\$s (đã ký)"))
    }

    @Test
    fun japaneseTemplateWithoutSpace() {
        assertEquals("契約書（署名済み）", copyName("契約書", "%1\$s（署名済み）"))
    }

    @Test
    fun watermarkCopyOfDefaultName() {
        assertEquals(
            "Scan 2026-10-01 09.37 (watermark)",
            copyName("Scan 2026-10-01 09.37", "%1\$s (watermark)"),
        )
    }

    @Test
    fun spacesAroundOriginalNameAreTrimmed() {
        assertEquals("Bill (signed)", copyName("  Bill  ", signed))
    }

    @Test
    fun longNameIsCutSoWholeNameFits80() {
        val name = copyName("x".repeat(100), signed)
        assertEquals(80, name.length)
        assertTrue(name, name.endsWith(" (signed)"))
        assertTrue(name, name.startsWith("x".repeat(71)))
    }

    @Test
    fun nameThatJustFitsIsNotCut() {
        // 71 + " (signed)" (9) = 80.
        assertEquals("y".repeat(71) + " (signed)", copyName("y".repeat(71), signed))
    }

    @Test
    fun cutNeverLeavesDoubleSpace() {
        // Cắt ở 71 ký tự rơi đúng vào dấu cách → bỏ dấu cách đó.
        assertEquals("a".repeat(70) + " (signed)", copyName("a".repeat(70) + " bcd", signed))
    }

    @Test
    fun customMaxLength() {
        assertEquals("Inv (signed)", copyName("Invoice", signed, max = 12))
    }

    @Test
    fun veryLongTemplateStillKeepsOneCharOfName() {
        val template = "%1\$s" + "z".repeat(100)
        assertEquals("I" + "z".repeat(100), copyName("Invoice", template))
    }

    @Test
    fun templateWithoutNameSlotIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { copyName("Invoice", "signed") }
        assertThrows(IllegalArgumentException::class.java) { copyName("Invoice", "%s (signed)") }
    }
}
