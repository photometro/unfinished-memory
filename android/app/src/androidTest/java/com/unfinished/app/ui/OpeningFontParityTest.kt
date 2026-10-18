package com.unfinished.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OpeningFontParityTest {
    @get:Rule val compose = createComposeRule()

    private fun checkParity(weight: FontWeight, scale: Float) {
        val small = mutableStateOf(false)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                Box(Modifier.testTag("font.compare")) {
                    Text(
                        "未完成\n过去不能修改，\n但可以重新想起。\n夜色 · 岸边\n留下一段 过去 ↗\n2026.10.12 · 一段曾经",
                        style = TextStyle(
                            fontFamily = if (small.value) OpeningChineseFont else EmbeddedChineseFont,
                            fontWeight = weight, fontSize = 22.sp, lineHeight = 30.sp
                        )
                    )
                }
            }
        }
        val original = compose.onNodeWithTag("font.compare").captureToImage().asAndroidBitmap()
        compose.runOnIdle { small.value = true }
        val subset = compose.onNodeWithTag("font.compare").captureToImage().asAndroidBitmap()
        assertTrue("UI subset must preserve actual rendered glyphs/metrics", original.sameAs(subset))
    }

    @Test fun regularUiGlyphsArePixelIdentical() = checkParity(FontWeight.Normal, 1f)
    @Test fun mediumOpeningGlyphsArePixelIdentical() = checkParity(FontWeight.Medium, 1f)
    @Test fun largeSystemTextRemainsPixelIdentical() = checkParity(FontWeight.Medium, 1.3f)
}
