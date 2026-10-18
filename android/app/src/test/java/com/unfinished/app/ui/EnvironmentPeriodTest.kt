package com.unfinished.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class EnvironmentPeriodTest {
    @Test
    fun localPeriodBoundariesAreHalfOpen() {
        assertEquals(EnvironmentPeriod.NIGHT, environmentPeriodAt(5, 59))
        assertEquals(EnvironmentPeriod.DAYLIGHT, environmentPeriodAt(6, 0))
        assertEquals(EnvironmentPeriod.DAYLIGHT, environmentPeriodAt(13, 59))
        assertEquals(EnvironmentPeriod.SLANT, environmentPeriodAt(14, 0))
        assertEquals(EnvironmentPeriod.SLANT, environmentPeriodAt(17, 59))
        assertEquals(EnvironmentPeriod.NIGHT, environmentPeriodAt(18, 0))
        assertEquals(EnvironmentPeriod.NIGHT, environmentPeriodAt(23, 59))
    }

    @Test
    fun forcedNightOverridesLocalPeriod() {
        assertEquals(EnvironmentPeriod.NIGHT, environmentPeriodAt(9, 30, forceNight = true))
        assertEquals(EnvironmentPeriod.NIGHT, environmentPeriodAt(16, 0, forceNight = true))
    }

    @Test
    fun shortWindowsAndLargeFontsSelectScrollableLayout() {
        assertEquals(true, shouldUseScrollableLayout(699f, 1f))
        assertEquals(false, shouldUseScrollableLayout(700f, 1f))
        assertEquals(true, shouldUseScrollableLayout(820f, 1.25f))
        assertEquals(false, shouldUseScrollableLayout(820f, 1.24f))
    }
}
