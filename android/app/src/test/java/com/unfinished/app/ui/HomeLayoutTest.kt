package com.unfinished.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeLayoutTest {
    @Test
    fun heroGapTracksWindowHeightAndClampsAtResponsiveBounds() {
        assertEquals(28f, homeHeroTopGapDp(320f), 0.001f)
        assertEquals(34.08f, homeHeroTopGapDp(568f), 0.001f)
        assertEquals(42f, homeHeroTopGapDp(700f), 0.001f)
        assertEquals(51.06f, homeHeroTopGapDp(851f), 0.001f)
        assertEquals(58f, homeHeroTopGapDp(1024f), 0.001f)
    }

    @Test
    fun glassHomeActionsKeepTintLightAndAccessibleFallbackOpaque() {
        val dayPrimary = glassActionSurfaceColor(
            isNight = false,
            reduceTransparency = false,
            isPrimary = true
        ).alpha
        val daySecondary = glassActionSurfaceColor(
            isNight = false,
            reduceTransparency = false,
            isPrimary = false
        ).alpha
        val nightPrimary = glassActionSurfaceColor(
            isNight = true,
            reduceTransparency = false,
            isPrimary = true
        ).alpha
        val nightSecondary = glassActionSurfaceColor(
            isNight = true,
            reduceTransparency = false,
            isPrimary = false
        ).alpha
        val reducedTransparency = glassActionSurfaceColor(
            isNight = false,
            reduceTransparency = true,
            isPrimary = true
        ).alpha

        assertEquals(dayPrimary, daySecondary, 0.001f)
        assertEquals(nightPrimary, nightSecondary, 0.001f)
        assertTrue("Day glass tint should not veil the shoreline", dayPrimary <= 0.15f)
        assertTrue("Night glass tint should not veil the shoreline", nightPrimary <= 0.15f)
        assertTrue("Reduced-transparency mode should retain an opaque surface", reducedTransparency >= 0.98f)
    }

    @Test
    fun glassActionRefractionIsVisibleAtRestAndDeepensGentlyOnPress() {
        val restingLens = glassActionRefractionProfile(pressProgress = 0f)
        val pressedLens = glassActionRefractionProfile(pressProgress = 1f)

        assertEquals(16f, restingLens.heightDp, 0.001f)
        assertEquals(30f, restingLens.amountDp, 0.001f)
        assertTrue("Home buttons should use the lens depth effect", restingLens.depthEffect)
        assertEquals(19f, pressedLens.heightDp, 0.001f)
        assertEquals(36f, pressedLens.amountDp, 0.001f)
    }
}
