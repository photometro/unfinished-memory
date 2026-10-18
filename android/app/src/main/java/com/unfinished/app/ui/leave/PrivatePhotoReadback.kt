package com.unfinished.app.ui.leave

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.unfinished.app.data.CaptureGateway
import com.unfinished.app.data.CapturePhoto
import com.unfinished.app.data.VisibleReceipt
import com.unfinished.app.ui.LocalNativePalette
import kotlinx.coroutines.CancellationException

private data class ReadbackPhoto(val bitmap: Bitmap? = null, val failed: Boolean = false)

/** Minimal read-only adapter, not item13's list/search/recall redesign. */
@Composable
internal fun PrivatePhotoReadback(
    photos: List<CapturePhoto>, gateway: CaptureGateway, actionId: String,
    onPhotoVisible: suspend (CapturePhoto) -> Unit
) {
    if (photos.isEmpty()) return
    var index by rememberSaveable { mutableIntStateOf(0) }
    val photo = photos[index.coerceIn(0, photos.lastIndex)]
    val palette = LocalNativePalette.current
    val windowHeight = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.height.toDp() }
    val imageHeight = (windowHeight * .4f).coerceIn(96.dp, 320.dp)
    Column(Modifier.fillMaxWidth().padding(top = 24.dp)) {
        LeaveText("照片 ${index + 1} / ${photos.size}", palette.muted, 12)
        // Re-key the whole node. produceState by itself retains its old value
        // on a new key: that would pair the old bitmap with the new asset ID.
        key(photo.id) {
            var retry by remember { mutableIntStateOf(0) }
            val render by produceState(ReadbackPhoto(), photo.id, retry) {
                value = ReadbackPhoto()
                value = if (!photo.available) ReadbackPhoto(failed = true) else try {
                    ReadbackPhoto(bitmap = gateway.preview(photo))
                } catch (cancelled: CancellationException) { throw cancelled
                } catch (_: Exception) { ReadbackPhoto(failed = true) }
            }
            Box(Modifier.fillMaxWidth().height(imageHeight).padding(top = 12.dp)) {
                val image = render.bitmap
                if (image != null && !image.isRecycled) {
                    VisibleReceipt("$actionId:photo:${photo.id}", fullyVisible = true,
                        onVisible = { onPhotoVisible(photo) }) {
                        Image(image.asImageBitmap(), "已保存照片的显示预览", Modifier.fillMaxSize()
                            .testTag("past.photo.image"), contentScale = ContentScale.Fit)
                    }
                } else LeaveText(if (render.failed) "照片未能打开，尚未记录照片曝光。" else "正在读取照片…",
                    palette.muted, 13, Modifier.padding(top = 24.dp), tag = "past.photo.pending")
            }
            if (render.failed && photo.available) LeaveAction("重试照片", true, { retry++ }, tag = "past.photo.retry")
            // Bounded image is not cached or owned by a ViewModel. Let its last
            // display-list reference release pixels, never recycle mid-draw.
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            LeaveAction("上一张", true, { index-- }, enabled = index > 0, tag = "past.photo.previous")
            LeaveAction("下一张", true, { index++ }, enabled = index < photos.lastIndex, tag = "past.photo.next")
        }
    }
}
