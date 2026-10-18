package com.unfinished.app.ui.leave

import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** One mesh remains mounted from printed page through final hull and drift. */
@Composable
internal fun SealJourney(
    memoryId: String, quiet: Boolean, isNight: Boolean, reducedMotion: Boolean,
    snapshot: ImageBitmap?, animate: Boolean, error: String?, onComplete: () -> Unit, onBack: () -> Unit
) {
    val owner = LocalLifecycleOwner.current
    var active by remember(owner) { mutableStateOf(owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, _ -> active = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    val staticBoat = reducedMotion || snapshot == null
    val elapsed = remember(memoryId) { mutableFloatStateOf(if (animate && !staticBoat) 0f else 5450f) }
    val flowTime = remember(memoryId) { mutableFloatStateOf(0f) }
    var phase by remember(memoryId) { mutableStateOf(if (animate && !staticBoat) SealPhase.WITHDRAWING else SealPhase.COMPLETE) }
    val finish by rememberUpdatedState(onComplete)
    LaunchedEffect(active, animate, reducedMotion, quiet, memoryId) {
        if (reducedMotion || snapshot == null) {
            elapsed.floatValue = 5450f
            phase = SealPhase.COMPLETE
            if (animate) finish()
        }
        if (!active || (reducedMotion || quiet) && !animate) return@LaunchedEffect
        var previous = 0L
        var previousFlow = 0L
        while (active) {
            val now = withFrameNanos { it }
            val dt = if (previous == 0L) 0f else ((now - previous) / 1_000_000f).coerceIn(0f, 64f)
            previous = now
            if (animate && phase != SealPhase.COMPLETE && !staticBoat) {
                elapsed.floatValue += dt
                val frame = sealFrame(elapsed.floatValue, quiet)
                if (phase != frame.phase) phase = frame.phase
                if (phase == SealPhase.COMPLETE) finish()
            }
            // Trails run only at the leaf drawing node, at ~30 Hz. Completion
            // does not end ordinary currents; background/removal does.
            if (!quiet && !reducedMotion && now - previousFlow >= 32_000_000L) {
                flowTime.floatValue += if (previousFlow == 0L) .033f else min(.065f, (now - previousFlow) / 1_000_000_000f)
                previousFlow = now
            }
            if ((quiet || reducedMotion) && phase == SealPhase.COMPLETE) break
        }
    }
    val blue = if (isNight) Color(0xFF274758) else Color(0xFF3C6074)
    val sourceColor = if (isNight) Color(0xFF302D29) else Color(0xFFF2EADF)
    val ink = Color(0xFFF1F3EB)
    val muted = Color(0xFFC7D8DE)
    val density = LocalDensity.current
    var rootBounds by remember { mutableStateOf(Rect.Zero) }
    var boatBounds by remember { mutableStateOf(Rect.Zero) }
    Box(Modifier.fillMaxSize().background(blue).testTag("leave.journey")
        .semantics { stateDescription = phase.name }
        .onGloballyPositioned { rootBounds = it.boundsInRoot() }) {
        val renderer = remember(memoryId) { JourneyRenderer() }
        val raster = remember(snapshot) { snapshot?.asAndroidBitmap() }
        Canvas(Modifier.fillMaxSize().clearAndSetSemantics { }.testTag("leave.paper.mesh")) {
            val frame = if (staticBoat) SealFrame(1f,0f,SealPhase.COMPLETE) else sealFrame(elapsed.floatValue,quiet)
            val relative = boatBounds.translate(-rootBounds.left, -rootBounds.top)
            val desiredWidth = (size.width * .29f).coerceIn(96.dp.toPx(),132.dp.toPx())
            val targetWidth = if (relative.height > 0) min(desiredWidth,max(48.dp.toPx(),(relative.height-16.dp.toPx())/1.16f)) else desiredWidth
            val restingY = if (relative.height > 0) relative.center.y else size.height*.61f
            val flowAlpha = if (staticBoat) 1f else smooth((frame.fold-.72f)/.23f)
            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas
                if (!quiet) renderer.drawCurrent(native,size.width,size.height,flowTime.floatValue,flowAlpha,density.density)
                val showBoat = staticBoat || quiet || frame.phase != SealPhase.COMPLETE
                if (showBoat) renderer.drawPaper(native,size.width,size.height,frame,raster,sourceColor.toArgb(),
                    targetWidth,restingY,relative,stationary = quiet || staticBoat)
            }
        }
        BoxWithConstraints(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
            val availableHeight = maxHeight
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Column(Modifier.fillMaxWidth().heightIn(min = availableHeight), verticalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(top = 30.dp)
                        .graphicsLayer { alpha = if (staticBoat) 1f else smooth((sealFrame(elapsed.floatValue,quiet).fold-.72f)/.23f) }
                        .testTag("leave.journey.header")) {
                        LeaveText(if (quiet) "静存" else "交给时间",muted,12)
                        LeaveText(if (quiet) "静静留下。" else "这一段，\n随时间流动。",ink,29,Modifier.padding(top = 17.dp))
                        LeaveText(if (quiet) "不主动上岸。想看的时候，它仍在「过去」。" else "不必知道它去向哪里。\n此刻，先让它离开眼前。",
                            muted,13,Modifier.padding(top = 20.dp))
                    }
                    Box(Modifier.fillMaxWidth().heightIn(min = 140.dp).onGloballyPositioned { boatBounds = it.boundsInRoot() })
                    Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(top = 20.dp,bottom = 25.dp)
                        .graphicsLayer { alpha = if (staticBoat) 1f else smooth((sealFrame(elapsed.floatValue,quiet).fold-.6f)/.2f) }
                        .testTag("leave.journey.footer")) {
                        if (error != null) LeaveText(error,muted,12,Modifier.padding(bottom = 8.dp),tag = "leave.journey.warning")
                        LeaveText(journeyStatus(phase,quiet),muted,13,Modifier.padding(bottom = 16.dp),tag = "leave.journey.status")
                        LeaveAction(if (phase == SealPhase.COMPLETE) "回到岸边 →" else "跳过动效，回到岸边 →",
                            reducedMotion,onBack,Modifier.fillMaxWidth(),solid = true,tag = "leave.journey.back")
                        LeaveText("本地保留 · 不依赖联网",muted,10,Modifier.padding(top = 14.dp))
                    }
                }
            }
        }
    }
}

private fun journeyStatus(phase: SealPhase, quiet: Boolean): String = when (phase) {
    SealPhase.WITHDRAWING -> "内容已留下。"
    SealPhase.LIFTING -> "把这一页，轻轻收好。"
    SealPhase.FOLDING -> "刚才写下的，藏进折痕。"
    SealPhase.FORMING -> "折起两角，留下船的形状。"
    SealPhase.OPENING -> "沿着折痕，轻轻展开。"
    SealPhase.READY -> "这一段，折成了一艘纸船。"
    SealPhase.DRIFTING -> "让洋流，带它远一点。"
    SealPhase.COMPLETE -> if (quiet) "已静静收存。" else "已封存。它只是离开眼前，仍保留在「过去」。"
}

private class CurrentParticle(
    var point: PaperPoint, val trail: MutableList<PaperPoint>, val length: Int,
    val alpha: Float, val width: Float, var age: Float, val life: Float
)

/** Reusable native paints/paths; animation clocks are not read by whole pages. */
private class JourneyRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { isFilterBitmap = true }
    private val tracePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val path = Path()
    private val matrix = Matrix()
    private val faceOrder = intArrayOf(5,6,7,8,9,0,1,2,3,4)
    private val lightColors = intArrayOf(0xffc3ccc1.toInt(),0xfffcf8ed.toInt(),0xffe3e0d2.toInt(),0xffb9c7bd.toInt(),0xffe4e7d8.toInt(),0xffe8e6d8.toInt(),0xffe8e6d8.toInt(),0xfff5f0e2.toInt(),0xfff5f0e2.toInt(),0xfff5f0e2.toInt())
    private val shadeColors = intArrayOf(0xff829a92.toInt(),0xffe9e4d6.toInt(),0xffb5c0b8.toInt(),0xff718b83.toInt(),0xffc5cfc2.toInt(),0xffc5cec4.toInt(),0xffc5cec4.toInt(),0xffdeddd0.toInt(),0xffdeddd0.toInt(),0xffdeddd0.toInt())
    private var fieldWidth = 0f
    private var fieldHeight = 0f
    private var fieldTime = 0f
    private var random = Random(73421)
    private val particles = mutableListOf<CurrentParticle>()
    private var carrierAspect = 0f
    private var carrierStart = 0f
    private var carrier = emptyList<PaperPoint>()
    private fun spawn(aspect: Float): CurrentParticle {
        val point = PaperPoint(random.nextFloat()*1.22f-.12f,.29f+random.nextFloat()*.61f)
        val length = 16+random.nextInt(31)
        val trail = mutableListOf(point)
        repeat(length-1) { trail += advanceCurrent(trail.last(),-.045f,aspect) }
        trail.reverse()
        return CurrentParticle(point,trail,length,.14f+random.nextFloat()*.26f,.45f+random.nextFloat()*.45f,0f,6+random.nextFloat()*12)
    }
    fun drawCurrent(canvas: android.graphics.Canvas, width: Float, height: Float, time: Float, opacity: Float, density: Float) {
        val aspect = (height/width).coerceAtLeast(.1f)
        if (fieldWidth != width || fieldHeight != height) {
            fieldWidth = width; fieldHeight = height; fieldTime = time; random = Random(73421); particles.clear()
            repeat((width/density*.52f).toInt().coerceIn(160,220)) { particles += spawn(aspect) }
        }
        val dt = (time-fieldTime).coerceIn(0f,.065f)
        fieldTime = time
        for (index in particles.indices) {
            var particle = particles[index]
            if (dt > 0f) {
                val next = advanceCurrent(particle.point,dt,aspect)
                if (particle.age+dt > particle.life || next.x < -.3f || next.x > 1.3f || next.y < .15f || next.y > 1.02f) {
                    particle = spawn(aspect); particles[index] = particle
                } else {
                    particle.point = next; particle.age += dt; particle.trail += next
                    if (particle.trail.size > particle.length) particle.trail.removeAt(0)
                }
            }
            val middleY = particle.point.y
            val mask = smooth((middleY-.24f)/.19f)*(1-smooth((middleY-.72f)/.19f))
            val fade = min(1f,(particle.life-particle.age)/.8f)*opacity*mask
            if (fade < .001f) continue
            for (band in 0..2) {
                val start = (particle.trail.lastIndex*band/3)
                val end = min(particle.trail.lastIndex,(particle.trail.lastIndex*(band+1)+2)/3)
                path.reset()
                for (i in start..end) {
                    val point = particle.trail[i]
                    if (i == start) path.moveTo(point.x*width,point.y*height) else path.lineTo(point.x*width,point.y*height)
                }
                tracePaint.color = Color(0xFFDAEBEA).copy(alpha = (particle.alpha*fade*(.25f+band*.3f)).coerceIn(0f,1f)).toArgb()
                tracePaint.strokeWidth = particle.width*density
                canvas.drawPath(path,tracePaint)
            }
        }
    }
    fun drawPaper(
        canvas: android.graphics.Canvas, width: Float, height: Float, frame: SealFrame,
        bitmap: android.graphics.Bitmap?, sourceColor: Int, targetWidth: Float,
        restingY: Float, allowed: Rect, stationary: Boolean
    ) {
        val p = frame.fold
        val aspect = (height/width).coerceAtLeast(.1f)
        val start = restingY/height
        if (carrier.isEmpty() || carrierAspect != aspect || carrierStart != start) {
            carrierAspect = aspect; carrierStart = start; carrier = carrierPath(aspect,start)
        }
        val point = if (stationary) PaperPoint(.5f,start) else samplePath(carrier,frame.drift)
        val halfSpan = targetWidth*.58f
        val safeMin = if (allowed.height > 0) allowed.top+halfSpan else height*.25f
        val safeMax = if (allowed.height > 0) allowed.bottom-halfSpan else height*.78f
        val centerY = point.y*height
        // Never draw the drifting hull over actual wrapped header/footer blocks.
        val nearBoundary = smooth((centerY-safeMin)/max(height*.025f,1f))
        val boundFade = if (stationary) 1f else 1f + (nearBoundary - 1f) * smooth(frame.drift / .12f)
        val driftFade = if (stationary) 1f else (1-smooth((frame.drift-.63f)/.37f))*boundFade
        if (driftFade < .001f) return
        val targetY = if (stationary) restingY else max(safeMin,min(safeMax,centerY))
        val centerX = width/2+(point.x*width-width/2)*smooth(p)
        val destinationY = height/2+(targetY-height/2)*smooth(p)
        val printedSize = bitmap?.let { printedPageSize(width,height,it.width.toFloat(),it.height.toFloat()) }
            ?: PaperPoint(width,height)
        val scale = paperScale(p,printedSize.x,targetWidth)*(if (stationary) 1f else 1-.4f*frame.drift)
        val rotation = if (stationary) -12f*smooth((p-.67f)/.33f)
            else (-6+(carrierRotation(point,aspect)+6)*min(1f,frame.drift/.12f))*smooth((p-.67f)/.33f)
        // Keep the printed page's aspect during a live window height change.
        // Window constraints place the hull, not the authored paper net.
        val pose = paperPose(p,printedSize.x,printedSize.y)
        canvas.save()
        canvas.translate(centerX,destinationY); canvas.rotate(rotation); canvas.scale(scale,scale)
        paint.shader = null; paint.alpha = (driftFade*255).toInt(); paint.style = Paint.Style.FILL
        polygon(pose.edge)
        paint.color = mixColor(sourceColor,0xffe9e4d6.toInt(),smooth((p-.08f)/.75f),driftFade)
        canvas.drawPath(path,paint)
        for (index in faceOrder) {
            val triangle = pose.triangle(index)
            polygon(triangle)
            val paper = smooth((p-.08f)/.55f)
            val material = smooth((p-.65f)/.35f)
            val light = mixColor(mixColor(sourceColor,0xfff3eee1.toInt(),paper),lightColors[index],material,driftFade)
            val shade = mixColor(mixColor(sourceColor,0xffe5e2d5.toInt(),paper),shadeColors[index],material,driftFade)
            paint.shader = LinearGradient(0f,-printedSize.y/2,printedSize.x*.3f,printedSize.y/2,light,shade,Shader.TileMode.CLAMP)
            paint.alpha = 255
            canvas.drawPath(path,paint)
            paint.shader = null
            val printed = if (index <= 4 || index == 9) pose.printTop else pose.printBottom
            if (printed && bitmap != null && !bitmap.isRecycled) {
                canvas.save(); canvas.clipPath(path)
                matrix.setValues(affineTriangle(sourceTriangle(bitmap.width.toFloat(),bitmap.height.toFloat(),index),triangle))
                paint.alpha = (driftFade*255).toInt(); paint.color = android.graphics.Color.WHITE
                canvas.drawBitmap(bitmap,matrix,paint); canvas.restore()
            }
        }
        val creaseAlpha = smooth((p-.44f)/.56f)*.65f*driftFade
        tracePaint.color = Color(0xFFFFFBED).copy(alpha = creaseAlpha).toArgb(); tracePaint.strokeWidth = printedSize.x/160*.65f
        canvas.drawLine(pose.edge[2].x,pose.edge[2].y,pose.hub.x,pose.hub.y,tracePaint)
        canvas.restore()
    }
    private fun polygon(points: List<PaperPoint>) {
        path.reset(); path.moveTo(points.first().x,points.first().y)
        for (point in points.drop(1)) path.lineTo(point.x,point.y)
        path.close()
    }
    private fun mixColor(source: Int, target: Int, amount: Float, opacity: Float = 1f): Int {
        val p = amount.coerceIn(0f,1f)
        fun channel(shift: Int): Int { val a = source shr shift and 255; val b = target shr shift and 255; return (a+(b-a)*p).toInt().coerceIn(0,255) }
        return android.graphics.Color.argb((opacity*255).toInt().coerceIn(0,255),channel(16),channel(8),channel(0))
    }
}
