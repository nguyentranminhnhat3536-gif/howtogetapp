package com.ethanstudio.snapsheet.util

import org.junit.Assert.assertEquals
import org.junit.Test

/** Tên file gửi cho dịch vụ in. Chỉ gọi hàm thuần, không đụng PrintManager. */
class PrintNameTest {

    @Test
    fun slashInNameBecomesUnderscore() {
        assertEquals("Hóa đơn 10_2026.pdf", printJobFileName("Hóa đơn 10/2026"))
    }

    @Test
    fun everyInvalidCharacterIsReplaced() {
        assertEquals("a_b_c_d_e_f_g_h_i_j.pdf", printJobFileName("a/b\\c:d*e?f\"g<h>i|j"))
    }

    @Test
    fun controlCharactersAreReplaced() {
        assertEquals("a_b_c.pdf", printJobFileName("a\tb\nc"))
    }

    @Test
    fun nameIsTrimmed() {
        assertEquals("Bill.pdf", printJobFileName("  Bill  "))
    }

    @Test
    fun blankNameFallsBackToDocument() {
        assertEquals("document.pdf", printJobFileName(""))
        assertEquals("document.pdf", printJobFileName("    "))
    }

    @Test
    fun longNameIsCutToEightyChars() {
        val result = printJobFileName("x".repeat(200))
        assertEquals("x".repeat(80) + ".pdf", result)
        assertEquals(80, result.removeSuffix(".pdf").length)
    }
}
