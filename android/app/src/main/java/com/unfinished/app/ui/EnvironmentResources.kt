package com.unfinished.app.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Borrowed from Window's BitmapDrawable; Window/Resources retain ownership. */
internal data class StartupSeaBitmap(@DrawableRes val resourceId: Int, val bitmap: Bitmap)

private data class LoadedEnvironmentBitmap(val resourceId: Int, val bitmap: Bitmap, val owned: Boolean)

@Composable
internal fun rememberBitmapResource(
    @DrawableRes resourceId: Int,
    enabled: Boolean = true,
    startupSea: StartupSeaBitmap? = null
): Bitmap? {
    val context = LocalContext.current
    val borrowed = startupSea?.takeIf {
        enabled && it.resourceId == resourceId && !it.bitmap.isRecycled
    }?.bitmap
    val initial = androidx.compose.runtime.remember(resourceId, borrowed) {
        borrowed?.let { LoadedEnvironmentBitmap(resourceId, it, owned = false) }
    }
    val state = produceState(initialValue = initial, context, resourceId, enabled, borrowed) {
        value = initial
        if (!enabled) return@produceState
        if (borrowed == null) value = withContext(Dispatchers.IO) {
            decodeBitmap(context, resourceId)?.let { LoadedEnvironmentBitmap(resourceId, it, owned = true) }
        }
    }
    val loaded = state.value
    DisposableEffect(loaded) {
        onDispose {
            // Never recycle a bitmap still used by the startup window. Only
            // this composable's independently decoded resources are owned here.
            // Ownership travels with the loaded result, not the *current* key:
            // changing time period must not reclassify the old borrowed bitmap.
            if (loaded?.owned == true) loaded.bitmap.takeUnless { it.isRecycled }?.recycle()
        }
    }
    return loaded?.takeIf { enabled && it.resourceId == resourceId }?.bitmap
}

private fun decodeBitmap(context: Context, @DrawableRes resourceId: Int): Bitmap? = runCatching {
    BitmapFactory.decodeResource(
        context.resources,
        resourceId,
        BitmapFactory.Options().apply {
            inPreferredConfig = Bitmap.Config.ARGB_8888
            inScaled = false
        }
    )
}.getOrNull()
