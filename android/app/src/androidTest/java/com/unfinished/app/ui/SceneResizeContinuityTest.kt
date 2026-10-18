package com.unfinished.app.ui

import android.graphics.Bitmap
import android.content.ComponentName
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.unfinished.app.MainActivity
import kotlin.math.abs

/** Exercises the actual scene renderer, not an unused geometry helper. */
@RunWith(AndroidJUnit4::class)
class SceneResizeContinuityTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun launcherActivityHandlesOnlySizeRelatedConfigurationChanges() {
        val component = ComponentName(compose.activity, MainActivity::class.java)
        val flags = compose.activity.packageManager.getActivityInfo(component, 0).configChanges
        val sizes = ActivityInfo.CONFIG_SCREEN_SIZE or ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE or
            ActivityInfo.CONFIG_SCREEN_LAYOUT or ActivityInfo.CONFIG_ORIENTATION
        assertEquals("Resize must stay on the live Compose configuration path", sizes, flags and sizes)
        val other = ActivityInfo.CONFIG_LOCALE or ActivityInfo.CONFIG_DENSITY or
            ActivityInfo.CONFIG_FONT_SCALE or ActivityInfo.CONFIG_UI_MODE
        assertEquals("Do not silently opt out of unrelated resource reloads", 0, flags and other)
    }

    @Test fun changingWindowHeightDoesNotReframeTheShore() {
        val height = mutableStateOf(400f)
        val shore = Bitmap.createBitmap(200, 400, Bitmap.Config.ARGB_8888).apply {
            // Every row differs, so a centered crop jump cannot accidentally pass.
            for (y in 0 until 400) for (x in 0 until 200) {
                setPixel(x, y, android.graphics.Color.rgb(40 + y/2, 80 + y/3, 120 + x/2))
            }
        }
        compose.setContent {
            Box(Modifier.size(200.dp, height.value.dp).testTag("scene.resize")) {
                OpeningScene(
                    period = EnvironmentPeriod.DAYLIGHT,
                    sea = shore,
                    shore = shore,
                    reference = null,
                    reducedMotion = true,
                    reduceTransparency = true,
                    onEntered = {},
                    hasEnteredShore = true,
                    framingAspectOverride = .5f
                )
            }
        }
        val full = compose.onNodeWithTag("scene.resize").captureToImage().toPixelMap()
        compose.runOnIdle { height.value = 350f }
        val mini = compose.onNodeWithTag("scene.resize").captureToImage().toPixelMap()
        assertEquals(full.width, mini.width)
        var worstDelta = 0f
        for (y in 10 until mini.height-10 step 7) for (x in 10 until mini.width-10 step 7) {
            val a = full[x,y]; val b = mini[x,y]
            worstDelta = maxOf(worstDelta, abs(a.red-b.red), abs(a.green-b.green), abs(a.blue-b.blue))
        }
        assertTrue("The same shore landmarks must not jump on resize; worst=$worstDelta", worstDelta < 3f/255f)
        compose.runOnIdle { height.value = 400f }
        val restored = compose.onNodeWithTag("scene.resize").captureToImage().toPixelMap()
        assertEquals(full.height, restored.height)
        assertEquals(full[full.width/2, full.height/2], restored[restored.width/2, restored.height/2])
    }
}
