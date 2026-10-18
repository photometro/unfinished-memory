package com.unfinished.app.ui.leave

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.CreationExtras
import com.unfinished.app.data.CaptureCue
import com.unfinished.app.data.CaptureDraft
import com.unfinished.app.data.CaptureGateway
import com.unfinished.app.data.CapturePhoto
import com.unfinished.app.ui.DarkPalette
import com.unfinished.app.ui.EmbeddedChineseFont
import com.unfinished.app.ui.LightPalette
import com.unfinished.app.ui.NativePalette
import kotlinx.coroutines.CancellationException
import kotlin.math.min
import kotlin.math.sqrt

/** The only production entry; the parent remains responsible for system exit. */
@Composable
fun NativeLeaveRoute(gateway: CaptureGateway, onBack: () -> Unit, isNight: Boolean, reducedMotion: Boolean) {
    val owner = LocalLifecycleOwner.current
    val storeOwner = owner as? ViewModelStoreOwner
        ?: error("The leaving route requires an Activity ViewModel owner")
    val controller = remember(storeOwner) {
        ViewModelProvider(storeOwner, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                @Suppress("UNCHECKED_CAST")
                return LeaveController(gateway, extras.createSavedStateHandle()) as T
            }
        })["item12.leave", LeaveController::class.java]
    }
    NativeLeaveContent(controller, gateway, onBack, isNight, reducedMotion)
}

/** Synthetic gateways/controller are the approved test seam; no user DB here. */
@Composable
internal fun NativeLeaveContent(
    controller: LeaveController, gateway: CaptureGateway, onBack: () -> Unit,
    isNight: Boolean, reducedMotion: Boolean
) {
    val state by controller.state.collectAsStateWithLifecycle()
    val palette = if (isNight) DarkPalette else LightPalette
    val owner = LocalLifecycleOwner.current
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val density = LocalDensity.current
    val imeVisible = WindowInsets.ime.getBottom(density) > 0
    val latestBack by rememberUpdatedState(onBack)
    var retryFailedItems by remember { mutableStateOf(false) }
    val remainingPhotoSlots = (LEAVE_PHOTO_LIMIT - state.draft.photos.size).coerceAtLeast(0)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(remainingPhotoSlots.coerceAtLeast(2))) { uris ->
        controller.importPhotos(uris, retryFailedItems)
        retryFailedItems = false
    }
    val singlePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) controller.importPhotos(listOf(uri), retryFailedItems)
        retryFailedItems = false
    }
    val openPicker: (Boolean) -> Unit = { retry ->
        focus.clearFocus(); keyboard?.hide()
        retryFailedItems = retry
        if (remainingPhotoSlots == 1) singlePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        else if (remainingPhotoSlots > 1) picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val back: () -> Unit = {
        when {
            imeVisible -> { keyboard?.hide(); focus.clearFocus() }
            !state.ready -> latestBack()
            state.page == LeavePage.JOURNEY || state.page == LeavePage.COMPLETE -> controller.exitComplete(latestBack)
            state.operationPending -> Unit
            state.page == LeavePage.PHOTO -> controller.leavePhoto()
            state.page == LeavePage.PREFERENCE -> controller.navigate(LeavePage.POLICY)
            state.page == LeavePage.POLICY || state.page == LeavePage.BACKGROUND -> controller.navigate(LeavePage.REVIEW)
            state.page == LeavePage.REVIEW -> controller.navigate(LeavePage.WRITING)
            else -> controller.navigate(LeavePage.WRITING, latestBack)
        }
    }
    BackHandler { back() }
    DisposableEffect(owner, controller) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) controller.checkpoint() }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            controller.checkpoint()
            if (controller.state.value.page == LeavePage.JOURNEY) controller.finishJourney()
        }
    }

    // Only a bounded in-memory rendering of the real review page is captured.
    // Nothing is written to disk or passed to analytics/AI/backends.
    val snapshotLayer = rememberGraphicsLayer()
    var pageSize by remember { mutableStateOf(IntSize.Zero) }
    var captureRequested by remember { mutableStateOf(false) }
    var captured by remember { mutableStateOf<ImageBitmap?>(null) }
    var consumedReceipt by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(state.receipt?.memoryId, state.receipt?.alreadySealed, reducedMotion) {
        val receipt = state.receipt ?: return@LaunchedEffect
        if (consumedReceipt == receipt.memoryId) return@LaunchedEffect
        focus.clearFocus(); keyboard?.hide()
        if (receipt.alreadySealed || reducedMotion) {
            controller.startJourney(false)
            consumedReceipt = receipt.memoryId
        } else {
            try {
                captureRequested = true
                withFrameNanos { }
                withFrameNanos { }
                check(pageSize.width > 0 && pageSize.height > 0) { "Review viewport is unavailable" }
                captured = snapshotLayer.toImageBitmap()
                check(captured != null) { "Review capture is unavailable" }
                captureRequested = false
                controller.startJourney(true)
                consumedReceipt = receipt.memoryId
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (_: RuntimeException) { controller.snapshotFailed(); consumedReceipt = receipt.memoryId
            } catch (_: OutOfMemoryError) { controller.snapshotFailed(); consumedReceipt = receipt.memoryId
            } finally { captureRequested = false }
        }
    }
    // Display lists can outlive composition disposal. These bounded, uncached
    // UI bitmaps become collectible with their last renderer reference; never
    // recycle pixels while Compose may still draw the outgoing layer.

    if (state.page == LeavePage.JOURNEY || state.page == LeavePage.COMPLETE) {
        SealJourney(
            memoryId = state.draft.id, quiet = state.draft.policy == "QUIET", isNight = isNight,
            reducedMotion = reducedMotion, snapshot = captured,
            animate = state.page == LeavePage.JOURNEY, error = state.error,
            onComplete = controller::finishJourney,
            onBack = { controller.exitComplete(latestBack) }
        )
        return
    }
    Box(Modifier.fillMaxSize().background(palette.leave).testTag("leave.viewport")
        .onSizeChanged { pageSize = it }
        .drawWithContent {
            if (captureRequested && state.page == LeavePage.REVIEW) {
                val bounded = boundedSnapshotSize(size.width.toInt(), size.height.toInt())
                val ratio = bounded.width.toFloat() / size.width
                snapshotLayer.record(size = bounded) {
                    scale(ratio, pivot = Offset.Zero) { this@drawWithContent.drawContent() }
                }
            }
            drawContent()
        }
    ) {
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).imePadding()) {
          Box(Modifier.fillMaxSize().testTag("leave.safe.viewport")) {
            if (!state.ready) {
                Column(Modifier.fillMaxSize().padding(28.dp), verticalArrangement = Arrangement.Center) {
                    LeaveText(state.error ?: "正在打开草稿…", palette.ink)
                    if (state.error != null) LeaveAction("重试", reducedMotion, { controller.retryLoad() }, tag = "leave.retry.load")
                    LeaveAction("返回岸边", reducedMotion, latestBack)
                }
            } else if (state.page == LeavePage.PHOTO) {
                PhotoPage(state, gateway, controller, palette, reducedMotion, back)
            } else {
                LeaveTaskPage(state, gateway, controller, palette, isNight, reducedMotion, back, openPicker,
                    exitDraft = { controller.navigate(LeavePage.WRITING, latestBack) })
            }
          }
        }
    }
}

/** <= 1 megapixel / 4 MiB raster; screen dimensions, never a device-model rule. */
internal fun boundedSnapshotSize(width: Int, height: Int): IntSize {
    val pixels = width.coerceAtLeast(1).toLong() * height.coerceAtLeast(1)
    val scale = min(1.0, sqrt(1_048_576.0 / pixels))
    return IntSize((width * scale).toInt().coerceAtLeast(1), (height * scale).toInt().coerceAtLeast(1))
}

@Composable
private fun LeaveTaskPage(
    state: LeaveState, gateway: CaptureGateway, controller: LeaveController,
    palette: NativePalette, isNight: Boolean, reduced: Boolean, back: () -> Unit, pick: (Boolean) -> Unit,
    exitDraft: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        val gutter = (maxWidth * .06f).coerceIn(22.dp, 38.dp)
        val scroll = rememberScrollState()
        Column(
            Modifier.fillMaxSize().verticalScroll(scroll).testTag("leave.scroll"),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(Modifier.widthIn(max = 600.dp).fillMaxWidth().heightIn(min = viewportHeight),
                verticalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.fillMaxWidth()) {
                    val title = when (state.page) {
                        LeavePage.WRITING -> "留下"; LeavePage.REVIEW -> "封存前"; LeavePage.POLICY -> "怎样回来"
                        LeavePage.BACKGROUND -> "补充背景"; LeavePage.PREFERENCE -> "回忆偏好"; else -> "留下"
                    }
                    LeaveHeader(title, back, palette, reduced, gutter, state.operationPending,
                        when (state.page) { LeavePage.WRITING -> "继续"; LeavePage.REVIEW -> null; else -> "完成" }) {
                        controller.navigate(if (state.page == LeavePage.WRITING) LeavePage.REVIEW
                            else if (state.page == LeavePage.PREFERENCE) LeavePage.POLICY else LeavePage.REVIEW,
                            validatePolicy = true)
                    }
                    Column(Modifier.fillMaxWidth().padding(horizontal = gutter).padding(top = 24.dp, bottom = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        when (state.page) {
                            LeavePage.WRITING -> WritingBody(state, gateway, controller, palette, reduced, viewportHeight)
                            LeavePage.REVIEW -> ReviewBody(state, gateway, controller, palette, reduced)
                            LeavePage.POLICY -> PolicyBody(state, controller, palette, reduced)
                            LeavePage.BACKGROUND -> BackgroundBody(state, controller, palette, reduced)
                            LeavePage.PREFERENCE -> PreferenceBody(state, controller, palette, reduced)
                            else -> Unit
                        }
                        ImportErrors(state, controller, palette, reduced, pick)
                        if (state.draft.maintenanceWarning.isNotBlank()) LeaveText(state.draft.maintenanceWarning,
                            palette.muted, 12, tag = "leave.maintenance.warning")
                        if (state.error != null) LeaveText(state.error, Color(0xFF9D5145), 12, tag = "leave.error")
                        if (state.page != LeavePage.REVIEW && state.page != LeavePage.WRITING) {
                            LeaveText(if (state.saving) "正在保留草稿…" else if (state.saved) "修改已保留" else "", palette.muted, 11)
                        }
                    }
                }
                Column(Modifier.fillMaxWidth().padding(horizontal = gutter).padding(top = 12.dp, bottom = 22.dp).testTag("leave.footer")) {
                    when (state.page) {
                        LeavePage.WRITING -> {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                LeaveAction(if (state.importing) "读取照片中…" else "＋ 添加照片", reduced, { pick(false) },
                                    enabled = !state.operationPending && state.draft.photos.size < LEAVE_PHOTO_LIMIT,
                                    tag = "leave.add.photos")
                                LeaveAction("暂存草稿", reduced, exitDraft,
                                    enabled = !state.operationPending && state.draft.hasContent, tag = "leave.save.draft")
                            }
                            LeaveText(if (state.saving) "正在保留草稿…" else if (state.saved) "草稿已保留" else "", palette.muted, 11, tag = "leave.saved.state")
                        }
                        LeavePage.REVIEW -> {
                            val waiting = state.sealing || state.draft.sealed
                            LeaveAction(if (waiting) "正在收好…" else if (state.draft.policy == "QUIET") "静存这一段 →" else "封存这一段 →",
                                reduced, { controller.seal() }, Modifier.fillMaxWidth(), enabled = !state.operationPending,
                                solid = true, isNight = isNight, tag = "leave.seal")
                            LeaveText("封存后，仍可在「过去」查看。", palette.muted, 11,
                                Modifier.fillMaxWidth().padding(top = 12.dp), centered = true)
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaveHeader(
    title: String, back: () -> Unit, palette: NativePalette, reduced: Boolean, gutter: Dp,
    disabled: Boolean = false, action: String? = null, onAction: () -> Unit = {}
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = gutter).padding(top = 16.dp, bottom = 12.dp).testTag("leave.header"),
        verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f)) { LeaveAction("←", reduced, back, enabled = !disabled, tag = "leave.back", description = "返回") }
        LeaveText(title, palette.ink, 15, weight = FontWeight.Medium, tag = "leave.page.title")
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            if (action != null) LeaveAction(action, reduced, onAction, enabled = !disabled, tag = "leave.next")
        }
    }
}

@Composable
private fun WritingBody(state: LeaveState, gateway: CaptureGateway, controller: LeaveController, palette: NativePalette, reduced: Boolean, viewportHeight: Dp) {
    val photoHeight = if (state.draft.photos.isEmpty()) 0.dp else 160.dp
    LeaveField(state.draft.text, { controller.edit { draft -> draft.copy(text = it) } }, "写下想留下的片刻…",
        palette, "leave.text", enabled = !state.locked, fontSize = 18,
        modifier = Modifier.heightIn(min = (viewportHeight - 204.dp - photoHeight).coerceAtLeast(96.dp)), underline = false, singleLine = false)
    PhotoStrip(state, gateway, controller, palette, reduced, removable = true)
}

@Composable
private fun ReviewBody(state: LeaveState, gateway: CaptureGateway, controller: LeaveController, palette: NativePalette, reduced: Boolean) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflows by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        LeaveText("这一段", palette.muted, 13)
        LeaveAction("编辑 ↗", reduced, { controller.navigate(LeavePage.WRITING) }, enabled = !state.operationPending, tag = "leave.edit.original")
    }
    if (state.draft.text.isNotEmpty()) {
        Text(state.draft.text, Modifier.fillMaxWidth().testTag("leave.review.original"), color = palette.ink,
            fontFamily = EmbeddedChineseFont, fontSize = 20.sp, lineHeight = 37.sp,
            maxLines = if (expanded) Int.MAX_VALUE else 4, overflow = TextOverflow.Ellipsis,
            onTextLayout = { overflows = it.hasVisualOverflow })
        if (overflows || expanded) LeaveAction(if (expanded) "收起 ↑" else "展开全文 ↓", reduced, { expanded = !expanded }, tag = "leave.expand.original")
    }
    PhotoStrip(state, gateway, controller, palette, reduced, removable = false)
    CueChoices(state, controller, palette, reduced)
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        LeaveText("以后", palette.muted, 12)
        LeaveAction(policyLabel(state.draft.policy) + if (state.draft.policy == "FIXED_DATE") " · ${state.fixedDateInput.ifBlank { "未选日期" }}" else "",
            reduced, { controller.navigate(LeavePage.POLICY) }, Modifier.fillMaxWidth(), enabled = !state.operationPending,
            tag = "leave.policy", trailing = "更改 →")
        val background = listOf(state.draft.occurredDate, state.draft.place, state.draft.people).filter { it.isNotBlank() }.joinToString(" · ")
        LeaveAction(if (background.isEmpty()) "补充背景 ＋" else "背景 · $background", reduced,
            { controller.navigate(LeavePage.BACKGROUND) }, enabled = !state.operationPending, tag = "leave.background")
    }
}

@Composable
private fun CueChoices(state: LeaveState, controller: LeaveController, palette: NativePalette, reduced: Boolean) {
    val selected = state.draft.cues.filterNot { it.id == "${state.draft.id}:self" }
    val candidates = remember(state.draft.text, state.draft.id) { localCueCandidates(state.draft.text, state.draft.id) }
    val options = (candidates.map { candidate ->
        selected.find { it.id == candidate.id || it.source == "LOCAL_EXTRACT" && it.value == candidate.value } ?: candidate
    } + selected)
        .distinctBy { it.id }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var writingOwn by rememberSaveable { mutableStateOf(false) }
    val self = state.draft.cues.firstOrNull { it.id == "${state.draft.id}:self" }?.value ?: ""
    Column(Modifier.fillMaxWidth().padding(top = 16.dp).drawBehind {
        drawLine(palette.hairline, Offset.Zero, Offset(size.width,0f), 1f)
    }.padding(top = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LeaveText("给未来一点线索", palette.ink, 16, weight = FontWeight.Medium)
            LeaveText("可选", palette.muted, 11)
        }
        if (options.isNotEmpty()) LeaveText("本地原文摘词 · 选择后才会保留", palette.muted, 11, Modifier.padding(top = 6.dp))
        for (cue in options) {
            val adopted = selected.any { it.id == cue.id }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                LeaveAction((if (adopted) "✓  " else "○  ") + cue.value, reduced,
                    { controller.adoptCue(cue, !adopted) }, Modifier.weight(1f), enabled = !state.operationPending,
                    tag = "leave.cue.${cue.id}", description = "保留线索 ${cue.value}", selected = adopted)
                LeaveAction("↗", reduced, { editingId = if (editingId == cue.id) null else cue.id },
                    enabled = !state.operationPending, description = "编辑线索 ${cue.value}", tag = "leave.cue.edit.${cue.id}")
            }
            if (editingId == cue.id) {
                LeaveField(cue.value, { controller.editCue(cue, it) }, "修改这条线索", palette,
                    "leave.cue.editor", enabled = !state.locked, maxLength = 80)
                LeaveAction("完成", reduced, { editingId = null }, tag = "leave.cue.edit.done")
            }
        }
        if (!writingOwn && self.isEmpty()) LeaveAction("＋ 自己留一点", reduced, { writingOwn = true },
            enabled = !state.operationPending, tag = "leave.cue.self.open")
        else {
            LeaveText("自己认得的细节", palette.muted, 12, Modifier.padding(top = 12.dp))
            LeaveField(self, controller::setSelfCue, "一个声音，一处地方…", palette, "leave.cue.self",
                enabled = !state.locked, maxLength = 80, singleLine = false)
            LeaveAction("不留自写线索", reduced, { controller.setSelfCue(""); writingOwn = false }, enabled = !state.operationPending)
        }
    }
}

@Composable
private fun PolicyBody(state: LeaveState, controller: LeaveController, palette: NativePalette, reduced: Boolean) {
    Choice("偶然遇见", "让它在未来某个时刻，偶然回来。", state.draft.policy == "SERENDIPITY", palette, reduced, !state.locked, "leave.policy.random") {
        controller.edit { it.copy(policy = "SERENDIPITY") }
    }
    Choice("留给某一天", "选一个想再次打开它的日子。", state.draft.policy == "FIXED_DATE", palette, reduced, !state.locked, "leave.policy.fixed") {
        controller.edit { it.copy(policy = "FIXED_DATE") }
    }
    Choice("静静保存", "不主动出现，随时可以在「过去」查看。", state.draft.policy == "QUIET", palette, reduced, !state.locked, "leave.policy.quiet") {
        controller.edit { it.copy(policy = "QUIET") }
    }
    if (state.draft.policy == "FIXED_DATE") {
        LeaveText("再见的日期", palette.ink, 14, Modifier.padding(top = 12.dp))
        LeaveField(state.fixedDateInput, controller::setDate, "YYYY-MM-DD", palette, "leave.fixed.date",
            enabled = !state.locked, keyboardType = KeyboardType.Text)
    }
    if (state.draft.policy != "QUIET") LeaveAction("回忆偏好", reduced, { controller.navigate(LeavePage.PREFERENCE) },
        Modifier.fillMaxWidth().padding(top = 16.dp), enabled = !state.operationPending, trailing = preferenceLabel(state.draft.preference) + " →", tag = "leave.preference")
}
@Composable
private fun BackgroundBody(state: LeaveState, controller: LeaveController, palette: NativePalette, reduced: Boolean) {
    LeaveText("时间", palette.ink, 14)
    LeaveField(state.draft.occurredDate, { controller.edit { draft -> draft.copy(occurredDate = it) } }, "例如，某年夏天", palette, "leave.background.date", enabled = !state.locked)
    LeaveText("地点", palette.ink, 14)
    LeaveField(state.draft.place, { controller.edit { draft -> draft.copy(place = it) } }, "一个你记得的地方", palette, "leave.background.place", enabled = !state.locked)
    LeaveText("人物", palette.ink, 14)
    LeaveField(state.draft.people, { controller.edit { draft -> draft.copy(people = it) } }, "你习惯的称呼", palette, "leave.background.people", enabled = !state.locked)
    LeaveText("记录来源", palette.ink, 14, Modifier.padding(top = 18.dp))
    for ((key,label) in listOf("now" to "现在留下", "backfill" to "补记过去", "imported" to "旧记录"))
        Choice(label, "", state.draft.provenance == key, palette, reduced, !state.locked, "leave.source.$key") { controller.edit { it.copy(provenance = key) } }
}
@Composable
private fun PreferenceBody(state: LeaveState, controller: LeaveController, palette: NativePalette, reduced: Boolean) {
    for ((key,hint) in listOf("inherit" to "不为这一段单独设置。", "light" to "更突出直接查看。", "normal" to "先给自己一次回忆的机会。", "deep" to "主动请求线索的入口更明显。"))
        Choice(preferenceLabel(key), hint, state.draft.preference == key, palette, reduced, !state.locked, "leave.preference.$key") { controller.edit { it.copy(preference = key) } }
}

@Composable
private fun PhotoStrip(state: LeaveState, gateway: CaptureGateway, controller: LeaveController, palette: NativePalette, reduced: Boolean, removable: Boolean) {
    if (state.draft.photos.isEmpty()) return
    LazyRow(Modifier.fillMaxWidth().testTag("leave.photos"), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        itemsIndexed(state.draft.photos, key = { _, photo -> photo.id }) { index, photo ->
            Column(Modifier.widthIn(min = 86.dp, max = 86.dp)) {
                Box(Modifier.size(86.dp).clip(RoundedCornerShape(6.dp)).clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    enabled = !state.operationPending, role = Role.Button,
                    onClickLabel = "查看照片 ${index + 1}", onClick = { controller.viewPhoto(photo.id) }
                ).semantics { contentDescription = "查看照片 ${index + 1}" }.testTag("leave.photo.open.${photo.id}")) {
                    PhotoImage(photo, gateway, palette, ContentScale.Crop)
                }
                if (removable) LeaveAction("× 移除", reduced, { controller.removePhoto(photo.id) }, Modifier.fillMaxWidth(),
                    enabled = !state.operationPending, description = "移除照片 ${index + 1}", tag = "leave.photo.remove.${photo.id}")
            }
        }
    }
}

@Composable
private fun PhotoPage(state: LeaveState, gateway: CaptureGateway, controller: LeaveController, palette: NativePalette, reduced: Boolean, back: () -> Unit) {
    val photo = state.draft.photos.firstOrNull { it.id == state.photoId }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        LeaveHeader("照片", back, palette, reduced, 24.dp)
        if (photo == null) { LeaveText("这张照片已移除。", palette.muted, modifier = Modifier.padding(24.dp)); return@Column }
        Box(Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 460.dp).padding(horizontal = 24.dp).testTag("leave.photo.preview")) {
            PhotoImage(photo, gateway, palette, ContentScale.Fit)
        }
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            LeaveAction(if (photo.cueAllowed) "✓ 可用于照片线索" else "○ 不用于照片线索", reduced,
                { controller.setPhotoCueAllowed(photo.id, !photo.cueAllowed) }, enabled = !state.operationPending,
                selected = photo.cueAllowed, tag = "leave.photo.authorize")
            LeaveText("仅在你明确允许时，才可用于未来的照片线索。允许不等于启动 AI。", palette.muted, 12)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                LeaveAction("前移", reduced, { controller.movePhoto(photo.id, -1) }, enabled = state.draft.photos.indexOf(photo) > 0 && !state.operationPending, tag = "leave.photo.earlier")
                LeaveAction("后移", reduced, { controller.movePhoto(photo.id, 1) }, enabled = state.draft.photos.indexOf(photo) < state.draft.photos.lastIndex && !state.operationPending, tag = "leave.photo.later")
                LeaveAction("移除", reduced, { controller.removePhoto(photo.id); controller.leavePhoto() }, enabled = !state.operationPending, tag = "leave.photo.remove")
            }
        }
    }
}

private data class PhotoRender(val bitmap: Bitmap? = null, val error: String? = null)
@Composable
private fun PhotoImage(photo: CapturePhoto, gateway: CaptureGateway, palette: NativePalette, contentScale: ContentScale) {
    val render by produceState(PhotoRender(), photo.id, photo.available) {
        value = if (!photo.available) PhotoRender(error = "照片不可用\n请移除或重选") else try { PhotoRender(bitmap = gateway.preview(photo)) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { PhotoRender(error = "照片未能打开\n请检查或重选") }
    }
    // No strong cache or ViewModel bitmap ownership. Releasing this node's
    // references lets Android free pixels after the renderer releases them.
    val bitmap = render.bitmap
    if (bitmap != null && !bitmap.isRecycled) Image(bitmap.asImageBitmap(), contentDescription = null,
        Modifier.fillMaxSize(), contentScale = contentScale)
    else Box(Modifier.fillMaxSize().background(palette.hairline.copy(alpha = .12f)), contentAlignment = Alignment.Center) {
        LeaveText(render.error ?: "读取中…", palette.muted, 11, centered = true)
    }
}

@Composable
private fun ImportErrors(state: LeaveState, controller: LeaveController, palette: NativePalette, reduced: Boolean, pick: (Boolean) -> Unit) {
    if (state.draft.importErrors.isEmpty()) return
    Column(Modifier.fillMaxWidth().testTag("leave.import.errors")) {
        LeaveText("未完成的照片", palette.ink, 13, weight = FontWeight.Medium)
        for ((index,error) in state.draft.importErrors.withIndex()) {
            LeaveText(error, Color(0xFF9D5145), 12, Modifier.padding(top = 8.dp))
            LeaveAction("移除这项失败记录", reduced, { controller.dismissImportError(index) }, enabled = !state.operationPending, tag = "leave.import.dismiss.$index")
        }
        LeaveAction("重新选择失败项", reduced, { pick(true) }, enabled = !state.operationPending, tag = "leave.import.retry")
    }
}

@Composable
private fun Choice(label: String, hint: String, selected: Boolean, palette: NativePalette, reduced: Boolean, enabled: Boolean, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().testTag(tag).quietPress(reduced, enabled, onClick, Role.RadioButton)
        .semantics { stateDescription = if (selected) "已选" else "未选" }.padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LeaveText(if (selected) "●" else "○", palette.accent, 18, Modifier.padding(top = 3.dp))
        Column(Modifier.weight(1f)) { LeaveText(label, palette.ink, 17); if (hint.isNotEmpty()) LeaveText(hint, palette.muted, 12, Modifier.padding(top = 4.dp)) }
    }
}

@Composable
internal fun LeaveAction(
    label: String, reduced: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, solid: Boolean = false, isNight: Boolean = false,
    tag: String? = null, description: String? = null, trailing: String? = null, selected: Boolean? = null
) {
    val foreground = if (solid) if (isNight) Color(0xFF233B43) else Color(0xFFF7F6EC)
        else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val background = if (solid) if (isNight) Color(0xFFB5C5BA) else Color(0xFF4F6A61) else Color.Transparent
    Row(modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier)
        .heightIn(min = if (solid) 54.dp else 44.dp).clip(RoundedCornerShape(if (solid) 12.dp else 0.dp))
        .background(background).quietPress(reduced, enabled, onClick)
        .semantics {
            if (description != null) contentDescription = description
            if (selected != null) stateDescription = if (selected) "已选" else "未选"
        }.padding(horizontal = if (solid) 16.dp else 0.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (trailing != null) Arrangement.SpaceBetween else if (solid) Arrangement.Center else Arrangement.Start) {
        LeaveText(label, foreground.copy(alpha = if (enabled) 1f else .45f), if (solid) 15 else 13,
            modifier = if (trailing != null) Modifier.weight(1f) else Modifier)
        if (trailing != null) LeaveText(trailing, foreground.copy(alpha = .7f), 12, Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun Modifier.quietPress(reduced: Boolean, enabled: Boolean, onClick: () -> Unit, role: Role = Role.Button): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (!reduced && pressed) .975f else 1f, tween(150), label = "leave.press")
    return graphicsLayer { scaleX = scale; scaleY = scale }.clickable(source, indication = null, enabled = enabled, role = role, onClick = onClick)
}

@Composable
internal fun LeaveText(
    text: String, color: Color, fontSize: Int = 16, modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal, centered: Boolean = false, tag: String? = null
) = Text(text, modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier), color = color,
    fontFamily = EmbeddedChineseFont, fontSize = fontSize.sp, lineHeight = (fontSize * 1.8f).sp,
    fontWeight = weight, textAlign = if (centered) androidx.compose.ui.text.style.TextAlign.Center else null)

@Composable
private fun LeaveField(
    value: String, onChange: (String) -> Unit, placeholder: String, palette: NativePalette, tag: String,
    modifier: Modifier = Modifier, enabled: Boolean = true, underline: Boolean = true,
    singleLine: Boolean = true, fontSize: Int = 15, maxLength: Int = Int.MAX_VALUE,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val focus = LocalFocusManager.current
    BasicTextField(value, { onChange(it.take(maxLength)) }, modifier.fillMaxWidth().heightIn(min = 48.dp)
        .testTag(tag).semantics { contentDescription = placeholder }.drawBehind {
            if (underline) drawLine(palette.hairline, Offset(0f,size.height), Offset(size.width,size.height), 1f)
        }.padding(vertical = if (underline) 12.dp else 0.dp),
        enabled = enabled, singleLine = singleLine, textStyle = TextStyle(color = palette.ink,
            fontFamily = EmbeddedChineseFont, fontSize = fontSize.sp, lineHeight = (fontSize * 1.95f).sp),
        cursorBrush = SolidColor(palette.accent),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        decorationBox = { inner -> Box(Modifier.fillMaxWidth()) {
            if (value.isEmpty()) LeaveText(placeholder, palette.muted, fontSize)
            inner()
        } })
}
private fun policyLabel(policy: String) = when (policy) { "FIXED_DATE" -> "留给某一天"; "QUIET" -> "静静保存"; else -> "偶然遇见" }
private fun preferenceLabel(preference: String) = when (preference) { "light" -> "轻一点"; "normal" -> "让我先想想"; "deep" -> "我喜欢慢慢想"; else -> "沿用全局偏好" }
