package com.unfinished.app.ui

import android.graphics.Bitmap
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unfinished.app.data.VisibleReceipt
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

@Composable
internal fun NativeHomeScreen(
    period: EnvironmentPeriod,
    shore: Bitmap?,
    hasMemories: Boolean,
    activeEventId: String?,
    activeEventLabel: String,
    receiptEnabled: Boolean,
    hasAvailableCue: Boolean,
    isNight: Boolean,
    reduceTransparency: Boolean,
    onShoreVisible: suspend () -> Unit,
    onLeave: () -> Unit,
    onPast: () -> Unit,
    onSettings: () -> Unit,
    onThink: () -> Unit,
    onMoreCue: () -> Unit,
    onDirectReveal: () -> Unit,
    onDefer: () -> Unit,
    backdropOverride: LayerBackdrop? = null,
    drawBackground: Boolean = true,
    foregroundAlpha: Float = 1f,
    interactionEnabled: Boolean = true,
    homeAccessible: Boolean = true,
    onInitialLayoutReady: () -> Unit = {}
) {
    val localBackdrop = com.kyant.backdrop.backdrops.rememberLayerBackdrop()
    val backdrop = backdropOverride ?: localBackdrop
    val colors = LocalNativePalette.current
    var didReportInitialLayout by remember { mutableStateOf(false) }
    val initialLayoutCallback by rememberUpdatedState(onInitialLayoutReady)
    CompositionLocalProvider(LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = OpeningChineseFont)) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(if (drawBackground) colors.page else Color.Transparent)
            .onGloballyPositioned { coordinates ->
                if (!didReportInitialLayout && coordinates.size.width > 0 && coordinates.size.height > 0) {
                    didReportInitialLayout = true
                    initialLayoutCallback()
                }
            }
            .then(if (homeAccessible) Modifier else Modifier.clearAndSetSemantics { })
    ) {
        if (drawBackground) {
            Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
                val image = remember(shore) { shore?.asImageBitmap() }
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.fillMaxSize().background(colors.page))
                }
            }
        }

        val foregroundColor = if (isNight) Color(0xFFF2EFE8) else Color(0xFF28302F)
        val foregroundMuted = if (isNight) Color(0xFFE0DDD5).copy(alpha = 0.86f) else Color(0xFF46534F).copy(alpha = 0.9f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .graphicsLayer { alpha = foregroundAlpha.coerceIn(0f, 1f) },
            contentAlignment = Alignment.Center
        ) {
            BoxWithConstraints(Modifier.fillMaxSize().testTag("home.viewport"), contentAlignment = Alignment.Center) {
                val heroTopGap = homeHeroTopGapDp(maxHeight.value).dp
                val scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .fillMaxWidth()
                        .verticalScroll(scrollState, enabled = interactionEnabled)
                        // A scroll container measures with unbounded height. Its content
                        // must still fill the viewport, so short windows retain a footer
                        // instead of placing the actions immediately after the message.
                        .heightIn(min = maxHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.Start
                ) {
                Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(46.dp).testTag("home.header"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        Box(Modifier.size(6.dp).background(colors.accent, CircleShape))
                        Text("${period.label} · 岸边", color = foregroundMuted, fontSize = 12.sp, letterSpacing = 0.6.sp)
                    }
                    GlassAction(
                        backdrop = backdrop,
                        label = "设置",
                        leading = null,
                        isNight = isNight,
                        reduceTransparency = reduceTransparency,
                        isPrimary = false,
                        round = true,
                        interactionEnabled = interactionEnabled,
                        onClick = onSettings,
                        modifier = Modifier.size(46.dp).testTag("home.settings")
                    ) {
                        SettingsGlyph(color = foregroundColor)
                    }
                }

                Spacer(Modifier.height(heroTopGap))
                Column(modifier = Modifier.fillMaxWidth().testTag("home.message")) {
                    if (activeEventId != null) {
                        Text("潮水带来一页过去", color = foregroundMuted, fontSize = 12.sp, letterSpacing = 0.8.sp)
                        Text(
                            "一段过去，\n抵达岸边。",
                            modifier = Modifier.padding(top = 12.dp),
                            color = foregroundColor,
                            fontSize = 31.sp,
                            lineHeight = 40.sp,
                            letterSpacing = (-0.8).sp
                        )
                        VisibleReceipt(
                            actionId = "shore:$activeEventId",
                            enabled = receiptEnabled,
                            onVisible = onShoreVisible
                        ) {
                            Text(activeEventLabel, modifier = Modifier.padding(top = 12.dp), color = foregroundMuted, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassAction(
                                    backdrop, "想一会儿", isNight, reduceTransparency, true,
                                    modifier = Modifier.weight(if (hasAvailableCue) 1.15f else 1f),
                                    interactionEnabled = interactionEnabled,
                                    onClick = onThink,
                                    testTag = "home.encounter.recall"
                                )
                                if (hasAvailableCue) GlassAction(
                                    backdrop, "再给一点", isNight, reduceTransparency, false,
                                    modifier = Modifier.weight(1f),
                                    interactionEnabled = interactionEnabled,
                                    onClick = onMoreCue,
                                    testTag = "home.encounter.cue"
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassAction(
                                    backdrop, "直接看看", isNight, reduceTransparency, false,
                                    modifier = Modifier.weight(1f),
                                    interactionEnabled = interactionEnabled,
                                    onClick = onDirectReveal,
                                    testTag = "home.encounter.reveal"
                                )
                                GlassAction(
                                    backdrop, "让它再待一会", isNight, reduceTransparency, false,
                                    modifier = Modifier.weight(1f),
                                    interactionEnabled = interactionEnabled,
                                    onClick = onDefer,
                                    testTag = "home.encounter.defer"
                                )
                            }
                        }
                    } else if (!hasMemories) {
                        Text("先留下一些东西，剩下的交给时间", color = foregroundMuted, fontSize = 12.sp, letterSpacing = 0.35.sp)
                        Text(
                            "留下一段，\n给以后的自己。",
                            modifier = Modifier.padding(top = 12.dp),
                            color = foregroundColor,
                            fontSize = 32.sp,
                            lineHeight = 41.sp,
                            letterSpacing = (-0.8).sp
                        )
                        Text(
                            "一件小事，也值得被好好留住。",
                            modifier = Modifier.padding(top = 15.dp),
                            color = foregroundMuted,
                            fontSize = 14.sp,
                            lineHeight = 23.sp
                        )
                    } else {
                        Text("潮水退去，痕迹留下", color = foregroundMuted, fontSize = 12.sp, letterSpacing = 0.8.sp)
                        Text(
                            "今天海面\n很安静。",
                            modifier = Modifier.padding(top = 12.dp),
                            color = foregroundColor,
                            fontSize = 34.sp,
                            lineHeight = 43.sp,
                            letterSpacing = (-1).sp
                        )
                        Text(
                            "可以留下一点，也可以只是停一会儿。",
                            modifier = Modifier.padding(top = 15.dp),
                            color = foregroundMuted,
                            fontSize = 14.sp,
                            lineHeight = 23.sp
                        )
                    }
                }
                }
                Column(Modifier.fillMaxWidth().padding(top = 32.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().testTag("home.actions"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassAction(
                        backdrop = backdrop,
                        label = "留下一段",
                        leading = "+",
                        isNight = isNight,
                        reduceTransparency = reduceTransparency,
                        isPrimary = true,
                        modifier = Modifier.weight(1.2f).testTag("home.leave"),
                        interactionEnabled = interactionEnabled,
                        onClick = onLeave
                    )
                    GlassAction(
                        backdrop = backdrop,
                        label = "过去",
                        trailing = "↗",
                        isNight = isNight,
                        reduceTransparency = reduceTransparency,
                        isPrimary = false,
                        modifier = Modifier.weight(0.8f).testTag("home.past"),
                        interactionEnabled = interactionEnabled,
                        onClick = onPast
                    )
                }
                    Spacer(Modifier.height(2.dp))
                }
                }
            }
        }
    }
}

}

@Composable
internal fun GlassAction(
    backdrop: Backdrop,
    label: String,
    isNight: Boolean,
    reduceTransparency: Boolean,
    isPrimary: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionEnabled: Boolean = true,
    round: Boolean = false,
    leading: String? = null,
    trailing: String? = null,
    testTag: String? = null,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.975f else 1f, tween(150), label = "glass-action-press")
    val refractionProgress by animateFloatAsState(
        if (pressed && interactionEnabled) 1f else 0f,
        tween(180),
        label = "glass-action-refraction"
    )
    val refraction = glassActionRefractionProfile(refractionProgress)
    val shape = if (round) CircleShape else RoundedCornerShape(999.dp)
    val surfaceColor = glassActionSurfaceColor(
        isNight = isNight,
        reduceTransparency = reduceTransparency,
        isPrimary = isPrimary
    )
    val textColor = if (isNight) Color(0xFFF4F0E7) else Color(0xFF28302F)
    var drawModifier: Modifier = Modifier
    if (reduceTransparency) {
        drawModifier = drawModifier.background(surfaceColor, shape).border(1.dp, textColor.copy(alpha = 0.12f), shape)
    } else {
        drawModifier = drawModifier.drawBackdrop(
            backdrop = backdrop,
            shape = { shape },
            effects = {
                vibrancy()
                blur(1.dp.toPx())
                lens(
                    refraction.heightDp.dp.toPx(),
                    refraction.amountDp.dp.toPx(),
                    depthEffect = refraction.depthEffect
                )
            },
            onDrawSurface = { drawRect(surfaceColor) }
        ).border(0.75.dp, Color.White.copy(alpha = 0.28f), shape)
    }
    val tagged = if (testTag == null) Modifier else Modifier.testTag(testTag)
    Box(
        modifier = modifier
            .then(tagged)
            .graphicsLayer { scaleX = scale; scaleY = scale; alpha = if (enabled) 1f else 0.5f }
            .then(drawModifier)
            .clip(shape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && interactionEnabled,
                role = Role.Button,
                onClick = onClick
            )
            .height(if (round) 46.dp else 48.dp)
            .padding(horizontal = if (round) 0.dp else 18.dp),
        contentAlignment = Alignment.Center
    ) {
        if (content != null) {
            content()
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leading != null) Text(leading, color = textColor, fontSize = 21.sp, lineHeight = 24.sp)
                Text(
                    label,
                    color = textColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 1
                )
                if (trailing != null) Text(trailing, color = textColor.copy(alpha = 0.78f), fontSize = 16.sp)
            }
        }
    }
}

internal fun glassActionSurfaceColor(
    isNight: Boolean,
    reduceTransparency: Boolean,
    isPrimary: Boolean
): Color = when {
        reduceTransparency && isNight -> Color(0xFF354A50)
        reduceTransparency && isPrimary -> Color(0xFFF4F0E7)
        reduceTransparency -> Color(0xFFE5E9E6)
        isNight -> Color.White.copy(alpha = 0.12f)
        isPrimary -> Color(0xFFF7F2E9).copy(alpha = 0.14f)
        else -> Color.White.copy(alpha = 0.14f)
    }

@Composable
private fun SettingsGlyph(color: Color) {
    Canvas(modifier = Modifier.fillMaxSize().padding(13.dp)) {
        val stroke = Stroke(width = 1.45.dp.toPx())
        val y1 = size.height * 0.2f
        val y2 = size.height * 0.5f
        val y3 = size.height * 0.8f
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, y1), androidx.compose.ui.geometry.Offset(size.width, y1), stroke.width)
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, y2), androidx.compose.ui.geometry.Offset(size.width, y2), stroke.width)
        drawLine(color, androidx.compose.ui.geometry.Offset(0f, y3), androidx.compose.ui.geometry.Offset(size.width, y3), stroke.width)
        drawCircle(Color(0xFFE9BE88), 2.5.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * 0.68f, y1))
        drawCircle(color, 2.5.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * 0.3f, y2))
        drawCircle(color, 2.5.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * 0.7f, y3))
    }
}
