package com.ethanstudio.snapsheet.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OcrTextTest {

    // --- pageText: chọn trang ---

    @Test
    fun allPagesSkipsBlankPagesAndJoinsWithEmptyLine() {
        assertEquals("A\n\nC", pageText(listOf("A", "", "C"), 0))
    }

    @Test
    fun allPagesSkipsWhitespaceOnlyPages() {
        assertEquals("A\n\nC", pageText(listOf("A", "  \n ", "C"), 0))
    }

    @Test
    fun singlePageReturnsExactlyThatPage() {
        assertEquals("B", pageText(listOf("A", "B"), 2))
        assertEquals("A", pageText(listOf("A", "B"), 1))
    }

    @Test
    fun pageOutOfRangeFallsBackToAllPages() {
        assertEquals("A\n\nB", pageText(listOf("A", "B"), 5))
        assertEquals("A\n\nB", pageText(listOf("A", "B"), -1))
    }

    @Test
    fun blankSinglePageStaysEmpty() {
        assertEquals("", pageText(listOf("A", ""), 2))
    }

    @Test
    fun noPagesGivesEmptyText() {
        assertEquals("", pageText(emptyList(), 0))
    }

    // --- joinLines: nối dòng ---

    @Test
    fun joinLinesKeepsParagraphBreaks() {
        assertEquals("one two\n\nthree four", joinLines("one\ntwo\n\nthree\nfour"))
    }

    @Test
    fun joinLinesCollapsesSpacesAroundBreakToOneSpace() {
        assertEquals("a b", joinLines("a  \n  b"))
        assertEquals("a b", joinLines("a\t\n\tb"))
    }

    @Test
    fun joinLinesOnEmptyText() {
        assertEquals("", joinLines(""))
    }

    @Test
    fun joinLinesKeepsTrailingAndLeadingBreak() {
        assertEquals("a\n", joinLines("a\n"))
        assertEquals("\na", joinLines("\na"))
    }

    @Test
    fun joinLinesLeavesSingleLineUntouched() {
        assertEquals("one line only", joinLines("one line only"))
    }

    // --- displayText ---

    @Test
    fun displayTextJoinedAppliesJoinLinesToSelectedPages() {
        val pages = listOf("one\ntwo", "", "three\nfour")
        assertEquals("one two\n\nthree four", displayText(pages, 0, joined = true))
        assertEquals("three four", displayText(pages, 3, joined = true))
    }

    @Test
    fun displayTextKeepsLineBreaksWhenNotJoined() {
        val pages = listOf("one\ntwo", "", "three\nfour")
        assertEquals("one\ntwo\n\nthree\nfour", displayText(pages, 0, joined = false))
        assertEquals("one\ntwo", displayText(pages, 1, joined = false))
    }

    // --- findMatches: tìm chữ ---

    @Test
    fun findMatchesIgnoresCase() {
        val matches = findMatches("Rent rent RENT", "rent")
        assertEquals(listOf(0..3, 5..8, 10..13), matches)
    }

    @Test
    fun findMatchesTrimsQuery() {
        assertEquals(listOf(4..7), findMatches("Pay rent now", "  rent "))
    }

    @Test
    fun findMatchesDoesNotOverlap() {
        assertEquals(listOf(0..1, 2..3), findMatches("aaaa", "aa"))
        assertEquals(listOf(0..1), findMatches("aaa", "aa"))
    }

    @Test
    fun blankQueryFindsNothing() {
        assertTrue(findMatches("some text", "  ").isEmpty())
        assertTrue(findMatches("some text", "").isEmpty())
    }

    @Test
    fun queryLongerThanTextFindsNothing() {
        assertTrue(findMatches("ab", "abc").isEmpty())
        assertTrue(findMatches("", "a").isEmpty())
    }

    @Test
    fun findMatchesWorksWithVietnamese() {
        assertEquals(listOf(0..1), findMatches("Đà Nẵng", "đà"))
        assertEquals(listOf(3..6), findMatches("Đà Nẵng", "NẴNG"))
    }

    @Test
    fun differentWordIsNotAMatch() {
        assertTrue(findMatches("lease", "rent").isEmpty())
    }

    // --- wordCount / charCount ---

    @Test
    fun wordCountOfEmptyOrBlankIsZero() {
        assertEquals(0, wordCount(""))
        assertEquals(0, wordCount("   \n "))
    }

    @Test
    fun wordCountSplitsOnAnyWhitespace() {
        assertEquals(3, wordCount("one  two\nthree"))
        assertEquals(2, wordCount("  leading\ttrailing  "))
    }

    @Test
    fun charCountCountsEmojiAsOne() {
        assertEquals(3, charCount("abc"))
        assertEquals(2, charCount("a😀"))
        assertEquals(0, charCount(""))
    }

    // --- txtFileName ---

    @Test
    fun fileNameReplacesSlash() {
        assertEquals("Lease 1_2.txt", txtFileName("Lease 1/2"))
    }

    @Test
    fun fileNameReplacesAllForbiddenCharacters() {
        val name = txtFileName("a\\b/c:d*e?f\"g<h>i|j\nk\tl")
        assertEquals("a_b_c_d_e_f_g_h_i_j_k_l.txt", name)
        assertFalse(name.contains("/"))
        assertFalse(name.contains(":"))
    }

    @Test
    fun blankNameUsesFallback() {
        assertEquals("SnapSheet.txt", txtFileName("   "))
        assertEquals("SnapSheet.txt", txtFileName(""))
        assertEquals("Doc.txt", txtFileName("", fallback = "Doc"))
    }

    @Test
    fun fileNameIsTrimmed() {
        assertEquals("Receipt.txt", txtFileName("  Receipt  "))
    }

    @Test
    fun longNameIsCutTo60CharactersPlusExtension() {
        val name = txtFileName("x".repeat(200))
        assertTrue(name, name.length <= 64)
        assertTrue(name, name.endsWith(".txt"))
        assertEquals("x".repeat(60) + ".txt", name)
    }
}
