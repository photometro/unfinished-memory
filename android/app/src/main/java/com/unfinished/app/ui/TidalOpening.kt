package com.unfinished.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.graphics.Rect as AndroidRect
import android.graphics.RectF
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Trace
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.pow

private const val OPENING_HOLD_MILLIS = 350L
private const val OPENING_ASSET_TIMEOUT_MILLIS = 1_500L
private const val OPENING_HOME_HANDOFF_START = 0.38f
private const val OPENING_HOME_HANDOFF_END = 0.12f

@Composable
private fun rememberSceneFramingAspect(): Float? {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) {
        runCatching {
            // This is a visual reference only, never a layout/interaction size.
            // A shorter window clips the same shore instead of recentering it.
            val bounds = context.getSystemService(WindowManager::class.java)
                .maximumWindowMetrics.bounds
            bounds.width().toFloat() / bounds.height().coerceAtLeast(1)
        }.getOrNull()
    }
}

internal data class OpeningRevealField(
    val width: Int,
    val height: Int,
    val texture: Bitmap
)

internal class OpeningSceneRenderer internal constructor(
    val graphicsLayer: GraphicsLayer,
    val backdrop: LayerBackdrop
)

@Composable
internal fun rememberOpeningSceneRenderer(): OpeningSceneRenderer {
    val graphicsLayer = rememberGraphicsLayer()
    val backdropDraw: ContentDrawScope.() -> Unit = remember(graphicsLayer) {
        { drawLayer(graphicsLayer) }
    }
    val backdrop = rememberLayerBackdrop(onDraw = backdropDraw)
    return remember(graphicsLayer, backdrop) { OpeningSceneRenderer(graphicsLayer, backdrop) }
}

@Composable
private fun rememberOpeningRevealField(reference: Bitmap?, aspectRatio: Float): OpeningRevealField? {
    val fieldState = produceState<OpeningRevealField?>(null, reference, aspectRatio) {
        value = null
        if (reference == null || reference.isRecycled) return@produceState
        value = withContext(Dispatchers.Default) { buildOpeningRevealField(reference, aspectRatio) }
    }
    val field = fieldState.value
    DisposableEffect(field) {
        onDispose { field?.texture?.takeUnless { it.isRecycled }?.recycle() }
    }
    return field
}

@Composable
private fun rememberOpeningShader(
    sea: Bitmap?,
    shore: Bitmap?,
    field: OpeningRevealField?,
    hasEnteredShore: Boolean
): RuntimeShader? {
    val shaderState = produceState<RuntimeShader?>(null, sea, shore, field, hasEnteredShore) {
        value = null
        if (!hasEnteredShore && sea != null && shore != null && field != null) {
            value = withContext(Dispatchers.Default) { createOpeningShader(sea, shore, field) }
        }
    }
    return shaderState.value
}

@Composable
internal fun OpeningScene(
    period: EnvironmentPeriod,
    sea: Bitmap?,
    shore: Bitmap?,
    reference: Bitmap?,
    reducedMotion: Boolean,
    reduceTransparency: Boolean,
    onEntered: () -> Unit,
    renderer: OpeningSceneRenderer? = null,
    progressState: MutableFloatState? = null,
    fontReady: Boolean = true,
    homeLayoutReady: Boolean = true,
    hasEnteredShore: Boolean = false,
    copyAlpha: Float = 1f,
    transparentWhileLoading: Boolean = false,
    framingAspectOverride: Float? = null
) {
    val resolvedRenderer = renderer ?: rememberOpeningSceneRenderer()
    val transitionProgress = progressState ?: remember { mutableFloatStateOf(1f) }
    val density = LocalDensity.current
    val framingAspect = framingAspectOverride ?: rememberSceneFramingAspect()
    val progress = remember { Animatable(if (hasEnteredShore) 0f else 1f) }
    val enteredCallback by rememberUpdatedState(onEntered)
    var preparationTimedOut by remember { mutableStateOf(false) }

    LaunchedEffect(progress) {
        snapshotFlow { progress.value }.collect { transitionProgress.floatValue = it }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().then(
            if (transparentWhileLoading) Modifier else Modifier.background(Color(0xFF426B79))
        )
    ) {
        val compact = shouldUseScrollableLayout(maxHeight.value, density.fontScale)
        val scrollState = rememberScrollState()
        val aspect = with(density) {
            (maxWidth.toPx() / sceneFrameHeight(maxWidth.toPx(), maxHeight.toPx(), framingAspect)
                .coerceAtLeast(1f)).coerceIn(0.25f, 1.2f)
        }
        val field = rememberOpeningRevealField(reference, aspect)
        val shader = rememberOpeningShader(sea, shore, field, hasEnteredShore)
        val sceneReady = sea != null && shore != null && field != null && shader != null &&
            fontReady && homeLayoutReady

        LaunchedEffect(sceneReady, hasEnteredShore) {
            if (!sceneReady && !hasEnteredShore) {
                delay(OPENING_ASSET_TIMEOUT_MILLIS)
                preparationTimedOut = true
            }
        }

        LaunchedEffect(sceneReady, preparationTimedOut, reducedMotion, hasEnteredShore) {
            when {
                hasEnteredShore -> {
                    progress.snapTo(0f)
                    transitionProgress.floatValue = 0f
                }
                reducedMotion || (preparationTimedOut && !sceneReady) -> {
                    progress.snapTo(0f)
                    transitionProgress.floatValue = 0f
                    enteredCallback()
                }
                sceneReady -> {
                    delay(OPENING_HOLD_MILLIS)
                    Trace.beginAsyncSection("Opening.Animation", 11)
                    try {
                        progress.animateTo(0f, tween(1450, easing = FastOutSlowInEasing))
                        transitionProgress.floatValue = 0f
                        enteredCallback()
                    } finally {
                        Trace.endAsyncSection("Opening.Animation", 11)
                    }
                }
            }
        }

        OpeningRevealCanvas(
            renderer = resolvedRenderer,
            sea = sea,
            shore = shore,
            field = field,
            shader = shader,
            progress = transitionProgress,
            hasEnteredShore = hasEnteredShore,
            framingAspect = framingAspect,
            modifier = Modifier.fillMaxSize(),
            transparentFallback = transparentWhileLoading
        )

        if (!hasEnteredShore && fontReady && !transparentWhileLoading) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(horizontal = 28.dp, vertical = 18.dp)
                    .graphicsLayer {
                        alpha = copyAlpha * (1f - openingHomeHandoffAlpha(transitionProgress.floatValue))
                    }
                    .then(if (compact) Modifier.verticalScroll(scrollState) else Modifier),
                verticalArrangement = Arrangement.Top
            ) {
                if (compact) Spacer(Modifier.padding(top = 24.dp)) else Spacer(Modifier.weight(0.24f))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                    Column(modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
                        Text(
                            "一片承载记忆的时间海域",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            letterSpacing = 1.4.sp,
                            style = TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color(0x660E2630), blurRadius = 12f
                                )
                            )
                        )
                        Text(
                            "未完成",
                            modifier = Modifier.padding(top = 20.dp).testTag("opening.title"),
                            color = Color.White,
                            fontSize = 42.sp,
                            lineHeight = 50.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp,
                            style = TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color(0x880E2630), blurRadius = 18f
                                )
                            )
                        )
                        Text(
                            "过去不能修改，\n但可以重新想起。",
                            modifier = Modifier.padding(top = 18.dp),
                            color = Color.White.copy(alpha = 0.96f),
                            fontSize = 16.sp,
                            lineHeight = 29.sp,
                            fontWeight = FontWeight.Medium,
                            style = TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = Color(0x990E2630), blurRadius = 14f
                                )
                            )
                        )
                    }
                }
                if (compact) Spacer(Modifier.padding(top = 40.dp)) else Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OpeningRevealCanvas(
    renderer: OpeningSceneRenderer,
    sea: Bitmap?,
    shore: Bitmap?,
    field: OpeningRevealField?,
    shader: RuntimeShader?,
    progress: MutableFloatState,
    hasEnteredShore: Boolean,
    framingAspect: Float?,
    modifier: Modifier,
    transparentFallback: Boolean
) {
    val paint = remember { AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG) }

    Box(modifier = modifier.layerBackdrop(renderer.backdrop)) {
        Canvas(Modifier.fillMaxSize()) {
            val sceneLayer = renderer.graphicsLayer
            val layerSize = IntSize(size.width.roundToInt(), size.height.roundToInt())
            val boundedProgress = progress.floatValue.coerceIn(0f, 1f)
            Trace.beginSection("Opening.Scene.Record")
            try {
                sceneLayer.record(size = layerSize) {
                    drawIntoCanvas { composeCanvas ->
                        val canvas = composeCanvas.nativeCanvas
                        val frameHeight = sceneFrameHeight(size.width, size.height, framingAspect)
                        val target = RectF(0f, 0f, size.width, frameHeight)
                        val save = canvas.save()
                        canvas.clipRect(0f, 0f, size.width, size.height)
                        try {
                        when {
                            hasEnteredShore && shore != null -> drawCover(canvas, shore, target, paint)
                            shader != null && sea != null && shore != null && field != null -> {
                                shader.setFloatUniform("viewportSize", size.width, frameHeight)
                                shader.setFloatUniform("seaSize", sea.width.toFloat(), sea.height.toFloat())
                                shader.setFloatUniform("shoreSize", shore.width.toFloat(), shore.height.toFloat())
                                shader.setFloatUniform("fieldSize", field.width.toFloat(), field.height.toFloat())
                                shader.setFloatUniform("progress", boundedProgress)
                                paint.shader = shader
                                canvas.drawRect(target, paint)
                                paint.shader = null
                            }
                            sea != null -> drawCover(canvas, sea, target, paint)
                            shore != null -> drawCover(canvas, shore, target, paint)
                            else -> if (!transparentFallback) canvas.drawColor(Color(0xFF426B79).toArgb())
                        }
                        } finally {
                            canvas.restoreToCount(save)
                        }
                    }
                }
                drawLayer(sceneLayer)
            } finally {
                Trace.endSection()
            }
        }
    }
}

private fun createOpeningShader(sea: Bitmap, shore: Bitmap, field: OpeningRevealField): RuntimeShader? =
    runCatching {
        RuntimeShader(OPENING_REVEAL_SHADER).apply {
            setInputShader("seaTexture", BitmapShader(sea, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
            setInputShader("shoreTexture", BitmapShader(shore, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
            setInputShader("fieldTexture", BitmapShader(field.texture, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP))
        }
    }.onFailure { android.util.Log.e("UnfinishedOpening", "Unable to prepare AGSL shoreline reveal", it) }
        .getOrNull()

private const val OPENING_REVEAL_SHADER = """
uniform shader seaTexture;
uniform shader shoreTexture;
uniform shader fieldTexture;
uniform float2 viewportSize;
uniform float2 seaSize;
uniform float2 shoreSize;
uniform float2 fieldSize;
uniform float progress;

float2 sourcePoint(float2 fragment, float2 viewport, float2 source) {
    float2 uv = fragment / viewport;
    float viewportAspect = viewport.x / viewport.y;
    float sourceAspect = source.x / source.y;
    if (sourceAspect > viewportAspect) {
        uv.x = 0.5 + (uv.x - 0.5) * (viewportAspect / sourceAspect);
    } else {
        uv.y = 0.5 + (uv.y - 0.5) * (sourceAspect / viewportAspect);
    }
    return uv * source;
}

half4 main(float2 fragment) {
    half4 shoreColor = shoreTexture.eval(sourcePoint(fragment, viewportSize, shoreSize));
    half4 seaColor = seaTexture.eval(sourcePoint(fragment, viewportSize, seaSize));
    float fieldValue = fieldTexture.eval((fragment / viewportSize) * fieldSize).r;
    float seaAmount = smoothstep(fieldValue - 0.025, fieldValue + 0.025, progress);
    return mix(shoreColor, seaColor, seaAmount);
}
"""

internal fun openingAlpha(fieldValue: Float, progress: Float): Float {
    val value = ((progress.coerceIn(0f, 1f) - fieldValue + 0.025f) / 0.05f).coerceIn(0f, 1f)
    return value * value * (3f - 2f * value)
}

internal fun openingHomeHandoffAlpha(progress: Float): Float =
    ((OPENING_HOME_HANDOFF_START - progress) /
        (OPENING_HOME_HANDOFF_START - OPENING_HOME_HANDOFF_END)).coerceIn(0f, 1f)

@Composable
internal fun OpeningHomeHandoff(
    progress: Float,
    openingContent: @Composable (Float) -> Unit,
    homeContent: @Composable (Float) -> Unit
) {
    val homeAlpha = openingHomeHandoffAlpha(progress)
    Box(modifier = Modifier.fillMaxSize()) {
        openingContent(1f - homeAlpha)
        if (homeAlpha > 0f) homeContent(homeAlpha)
    }
}

private fun drawCover(canvas: AndroidCanvas, bitmap: Bitmap, target: RectF, paint: AndroidPaint) {
    val targetAspect = target.width() / target.height().coerceAtLeast(1f)
    val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
    val source = if (imageAspect > targetAspect) {
        val width = (bitmap.height * targetAspect).roundToInt().coerceIn(1, bitmap.width)
        val left = ((bitmap.width - width) / 2).coerceAtLeast(0)
        AndroidRect(left, 0, left + width, bitmap.height)
    } else {
        val height = (bitmap.width / targetAspect).roundToInt().coerceIn(1, bitmap.height)
        val top = ((bitmap.height - height) / 2).coerceAtLeast(0)
        AndroidRect(0, top, bitmap.width, top + height)
    }
    canvas.drawBitmap(bitmap, source, target, paint)
}

private suspend fun buildOpeningRevealField(reference: Bitmap?, aspectRatio: Float): OpeningRevealField {
    Trace.beginSection("Opening.RevealField.Build")
    try {
        val width = 256
        val height = (width / aspectRatio.coerceIn(0.25f, 1.2f)).roundToInt().coerceIn(256, 1536)
        val referencePixels = IntArray(width * height)
        if (reference != null && !reference.isRecycled) {
            val target = RectF(0f, 0f, width.toFloat(), height.toFloat())
            val source = sourceCrop(reference, aspectRatio)
            val referenceRaster = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            AndroidCanvas(referenceRaster).drawBitmap(
                reference,
                source,
                target,
                AndroidPaint(AndroidPaint.ANTI_ALIAS_FLAG or AndroidPaint.FILTER_BITMAP_FLAG)
            )
            referenceRaster.getPixels(referencePixels, 0, width, 0, 0, width, height)
            referenceRaster.recycle()
        }

        val distances = FloatArray(width * height) { Float.POSITIVE_INFINITY }
        var waterPixels = 0
        for (index in referencePixels.indices) {
            val color = referencePixels[index]
            val red = (color shr 16) and 0xFF
            val green = (color shr 8) and 0xFF
            val blue = color and 0xFF
            if (green - red > 7 && blue - red > 12 && red < 210) {
                distances[index] = 0f
                waterPixels++
            }
        }

        if (waterPixels == 0) {
            for (y in 0 until height) for (x in 0 until width) {
                distances[y * width + x] = hypot((width - 1 - x) * 0.7f, (height - 1 - y) * 0.45f)
            }
        } else {
            val diagonal = 1.41421356f
            for (y in 0 until height) for (x in 0 until width) {
                val index = y * width + x
                var best = distances[index]
                if (x > 0) best = minOf(best, distances[index - 1] + 1f)
                if (y > 0) best = minOf(best, distances[index - width] + 1f)
                if (x > 0 && y > 0) best = minOf(best, distances[index - width - 1] + diagonal)
                if (x + 1 < width && y > 0) best = minOf(best, distances[index - width + 1] + diagonal)
                distances[index] = best
            }
            for (y in height - 1 downTo 0) for (x in width - 1 downTo 0) {
                val index = y * width + x
                var best = distances[index]
                if (x + 1 < width) best = minOf(best, distances[index + 1] + 1f)
                if (y + 1 < height) best = minOf(best, distances[index + width] + 1f)
                if (x + 1 < width && y + 1 < height) best = minOf(best, distances[index + width + 1] + diagonal)
                if (x > 0 && y + 1 < height) best = minOf(best, distances[index + width - 1] + diagonal)
                distances[index] = best
            }
        }

        val pixels = IntArray(width * height)
        for (y in 0 until height) for (x in 0 until width) {
            val index = y * width + x
            val normalized = (distances[index] / (width * 0.34f)).coerceIn(0f, 1f)
            val value = 0.065f + 0.80f * normalized.pow(0.52f) +
                0.04f * (1f - y / (height - 1f).coerceAtLeast(1f))
            val channel = (value * 255f).roundToInt().coerceIn(0, 255)
            pixels[index] = (0xFF shl 24) or (channel shl 16) or (channel shl 8) or channel
        }
        val texture = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        texture.setPixels(pixels, 0, width, 0, 0, width, height)
        return OpeningRevealField(width, height, texture)
    } finally {
        Trace.endSection()
    }
}

private fun sourceCrop(bitmap: Bitmap, aspectRatio: Float): AndroidRect {
    val targetAspect = aspectRatio.coerceIn(0.25f, 1.2f)
    val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
    return if (imageAspect > targetAspect) {
        val width = (bitmap.height * targetAspect).roundToInt().coerceIn(1, bitmap.width)
        val left = ((bitmap.width - width) / 2).coerceAtLeast(0)
        AndroidRect(left, 0, left + width, bitmap.height)
    } else {
        val height = (bitmap.width / targetAspect).roundToInt().coerceIn(1, bitmap.height)
        val top = ((bitmap.height - height) / 2).coerceAtLeast(0)
        AndroidRect(0, top, bitmap.width, top + height)
    }
}
