package com.unfinished.app.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Renders synthetic home state only; never opens the repository or saves user content. */
@RunWith(AndroidJUnit4::class)
class HomeViewportLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun shortEmptyHomeKeepsActionsAtViewportBottom() {
        showHome()
        assertActionsAtBottom()
    }

    @Test
    fun shortQuietHomeKeepsActionsAtViewportBottom() {
        showHome(hasMemories = true)
        assertActionsAtBottom()
    }

    @Test
    fun shortEncounterHomeKeepsActionsAtViewportBottom() {
        showHome(heightDp = 700f, activeEvent = true)
        compose.onNodeWithTag("home.encounter.recall").assertIsDisplayed()
        compose.onNodeWithTag("home.encounter.cue").assertIsDisplayed()
        assertActionsAtBottom()
    }

    @Test
    fun largeTextDoesNotMoveActionsUpWhenContentFits() {
        showHome(heightDp = 700f, fontScale = 1.3f)
        assertActionsAtBottom()
    }

    @Test
    fun resizingTheSameCompositionPreservesBottomAnchoring() {
        val height = mutableStateOf(760f)
        compose.setContent {
            Box(Modifier.fillMaxSize()) {
                Box(Modifier.size(width = 360.dp, height = height.value.dp)) {
                    SyntheticHome()
                }
            }
        }
        assertActionsAtBottom()
        compose.runOnIdle { height.value = 600f }
        assertActionsAtBottom()
        compose.runOnIdle { height.value = 760f }
        assertActionsAtBottom()
    }

    @Test
    fun overflowingEncounterCanScrollToBothActionsWithoutOverlap() {
        var leaveClicks = 0
        var pastClicks = 0
        showHome(
            heightDp = 320f,
            fontScale = 1.5f,
            activeEvent = true,
            onLeave = { leaveClicks++ },
            onPast = { pastClicks++ }
        )
        compose.onNodeWithTag("home.leave").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("home.past").performScrollTo().assertIsDisplayed().performClick()
        val message = compose.onNodeWithTag("home.message").fetchSemanticsNode().boundsInRoot
        val actions = compose.onNodeWithTag("home.actions").fetchSemanticsNode().boundsInRoot
        assertTrue("Overflow must scroll instead of overlaying the message", actions.top > message.bottom)
        compose.runOnIdle {
            assertEquals(1, leaveClicks)
            assertEquals(1, pastClicks)
        }
    }

    private fun showHome(
        heightDp: Float = 640f,
        fontScale: Float = 1f,
        hasMemories: Boolean = false,
        activeEvent: Boolean = false,
        onLeave: () -> Unit = {},
        onPast: () -> Unit = {}
    ) {
        compose.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale)) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.size(width = 360.dp, height = heightDp.dp)) {
                        SyntheticHome(hasMemories, activeEvent, onLeave, onPast)
                    }
                }
            }
        }
    }

    private fun assertActionsAtBottom() {
        val viewport = compose.onNodeWithTag("home.viewport").fetchSemanticsNode().boundsInRoot
        val actions = compose.onNodeWithTag("home.actions").assertIsDisplayed().fetchSemanticsNode().boundsInRoot
        val density = compose.activity.resources.displayMetrics.density
        val gapDp = (viewport.bottom - actions.bottom) / density
        assertTrue("Footer must stay at the viewport bottom; gap was ${gapDp}dp", abs(gapDp - 2f) <= 1f)
        compose.onNodeWithTag("home.leave").assertIsDisplayed()
        compose.onNodeWithTag("home.past").assertIsDisplayed()
    }

    @androidx.compose.runtime.Composable
    private fun SyntheticHome(
        hasMemories: Boolean = false,
        activeEvent: Boolean = false,
        onLeave: () -> Unit = {},
        onPast: () -> Unit = {}
    ) {
        UnfinishedTheme(isNight = false) {
            NativeHomeScreen(
                period = EnvironmentPeriod.DAYLIGHT,
                shore = null,
                hasMemories = hasMemories,
                activeEventId = if (activeEvent) "synthetic-event" else null,
                activeEventLabel = "一段合成测试记忆",
                receiptEnabled = false,
                hasAvailableCue = activeEvent,
                isNight = false,
                reduceTransparency = true,
                onShoreVisible = {},
                onLeave = onLeave,
                onPast = onPast,
                onSettings = {},
                onThink = {},
                onMoreCue = {},
                onDirectReveal = {},
                onDefer = {}
            )
        }
    }
}
