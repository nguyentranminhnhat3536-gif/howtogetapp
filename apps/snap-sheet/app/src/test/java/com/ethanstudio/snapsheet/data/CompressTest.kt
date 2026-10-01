package com.ethanstudio.snapsheet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CompressTest {

    @Test
    fun landscapePhotoShrinksToLongSide() {
        assertEquals(1200 to 900, scaledSize(4000, 3000, 1200))
    }

    @Test
    fun portraitPhotoShrinksToLongSide() {
        assertEquals(900 to 1800, scaledSize(1500, 3000, 1800))
    }

    @Test
    fun smallPhotoIsNeverEnlarged() {
        assertEquals(800 to 600, scaledSize(800, 600, 1200))
        assertEquals(1200 to 900, scaledSize(1200, 900, 1200))
    }

    @Test
    fun veryThinPhotoKeepsAtLeastOnePixel() {
        assertEquals(1200 to 1, scaledSize(10_000, 3, 1200))
        assertEquals(1 to 1800, scaledSize(2, 50_000, 1800))
    }

    @Test
    fun invalidSizeIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { scaledSize(0, 10, 1200) }
        assertThrows(IllegalArgumentException::class.java) { scaledSize(10, -5, 1200) }
        assertThrows(IllegalArgumentException::class.java) { scaledSize(10, 10, 0) }
    }

    @Test
    fun shareOriginalWhenCompressedIsNotSmaller() {
        assertTrue(shouldShareOriginal(100, 100))
        assertTrue(shouldShareOriginal(100, 150))
        assertFalse(shouldShareOriginal(100, 99))
    }

    @Test
    fun smallLevelIsSmallerThanMedium() {
        assertEquals(1200, CompressLevel.SMALL.maxSide)
        assertEquals(60, CompressLevel.SMALL.quality)
        assertEquals(1800, CompressLevel.MEDIUM.maxSide)
        assertEquals(75, CompressLevel.MEDIUM.quality)
    }
}
