package com.unfinished.app.ui

import android.content.Context
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.unfinished.app.R
import java.time.LocalTime
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

internal data class DisplaySettings(
    val forceNight: Boolean = false,
    val reduceTransparency: Boolean = false
)

internal class DisplaySettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("display_settings_v1", Context.MODE_PRIVATE)

    fun read() = DisplaySettings(
        forceNight = preferences.getBoolean(KEY_FORCE_NIGHT, false),
        reduceTransparency = preferences.getBoolean(KEY_REDUCE_TRANSPARENCY, false)
    )

    fun write(settings: DisplaySettings) {
        preferences.edit()
            .putBoolean(KEY_FORCE_NIGHT, settings.forceNight)
            .putBoolean(KEY_REDUCE_TRANSPARENCY, settings.reduceTransparency)
            .apply()
    }

    private companion object {
        const val KEY_FORCE_NIGHT = "force_night"
        const val KEY_REDUCE_TRANSPARENCY = "reduce_transparency"
    }
}

internal enum class EnvironmentPeriod(val label: String, val shoreDrawable: Int, val seaDrawable: Int) {
    DAYLIGHT("日光", R.drawable.shore_daylight, R.drawable.sea_daylight),
    SLANT("斜阳", R.drawable.shore_slant, R.drawable.sea_slant),
    NIGHT("夜色", R.drawable.shore_night, R.drawable.sea_night)
}

internal fun environmentPeriodAt(hour: Int, minute: Int, forceNight: Boolean = false): EnvironmentPeriod {
    require(hour in 0..23 && minute in 0..59)
    if (forceNight) return EnvironmentPeriod.NIGHT
    val minutes = hour * 60 + minute
    return when {
        minutes >= 6 * 60 && minutes < 14 * 60 -> EnvironmentPeriod.DAYLIGHT
        minutes >= 14 * 60 && minutes < 18 * 60 -> EnvironmentPeriod.SLANT
        else -> EnvironmentPeriod.NIGHT
    }
}

internal fun shouldUseScrollableLayout(availableHeightDp: Float, fontScale: Float): Boolean =
    availableHeightDp < 700f || fontScale >= 1.25f

internal fun homeHeroTopGapDp(availableHeightDp: Float): Float =
    (availableHeightDp * 0.06f).coerceIn(28f, 58f)

internal data class GlassActionRefractionProfile(
    val heightDp: Float,
    val amountDp: Float,
    val depthEffect: Boolean
)

internal fun glassActionRefractionProfile(pressProgress: Float): GlassActionRefractionProfile {
    val progress = pressProgress.coerceIn(0f, 1f)
    return GlassActionRefractionProfile(
        heightDp = 16f + 3f * progress,
        amountDp = 30f + 6f * progress,
        depthEffect = true
    )
}

internal data class NativePalette(
    val page: Color,
    val leave: Color,
    val past: Color,
    val encounter: Color,
    val settings: Color,
    val ink: Color,
    val muted: Color,
    val accent: Color,
    val signal: Color,
    val hairline: Color
)

internal val LightPalette = NativePalette(
    page = Color(0xFFF2EADF),
    leave = Color(0xFFF2EADF),
    past = Color(0xFFE6EDF0),
    encounter = Color(0xFFF2EADF),
    settings = Color(0xFFE8ECE6),
    ink = Color(0xFF28302F),
    muted = Color(0xFF68716D),
    accent = Color(0xFF416C78),
    signal = Color(0xFFE9BE88),
    hairline = Color(0x332D3A3B)
)

internal val DarkPalette = NativePalette(
    page = Color(0xFF302D29),
    leave = Color(0xFF302D29),
    past = Color(0xFF263842),
    encounter = Color(0xFF302D29),
    settings = Color(0xFF2D3732),
    ink = Color(0xFFECE7DE),
    muted = Color(0xFFB9B1A6),
    accent = Color(0xFF9AC1CB),
    signal = Color(0xFFE7BE8E),
    hairline = Color(0x44E6E0D6)
)

internal val LocalNativePalette = staticCompositionLocalOf { LightPalette }
internal val LocalNativeNightMode = staticCompositionLocalOf { false }

internal val Page: Color
    @Composable get() = LocalNativePalette.current.page
internal val Ink: Color
    @Composable get() = LocalNativePalette.current.ink
internal val Muted: Color
    @Composable get() = LocalNativePalette.current.muted
internal val Accent: Color
    @Composable get() = LocalNativePalette.current.accent
internal val Signal: Color
    @Composable get() = LocalNativePalette.current.signal
internal val Hairline: Color
    @Composable get() = LocalNativePalette.current.hairline

@OptIn(ExperimentalTextApi::class)
private fun variableFont(weight: Int, resourceId: Int = R.font.noto_sans_sc_variable) = Font(
    resId = resourceId,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

internal val EmbeddedChineseFont = FontFamily(
    variableFont(300),
    variableFont(400),
    variableFont(500),
    variableFont(600),
    variableFont(700)
)

// Identical Noto outlines/axes, limited to fixed UI copy. Never use this family
// for arbitrary memory text; the full EmbeddedChineseFont remains bundled.
internal val OpeningChineseFont = FontFamily(
    variableFont(300, R.font.noto_sans_sc_ui_variable),
    variableFont(400, R.font.noto_sans_sc_ui_variable),
    variableFont(500, R.font.noto_sans_sc_ui_variable),
    variableFont(600, R.font.noto_sans_sc_ui_variable),
    variableFont(700, R.font.noto_sans_sc_ui_variable)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnfinishedTheme(
    isNight: Boolean,
    palette: NativePalette = if (isNight) DarkPalette else LightPalette,
    content: @Composable () -> Unit
) {
    val scheme = if (isNight) {
        darkColorScheme(
            primary = palette.accent,
            secondary = palette.signal,
            background = palette.page,
            surface = palette.page,
            onBackground = palette.ink,
            onSurface = palette.ink
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            secondary = palette.signal,
            background = palette.page,
            surface = palette.page,
            onBackground = palette.ink,
            onSurface = palette.ink
        )
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(
            LocalNativePalette provides palette,
            LocalNativeNightMode provides isNight,
            LocalTextStyle provides TextStyle(
                fontFamily = EmbeddedChineseFont,
                fontSize = 16.sp,
                lineHeight = 26.sp,
                color = palette.ink
            ),
            LocalRippleConfiguration provides null
        ) {
            content()
        }
    }
}

internal fun panelSurface(palette: NativePalette, kind: String): Color = when (kind) {
    "past" -> palette.past
    "settings" -> palette.settings
    "encounter" -> palette.encounter
    else -> palette.leave
}

@Composable
internal fun rememberEnvironmentPeriod(forceNight: Boolean): EnvironmentPeriod {
    var localTime by remember { mutableStateOf(LocalTime.now()) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) localTime = LocalTime.now()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            localTime = LocalTime.now()
            val now = LocalTime.now()
            val delayToNextMinute = ((60 - now.second) * 1000L - now.nano / 1_000_000L).coerceAtLeast(250L)
            delay(delayToNextMinute)
        }
    }
    return environmentPeriodAt(localTime.hour, localTime.minute, forceNight)
}
