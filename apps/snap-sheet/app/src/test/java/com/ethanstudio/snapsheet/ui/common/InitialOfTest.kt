package com.ethanstudio.snapsheet.ui.common

import org.junit.Assert.assertEquals
import org.junit.Test

/** Chữ cái đầu hiện trong avatar: trim, lấy một ký tự hiển thị, viết hoa. */
class InitialOfTest {

    @Test
    fun plainNameGivesUppercaseFirstLetter() {
        assertEquals("A", initialOf("an"))
        assertEquals("Z", initialOf("Zoe"))
        assertEquals("1", initialOf("1abc"))
    }

    @Test
    fun surroundingSpacesAreIgnored() {
        assertEquals("B", initialOf("  bình  "))
        assertEquals("N", initialOf("\t\nnam"))
        // Dấu cách không ngắt dòng (NBSP) cũng là khoảng trắng với trim() của Kotlin.
        assertEquals("A", initialOf(" an"))
    }

    @Test
    fun emptyOrBlankNameGivesEmptyString() {
        assertEquals("", initialOf(""))
        assertEquals("", initialOf("   "))
        assertEquals("", initialOf("\t\n "))
        assertEquals("", initialOf(" 　"))
    }

    @Test
    fun vietnameseLettersKeepTheirMarks() {
        assertEquals("Á", initialOf("ánh"))
        assertEquals("Đ", initialOf("đức"))
        assertEquals("Ợ", initialOf("ợt"))
        assertEquals("Ă", initialOf("Ăn"))
    }

    @Test
    fun decomposedAccentStaysWithItsLetter() {
        // "e" + dấu sắc rời (U+0301): phải lấy cả hai, không cắt mất dấu.
        assertEquals("É", initialOf("ét"))
        // "ạ" + dấu mũ rời: một ký tự hiển thị gồm 2 mã.
        assertEquals("Ậ", initialOf("ận"))
    }

    @Test
    fun leadingEmojiIsKeptWhole() {
        // 😀 là cặp surrogate; không được cắt còn nửa ký tự.
        assertEquals("😀", initialOf("😀 Nam"))
    }

    @Test
    fun emojiSequencesAreKeptWhole() {
        val family = "👨‍👩‍👧" // 👨‍👩‍👧
        assertEquals(family, initialOf("$family Nhà"))

        val vietnamFlag = "🇻🇳" // 🇻🇳
        assertEquals(vietnamFlag, initialOf("$vietnamFlag VN"))

        val thumbsUpSkinTone = "👍🏽" // 👍🏽
        assertEquals(thumbsUpSkinTone, initialOf("${thumbsUpSkinTone}ok"))
    }

    @Test
    fun resultIsNeverHalfASurrogatePair() {
        val result = initialOf("😀")
        assertEquals(2, result.length)
        assertEquals(0x1F600, result.codePointAt(0))
    }
}
