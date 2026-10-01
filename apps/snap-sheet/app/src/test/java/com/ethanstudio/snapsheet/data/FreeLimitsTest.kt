package com.ethanstudio.snapsheet.data

import org.junit.Assert.assertEquals
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
}
