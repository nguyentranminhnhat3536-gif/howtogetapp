package com.ethanstudio.snapsheet.data

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DocListTest {
    private val vn = ZoneOffset.ofHours(7)

    private fun vnMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        OffsetDateTime.of(year, month, day, hour, minute, 0, 0, vn).toInstant().toEpochMilli()

    private val now = vnMillis(2026, 10, 1, 10, 0)

    // --- sortDocs NEWEST ---

    @Test
    fun newestPutsLatestFirst() {
        val docs = listOf(Doc(1, "a", 100, 1), Doc(2, "b", 300, 1), Doc(3, "c", 200, 1))
        assertEquals(listOf(2L, 3L, 1L), sortDocs(docs, DocSort.NEWEST).map { it.id })
    }

    @Test
    fun newestBreaksTiesByBiggerIdFirst() {
        val docs = listOf(Doc(4, "a", 500, 1), Doc(9, "b", 500, 1), Doc(1, "c", 900, 1))
        assertEquals(listOf(1L, 9L, 4L), sortDocs(docs, DocSort.NEWEST).map { it.id })
    }

    // --- sortDocs NAME ---

    @Test
    fun nameSortFollowsVietnameseAlphabet() {
        // "ăn", "bảng", "Bình" viết bằng mã Unicode để chắc chắn là ký tự dựng sẵn.
        val an = "An"
        val anBreve = "ăn"
        val bang = "bảng"
        val binh = "Bình"
        val docs = listOf(Doc(1, bang, 1, 1), Doc(2, an, 2, 1), Doc(3, binh, 3, 1), Doc(4, anBreve, 4, 1))
        val sorted = sortDocs(docs, DocSort.NAME, Locale.forLanguageTag("vi")).map { it.name }
        assertEquals(listOf(an, anBreve, bang, binh), sorted)
    }

    @Test
    fun nameSortIgnoresCaseAndTiesGoNewestFirst() {
        val docs = listOf(Doc(1, "an", 100, 1), Doc(2, "Zebra", 50, 1), Doc(3, "An", 200, 1))
        val sorted = sortDocs(docs, DocSort.NAME, Locale.US).map { it.id }
        // "an" và "An" coi như trùng tên → bản tạo sau (id 3) đứng trước, "Zebra" cuối.
        assertEquals(listOf(3L, 1L, 2L), sorted)
    }

    @Test
    fun sortingEmptyListGivesEmptyList() {
        assertTrue(sortDocs(emptyList(), DocSort.NEWEST).isEmpty())
        assertTrue(sortDocs(emptyList(), DocSort.NAME, Locale.US).isEmpty())
    }

    // --- splitToday ---

    @Test
    fun earlyMorningTodayCountsAsToday() {
        val doc = Doc(1, "a", vnMillis(2026, 10, 1, 0, 30), 1)
        val (today, earlier) = splitToday(listOf(doc), now, vn)
        assertEquals(listOf(doc), today)
        assertTrue(earlier.isEmpty())
    }

    @Test
    fun lastMinuteOfYesterdayCountsAsEarlier() {
        val doc = Doc(1, "a", vnMillis(2026, 9, 30, 23, 59), 1)
        val (today, earlier) = splitToday(listOf(doc), now, vn)
        assertTrue(today.isEmpty())
        assertEquals(listOf(doc), earlier)
    }

    @Test
    fun futureDateCountsAsToday() {
        val doc = Doc(1, "a", vnMillis(2026, 10, 3, 8, 0), 1)
        val (today, earlier) = splitToday(listOf(doc), now, vn)
        assertEquals(listOf(doc), today)
        assertTrue(earlier.isEmpty())
    }

    @Test
    fun splitUsesTheGivenTimeZone() {
        // 00:30 ngày 1/10 giờ VN = 17:30 ngày 30/9 giờ UTC → ở UTC phải là "trước đó".
        val doc = Doc(1, "a", vnMillis(2026, 10, 1, 0, 30), 1)
        val (today, earlier) = splitToday(listOf(doc), now, ZoneOffset.UTC)
        assertTrue(today.isEmpty())
        assertEquals(listOf(doc), earlier)
    }

    @Test
    fun splitKeepsOrderInsideEachGroup() {
        val t1 = Doc(1, "t1", vnMillis(2026, 10, 1, 9, 0), 1)
        val e1 = Doc(2, "e1", vnMillis(2026, 9, 20, 9, 0), 1)
        val t2 = Doc(3, "t2", vnMillis(2026, 10, 1, 1, 0), 1)
        val e2 = Doc(4, "e2", vnMillis(2026, 9, 30, 12, 0), 1)
        val (today, earlier) = splitToday(listOf(t1, e1, t2, e2), now, vn)
        assertEquals(listOf(t1, t2), today)
        assertEquals(listOf(e1, e2), earlier)
    }

    @Test
    fun splitEmptyListGivesTwoEmptyLists() {
        val (today, earlier) = splitToday(emptyList(), now, vn)
        assertTrue(today.isEmpty())
        assertTrue(earlier.isEmpty())
    }
}
