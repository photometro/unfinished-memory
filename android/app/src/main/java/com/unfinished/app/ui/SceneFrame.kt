package com.unfinished.app.ui

/** Keep a shore's framing stable while a window becomes shorter, without stretching
 * the image or using display dimensions to position any interactive content. */
internal fun sceneFrameHeight(width: Float, height: Float, referenceAspect: Float?): Float {
    if (width <= 0f || !width.isFinite() || height <= 0f || !height.isFinite() ||
        referenceAspect == null || !referenceAspect.isFinite() || referenceAspect <= 0f
    ) return height
    return maxOf(height, width / referenceAspect)
}
