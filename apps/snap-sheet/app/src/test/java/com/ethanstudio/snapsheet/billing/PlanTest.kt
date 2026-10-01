package com.ethanstudio.snapsheet.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import java.util.Locale
import org.junit.Test

class PlanTest {
    @Test
    fun monthlyAndYearlyShareOneSubscriptionProduct() {
        assertEquals(Plan.MONTHLY.productId, Plan.YEARLY.productId)
        assertEquals("monthly", Plan.MONTHLY.basePlanId)
        assertEquals("yearly", Plan.YEARLY.basePlanId)
        assertFalse(Plan.LIFETIME.isSubscription)
    }

    @Test
    fun noPurchasesMeansNotPro() {
        assertNull(activeKind(emptyList()))
    }

    @Test
    fun pendingPurchaseDoesNotUnlockPro() {
        assertNull(activeKind(listOf(PurchaseInfo(listOf(SUBSCRIPTION_ID), purchased = false))))
    }

    @Test
    fun subscriptionUnlocksPro() {
        assertEquals(ProKind.SUBSCRIPTION, activeKind(listOf(PurchaseInfo(listOf(SUBSCRIPTION_ID), true))))
    }

    @Test
    fun lifetimeWinsOverSubscription() {
        val purchases = listOf(PurchaseInfo(listOf(SUBSCRIPTION_ID), true), PurchaseInfo(listOf(LIFETIME_ID), true))
        assertEquals(ProKind.LIFETIME, activeKind(purchases))
    }

    @Test
    fun unknownProductsAreIgnored() {
        assertNull(activeKind(listOf(PurchaseInfo(listOf("something_else"), true))))
    }

    private val base = OfferInfo(null, "t1", listOf(PhaseInfo(9_990_000, "$9.99")))
    private val trial = OfferInfo("trial", "t2", listOf(PhaseInfo(0, "Free"), PhaseInfo(9_990_000, "$9.99")))

    @Test
    fun pickOfferPrefersFreeTrial() {
        assertEquals(trial, pickOffer(listOf(base, trial)))
        assertTrue(trial.hasFreeTrial)
        assertEquals("$9.99", trial.recurring.formattedPrice)
    }

    @Test
    fun pickOfferFallsBackToBasePlan() {
        assertEquals(base, pickOffer(listOf(base)))
        assertFalse(base.hasFreeTrial)
        assertNull(pickOffer(emptyList()))
    }

    @Test
    fun yearlySavingIsComparedWithTwelveMonths() {
        assertEquals(50, yearlySavingPercent(1_000_000, 6_000_000))
        assertEquals(33, yearlySavingPercent(1_000_000, 8_000_000))
        assertNull(yearlySavingPercent(1_000_000, 12_000_000))
        assertNull(yearlySavingPercent(0, 5_000_000))
    }

    @Test
    fun periodDaysReadsGooglePlayPeriods() {
        assertEquals(7, periodDays("P7D"))
        assertEquals(7, periodDays("P1W"))
        assertEquals(30, periodDays("P1M"))
        assertNull(periodDays(""))
        assertNull(periodDays("7 days"))
    }

    @Test
    fun trialDaysComeFromTheFreePhase() {
        val sevenDay = OfferInfo("trial7", "t", listOf(PhaseInfo(0, "Free", "P7D"), PhaseInfo(990_000, "$0.99", "P1M")))
        assertEquals(7, sevenDay.trialDays)
        assertNull(OfferInfo(null, "t", listOf(PhaseInfo(990_000, "$0.99", "P1M"))).trialDays)
    }

    // --- formatPerMonth (giá gói năm quy ra mỗi tháng) ---

    @Test
    fun perMonthPriceInDollars() {
        // 9.99 / 12 = 0.8325 → làm tròn 0.83
        assertEquals("$0.83", formatPerMonth(9_990_000, "USD", Locale.US))
    }

    @Test
    fun perMonthPriceRoundsHalfUp() {
        // 29.99 / 12 = 2.49916… → 2.50
        assertEquals("$2.50", formatPerMonth(29_990_000, "USD", Locale.US))
    }

    @Test
    fun perMonthPriceForCurrencyWithoutDecimals() {
        val text = formatPerMonth(1_200_000_000, "JPY", Locale.US)
        assertTrue(text, text!!.contains("100"))
        assertFalse(text, text.contains("."))
    }

    @Test
    fun perMonthPriceFollowsLocaleFormat() {
        // 11.99 EUR / 12 = 0.99916… → 1,00 (dấu phẩy thập phân kiểu Đức)
        val text = formatPerMonth(11_990_000, "EUR", Locale.GERMANY)
        assertTrue(text, text!!.contains("1,00"))
    }

    @Test
    fun perMonthPriceIsNullForZeroOrNegativePrice() {
        assertNull(formatPerMonth(0, "USD", Locale.US))
        assertNull(formatPerMonth(-1_000_000, "USD", Locale.US))
    }

    @Test
    fun perMonthPriceIsNullForUnknownOrMissingCurrency() {
        assertNull(formatPerMonth(9_990_000, "ZZZ", Locale.US))
        assertNull(formatPerMonth(9_990_000, "", Locale.US))
    }

    @Test
    fun phaseInfoCurrencyDefaultsToEmpty() {
        assertEquals("", PhaseInfo(990_000, "$0.99", "P1M").currencyCode)
    }
}
