package com.unfinished.app

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.unfinished.app.ai.AiProviderConfig
import com.unfinished.app.ai.AiProviderConfigStore
import com.unfinished.app.ai.CueSuggestion
import com.unfinished.app.ai.CueSuggestionProvider
import com.unfinished.app.data.AppLockStore
import com.unfinished.app.data.CueEntity
import com.unfinished.app.data.MemoryEntity
import com.unfinished.app.data.MemoryRepository
import com.unfinished.app.data.VaultIdentityStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val Page = Color(0xFFE7EBED)
private val PageTop = Color(0xFFE2E8EA)
private val Ink = Color(0xFF20282C)
private val Muted = Color(0xFF627077)
private val Accent = Color(0xFF315D78)
private val Signal = Color(0xFFE87855)
// The prototype uses quiet, low-saturation surfaces. These softened display
// values preserve the three card identities while preventing Android's flat
// Card surface from reading more vivid than the prototype.
private val PastColor = Color(0xFFD0D9DD)
private val EncounterColor = Color(0xFFE0E7E9)
private val LeaveColor = Color(0xFFF1EBE3)
private val Hairline = Color(0x3326343B)

private object MotionTokens {
    val easeOut = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val prototypeEase = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    val flingEase = CubicBezierEasing(0.18f, 0.85f, 0.28f, 1f)
    const val pressMillis = 140
    const val opacityMillis = 180
    const val panelMillis = 300
    const val flingMillis = 280
    const val selectionMillis = 250
}

private data class PressFeedback(
    val interactionSource: MutableInteractionSource,
    val modifier: Modifier
)

private fun readReducedMotion(context: Context): Boolean =
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    ) == 0f

@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    var reduced by remember { mutableStateOf(readReducedMotion(context)) }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = readReducedMotion(context)
            }
        }
        val uri = Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE)
        context.contentResolver.registerContentObserver(uri, false, observer)
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    return reduced
}

@Composable
private fun rememberPressFeedback(reducedMotion: Boolean): PressFeedback {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (!reducedMotion && pressed) 0.97f else 1f,
        animationSpec = tween(MotionTokens.pressMillis, easing = MotionTokens.easeOut),
        label = "press-scale"
    )
    val modifier = Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
    return PressFeedback(interactionSource, modifier)
}

@Composable
private fun Modifier.noRippleClickable(
    interactionSource: MutableInteractionSource,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = clickable(
    interactionSource = interactionSource,
    indication = null,
    enabled = enabled,
    onClick = onClick
)

private fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = clickable(
    interactionSource = MutableInteractionSource(),
    indication = null,
    onClick = onClick
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { UnfinishedApp() }
    }
}

private enum class CardKind(
    val label: String,
    val color: Color,
    val meta: String,
    val cardTitle: String,
    val cardDescription: String,
    val action: String
) {
    Past("过去", PastColor, "28 段", "很久以前", "明确寻找一段过去。", "查看"),
    Encounter("遇见", EncounterColor, "随机", "遇见过去", "不知道会遇见什么。", "遇见"),
    Leave("留下", LeaveColor, "空白页", "留下一段", "今天有一些东西，想留给以后。", "开始写"),
    // Settings is not a fourth card; its panel follows the prototype's
    // neutral panel surface while the entry button keeps the orange accent.
    Settings("设置", Color(0xFFF2F4F4), "", "设置", "让它保持私人。", "")
}

@Composable
private fun UnfinishedApp() {
    val context = LocalContext.current
    val repository = remember { MemoryRepository(context) }
    val aiProvider = remember { CueSuggestionProvider(AiProviderConfigStore(context)) }
    val lockStore = remember { AppLockStore(context) }
    val reducedMotion = rememberReducedMotion()
    var openingCard by remember { mutableStateOf<CardKind?>(null) }
    var selectedCard by remember { mutableStateOf<CardKind?>(null) }
    var surface by rememberSaveable { mutableStateOf("quiet") }
    var unlocked by remember { mutableStateOf(!lockStore.isEnabled()) }

    LaunchedEffect(openingCard, reducedMotion) {
        val target = openingCard ?: return@LaunchedEffect
        if (!reducedMotion) delay(MotionTokens.selectionMillis.toLong())
        if (openingCard == target) selectedCard = target
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Page) {
            if (!unlocked) {
                LockPanel(lockStore) { unlocked = true }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(
                        visible = selectedCard == null,
                        modifier = Modifier.fillMaxSize(),
                        enter = fadeIn(tween(MotionTokens.opacityMillis)),
                        exit = fadeOut(tween(MotionTokens.opacityMillis))
                    ) {
                        HomeScreen(
                            surface = surface,
                            openingCard = openingCard,
                            reducedMotion = reducedMotion,
                            onCardClick = { openingCard = it },
                            onSettingsClick = {
                                openingCard = null
                                selectedCard = CardKind.Settings
                            }
                        )
                    }
                    AnimatedVisibility(
                        visible = selectedCard != null,
                        modifier = Modifier.fillMaxSize(),
                        enter = if (reducedMotion) {
                            fadeIn(tween(MotionTokens.opacityMillis))
                        } else {
                            slideInVertically(
                                initialOffsetY = { (it * 1.02f).toInt() },
                                animationSpec = tween(MotionTokens.panelMillis, easing = MotionTokens.prototypeEase)
                            ) + fadeIn(tween(MotionTokens.opacityMillis))
                        },
                        exit = if (reducedMotion) {
                            fadeOut(tween(MotionTokens.opacityMillis))
                        } else {
                            slideOutVertically(
                                targetOffsetY = { (it * 1.02f).toInt() },
                                animationSpec = tween(MotionTokens.panelMillis, easing = MotionTokens.prototypeEase)
                            ) + fadeOut(tween(MotionTokens.opacityMillis))
                        }
                    ) {
                        selectedCard?.let { kind ->
                            FunctionPanel(
                                kind = kind,
                                repository = repository,
                                aiProvider = aiProvider,
                                surface = surface,
                                onSurfaceChange = { surface = it },
                                onBack = {
                                    openingCard = null
                                    selectedCard = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LockPanel(lockStore: AppLockStore, onUnlock: () -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Surface(modifier = Modifier.fillMaxSize(), color = Page) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(100.dp))
            Text("这段记忆只属于你", color = Ink, style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                label = { Text("输入 PIN") },
                singleLine = true
            )
            Button(onClick = { if (lockStore.verify(pin)) onUnlock() else error = "PIN 不正确" }) { Text("解锁") }
            if (error.isNotBlank()) Text(error, color = Color(0xFF9B3D32))
        }
    }
}

@Composable
private fun HomeScreen(
    surface: String,
    openingCard: CardKind?,
    reducedMotion: Boolean,
    onCardClick: (CardKind) -> Unit,
    onSettingsClick: () -> Unit
) {
    var order by remember {
        mutableStateOf(if (surface == "returned") listOf(CardKind.Encounter, CardKind.Leave, CardKind.Past) else listOf(CardKind.Leave, CardKind.Encounter, CardKind.Past))
    }
    var gestureProgress by remember { mutableStateOf(0f) }
    val returnedPress = rememberPressFeedback(reducedMotion)
    val settingsPress = rememberPressFeedback(reducedMotion)
    LaunchedEffect(surface) {
        order = if (surface == "returned") listOf(CardKind.Encounter, CardKind.Leave, CardKind.Past) else listOf(CardKind.Leave, CardKind.Encounter, CardKind.Past)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PageTop, Page)))
    ) {
        // The prototype's stage is visually bottom-congregated. Anchor the
        // whole deck from the bottom safe area and keep all slot relationships
        // unchanged, instead of hard-coding a top-only offset.
        val cardHeight = 430.dp
        val frontBottomWithinDeck = 138.dp + cardHeight
        val bottomReserve = 88.dp
        val deckTop = (maxHeight - frontBottomWithinDeck - bottomReserve).coerceAtLeast(24.dp)

        if (surface == "returned") {
            OutlinedButton(
                onClick = { onCardClick(CardKind.Encounter) },
                modifier = Modifier.align(Alignment.TopStart).padding(top = 24.dp, start = 24.dp).then(returnedPress.modifier),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = Accent),
                interactionSource = returnedPress.interactionSource
            ) { Text("●  有一页回来") }
        }
        OutlinedButton(
            onClick = onSettingsClick,
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 24.dp, end = 24.dp).size(42.dp).then(settingsPress.modifier),
            shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Signal),
            border = androidx.compose.foundation.BorderStroke(1.dp, Signal.copy(alpha = 0.45f)),
            interactionSource = settingsPress.interactionSource
        ) { Text("☷", fontSize = 19.sp) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(650.dp)
                .align(Alignment.TopCenter)
                .offset(y = deckTop)
                .padding(start = 26.dp, end = 26.dp)
        ) {
            order.forEachIndexed { index, kind ->
                val slot = when (index) {
                    0 -> CardSlot(0, 138, -0.5f, 1f, 3f)
                    1 -> CardSlot(10, 70, 1.2f, 0.97f, 2f)
                    else -> CardSlot(4, 4, -1.6f, 0.94f, 1f)
                }
                key(kind) {
                    MemoryCard(
                        kind = kind,
                        slot = slot,
                        slotIndex = index,
                        isFront = index == 0,
                        isPicked = openingCard == kind,
                        reducedMotion = reducedMotion,
                        gestureProgress = gestureProgress,
                        onGestureProgress = { gestureProgress = it },
                        onClick = { onCardClick(kind) },
                        onSwipe = {
                            gestureProgress = 0f
                            order = order.drop(1) + order.first()
                        },
                        modifier = Modifier.align(Alignment.TopCenter).zIndex(slot.z)
                    )
                }
            }
        }
        Text(
            "左右拨动切换，点按打开",
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 13.dp),
            color = Muted,
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private data class CardSlot(val x: Int, val y: Int, val rotation: Float, val scale: Float, val z: Float)

@Composable
private fun MemoryCard(
    kind: CardKind,
    slot: CardSlot,
    slotIndex: Int,
    isFront: Boolean,
    isPicked: Boolean,
    reducedMotion: Boolean,
    gestureProgress: Float,
    onGestureProgress: (Float) -> Unit,
    onClick: () -> Unit,
    onSwipe: () -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(22.dp)
    val pressFeedback = rememberPressFeedback(reducedMotion)
    val slotX = remember(kind) { Animatable(with(density) { slot.x.dp.toPx() }) }
    val slotY = remember(kind) { Animatable(with(density) { slot.y.dp.toPx() }) }
    val slotRotation = remember(kind) { Animatable(slot.rotation) }
    val slotScale = remember(kind) { Animatable(slot.scale) }
    val dragX = remember(kind) { Animatable(0f) }
    val dragY = remember(kind) { Animatable(0f) }
    val dragRotation = remember(kind) { Animatable(0f) }
    val cardAlpha = remember(kind) { Animatable(1f) }
    var moved by remember(kind) { mutableStateOf(false) }
    var suppressClickUntil by remember(kind) { mutableStateOf(0L) }
    var thresholdCrossed by remember(kind) { mutableStateOf(false) }
    var cardWidth by remember(kind) { mutableStateOf(0) }
    var animationJob by remember(kind) { mutableStateOf<Job?>(null) }
    var dragUpdateJob by remember(kind) { mutableStateOf<Job?>(null) }
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val pickedProgress by animateFloatAsState(
        targetValue = if (isPicked) 1f else 0f,
        animationSpec = tween(MotionTokens.selectionMillis, easing = MotionTokens.prototypeEase),
        label = "card-picked"
    )
    val elevation by animateDpAsState(
        targetValue = if (isPicked) 16.dp else if (isFront) 10.dp else 6.dp,
        animationSpec = tween(MotionTokens.selectionMillis, easing = MotionTokens.prototypeEase),
        label = "card-elevation"
    )

    LaunchedEffect(slot.x, slot.y, slot.rotation, slot.scale, reducedMotion) {
        val targetX = with(density) { slot.x.dp.toPx() }
        val targetY = with(density) { slot.y.dp.toPx() }
        if (reducedMotion) {
            slotX.snapTo(targetX)
            slotY.snapTo(targetY)
            slotRotation.snapTo(slot.rotation)
            slotScale.snapTo(slot.scale)
        } else {
            launch { slotX.animateTo(targetX, tween(MotionTokens.panelMillis, easing = MotionTokens.easeOut)) }
            launch { slotY.animateTo(targetY, tween(MotionTokens.panelMillis, easing = MotionTokens.easeOut)) }
            launch { slotRotation.animateTo(slot.rotation, tween(MotionTokens.panelMillis, easing = MotionTokens.easeOut)) }
            launch { slotScale.animateTo(slot.scale, tween(MotionTokens.panelMillis, easing = MotionTokens.easeOut)) }
        }
    }

    suspend fun settleBack() {
        if (reducedMotion) {
            dragX.snapTo(0f)
            dragY.snapTo(0f)
            dragRotation.snapTo(0f)
        } else {
            coroutineScope {
                launch { dragX.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 300f)) }
                launch { dragY.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 300f)) }
                launch { dragRotation.animateTo(0f, spring(dampingRatio = 0.8f, stiffness = 300f)) }
            }
        }
        onGestureProgress(0f)
        moved = false
        thresholdCrossed = false
    }

    fun startFling(direction: Float) {
        animationJob?.cancel()
        animationJob = scope.launch {
            val width = cardWidth.toFloat().coerceAtLeast(1f)
            val flingSpec = tween<Float>(MotionTokens.flingMillis, easing = MotionTokens.flingEase)
            val xJob = launch { dragX.animateTo(direction * width * 1.35f, flingSpec) }
            val rotationJob = launch { dragRotation.animateTo(direction * 10f, flingSpec) }
            val alphaJob = launch { cardAlpha.animateTo(0f, tween(MotionTokens.flingMillis)) }
            xJob.join()
            rotationJob.join()
            alphaJob.join()
            onGestureProgress(0f)
            onSwipe()
            dragX.snapTo(0f)
            dragY.snapTo(0f)
            dragRotation.snapTo(0f)
            cardAlpha.snapTo(1f)
            moved = false
            thresholdCrossed = false
            animationJob = null
        }
    }

    val gestureLift = when (slotIndex) {
        1 -> gestureProgress * 16f
        2 -> gestureProgress * 8f
        else -> 0f
    }
    val gestureScale = when (slotIndex) {
        1 -> gestureProgress * 0.03f
        2 -> gestureProgress * 0.02f
        else -> 0f
    }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(430.dp)
            .onSizeChanged { cardWidth = it.width }
            .clip(shape)
            .then(pressFeedback.modifier)
            .graphicsLayer {
                rotationZ = slotRotation.value * (1f - pickedProgress) + dragRotation.value
                scaleX = slotScale.value + gestureScale + pickedProgress * 0.015f
                scaleY = slotScale.value + gestureScale + pickedProgress * 0.015f
                translationX = slotX.value + dragX.value
                translationY = slotY.value - gestureLift - pickedProgress * with(density) { 22.dp.toPx() } + dragY.value
                alpha = cardAlpha.value
            }
            .then(
                if (isFront) Modifier.pointerInput(kind, reducedMotion) {
                    detectDragGestures(
                        onDragStart = {
                            animationJob?.cancel()
                            dragUpdateJob?.cancel()
                            moved = false
                            thresholdCrossed = false
                        },
                        onDragCancel = {
                            animationJob?.cancel()
                            scope.launch { settleBack() }
                        },
                        onDragEnd = {
                            val threshold = maxOf(82f, cardWidth * 0.28f)
                            if (moved && kotlin.math.abs(dragX.value) >= threshold) {
                                val direction = if (dragX.value >= 0f) 1f else -1f
                                suppressClickUntil = System.currentTimeMillis() + 520
                                startFling(direction)
                            } else {
                                animationJob?.cancel()
                                scope.launch { settleBack() }
                            }
                        },
                        onDrag = { change, amount ->
                            val nextX = dragX.value + amount.x
                            val nextY = (dragY.value + amount.y).coerceIn(-14f, 14f)
                            val width = cardWidth.toFloat().coerceAtLeast(1f)
                            val nextRotation = ((nextX / width) * 5f).coerceIn(-6f, 6f)
                            val threshold = maxOf(82f, cardWidth * 0.28f)
                            val progress = (kotlin.math.abs(nextX) / threshold).coerceIn(0f, 1f)
                            dragUpdateJob?.cancel()
                            dragUpdateJob = scope.launch {
                                dragX.stop()
                                dragY.stop()
                                dragRotation.stop()
                                dragX.snapTo(nextX)
                                dragY.snapTo(nextY)
                                dragRotation.snapTo(nextRotation)
                            }
                            onGestureProgress(progress)
                            if (!thresholdCrossed && kotlin.math.abs(nextX) >= threshold) {
                                thresholdCrossed = true
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            if (kotlin.math.abs(nextX) > 5f || kotlin.math.abs(nextY) > 5f) moved = true
                            change.consume()
                        }
                    )
                } else Modifier
            )
            .noRippleClickable(pressFeedback.interactionSource) {
                if (System.currentTimeMillis() >= suppressClickUntil && !moved) onClick()
            },
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = kind.color),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(kind.label, color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (kind.meta.isNotBlank()) Text(kind.meta, color = Muted, style = MaterialTheme.typography.labelMedium)
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Hairline))
            Column(
                modifier = Modifier.fillMaxSize().padding(start = 24.dp, end = 24.dp, top = 36.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Column {
                    if (kind == CardKind.Leave) Text("＋", color = Accent, fontSize = 34.sp, fontWeight = FontWeight.Light)
                    Text(kind.cardTitle, color = Ink, fontSize = 31.sp, lineHeight = 35.sp, fontWeight = FontWeight.Normal)
                    Text(kind.cardDescription, modifier = Modifier.padding(top = 14.dp), color = Muted, fontSize = 14.sp, lineHeight = 23.sp)
                }
                if (isFront && kind.action.isNotBlank()) {
                    OutlinedButton(
                        onClick = onClick,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Ink.copy(alpha = 0.3f))
                    ) { Text(kind.action) }
                }
            }
        }
    }
}

@Composable
private fun FunctionPanel(
    kind: CardKind,
    repository: MemoryRepository,
    aiProvider: CueSuggestionProvider,
    surface: String,
    onSurfaceChange: (String) -> Unit,
    onBack: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxSize(), color = Page) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            kind.color.copy(alpha = 0.96f),
                            kind.color.copy(alpha = 0.72f),
                            Page
                        )
                    )
                )
        ) {
            when (kind) {
                CardKind.Leave -> LeavePanel(repository, aiProvider, onBack)
                CardKind.Encounter -> EncounterPanel(repository, surface, onSurfaceChange, onBack)
                CardKind.Past -> PastPanel(repository, onBack)
                CardKind.Settings -> SettingsPanel(aiProvider, onBack)
            }
        }
    }
}

@Composable
private fun PanelScaffold(
    name: String,
    progress: String = "",
    color: Color,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp, 0.dp, 26.dp, 38.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        PanelHeader(name, progress, onBack)
        content()
    }
}

@Composable
private fun PanelHeader(name: String, progress: String = "", onBack: () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val backPress = rememberPressFeedback(reducedMotion)
    val backShape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "←  返回",
            modifier = Modifier
                .clip(backShape)
                .then(backPress.modifier)
                .noRippleClickable(backPress.interactionSource) { onBack() }
                .padding(8.dp),
            color = Ink,
            fontSize = 14.sp
        )
        Text(name + if (progress.isBlank()) "" else "  $progress", color = if (progress.isBlank()) Muted else Accent, fontSize = 13.sp)
    }
}

@Composable
private fun PanelTitle(text: String) {
    Text(text, color = Ink, fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.Normal, letterSpacing = (-1.8).sp)
}

@Composable
private fun PanelCopy(text: String) {
    Text(text, modifier = Modifier.padding(top = 18.dp), color = Muted, fontSize = 15.sp, lineHeight = 26.sp)
}

@Composable
private fun PrototypeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    minHeight: Dp = 48.dp,
    singleLine: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    topPadding: Dp = 28.dp
) {
    Column(modifier = modifier.fillMaxWidth().padding(top = topPadding)) {
        if (label.isNotBlank()) Text(label, color = Muted, fontSize = 13.sp)
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = minHeight).padding(top = 9.dp).drawBehind {
                drawLine(Hairline, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f)
            }
        ) {
            if (value.isBlank()) Text(placeholder, color = Muted.copy(alpha = 0.72f), fontSize = 17.sp)
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = singleLine,
                textStyle = TextStyle(color = Ink, fontSize = 17.sp, lineHeight = 29.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun PrimaryAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val press = rememberPressFeedback(rememberReducedMotion())
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
        modifier = Modifier.heightIn(min = 48.dp).then(press.modifier),
        interactionSource = press.interactionSource
    ) { Text(text, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun SecondaryAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val press = rememberPressFeedback(rememberReducedMotion())
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(999.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
        border = androidx.compose.foundation.BorderStroke(1.dp, Ink.copy(alpha = 0.3f)),
        modifier = Modifier.heightIn(min = 48.dp).then(press.modifier),
        interactionSource = press.interactionSource
    ) { Text(text) }
}

@Composable
private fun TextAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    val press = rememberPressFeedback(rememberReducedMotion())
    TextButton(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(contentColor = Accent),
        modifier = Modifier.heightIn(min = 48.dp).then(press.modifier),
        interactionSource = press.interactionSource
    ) { Text(text) }
}

@Composable
private fun ActionRow(content: @Composable RowScope.() -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(top = 30.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically, content = content)
}

@Composable
private fun LeavePanel(repository: MemoryRepository, aiProvider: CueSuggestionProvider, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableStateOf("compose") }
    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var policy by rememberSaveable { mutableStateOf("SERENDIPITY") }
    var memoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<CueSuggestion>>(emptyList()) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("随时自动保存") }

    LaunchedEffect(Unit) {
        val draft = repository.listMemories()
            .filter { it.state == "DRAFT" }
            .maxByOrNull { it.updatedAt }
        if (draft != null && content.isBlank()) {
            memoryId = draft.id
            title = draft.title.takeUnless { it == "未命名记忆" }.orEmpty()
            content = repository.readOriginal(draft.id).orEmpty()
            suggestions = repository.cues(draft.id).map { CueSuggestion(it.type, it.value, if (it.source == "AI_SUGGESTED") "AI_SUGGESTED" else "手动添加") }
            policy = draft.policy
            step = "draft"
            message = "草稿已经保存"
        }
    }
    LaunchedEffect(content) {
        if (content.isBlank()) suggestions = emptyList() else {
            delay(550)
            suggestions = aiProvider.suggest(content)
        }
    }
    LaunchedEffect(title, content, policy) {
        if (content.isNotBlank() && step == "compose") {
            delay(1_000)
            runCatching {
                memoryId = repository.saveDraft(memoryId, title, content, System.currentTimeMillis(), policy)
                message = "已自动保存草稿"
            }
        }
    }

    suspend fun persist(seal: Boolean = false) {
        if (content.isBlank()) return
        busy = true
        runCatching {
            val id = repository.saveDraft(memoryId, title, content, System.currentTimeMillis(), policy)
            memoryId = id
            repository.saveCues(id, suggestions.map { repository.cue(UUID.randomUUID().toString(), it.type, it.value, it.source) })
            if (seal) repository.seal(id)
            message = if (seal) "已封存，之后会在合适的时机回来" else "已保存草稿"
        }.onFailure { message = "暂时无法保存，请稍后再试" }
        busy = false
    }

    when (step) {
        "compose" -> PanelScaffold("留下", color = LeaveColor, onBack = onBack) {
            PanelTitle("想留下些什么？")
            PanelCopy("不需要完整。只留下现在不想失去的部分。写下的内容会自动保存。")
            PrototypeTextField(content, { content = it }, "记忆内容", "从此刻开始写……", minHeight = 180.dp)
            PrototypeTextField(title, { title = it }, "给它一个名字（可选）", "例如：夏天的晚饭", singleLine = true)
            Text(message, modifier = Modifier.padding(top = 18.dp), color = Muted, fontSize = 12.sp)
            ActionRow {
                PrimaryAction("下一步：添加线索", enabled = !busy && content.isNotBlank()) {
                    scope.launch { persist(); step = "cues" }
                }
                SecondaryAction("保存并退出", enabled = !busy && content.isNotBlank()) {
                    scope.launch { persist(); onBack() }
                }
            }
        }
        "draft" -> PanelScaffold("留下", color = LeaveColor, onBack = onBack) {
            PanelTitle("继续完成这段记忆。")
            PanelCopy("草稿已经保存。你可以继续修改，或从线索开始整理它。")
            Column(modifier = Modifier.fillMaxWidth().padding(top = 48.dp).drawBehind { drawLine(Hairline, Offset(0f, 0f), Offset(size.width, 0f), 1f); drawLine(Hairline, Offset(0f, size.height), Offset(size.width, size.height), 1f) }.padding(vertical = 22.dp)) {
                Text("草稿内容", color = Muted, fontSize = 12.sp)
                Text(content.ifBlank { "还没有写下内容。" }, modifier = Modifier.padding(top = 14.dp), color = Ink, fontSize = 18.sp, lineHeight = 29.sp)
            }
            ActionRow {
                PrimaryAction("添加线索") { step = "cues" }
                SecondaryAction("继续修改") { step = "compose" }
            }
            TextAction("删除草稿", enabled = memoryId != null) {
                scope.launch {
                    memoryId?.let { repository.delete(it) }
                    memoryId = null
                    title = ""
                    content = ""
                    suggestions = emptyList()
                    step = "compose"
                    message = "草稿已删除"
                }
            }
        }
        "cues" -> PanelScaffold("留下", "线索", color = LeaveColor, onBack = onBack) {
            PanelTitle("我找到了几条可能的线索。")
            PanelCopy("这些内容由 AI 从你的原文中提取，只是建议。你可以修改、移除或自己添加。")
            if (suggestions.isEmpty()) {
                Text("还没有线索，你可以自己添加一条。", modifier = Modifier.padding(top = 30.dp), color = Muted, fontSize = 13.sp)
            } else {
                Column(modifier = Modifier.padding(top = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    suggestions.forEachIndexed { index, suggestion ->
                        CueSuggestionRow(
                            suggestion = suggestion,
                            onChange = { value -> suggestions = suggestions.toMutableList().also { it[index] = suggestion.copy(value = value) } },
                            onRemove = { suggestions = suggestions.filterIndexed { i, _ -> i != index } }
                        )
                    }
                }
            }
            TextAction("＋ 自己添加一条") {
                suggestions = suggestions + CueSuggestion("KEYWORD", "", "手动添加")
            }
            ActionRow {
                PrimaryAction("确认线索并继续", enabled = !busy) { scope.launch { persist(); step = "policy" } }
                TextAction("稍后再说") { step = "draft" }
            }
        }
        "policy" -> PanelScaffold("留下", "浮现方式", color = LeaveColor, onBack = onBack) {
            PanelTitle("它以后要怎么回来？")
            ChoiceCard("静静保存", "只在你主动寻找时出现", policy == "QUIET") { policy = "QUIET" }
            ChoiceCard("以后偶尔出现", "在不可预测的某一天回来", policy == "SERENDIPITY") { policy = "SERENDIPITY" }
            ChoiceCard("某一天回来", "指定一个未来日期", policy == "FIXED_DATE") { policy = "FIXED_DATE" }
            ActionRow { PrimaryAction("查看封存确认") { step = "review" } }
        }
        "review" -> PanelScaffold("留下", "确认封存", color = LeaveColor, onBack = onBack) {
            PanelTitle("准备好把它交给未来了吗？")
            PanelCopy("封存是一次明确的完成动作。确认后，Original 原文不会在浮现过程中被覆盖。")
            Column(modifier = Modifier.padding(top = 42.dp).fillMaxWidth().border(1.dp, Hairline).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ReviewRow("这段记忆", title.ifBlank { "未命名的一段" })
                ReviewRow("回来方式", policyLabel(policy))
                ReviewRow("浮现线索", if (suggestions.count { it.value.isNotBlank() } == 0) "未设置" else "${suggestions.count { it.value.isNotBlank() }} 条")
            }
            Text("之后仍可删除，但不能直接修改已经封存的原文。", modifier = Modifier.padding(top = 18.dp), color = Muted, fontSize = 12.sp)
            ActionRow {
                PrimaryAction("确认并封存", enabled = !busy && content.isNotBlank()) { scope.launch { persist(true); step = "done" } }
                SecondaryAction("返回修改") { step = "policy" }
            }
        }
        else -> PanelScaffold("已封存", color = LeaveColor, onBack = onBack) {
            PanelTitle("它会安静地待在那里。")
            PanelCopy("以后是否回来，由你刚刚选择的方式决定。现在不需要再做什么。")
            Box(modifier = Modifier.fillMaxWidth().padding(top = 66.dp), contentAlignment = Alignment.Center) {
                Box(modifier = Modifier.size(142.dp).border(1.dp, Accent, androidx.compose.foundation.shape.RoundedCornerShape(999.dp)), contentAlignment = Alignment.Center) {
                    Text("原\n已封存", color = Accent, fontSize = 17.sp, lineHeight = 26.sp)
                }
            }
            ActionRow { PrimaryAction("回到首页") { onBack() } }
        }
    }
}

private fun policyLabel(policy: String): String = when (policy) {
    "QUIET" -> "静静保存"
    "FIXED_DATE" -> "某一天回来"
    else -> "以后偶尔出现"
}

@Composable
private fun CueSuggestionRow(suggestion: CueSuggestion, onChange: (String) -> Unit, onRemove: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).drawBehind { drawLine(Hairline, Offset(0f, 0f), Offset(size.width, 0f), 1f) }.padding(top = 14.dp, bottom = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (suggestion.type == "TIME") "时间" else "人物或关键词", color = Muted, fontSize = 12.sp)
            Text(if (suggestion.source == "AI_SUGGESTED") "AI 建议" else "手动添加", color = Accent, fontSize = 12.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PrototypeTextField(suggestion.value, onChange, "", "写下一条线索", singleLine = true, modifier = Modifier.weight(1f), topPadding = 0.dp)
            TextAction("移除", onClick = onRemove)
        }
    }
}

@Composable
private fun ChoiceCard(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val press = rememberPressFeedback(reducedMotion)
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(shape)
            .then(press.modifier)
            .border(1.dp, if (selected) Accent else Hairline, shape)
            .noRippleClickable(press.interactionSource) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, modifier = Modifier.padding(top = 5.dp), color = Muted, fontSize = 12.sp)
        }
        Text(if (selected) "已选" else "", color = Accent, fontSize = 11.sp)
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Column {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(value, modifier = Modifier.padding(top = 7.dp), color = Ink, fontSize = 17.sp)
    }
}

@Composable
private fun EncounterPanel(repository: MemoryRepository, surface: String, onSurfaceChange: (String) -> Unit, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableStateOf(if (surface == "returned") "returned" else "idle") }
    var memory by remember { mutableStateOf<MemoryEntity?>(null) }
    var cues by remember { mutableStateOf<List<CueEntity>>(emptyList()) }
    var cueLevel by rememberSaveable { mutableStateOf(1) }
    var sessionId by rememberSaveable { mutableStateOf<String?>(null) }
    var eventId by rememberSaveable { mutableStateOf<String?>(null) }
    var recall by rememberSaveable { mutableStateOf("") }
    var clarity by rememberSaveable { mutableStateOf("PARTIAL") }
    var reflection by rememberSaveable { mutableStateOf("") }
    var original by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("候选池暂时没有结果") }

    LaunchedEffect(surface) { if (surface == "returned" && step == "idle") step = "returned" }

    suspend fun openCandidate(): Boolean {
        val picked = repository.pickEncounter()
        if (picked == null) {
            message = "候选池暂时没有结果"
            return false
        }
        memory = picked
        cues = repository.cues(picked.id)
        cueLevel = 1
        val opened = repository.openEncounter(picked.id)
        eventId = opened.first.id
        sessionId = opened.second.id
        step = "recall"
        return true
    }

    when (step) {
        "idle" -> PanelScaffold("遇见过去", color = EncounterColor, onBack = onBack) {
            PanelTitle("今天很安静。")
            PanelCopy("没有一页主动回来。你也可以自己走近一段过去。")
            EmptyBlock("候选池暂时没有结果", "今天可以什么都不想起。")
            ActionRow {
                PrimaryAction("主动遇见") { scope.launch { openCandidate() } }
                TextAction("改天再说") { onBack() }
            }
        }
        "returned" -> PanelScaffold("回来的一页", color = EncounterColor, onBack = onBack) {
            PanelTitle("先别看以前。")
            PanelCopy("打开以后，先记录你现在还能想起的内容。")
            CueBlock("第一条线索", "2022 年，夏天。")
            ActionRow {
                PrimaryAction("打开") { scope.launch { if (!openCandidate()) step = "idle" } }
                SecondaryAction("再待一会") { onSurfaceChange("quiet"); onBack() }
            }
        }
        "recall" -> PanelScaffold("你还记得吗？", "${cueLevel} / ${maxOf(3, cues.size)}", color = EncounterColor, onBack = onBack) {
            PanelTitle("你想起什么了吗？")
            val cueValue = cues.getOrNull((cueLevel - 1).coerceAtMost(cues.lastIndex))?.value ?: "还没有设置线索。"
            CueBlock(if (cueLevel == 1) "第一条线索" else "再近一点", cueValue)
            Row(modifier = Modifier.fillMaxWidth().padding(top = 26.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                listOf("CLEAR" to "清楚", "PARTIAL" to "一部分", "VAGUE" to "模糊", "NONE" to "不记得").forEach { (value, label) ->
                    FilterChip(selected = clarity == value, onClick = { clarity = value }, label = { Text(label, fontSize = 11.sp) })
                }
            }
            PrototypeTextField(recall, { recall = it }, "现在记得的版本", "写下你现在记得的……", minHeight = 140.dp)
            ActionRow {
                PrimaryAction("提交回忆", enabled = recall.isNotBlank() && sessionId != null) {
                    scope.launch {
                        sessionId?.let { repository.answer(it, recall, clarity) }
                        step = "reveal"
                    }
                }
                SecondaryAction(if (cueLevel < cues.size) "再给一点" else "完全想不起来") {
                    if (cueLevel < cues.size) cueLevel += 1 else { clarity = "NONE"; step = "reveal" }
                }
            }
        }
        "reveal" -> PanelScaffold("揭晓", color = EncounterColor, onBack = onBack) {
            PanelTitle("那时候的你，是这样留下的。")
            PanelCopy("这里没有正确答案。只是让两个时间里的你，见上一面。")
            CompareBlock("现在记得的", if (clarity == "NONE") "想不起来。" else recall.ifBlank { "我记得那天应该挺开心，好像还一起吃了饭。" })
            CompareBlock("当时留下的", original ?: "今天和他最后一次吃饭。整个晚上我们都没有聊以后。")
            ActionRow { PrimaryAction("现在呢？") {
                scope.launch {
                    if (sessionId != null) original = repository.reveal(sessionId!!).original ?: original
                    step = "reflection"
                }
            } }
        }
        "reflection" -> PanelScaffold("回应", color = EncounterColor, onBack = onBack) {
            PanelTitle("现在呢？")
            PanelCopy("可以写，也可以不回答。回应只属于现在。")
            PrototypeTextField(reflection, { reflection = it }, "现在的回应（可选）", "如果你愿意，可以写一点……", minHeight = 140.dp)
            ActionRow {
                PrimaryAction("完成这次相遇") {
                    scope.launch { sessionId?.let { repository.complete(it, reflection) }; step = "done" }
                }
                TextAction("不写了") { step = "done" }
            }
        }
        else -> PanelScaffold("回应", color = EncounterColor, onBack = onBack) {
            PanelTitle("这次相遇已经完成。")
            PanelCopy("它会回到安静的地方，等下一次合适的时机。")
            ActionRow { PrimaryAction("回到首页") { onBack() } }
        }
    }
}

@Composable
private fun EmptyBlock(label: String, headline: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 58.dp, bottom = 8.dp).drawBehind { drawLine(Hairline, Offset(0f, 0f), Offset(size.width, 0f), 1f); drawLine(Hairline, Offset(0f, size.height), Offset(size.width, size.height), 1f) }.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(headline, color = Ink, fontSize = 22.sp, lineHeight = 29.sp)
    }
}

@Composable
private fun CueBlock(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 62.dp, bottom = 8.dp).drawBehind { drawLine(Hairline, Offset(0f, 0f), Offset(size.width, 0f), 1f); drawLine(Hairline, Offset(0f, size.height), Offset(size.width, size.height), 1f) }.padding(vertical = 26.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(value, modifier = Modifier.padding(top = 14.dp), color = Ink, fontSize = 27.sp, lineHeight = 34.sp)
    }
}

@Composable
private fun CompareBlock(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 22.dp).drawBehind { drawLine(Hairline, Offset(0f, 0f), Offset(size.width, 0f), 1f) }.padding(top = 20.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(value, modifier = Modifier.padding(top = 12.dp), color = Ink, fontSize = 17.sp, lineHeight = 29.sp)
    }
}

@Composable
private fun PastPanel(repository: MemoryRepository, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var memories by remember { mutableStateOf<List<MemoryEntity>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<MemoryEntity?>(null) }
    var original by remember { mutableStateOf<String?>(null) }
    var historyCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) { memories = repository.listMemories().filter { it.state != "DELETED" && it.state != "DRAFT" } }

    if (selected == null) {
        PanelScaffold("很久以前", color = PastColor, onBack = onBack) {
            PanelTitle("你留下的过去。")
            PanelCopy("按年份寻找。这里不会主动替你解释任何一段记忆。")
            PrototypeTextField(query, { query = it }, "搜索过去", "人物、地点或关键词", singleLine = true)
            val visible = memories.filter { query.isBlank() || it.title.contains(query, ignoreCase = true) }
            if (visible.isEmpty()) {
                Text("还没有留下任何记忆。", modifier = Modifier.padding(top = 40.dp), color = Muted, fontSize = 13.sp)
            } else {
                Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp)) {
                    visible.groupBy { SimpleDateFormat("yyyy", Locale.CHINA).format(Date(it.occurredAt)) }.toSortedMap(compareByDescending { it }).forEach { (year, entries) ->
                        Row(modifier = Modifier.fillMaxWidth().height(84.dp).noRippleClickable { selected = entries.first() }.drawBehind { drawLine(Hairline, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(year, color = Ink, fontSize = 30.sp)
                            Text("${entries.size} 段", color = Muted, fontSize = 13.sp)
                        }
                        entries.drop(1).forEach { memoryItem ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp).noRippleClickable { selected = memoryItem }.drawBehind { drawLine(Hairline, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f) }, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(memoryItem.title, color = Ink, fontSize = 15.sp)
                                Text(formatDate(memoryItem.occurredAt), color = Muted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    } else {
        PanelScaffold("2022", color = PastColor, onBack = { selected = null }) {
            PanelTitle(selected!!.title)
            PanelCopy("一段已经封存的记录。")
            LaunchedEffect(selected!!.id) {
                original = repository.readOriginal(selected!!.id)
                historyCount = repository.recallHistory(selected!!.id).size
            }
            CompareBlock("当时留下", original ?: "正在读取原文…")
            CompareBlock("回忆次数", "$historyCount 次")
            ActionRow { PrimaryAction("回到过去") { selected = null } }
        }
    }
}

@Composable
private fun SettingsPanel(aiProvider: CueSuggestionProvider, onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { AiProviderConfigStore(context) }
    val identity = remember { VaultIdentityStore(context) }
    val lockStore = remember { AppLockStore(context) }
    val scope = rememberCoroutineScope()
    val initial = remember { store.read() }
    var baseUrl by rememberSaveable { mutableStateOf(initial.baseUrl) }
    var model by rememberSaveable { mutableStateOf(initial.model) }
    var apiKey by rememberSaveable { mutableStateOf(initial.apiKey) }
    var lockPin by rememberSaveable { mutableStateOf("") }
    var lockEnabled by remember { mutableStateOf(lockStore.isEnabled()) }
    var proactive by rememberSaveable { mutableStateOf(true) }
    var notification by rememberSaveable { mutableStateOf(true) }
    var status by remember { mutableStateOf("配置只保存在本机，API Key 使用 Android Keystore 加密") }

    PanelScaffold("设置", color = CardKind.Settings.color, onBack = onBack) {
        PanelTitle("让它保持私人。")
        PanelCopy("你可以随时暂停主动浮现，也可以删除已经封存的记忆。")
        SettingRow("主动浮现", "允许被随机选中", proactive, "开") { proactive = !proactive }
        SettingRow("浮现通知", "只提醒“有一页回来”", notification, "开") { notification = !notification }
        SettingRow("本地锁", "打开 App 时再确认一次", lockEnabled, if (lockEnabled) "开" else "关") { lockEnabled = !lockEnabled }
        Column(modifier = Modifier.padding(top = 40.dp).drawBehind { drawLine(Hairline, Offset(0f, 0f), Offset(size.width, 0f), 1f) }.padding(top = 18.dp)) {
            Text("关于 Original", color = Ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("封存后的原始记录不会被浮现流程提前展示。管理员默认不能查看正文。", modifier = Modifier.padding(top = 10.dp), color = Muted, fontSize = 13.sp, lineHeight = 22.sp)
        }
        Text("AI 提供方", modifier = Modifier.padding(top = 40.dp), color = Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        PanelCopy("可复用观己的自定义模式，也兼容 OpenAI 风格接口。")
        PrototypeTextField(baseUrl, { baseUrl = it }, "Base URL", "https://your-provider.example/v1", singleLine = true)
        PrototypeTextField(model, { model = it }, "模型名", "例如：qwen-plus", singleLine = true)
        PrototypeTextField(apiKey, { apiKey = it }, "API Key", "只保存在本机", singleLine = true)
        ActionRow {
            PrimaryAction("保存配置") { store.save(AiProviderConfig(baseUrl, model, apiKey)); status = "已保存 AI 配置" }
            SecondaryAction("测试连接") {
                store.save(AiProviderConfig(baseUrl, model, apiKey))
                scope.launch { status = if (aiProvider.testConnection()) "连接成功" else "连接失败，仍会使用本地线索建议" }
            }
        }
        Text(status, modifier = Modifier.padding(top = 14.dp), color = Muted, fontSize = 12.sp)
        Text("Vault：${identity.getOrCreate().vaultId}", modifier = Modifier.padding(top = 26.dp), color = Muted, fontSize = 12.sp)
        Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PrototypeTextField(lockPin, { lockPin = it.filter(Char::isDigit).take(8) }, "本地锁 PIN", "至少 4 位", singleLine = true)
            if (lockEnabled) SecondaryAction("关闭") { lockStore.disable(); lockEnabled = false; status = "App 锁已关闭" }
            else PrimaryAction("启用", enabled = lockPin.length >= 4) { lockStore.setPin(lockPin); lockPin = ""; lockEnabled = true; status = "App 锁已启用" }
        }
        Text("账号登录、管理后台：已预留接口，V1 不显示入口。", modifier = Modifier.padding(top = 26.dp), color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun SettingRow(title: String, detail: String, enabled: Boolean, value: String, onClick: () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    val press = rememberPressFeedback(reducedMotion)
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(shape)
            .then(press.modifier)
            .border(1.dp, Hairline, shape)
            .noRippleClickable(press.interactionSource) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, modifier = Modifier.padding(top = 5.dp), color = Muted, fontSize = 12.sp)
        }
        Text(if (enabled) value else "关", color = if (enabled) Accent else Muted, fontSize = 12.sp)
    }
}

private fun formatDate(timestamp: Long): String = SimpleDateFormat("yyyy/MM/dd", Locale.CHINA).format(Date(timestamp))
