package com.ethanstudio.snapsheet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeLimitsTest {
    private val today = 20_000L

    @Test
    fun freshUserHasAllExportsLeft() {
        assertEquals(FreeLimits.DAILY_EXPORTS, FreeLimits.remaining(false, FreeLimits.Usage(), today))
    }

    @Test
    fun consumingReducesRemainingUntilZero() {
        var usage = FreeLimits.Usage()
        repeat(FreeLimits.DAILY_EXPORTS) { usage = FreeLimits.consume(usage, today) }
        assertEquals(0, FreeLimits.remaining(false, usage, today))
    }

    @Test
    fun remainingNeverGoesNegative() {
        val usage = FreeLimits.Usage(today, FreeLimits.DAILY_EXPORTS + 5)
        assertEquals(0, FreeLimits.remaining(false, usage, today))
    }

    @Test
    fun newDayResetsTheCount() {
        val yesterday = FreeLimits.Usage(today - 1, FreeLimits.DAILY_EXPORTS)
        assertEquals(FreeLimits.DAILY_EXPORTS, FreeLimits.remaining(false, yesterday, today))
        assertEquals(FreeLimits.Usage(today, 1), FreeLimits.consume(yesterday, today))
    }

    @Test
    fun proIsUnlimitedAndGetsMorePages() {
        val spent = FreeLimits.Usage(today, 99)
        assertEquals(Int.MAX_VALUE, FreeLimits.remaining(true, spent, today))
        assertEquals(FreeLimits.PRO_PAGES, FreeLimits.pageLimit(true))
        assertEquals(FreeLimits.FREE_PAGES, FreeLimits.pageLimit(false))
    }

    // --- Thêm trang vào tài liệu cũ ---

    @Test
    fun freeUserCanAddUpToFivePagesInTotal() {
        assertEquals(2, FreeLimits.pagesCanAdd(false, 3))
        assertEquals(5, FreeLimits.pagesCanAdd(false, 0))
        assertEquals(0, FreeLimits.pagesCanAdd(false, 5))
    }

    @Test
    fun pagesCanAddNeverGoesNegative() {
        // Tài liệu 8 trang tạo lúc còn Pro, nay hết Pro: không thêm được nhưng không âm.
        assertEquals(0, FreeLimits.pagesCanAdd(false, 8))
        assertEquals(0, FreeLimits.pagesCanAdd(true, 31))
    }

    @Test
    fun proUserCanAddUpToThirtyPagesInTotal() {
        assertEquals(1, FreeLimits.pagesCanAdd(true, 29))
        assertEquals(0, FreeLimits.pagesCanAdd(true, 30))
        assertEquals(30, FreeLimits.pagesCanAdd(true, 0))
    }

    // --- Xuất nhiều file một lần ---

    @Test
    fun canConsumeChecksEnoughExportsLeft() {
        val usedOne = FreeLimits.Usage(today, 1)
        assertTrue(FreeLimits.canConsume(false, usedOne, today, 2))
        assertFalse(FreeLimits.canConsume(false, usedOne, today, 3))
    }

    @Test
    fun canConsumeFailsWhenNoExportsLeft() {
        val spent = FreeLimits.Usage(today, FreeLimits.DAILY_EXPORTS)
        assertFalse(FreeLimits.canConsume(false, spent, today, 1))
    }

    @Test
    fun canConsumeAlwaysTrueForProOrNothingToExport() {
        val spent = FreeLimits.Usage(today, 99)
        assertTrue(FreeLimits.canConsume(true, spent, today, 10))
        assertTrue(FreeLimits.canConsume(false, spent, today, 0))
    }

    @Test
    fun canConsumeCountsAgainOnNewDay() {
        val yesterday = FreeLimits.Usage(today - 1, FreeLimits.DAILY_EXPORTS)
        assertTrue(FreeLimits.canConsume(false, yesterday, today, 3))
        assertFalse(FreeLimits.canConsume(false, yesterday, today, 4))
    }

    @Test
    fun consumeManyAddsOnSameDayAndRestartsOnNewDay() {
        assertEquals(FreeLimits.Usage(today, 3), FreeLimits.consumeMany(FreeLimits.Usage(today, 1), today, 2))
        assertEquals(FreeLimits.Usage(today, 2), FreeLimits.consumeMany(FreeLimits.Usage(today - 1, 3), today, 2))
    }

    @Test
    fun consumingAllAtOnceLeavesNothing() {
        val usage = FreeLimits.consumeMany(FreeLimits.Usage(), today, FreeLimits.DAILY_EXPORTS)
        assertEquals(0, FreeLimits.remaining(false, usage, today))
        assertFalse(FreeLimits.canConsume(false, usage, today, 1))
    }
}
