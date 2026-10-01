package com.ethanstudio.lunartasks.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {
    @Test
    fun textSizeStepsUpAndDownAndStopsAtEnds() {
        assertEquals(1.15f, AppSettings.nextScale(1f, +1))
        assertEquals(1f, AppSettings.nextScale(1f, -1))
        assertEquals(2f, AppSettings.nextScale(2f, +1))
        assertEquals(1.5f, AppSettings.nextScale(1.75f, -1))
    }

    @Test
    fun oldLargeTextValueSnapsToNearestLevel() {
        assertEquals(1.5f, AppSettings.nextScale(1.3f, +1))
    }
}
