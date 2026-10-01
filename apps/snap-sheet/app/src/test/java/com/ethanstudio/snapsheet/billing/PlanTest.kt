package com.ethanstudio.snapsheet.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
}
