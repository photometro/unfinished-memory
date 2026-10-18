package com.unfinished.app.ui.leave

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Shared-vertex authored paper net, ported from the protected Study18 geometry. */
internal data class PaperPoint(val x: Float, val y: Float) {
    fun mix(other: PaperPoint, amount: Float) = PaperPoint(x + (other.x - x) * amount, y + (other.y - y) * amount)
}
internal data class PaperPose(val edge: List<PaperPoint>, val hub: PaperPoint, val printTop: Boolean, val printBottom: Boolean) {
    fun triangle(index: Int) = listOf(edge[index], edge[(index + 1) % edge.size], hub)
}
internal val paperEdge = listOf(
    PaperPoint(0f,0f), PaperPoint(.25f,0f), PaperPoint(.5f,0f), PaperPoint(.75f,0f), PaperPoint(1f,0f),
    PaperPoint(1f,.5f), PaperPoint(1f,1f), PaperPoint(.5f,1f), PaperPoint(0f,1f), PaperPoint(0f,.5f)
)
internal val boatEdge = listOf(
    PaperPoint(12f,47f), PaperPoint(58f,33f), PaperPoint(84f,10f), PaperPoint(107f,36f), PaperPoint(132f,42f),
    PaperPoint(148f,40f), PaperPoint(113f,79f), PaperPoint(78f,81f), PaperPoint(46f,82f), PaperPoint(34f,66f)
)
internal val boatHub = PaperPoint(83f,63f)
internal fun smooth(value: Float): Float = value.coerceIn(0f, 1f).let { it * it * (3 - 2 * it) }
internal fun printedPageSize(viewportWidth: Float, viewportHeight: Float, sourceWidth: Float, sourceHeight: Float): PaperPoint {
    require(viewportWidth > 0 && viewportHeight > 0 && sourceWidth > 0 && sourceHeight > 0)
    val fit = min(viewportWidth / sourceWidth, viewportHeight / sourceHeight)
    // Clamp floating-point rounding at the containing viewport boundary.
    return PaperPoint(min(viewportWidth, sourceWidth * fit), min(viewportHeight, sourceHeight * fit))
}
internal fun sourceTriangle(width: Float, height: Float, index: Int) = listOf(
    PaperPoint(paperEdge[index].x * width, paperEdge[index].y * height),
    PaperPoint(paperEdge[(index + 1) % 10].x * width, paperEdge[(index + 1) % 10].y * height), PaperPoint(width/2, height/2)
)
internal fun paperPose(progress: Float, width: Float, height: Float): PaperPose {
    require(width > 0 && height > 0)
    val p = progress.coerceIn(0f,1f)
    val initial = paperEdge.map { PaperPoint((it.x-.5f)*width,(it.y-.5f)*height) } + PaperPoint(0f,0f)
    val angle = PI.toFloat()*smooth((p-.035f)/.405f)
    var points: List<PaperPoint> = if (p <= .44f) {
        val perspective = max(width,height)*3
        initial.map { if (it.y >= 0) it else {
            val ratio = perspective/(perspective + it.y*sin(angle))
            PaperPoint(it.x*ratio,it.y*cos(angle)*ratio)
        } }
    } else {
        val closed = initial.map { PaperPoint(it.x,kotlin.math.abs(it.y)) }
        val roof = listOf(PaperPoint(-.5f,.20f),PaperPoint(-.25f,.20f),PaperPoint(0f,-.36f),PaperPoint(.25f,.20f),
            PaperPoint(.5f,.20f),PaperPoint(.5f,.24f),PaperPoint(.5f,.26f),PaperPoint(0f,.26f),PaperPoint(-.5f,.26f),PaperPoint(-.5f,.24f),PaperPoint(0f,.20f))
            .map { PaperPoint(it.x*width,it.y*width) }
        val hat = listOf(PaperPoint(-.48f,.20f),PaperPoint(-.24f,-.08f),PaperPoint(0f,-.36f),PaperPoint(.24f,-.08f),
            PaperPoint(.48f,.20f),PaperPoint(.48f,.20f),PaperPoint(.18f,.24f),PaperPoint(0f,.24f),PaperPoint(-.18f,.24f),PaperPoint(-.48f,.20f),PaperPoint(0f,.16f))
            .map { PaperPoint(it.x*width,it.y*width) }
        val boat = (boatEdge+boatHub).map { PaperPoint((it.x-80)*width/160,(it.y-50)*width/160) }
        when {
            p < .67f -> closed.mapIndexed { i, point -> point.mix(roof[i],smooth((p-.44f)/.23f)) }
            p < .84f -> roof.mapIndexed { i, point -> point.mix(hat[i],smooth((p-.67f)/.17f)) }
            else -> hat.mapIndexed { i, point -> point.mix(boat[i],smooth((p-.84f)/.16f)) }
        }
    }
    val centerY = (points.minOf { it.y }+points.maxOf { it.y })/2
    points = points.map { PaperPoint(it.x,it.y-centerY*(1-smooth((p-.84f)/.16f))) }
    return PaperPose(points.take(10),points.last(),p <= .44f && cos(angle) > .015f,p < .44f)
}
/** Android Matrix-compatible 3x3; source triangles remain nondegenerate. */
internal fun affineTriangle(source: List<PaperPoint>, destination: List<PaperPoint>): FloatArray {
    val (a,b,c) = source; val (u,v,w) = destination
    val determinant = (b.x-a.x)*(c.y-a.y)-(c.x-a.x)*(b.y-a.y)
    require(kotlin.math.abs(determinant) > .00001f)
    val xx = ((v.x-u.x)*(c.y-a.y)-(w.x-u.x)*(b.y-a.y))/determinant
    val xy = ((w.x-u.x)*(b.x-a.x)-(v.x-u.x)*(c.x-a.x))/determinant
    val yx = ((v.y-u.y)*(c.y-a.y)-(w.y-u.y)*(b.y-a.y))/determinant
    val yy = ((w.y-u.y)*(b.x-a.x)-(v.y-u.y)*(c.x-a.x))/determinant
    return floatArrayOf(xx,xy,u.x-xx*a.x-xy*a.y,yx,yy,u.y-yx*a.x-yy*a.y,0f,0f,1f)
}
internal fun paperScale(p: Float, width: Float, targetBoatWidth: Float): Float {
    val stops = floatArrayOf(0f,.44f,.67f,.84f,1f); val scales = floatArrayOf(1f,.8f,.67f,.49f,targetBoatWidth/width)
    val index = (0..3).firstOrNull { p <= stops[it+1] } ?: 3
    return scales[index]+(scales[index+1]-scales[index])*smooth((p-stops[index])/(stops[index+1]-stops[index]))
}
private val currentEddies = arrayOf(floatArrayOf(.06f,.49f,.26f,-2.9f),floatArrayOf(.94f,.78f,.32f,2.4f))
internal fun currentVelocity(point: PaperPoint, aspect: Float): PaperPoint {
    var vx = .21f+.025f*sin(point.y*8+point.x*2); var vy = -.095f+.022f*sin(point.x*5-point.y*3)
    for (eddy in currentEddies) {
        val dx = point.x-eddy[0]; val dy = (point.y-eddy[1])*aspect; val weight = exp(-(dx*dx+dy*dy)/(eddy[2]*eddy[2]))
        vx = vx*(1-.72f*weight)-dy*eddy[3]*weight; vy = vy*(1-.72f*weight)+dx*eddy[3]*weight/aspect
    }
    return PaperPoint(vx,vy)
}
internal fun advanceCurrent(point: PaperPoint, dt: Float, aspect: Float): PaperPoint {
    val velocity = currentVelocity(point,aspect); val mid = currentVelocity(PaperPoint(point.x+velocity.x*dt/2,point.y+velocity.y*dt/2),aspect)
    return PaperPoint(point.x+mid.x*dt,point.y+mid.y*dt)
}
internal fun carrierPath(aspect: Float, startY: Float): List<PaperPoint> {
    val path = mutableListOf(PaperPoint(.39f,startY)); repeat(120) { path += advanceCurrent(path.last(),3.2f/120,aspect) }; return path
}
internal fun samplePath(path: List<PaperPoint>, progress: Float): PaperPoint {
    val position = progress.coerceIn(0f,1f)*(path.size-1); val index = position.toInt()
    return path[index].mix(path[min(path.lastIndex,index+1)],position%1)
}
internal fun carrierRotation(point: PaperPoint, aspect: Float): Float = currentVelocity(point,aspect).let { atan2(it.y*aspect,it.x)*180/PI.toFloat()+12 }
internal enum class SealPhase { WITHDRAWING, LIFTING, FOLDING, FORMING, OPENING, READY, DRIFTING, COMPLETE }
internal data class SealFrame(val fold: Float, val drift: Float, val phase: SealPhase)
internal fun sealFrame(milliseconds: Float, quiet: Boolean = false): SealFrame = when {
    milliseconds < 180 -> SealFrame(0f,0f,SealPhase.WITHDRAWING)
    milliseconds < 1280 -> SealFrame(.44f*((milliseconds-180)/1100),0f,SealPhase.LIFTING)
    milliseconds < 1930 -> SealFrame(.44f+.23f*((milliseconds-1280)/650),0f,SealPhase.FOLDING)
    milliseconds < 2480 -> SealFrame(.67f+.17f*((milliseconds-1930)/550),0f,SealPhase.FORMING)
    milliseconds < 3130 -> SealFrame(.84f+.16f*((milliseconds-2480)/650),0f,SealPhase.OPENING)
    quiet -> SealFrame(1f,0f,SealPhase.COMPLETE)
    milliseconds < 3350 -> SealFrame(1f,0f,SealPhase.READY)
    milliseconds < 5450 -> SealFrame(1f,smooth((milliseconds-3350)/2100),SealPhase.DRIFTING)
    else -> SealFrame(1f,1f,SealPhase.COMPLETE)
}
