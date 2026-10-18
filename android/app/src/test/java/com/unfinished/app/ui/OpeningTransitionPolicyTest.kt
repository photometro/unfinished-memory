package com.unfinished.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class OpeningTransitionPolicyTest {
    @Test
    fun readyOpeningAutomaticallyBeginsWithoutWaitingForATap() {
        assertEquals(
            OpeningTransitionAction.HoldThenAnimate,
            openingTransitionAction(
                leaving = false,
                assets = OpeningAssetAvailability.Ready,
                assetWaitExpired = false,
                reducedMotion = false
            )
        )
    }

    @Test
    fun pendingAssetsWaitBrieflyThenFailOpenToHome() {
        assertEquals(
            OpeningTransitionAction.WaitForAssets,
            openingTransitionAction(
                leaving = false,
                assets = OpeningAssetAvailability.Loading,
                assetWaitExpired = false,
                reducedMotion = false
            )
        )
        assertEquals(
            OpeningTransitionAction.EnterHome,
            openingTransitionAction(
                leaving = false,
                assets = OpeningAssetAvailability.Loading,
                assetWaitExpired = true,
                reducedMotion = false
            )
        )
    }

    @Test
    fun reducedMotionAndSkipShareTheSameHomeTerminalState() {
        assertEquals(
            OpeningTransitionAction.EnterHome,
            openingTransitionAction(
                leaving = false,
                assets = OpeningAssetAvailability.Ready,
                assetWaitExpired = false,
                reducedMotion = true
            )
        )
        assertEquals(
            OpeningTransitionAction.EnterHome,
            openingTransitionAction(
                leaving = true,
                assets = OpeningAssetAvailability.Ready,
                assetWaitExpired = false,
                reducedMotion = true
            )
        )
    }
}
