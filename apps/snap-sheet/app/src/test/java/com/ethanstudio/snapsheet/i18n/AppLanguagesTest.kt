package com.ethanstudio.snapsheet.i18n

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguagesTest {
    @Test
    fun tagsHaveTwelveUniqueLanguages() {
        assertEquals(12, AppLanguages.TAGS.size)
        assertEquals(12, AppLanguages.TAGS.toSet().size)
        assertTrue("pt-BR" in AppLanguages.TAGS)
        assertTrue("id" in AppLanguages.TAGS)
    }

    @Test
    fun normalizeKeepsExactTags() {
        assertEquals("vi", AppLanguages.normalize("vi"))
        assertEquals("en", AppLanguages.normalize("EN"))
        for (tag in AppLanguages.TAGS) assertEquals(tag, AppLanguages.normalize(tag))
    }

    @Test
    fun normalizeFixesCaseUnderscoreAndSpaces() {
        assertEquals("pt-BR", AppLanguages.normalize("PT-br"))
        assertEquals("pt-BR", AppLanguages.normalize("pt_BR"))
        assertEquals("ja", AppLanguages.normalize("  ja "))
    }

    @Test
    fun normalizeMapsOldIndonesianCode() {
        assertEquals("id", AppLanguages.normalize("in"))
        assertEquals("id", AppLanguages.normalize("in-ID"))
    }

    @Test
    fun normalizeFallsBackToLanguageWithoutRegion() {
        assertEquals("en", AppLanguages.normalize("en-US"))
        assertEquals("vi", AppLanguages.normalize("vi_VN"))
        assertEquals("fr", AppLanguages.normalize("fr-CA"))
    }

    @Test
    fun normalizeReturnsSystemDefaultForUnknownOrBrokenTags() {
        assertEquals("", AppLanguages.normalize(""))
        assertEquals("", AppLanguages.normalize("   "))
        assertEquals("", AppLanguages.normalize("xx"))
        assertEquals("", AppLanguages.normalize("pt-PT"))
        assertEquals("", AppLanguages.normalize("pt"))
        assertEquals("", AppLanguages.normalize("zh-CN"))
        assertEquals("", AppLanguages.normalize("-"))
    }

    @Test
    fun bundledSerifOnlyForLatinLanguagesWithoutVietnameseMarks() {
        for (tag in listOf("en", "pt-BR", "in", "id", "tr", "es", "fr", "de", "en-US", "de_DE")) {
            assertTrue(tag, AppLanguages.usesBundledSerif(tag))
        }
    }

    @Test
    fun systemSerifForVietnameseAndNonLatinScripts() {
        for (tag in listOf("vi", "vi-VN", "ru", "ja", "ko", "hi", "", "xx")) {
            assertFalse(tag, AppLanguages.usesBundledSerif(tag))
        }
    }
}
