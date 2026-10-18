package com.unfinished.app.data

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import kotlinx.coroutines.delay

val LocalReceiptAllowed = staticCompositionLocalOf { true }

/** Attach ONLY to successfully loaded content, never a placeholder or a loading container. */
@Composable
fun VisibleReceipt(actionId: String, enabled: Boolean = true, fullyVisible: Boolean = false,
                   onVisible: suspend () -> Unit, content: @Composable () -> Unit) {
    val view = LocalView.current
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    var bounds by remember(actionId) { mutableStateOf(Rect.Zero) }
    var fullBounds by remember(actionId) { mutableStateOf(Rect.Zero) }
    val callback by rememberUpdatedState(onVisible)
    val allowed = LocalReceiptAllowed.current
    fun canSee(): Boolean {
        val viewport = Rect(0f, 0f, view.width.toFloat(), view.height.toFloat())
        return bounds.width > 0 && bounds.height > 0 && bounds.overlaps(viewport) &&
            (!fullyVisible || (bounds.left >= 0 && bounds.top >= 0 && bounds.right <= viewport.right && bounds.bottom <= viewport.bottom)) &&
            (!fullyVisible || (kotlin.math.abs(bounds.width - fullBounds.width) < 1 && kotlin.math.abs(bounds.height - fullBounds.height) < 1)) &&
            view.hasWindowFocus() && view.isShown
    }
    LaunchedEffect(actionId, enabled, allowed, lifecycle) {
        if (!enabled || !allowed || lifecycle != Lifecycle.State.RESUMED) return@LaunchedEffect
        while (true) {
            if (canSee()) {
                withFrameNanos { }
                withFrameNanos { }
                if (canSee()) {
                    // Retry transient DB failures with the same ID; storage is the dedupe authority.
                    if (runCatching { callback() }.isSuccess) break
                }
            }
            delay(100)
        }
    }
    Box(Modifier.onGloballyPositioned {
        bounds = it.boundsInWindow()
        val root = it.findRootCoordinates()
        fullBounds = root.localBoundingBoxOf(it, clipBounds = false).translate(root.localToWindow(Offset.Zero))
    }) { content() }
}
