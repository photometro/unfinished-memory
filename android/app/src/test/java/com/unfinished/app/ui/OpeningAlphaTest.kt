package com.unfinished.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpeningAlphaTest {
    @Test
    fun revealStartsHiddenAndEndsFullyVisible() {
        assertEquals(0f, openingAlpha(fieldValue = 0.5f, progress = 0f), 0.0001f)
        assertEquals(1f, openingAlpha(fieldValue = 0.5f, progress = 1f), 0.0001f)
    }

    @Test
    fun revealIsMonotonicAcrossTheFoldProgress() {
        val alpha = (0..20).map { step -> openingAlpha(0.5f, step / 20f) }
        assertTrue(alpha.zipWithNext().all { (previous, next) -> next >= previous })
    }
}
