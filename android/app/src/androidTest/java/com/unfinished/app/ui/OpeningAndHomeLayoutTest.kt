package com.unfinished.app.ui

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.unfinished.app.UnfinishedApp
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class OpeningAndHomeLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun openingEntersHomeAutomaticallyWithoutATap() {
        val entered = AtomicBoolean(false)
        val sea = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFF497B88.toInt()) }
        val shore = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(0xFFE6DED1.toInt()) }

        compose.setContent {
            UnfinishedTheme(isNight = false) {
                OpeningScene(
                    period = EnvironmentPeriod.DAYLIGHT,
                    sea = sea,
                    shore = shore,
                    reference = null,
                    reducedMotion = false,
                    reduceTransparency = true,
                    onEntered = { entered.set(true) }
                )
            }
        }

        compose.onNodeWithText("抵达岸边").assertDoesNotExist()
        compose.onNodeWithText("略过").assertDoesNotExist()
        compose.waitUntil(timeoutMillis = 5_000) { entered.get() }
        assertTrue("Opening must finish without tapping the top action", entered.get())
    }

    @Test
    fun quietHomeShowsBothPrimaryActionsAndKeepsMessageNearTheTop() {
        compose.setContent {
            UnfinishedTheme(isNight = false) {
                NativeHomeScreen(
                    period = EnvironmentPeriod.DAYLIGHT,
                    shore = null,
                    hasMemories = false,
                    activeEventId = null,
                    activeEventLabel = "",
                    receiptEnabled = false,
                    hasAvailableCue = false,
                    isNight = false,
                    reduceTransparency = true,
                    onShoreVisible = {},
                    onLeave = {},
                    onPast = {},
                    onSettings = {},
                    onThink = {},
                    onMoreCue = {},
                    onDirectReveal = {},
                    onDefer = {}
                )
            }
        }

        val headerNode = compose.onNodeWithTag("home.header").assertIsDisplayed().fetchSemanticsNode()
        val messageNode = compose.onNodeWithTag("home.message").assertIsDisplayed().fetchSemanticsNode()
        val leaveNode = compose.onNodeWithTag("home.leave").assertIsDisplayed().fetchSemanticsNode()
        val pastNode = compose.onNodeWithTag("home.past").assertIsDisplayed().fetchSemanticsNode()
        val rootHeight = compose.onRoot().fetchSemanticsNode().boundsInRoot.height
        val gapFraction = (messageNode.boundsInRoot.top - headerNode.boundsInRoot.bottom) / rootHeight

        assertTrue("Home message should follow a proportional, top-biased gap", gapFraction in 0.025f..0.08f)
        assertTrue("Prototype order is Leave then Past", leaveNode.boundsInRoot.center.x < pastNode.boundsInRoot.center.x)
    }

    @Test
    fun normalHomeGlassActionsUseTheSameTransparencyAcrossRoles() {
        val leaveAlpha = glassActionSurfaceColor(
            isNight = false,
            reduceTransparency = false,
            isPrimary = true
        ).alpha
        val pastAlpha = glassActionSurfaceColor(
            isNight = false,
            reduceTransparency = false,
            isPrimary = false
        ).alpha
        val nightLeaveAlpha = glassActionSurfaceColor(
            isNight = true,
            reduceTransparency = false,
            isPrimary = true
        ).alpha
        val nightPastAlpha = glassActionSurfaceColor(
            isNight = true,
            reduceTransparency = false,
            isPrimary = false
        ).alpha
        assertEquals("Primary and secondary glass actions should have a shared opacity", leaveAlpha, pastAlpha, 0.001f)
        assertEquals("Night glass actions should also use a shared opacity", nightLeaveAlpha, nightPastAlpha, 0.001f)
        assertTrue("The day glass should be more transparent than its previous 0.32 secondary value", leaveAlpha < 0.32f)
        assertTrue("The night glass should remain slightly more transparent than before", nightLeaveAlpha < 0.24f)
    }

    @Test
    fun openingAndHomeCopyCrossfadeWithinTheEndOfTheReveal() {
        assertEquals(0f, openingHomeHandoffAlpha(0.4f), 0.001f)
        assertTrue(openingHomeHandoffAlpha(0.25f) in 0.45f..0.55f)
        assertEquals(1f, openingHomeHandoffAlpha(0.1f), 0.001f)
    }

    @Test
    fun openingAndHomeCopyAreBothVisibleDuringTheHandoff() {
        compose.setContent {
            OpeningHomeHandoff(
                progress = 0.25f,
                openingContent = { alpha ->
                    Text("开屏", modifier = Modifier.graphicsLayer { this.alpha = alpha }.testTag("opening.title"))
                },
                homeContent = { alpha ->
                    Text("首页", modifier = Modifier.graphicsLayer { this.alpha = alpha }.testTag("home.header"))
                }
            )
        }

        compose.onNodeWithTag("opening.title").assertIsDisplayed()
        compose.onNodeWithTag("home.header").assertIsDisplayed()
    }

    @Test
    fun appRootOpensAutomaticallyIntoThePersistentHome() {
        compose.setContent { UnfinishedApp(onNightModeChanged = {}) }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag("home.header").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("home.header").assertIsDisplayed()
        compose.onNodeWithTag("opening.title").assertDoesNotExist()
        compose.onNodeWithTag("home.leave").assertIsDisplayed()
        compose.onNodeWithTag("home.past").assertIsDisplayed()
    }

    @Test
    fun sharedPanelContentAxisIsCenteredAndCappedOnWideWindow() {
        compose.setContent { UnfinishedApp(onNightModeChanged = {}) }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag("home.header").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("home.settings").assertIsDisplayed().performClick()

        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val panel = compose.onNodeWithTag("panel.contentAxis").assertExists().fetchSemanticsNode().boundsInRoot
        val density = compose.activity.resources.displayMetrics.density
        val centerErrorPx = kotlin.math.abs(panel.center.x - root.center.x)

        assertTrue("Shared panel content should stay within 460dp", panel.width <= 460f * density + 2f)
        assertTrue("Shared panel content should be centered on a wide window", centerErrorPx <= 2f * density)
    }

    @Test
    fun compactHomeActionsRemainReachableAtLargeText() {
        compose.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(baseDensity.density, fontScale = 1.3f)) {
                UnfinishedTheme(isNight = false) {
                    NativeHomeScreen(
                        period = EnvironmentPeriod.DAYLIGHT,
                        shore = null,
                        hasMemories = false,
                        activeEventId = null,
                        activeEventLabel = "",
                        receiptEnabled = false,
                        hasAvailableCue = false,
                        isNight = false,
                        reduceTransparency = true,
                        onShoreVisible = {},
                        onLeave = {},
                        onPast = {},
                        onSettings = {},
                        onThink = {},
                        onMoreCue = {},
                        onDirectReveal = {},
                        onDefer = {}
                    )
                }
            }
        }

        compose.onNodeWithTag("home.leave").assertExists().performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("home.past").assertExists().performScrollTo().assertIsDisplayed()
    }

    @Test
    fun leaveEditorStaysVisibleWhenKeyboardOpensWithoutSaving() {
        compose.setContent { UnfinishedApp(onNightModeChanged = {}) }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithTag("home.header").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("home.leave").assertIsDisplayed().performClick()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag("leave.text").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(hasSetTextAction()).onFirst().performClick()

        compose.waitUntil(timeoutMillis = 5_000) {
            compose.activity.window.decorView.rootWindowInsets
                ?.isVisible(android.view.WindowInsets.Type.ime()) == true
        }
        compose.onAllNodes(hasSetTextAction()).onFirst().assertIsDisplayed()
        // IME visibility flips at animation start; await the resized layout,
        // not just the platform flag before comparing bounds.
        compose.waitUntil(timeoutMillis = 5_000) {
            val field = compose.onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().boundsInRoot
            val safe = compose.onNodeWithTag("leave.safe.viewport").fetchSemanticsNode().boundsInRoot
            field.bottom <= safe.bottom + 1f
        }
        val editor = compose.onAllNodes(hasSetTextAction()).onFirst().fetchSemanticsNode().boundsInRoot
        val panel = compose.onNodeWithTag("leave.safe.viewport").fetchSemanticsNode().boundsInRoot
        assertTrue("The focused editor should remain inside the IME-adjusted panel: editor=$editor panel=$panel", editor.bottom <= panel.bottom + 1f)
    }
}
