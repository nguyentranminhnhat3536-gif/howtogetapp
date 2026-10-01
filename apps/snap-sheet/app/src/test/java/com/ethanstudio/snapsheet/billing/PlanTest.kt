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

    // --- saleOf (giảm giá gói Vĩnh viễn) ---

    private fun lifetime(
        price: Long,
        full: Long? = null,
        percent: Int? = null,
        token: String = "tok",
    ) = OneTimeOfferInfo(token, price, "price", "USD", full, percent)

    @Test
    fun saleOfComputesPercentWhenPlayGivesNone() {
        // (100 - 90) * 100 / 100 = 10
        assertEquals(Sale(100_000_000, 10), saleOf(lifetime(90_000_000, 100_000_000)))
    }

    @Test
    fun saleOfComputedPercentRoundsDown() {
        // (100 - 89.99) * 100 / 100 = 10.01 → 10; (100 - 80.01) → 19.99 → 19
        assertEquals(10, saleOf(lifetime(89_990_000, 100_000_000))!!.percent)
        assertEquals(19, saleOf(lifetime(80_010_000, 100_000_000))!!.percent)
    }

    @Test
    fun saleOfPrefersPercentFromPlay() {
        // Tự tính ra 15, nhưng Play báo 10 thì dùng 10
        assertEquals(Sale(100_000_000, 10), saleOf(lifetime(85_000_000, 100_000_000, percent = 10)))
        assertEquals(10, saleOf(lifetime(89_990_000, 100_000_000, percent = 10))!!.percent)
    }

    @Test
    fun saleOfIgnoresPlayPercentOutsideRange() {
        // 0 hoặc 150 là số vô lý → tự tính lại = 10
        assertEquals(10, saleOf(lifetime(90_000_000, 100_000_000, percent = 0))!!.percent)
        assertEquals(10, saleOf(lifetime(90_000_000, 100_000_000, percent = 150))!!.percent)
    }

    @Test
    fun noFullPriceMeansNoSale() {
        assertNull(saleOf(lifetime(90_000_000)))
        assertNull(saleOf(lifetime(90_000_000, percent = 10)))
    }

    @Test
    fun fullPriceEqualToPriceIsNotASale() {
        assertNull(saleOf(lifetime(100_000_000, 100_000_000)))
        assertNull(saleOf(lifetime(100_000_000, 100_000_000, percent = 10)))
    }

    @Test
    fun fullPriceBelowPriceIsNotASale() {
        assertNull(saleOf(lifetime(100_000_000, 90_000_000)))
    }

    @Test
    fun zeroPriceIsNotASale() {
        assertNull(saleOf(lifetime(0, 100_000_000)))
    }

    @Test
    fun tinyDiscountUnderOnePercentIsNotASale() {
        // (100 - 99.5) * 100 / 100 = 0.5 → 0 → không tính là giảm giá
        assertNull(saleOf(lifetime(99_500_000, 100_000_000)))
    }

    // --- pickOneTimeOffer ---

    @Test
    fun pickOneTimeOfferTakesCheapest() {
        val regular = lifetime(100_000_000, token = "regular")
        val sale = lifetime(90_000_000, 100_000_000, 10, token = "sale")
        assertEquals("sale", pickOneTimeOffer(listOf(regular, sale))!!.token)
        assertEquals("sale", pickOneTimeOffer(listOf(sale, regular))!!.token)
    }

    @Test
    fun pickOneTimeOfferPrefersSaleOnTie() {
        val plain = lifetime(90_000_000, token = "plain")
        val sale = lifetime(90_000_000, 100_000_000, 10, token = "sale")
        assertEquals("sale", pickOneTimeOffer(listOf(plain, sale))!!.token)
        assertEquals("sale", pickOneTimeOffer(listOf(sale, plain))!!.token)
    }

    @Test
    fun pickOneTimeOfferSkipsFreeOrBrokenOffers() {
        val free = lifetime(0, token = "free")
        val real = lifetime(100_000_000, token = "real")
        assertEquals("real", pickOneTimeOffer(listOf(free, real))!!.token)
        assertNull(pickOneTimeOffer(listOf(free)))
        assertNull(pickOneTimeOffer(listOf(lifetime(-1, token = "neg"))))
    }

    @Test
    fun pickOneTimeOfferOfEmptyListIsNull() {
        assertNull(pickOneTimeOffer(emptyList()))
    }

    // --- formatMicros (giá gạch ngang) ---

    @Test
    fun formatMicrosInDollars() {
        assertEquals("$100.00", formatMicros(100_000_000, "USD", Locale.US))
        assertEquals("$90.00", formatMicros(90_000_000, "USD", Locale.US))
    }

    @Test
    fun formatMicrosForCurrencyWithoutDecimals() {
        val text = formatMicros(1_000_000_000, "JPY", Locale.US)
        assertTrue(text, text!!.contains("1,000"))
        assertFalse(text, text.contains("."))
    }

    @Test
    fun formatMicrosIsNullForUnknownCurrency() {
        assertNull(formatMicros(100_000_000, "ZZZ", Locale.US))
        assertNull(formatMicros(100_000_000, "", Locale.US))
    }

    @Test
    fun formatMicrosIsNullForZeroOrNegative() {
        assertNull(formatMicros(0, "USD", Locale.US))
        assertNull(formatMicros(-100_000_000, "USD", Locale.US))
    }
}
