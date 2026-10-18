package com.unfinished.app.ui

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.unfinished.app.R
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EnvironmentBitmapOwnershipTest {
    @get:Rule val compose = createComposeRule()

    @Test fun borrowsWindowBitmapImmediatelyAndNeverRecyclesIt() {
        val windowBitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val visible = mutableStateOf(true)
        var observed: Bitmap? = null
        compose.setContent {
            if (visible.value) observed = rememberBitmapResource(
                R.drawable.sea_daylight, startupSea = StartupSeaBitmap(R.drawable.sea_daylight, windowBitmap)
            )
        }
        compose.runOnIdle {
            assertSame(windowBitmap, observed)
            visible.value = false
        }
        compose.runOnIdle { assertFalse(windowBitmap.isRecycled) }
        windowBitmap.recycle()
    }

    @Test fun periodChangePreservesBorrowedBitmapButReleasesOwnedDecode() {
        val windowBitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        val resource = mutableStateOf(R.drawable.sea_daylight)
        val visible = mutableStateOf(true)
        var observed: Bitmap? = null
        compose.setContent {
            if (visible.value) observed = rememberBitmapResource(
                resource.value, startupSea = StartupSeaBitmap(R.drawable.sea_daylight, windowBitmap)
            )
        }
        compose.runOnIdle { resource.value = R.drawable.sea_night }
        compose.waitUntil(5_000) { observed != null && observed !== windowBitmap }
        val decoded = observed!!
        compose.runOnIdle {
            assertFalse(windowBitmap.isRecycled)
            assertFalse(decoded.isRecycled)
            visible.value = false
        }
        compose.runOnIdle {
            assertFalse(windowBitmap.isRecycled)
            assertTrue(decoded.isRecycled)
        }
        windowBitmap.recycle()
    }

    @Test fun disabledResourceDoesNotBorrowOrDecode() {
        val windowBitmap = Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        var observed: Bitmap? = windowBitmap
        compose.setContent {
            observed = rememberBitmapResource(
                R.drawable.sea_daylight, enabled = false,
                startupSea = StartupSeaBitmap(R.drawable.sea_daylight, windowBitmap)
            )
        }
        compose.runOnIdle {
            assertTrue(observed == null)
            assertFalse(windowBitmap.isRecycled)
        }
        windowBitmap.recycle()
    }
}
