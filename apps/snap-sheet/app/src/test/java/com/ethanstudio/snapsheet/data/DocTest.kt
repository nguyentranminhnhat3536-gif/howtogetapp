package com.ethanstudio.snapsheet.data

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocTest {
    private val docs = listOf(
        Doc(1, "Hợp đồng thuê nhà", 3, 2),
        Doc(2, "Scan 2026-10-01 09.37", 2, 1),
        Doc(3, "Invoice October", 1, 4),
    )

    @Test
    fun emptyQueryKeepsEverything() {
        assertEquals(docs, filterDocs(docs, "  "))
    }

    @Test
    fun filterIgnoresCaseAndSpaces() {
        assertEquals(listOf(docs[2]), filterDocs(docs, " invoice "))
        assertEquals(listOf(docs[1]), filterDocs(docs, "2026-10"))
    }

    @Test
    fun filterMatchesVietnameseText() {
        assertEquals(listOf(docs[0]), filterDocs(docs, "thuê"))
    }

    @Test
    fun defaultNameUsesPrefixAndLocalTime() {
        // 2026-10-01 02:37:00 UTC
        val millis = 1_790_822_220_000L
        assertEquals("Scan 2026-10-01 02.37", defaultDocName("Scan", millis, ZoneOffset.UTC))
        assertEquals("Quét 2026-10-01 09.37", defaultDocName("Quét", millis, ZoneOffset.ofHours(7)))
    }

    @Test
    fun cleanNameTrimsAndLimitsLength() {
        assertEquals("Bill", cleanDocName("  Bill  "))
        assertEquals(80, cleanDocName("x".repeat(200))?.length)
        assertNull(cleanDocName("   "))
    }

    @Test
    fun sampleSizeShrinksByPowersOfTwo() {
        assertEquals(1, sampleSizeFor(1500, 2000))
        assertEquals(1, sampleSizeFor(3000, 2000))
        assertEquals(2, sampleSizeFor(4000, 2000))
        assertEquals(4, sampleSizeFor(9000, 2000))
    }

    @Test
    fun exifOrientationMapsToDegrees() {
        assertEquals(0, exifDegrees(1))
        assertEquals(90, exifDegrees(6))
        assertEquals(180, exifDegrees(3))
        assertEquals(270, exifDegrees(8))
        assertEquals(0, exifDegrees(0))
    }
}
