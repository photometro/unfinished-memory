package com.unfinished.app.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SceneFrameTest {
    @Test fun shorterWindowUsesTheSamePortraitFraming() {
        assertEquals(2340f, sceneFrameHeight(1080f, 2023f, 1080f/2340f), .001f)
        assertEquals(2340f, sceneFrameHeight(1080f, 2340f, 1080f/2340f), .001f)
    }
    @Test fun framingScalesWithTheWindowWidthNotDeviceCoordinates() {
        assertEquals(600f, sceneFrameHeight(300f, 500f, .5f), .001f)
    }
    @Test fun tallerWindowStillHasFullCoverage() {
        assertEquals(800f, sceneFrameHeight(300f, 800f, .5f), .001f)
    }
    @Test fun landscapeIsNotForcedIntoPortrait() {
        assertEquals(400f, sceneFrameHeight(700f, 400f, 2f), .001f)
    }
    @Test fun missingOrInvalidReferenceKeepsTheActualViewport() {
        for (aspect in listOf(null, 0f, -1f, Float.NaN, Float.POSITIVE_INFINITY)) {
            assertEquals(500f, sceneFrameHeight(300f, 500f, aspect), .001f)
        }
    }
}
