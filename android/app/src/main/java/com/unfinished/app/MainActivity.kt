package com.unfinished.app

import android.content.Context
import android.database.ContentObserver
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
import com.unfinished.app.data.V1Rules
import com.unfinished.app.data.VisibleReceipt
import com.unfinished.app.data.ReadRequest
import com.unfinished.app.data.LocalReceiptAllowed
import com.unfinished.app.ui.Accent
import com.unfinished.app.ui.DarkPalette
import com.unfinished.app.ui.DisplaySettings
import com.unfinished.app.ui.DisplaySettingsStore
import com.unfinished.app.ui.EmbeddedChineseFont
import com.unfinished.app.ui.OpeningChineseFont
import com.unfinished.app.ui.EnvironmentPeriod
import com.unfinished.app.ui.Hairline
import com.unfinished.app.ui.Ink
import com.unfinished.app.ui.LightPalette
import com.unfinished.app.ui.Muted
import com.unfinished.app.ui.NativeHomeScreen
import com.unfinished.app.ui.LocalNativePalette
import com.unfinished.app.ui.OpeningScene
import com.unfinished.app.ui.Page
import com.unfinished.app.ui.Signal
import com.unfinished.app.ui.UnfinishedTheme
import com.unfinished.app.ui.leave.NativeLeaveRoute
import com.unfinished.app.ui.leave.PrivatePhotoReadback
import com.unfinished.app.data.CapturePhoto
import com.unfinished.app.data.LocalCaptureGateway
import com.unfinished.app.ui.environmentPeriodAt
import com.unfinished.app.ui.openingHomeHandoffAlpha
import com.unfinished.app.ui.panelSurface
import com.unfinished.app.ui.rememberBitmapResource
import com.unfinished.app.ui.rememberEnvironmentPeriod
import com.unfinished.app.ui.rememberOpeningSceneRenderer
import com.unfinished.app.ui.prepareOpeningText
import com.unfinished.app.ui.StartupSeaBitmap
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

// The prototype uses quiet, low-saturation surfaces. These softened display
// values preserve the three card identities while preventing Android's flat
// Card surface from reading more vivid than the prototype.
private val PastColor = Color(0xFFD0D9DD)
private val EncounterColor = Color(0xFFE0E7E9)
private val LeaveColor = Color(0xFFF1EBE3)
private val PageTop = Color(0xFFE2E8EA)

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
    private val startupVisualReady = AtomicBoolean(false)
    private val startupHandler = Handler(Looper.getMainLooper())
    private val startupFallback = Runnable { startupVisualReady.set(true) }
    private var startupExperimentSeaDrawable = com.unfinished.app.R.drawable.sea_daylight
    private var startupOpaqueFramePending = false
    private var startupWindowOpaque = false
    private var startupEnterAnimationComplete = false
    private var startupOpaqueDrawListener: android.view.ViewTreeObserver.OnDrawListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        if (!BuildConfig.IMAGE_FIRST_FRAME_STARTUP) {
            installSplashScreen().setKeepOnScreenCondition { !startupVisualReady.get() }
        }
        super.onCreate(savedInstanceState)
        startupHandler.postDelayed(startupFallback, STARTUP_VISUAL_TIMEOUT_MILLIS)
        var startupSeaBitmap: StartupSeaBitmap? = null
        if (BuildConfig.IMAGE_FIRST_FRAME_STARTUP) {
            val forceNight = DisplaySettingsStore(this).read().forceNight
            val localTime = LocalTime.now()
            startupExperimentSeaDrawable = environmentPeriodAt(
                localTime.hour,
                localTime.minute,
                forceNight
            ).seaDrawable
            val background = resources.getDrawable(startupExperimentSeaDrawable, theme)
            window.setBackgroundDrawable(background)
            (background as? android.graphics.drawable.BitmapDrawable)?.bitmap?.let {
                startupSeaBitmap = StartupSeaBitmap(startupExperimentSeaDrawable, it)
            }
        } else {
            window.setBackgroundDrawableResource(com.unfinished.app.R.color.opening_startup_background)
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        if (android.os.Build.VERSION.SDK_INT >= 29) window.isNavigationBarContrastEnforced = false
        setContent {
            UnfinishedApp(
                onNightModeChanged = { isNight ->
                    WindowInsetsControllerCompat(window, window.decorView).apply {
                        isAppearanceLightStatusBars = !isNight
                        isAppearanceLightNavigationBars = !isNight
                    }
                },
                onStartupVisualReady = ::onStartupVisualReady,
                startupSeaBitmap = startupSeaBitmap
            )
        }
    }

    private fun onStartupVisualReady() {
        releaseStartupSplash()
        restoreOpaqueWindowAfterContentFrame()
    }

    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        startupEnterAnimationComplete = true
        restoreOpaqueWindowAfterContentFrame()
    }

    private fun restoreOpaqueWindowAfterContentFrame() {
        if (!BuildConfig.IMAGE_FIRST_FRAME_STARTUP || startupWindowOpaque ||
            !startupVisualReady.get() || !startupEnterAnimationComplete ||
            startupOpaqueFramePending || isFinishing || isDestroyed
        ) return

        // Translucency is only the starting-window strategy, not the lifetime
        // of the app. Keeping it after launch makes system back use a different
        // transition from Home. A ready bitmap/Compose SideEffect is not a
        // presented frame. Preserve the launcher's icon expansion until the
        // system reports entry complete, then wait for a hardware frame commit
        // before allowing WindowManager to hide the launcher underneath.
        startupOpaqueFramePending = true
        val decor = window.decorView
        val listener = object : android.view.ViewTreeObserver.OnDrawListener {
            private var armed = false
            override fun onDraw() {
                if (armed) return
                armed = true
                decor.viewTreeObserver.registerFrameCommitCallback {
                    startupHandler.post {
                        if (!isFinishing && !isDestroyed) {
                            startupWindowOpaque = setTranslucent(false)
                            if (!startupWindowOpaque) {
                                android.util.Log.w("UnfinishedStartup", "Content frame committed but opaque window handoff failed")
                            }
                        }
                        startupOpaqueFramePending = false
                    }
                }
                // Removing a listener inside onDraw is prohibited by the view
                // tree. Post removal while keeping frame-commit ordering.
                startupHandler.post {
                    if (decor.viewTreeObserver.isAlive) decor.viewTreeObserver.removeOnDrawListener(this)
                    if (startupOpaqueDrawListener === this) startupOpaqueDrawListener = null
                }
            }
        }
        startupOpaqueDrawListener = listener
        decor.viewTreeObserver.addOnDrawListener(listener)
        decor.invalidate()
    }

    private fun releaseStartupSplash() {
        startupVisualReady.set(true)
        startupHandler.removeCallbacks(startupFallback)
    }

    override fun onDestroy() {
        startupHandler.removeCallbacks(startupFallback)
        startupOpaqueDrawListener?.let { listener ->
            window.decorView.viewTreeObserver.takeIf { it.isAlive }?.removeOnDrawListener(listener)
        }
        startupOpaqueDrawListener = null
        super.onDestroy()
    }
}

private const val STARTUP_VISUAL_TIMEOUT_MILLIS = 2_500L

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
internal fun UnfinishedApp(
    onNightModeChanged: (Boolean) -> Unit,
    onStartupVisualReady: () -> Unit = {},
    startupSeaBitmap: StartupSeaBitmap? = null
) {
    val context = LocalContext.current
    val repository = remember { MemoryRepository(context) }
    val aiProvider = remember { CueSuggestionProvider(AiProviderConfigStore(context)) }
    val lockStore = remember { AppLockStore(context) }
    val displayStore = remember(context) { DisplaySettingsStore(context) }
    var displaySettings by remember { mutableStateOf(displayStore.read()) }
    val period = rememberEnvironmentPeriod(displaySettings.forceNight)
    val isNight = period == EnvironmentPeriod.NIGHT
    val palette = if (isNight) DarkPalette else LightPalette
    val seaBitmap = rememberBitmapResource(period.seaDrawable, startupSea = startupSeaBitmap)
    val shoreBitmap = rememberBitmapResource(period.shoreDrawable, enabled = seaBitmap != null)
    val otherDaylightReference = rememberBitmapResource(
        EnvironmentPeriod.DAYLIGHT.shoreDrawable,
        enabled = seaBitmap != null && period != EnvironmentPeriod.DAYLIGHT
    )
    val daylightReference = if (period == EnvironmentPeriod.DAYLIGHT) shoreBitmap else otherDaylightReference
    val reducedMotion = rememberReducedMotion()
    val sceneRenderer = rememberOpeningSceneRenderer()
    var startupImageFallback by remember { mutableStateOf(false) }
    var selectedCard by rememberSaveable { mutableStateOf<CardKind?>(null) }
    var hasEnteredShore by rememberSaveable { mutableStateOf(false) }
    val openingProgress = remember { mutableFloatStateOf(1f) }
    var openingFontReady by remember { mutableStateOf(false) }
    var initialHomeLayoutReady by remember { mutableStateOf(false) }
    var initialCue by remember { mutableStateOf<CueEntity?>(null) }
    var directRevealOnOpen by remember { mutableStateOf(false) }
    val persisted by repository.state.collectAsStateWithLifecycle()
    val surface = if (persisted.encounter != null) "returned" else "quiet"
    val commandScope = rememberCoroutineScope()
    var homeReadyForReceipt by remember { mutableStateOf(false) }
    LaunchedEffect(selectedCard, hasEnteredShore) {
        homeReadyForReceipt = false
        if (selectedCard == null && hasEnteredShore) {
            delay(MotionTokens.opacityMillis.toLong())
            homeReadyForReceipt = true
        }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, repository) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) commandScope.launch { repository.schedule() }
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) { repository.schedule() }
    val initiallyLocked = remember(lockStore) { lockStore.isEnabled() }
    var unlocked by remember { mutableStateOf(!initiallyLocked) }
    val transparentImageStartup = BuildConfig.IMAGE_FIRST_FRAME_STARTUP && !initiallyLocked
    LaunchedEffect(transparentImageStartup, unlocked, seaBitmap) {
        if (transparentImageStartup && unlocked && seaBitmap == null) {
            delay(STARTUP_VISUAL_TIMEOUT_MILLIS)
            startupImageFallback = true
        }
    }

    val homeHasMemories = persisted.memories.any { it.state in listOf("SEALED", "ARCHIVED") }
    val activeEventId = persisted.encounter?.event?.id
    val activeEventLabel = persisted.encounter?.memory?.let { formatDate(it.occurredAt) + " · 一段曾经" }.orEmpty()
    val homeHasAvailableCue = persisted.encounter?.let {
        it.remainingCues.isNotEmpty() && !it.session.cueEngineStopped && !it.session.snapshotLocked
    } == true
    val homeScreen: @Composable (Boolean, Boolean) -> Unit = { interactionEnabled, receiptEnabled ->
        NativeHomeScreen(
            period = period,
            shore = shoreBitmap,
            hasMemories = homeHasMemories,
            activeEventId = activeEventId,
            activeEventLabel = activeEventLabel,
            receiptEnabled = receiptEnabled,
            hasAvailableCue = homeHasAvailableCue,
            isNight = isNight,
            reduceTransparency = displaySettings.reduceTransparency,
            onShoreVisible = { activeEventId?.let { repository.shoreVisible(it) } },
            onLeave = { selectedCard = CardKind.Leave },
            onPast = { selectedCard = CardKind.Past },
            onSettings = { selectedCard = CardKind.Settings },
            onThink = {
                initialCue = null
                directRevealOnOpen = false
                selectedCard = CardKind.Encounter
            },
            onMoreCue = {
                persisted.encounter?.let { active ->
                    commandScope.launch {
                        val cue = repository.nextCue(active.session.id)
                        if (cue != null) {
                            initialCue = cue
                            directRevealOnOpen = false
                            selectedCard = CardKind.Encounter
                        }
                    }
                }
            },
            onDirectReveal = {
                initialCue = null
                directRevealOnOpen = true
                selectedCard = CardKind.Encounter
            },
            onDefer = {
                activeEventId?.let { eventId ->
                    commandScope.launch { runCatching { repository.defer(eventId) } }
                }
            },
            backdropOverride = sceneRenderer.backdrop,
            drawBackground = false,
            foregroundAlpha = 1f,
            interactionEnabled = interactionEnabled,
            homeAccessible = hasEnteredShore,
            onInitialLayoutReady = { initialHomeLayoutReady = true }
        )
    }

    SideEffect {
        onNightModeChanged(isNight)
        if (!unlocked || seaBitmap != null || startupImageFallback) onStartupVisualReady()
    }
    UnfinishedTheme(isNight = isNight, palette = palette) {
        val fontFamilyResolver = LocalFontFamilyResolver.current
        val textDensity = LocalDensity.current
        val textDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
        val openingTextMeasurer = remember(fontFamilyResolver, textDensity, textDirection) {
            androidx.compose.ui.text.TextMeasurer(fontFamilyResolver, textDensity, textDirection, cacheSize = 0)
        }
        LaunchedEffect(fontFamilyResolver, openingTextMeasurer) {
            try {
                // Resource fonts are blocking by default. Preload their Typeface
                // variants off the UI thread, then expose text only after the
                // shared resolver cache is ready (no fallback-font reflow).
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    android.os.Trace.beginSection("Opening.Font.Preload")
                    try {
                        fontFamilyResolver.preload(OpeningChineseFont)
                        prepareOpeningText(openingTextMeasurer)
                    } finally {
                        android.os.Trace.endSection()
                    }
                }
            } catch (error: Exception) {
                android.util.Log.e("UnfinishedOpening", "Embedded font preload failed", error)
            } finally {
                openingFontReady = true
            }
        }
        Surface(
            modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
            color = if (
                transparentImageStartup && unlocked && seaBitmap == null && !startupImageFallback
            ) Color.Transparent else palette.page
        ) {
            if (!unlocked) {
                LockPanel(lockStore) { unlocked = true }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    OpeningScene(
                        period = period,
                        sea = seaBitmap,
                        shore = shoreBitmap,
                        reference = daylightReference,
                        reducedMotion = reducedMotion,
                        reduceTransparency = displaySettings.reduceTransparency,
                        onEntered = { hasEnteredShore = true },
                        renderer = sceneRenderer,
                        progressState = openingProgress,
                        fontReady = openingFontReady,
                        homeLayoutReady = initialHomeLayoutReady,
                        hasEnteredShore = hasEnteredShore,
                        transparentWhileLoading = transparentImageStartup &&
                            unlocked && seaBitmap == null && !startupImageFallback
                    )
                    if (openingFontReady) {
                        AnimatedVisibility(
                            visible = selectedCard == null,
                            modifier = Modifier.fillMaxSize(),
                            enter = if (hasEnteredShore) fadeIn(tween(MotionTokens.opacityMillis)) else EnterTransition.None,
                            exit = fadeOut(tween(MotionTokens.opacityMillis))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        // Fade non-overlapping home regions without an
                                        // extra full-window offscreen texture. Glass
                                        // keeps its own bounded effect layers.
                                        compositingStrategy = androidx.compose.ui.graphics.CompositingStrategy.ModulateAlpha
                                        alpha = openingHomeHandoffAlpha(openingProgress.floatValue)
                                    }
                                    .then(if (hasEnteredShore) Modifier else Modifier.clearAndSetSemantics { })
                            ) {
                                homeScreen(
                                    hasEnteredShore && selectedCard == null,
                                    homeReadyForReceipt && hasEnteredShore && selectedCard == null
                                )
                            }
                        }
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
                                onSurfaceChange = { },
                                displaySettings = displaySettings,
                                onDisplaySettingsChange = { changed ->
                                    displaySettings = changed
                                    displayStore.write(changed)
                                },
                                initialCue = initialCue,
                                directRevealOnOpen = directRevealOnOpen,
                                onBack = {
                                    selectedCard = null
                                    initialCue = null
                                    directRevealOnOpen = false
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
private fun LockPanel(
    lockStore: AppLockStore,
    onUnlock: () -> Unit
) {
    var pin by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    Surface(modifier = Modifier.fillMaxSize(), color = Page) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(32.dp),
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
    shoreEventId: String?,
    receiptEnabled: Boolean,
    onShoreVisible: suspend () -> Unit,
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
            ) {
                if (shoreEventId != null) VisibleReceipt("shore:$shoreEventId", enabled = receiptEnabled, onVisible = onShoreVisible) { Text("●  有一页回来") }
            }
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
    displaySettings: DisplaySettings,
    onDisplaySettingsChange: (DisplaySettings) -> Unit,
    initialCue: CueEntity?,
    directRevealOnOpen: Boolean,
    onBack: () -> Unit
) {
    var readyForReceipt by remember(kind) { mutableStateOf(false) }
    val reduced = rememberReducedMotion()
    LaunchedEffect(kind) { delay(if (reduced) MotionTokens.opacityMillis.toLong() else MotionTokens.panelMillis.toLong()); readyForReceipt = true }
    val palette = LocalNativePalette.current
    val surfaceColor = panelSurface(palette, when (kind) {
        CardKind.Past -> "past"
        CardKind.Settings -> "settings"
        CardKind.Encounter -> "encounter"
        CardKind.Leave -> "leave"
    })
    val captureContext = LocalContext.current.applicationContext
    val captureGateway = remember(repository) { LocalCaptureGateway(captureContext, repository) }
    Surface(modifier = Modifier.fillMaxSize(), color = surfaceColor) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(surfaceColor)
        ) {
            CompositionLocalProvider(LocalReceiptAllowed provides readyForReceipt) { when (kind) {
                CardKind.Leave -> NativeLeaveRoute(captureGateway, onBack, palette == DarkPalette, reduced)
                CardKind.Encounter -> EncounterPanel(repository, surface, onSurfaceChange, onBack, initialCue, directRevealOnOpen)
                CardKind.Past -> PastPanel(repository, onBack)
                CardKind.Settings -> SettingsPanel(repository, aiProvider, displaySettings, onDisplaySettingsChange, onBack)
            } }
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
    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .fillMaxHeight()
                .testTag("panel.contentAxis")
                .verticalScroll(rememberScrollState())
                .padding(28.dp, 0.dp, 26.dp, 38.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            PanelHeader(name, progress, onBack)
            content()
        }
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
    val hairline = Hairline
    Column(modifier = modifier.fillMaxWidth().padding(top = topPadding)) {
        if (label.isNotBlank()) Text(label, color = Muted, fontSize = 13.sp)
        Box(
            modifier = Modifier.fillMaxWidth().heightIn(min = minHeight).padding(top = 9.dp).drawBehind {
                drawLine(hairline, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f)
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
internal fun LeavePanel(repository: MemoryRepository, aiProvider: CueSuggestionProvider, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var step by rememberSaveable { mutableStateOf("compose") }
    var title by rememberSaveable { mutableStateOf("") }
    var titleEdited by rememberSaveable { mutableStateOf(false) }
    var content by rememberSaveable { mutableStateOf("") }
    var policy by rememberSaveable { mutableStateOf("SERENDIPITY") }
    var memoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var restoredDraftId by rememberSaveable { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<CueSuggestion>>(emptyList()) }
    var adoptedCues by remember { mutableStateOf<List<CueEntity>>(emptyList()) }
    var fixedDateText by rememberSaveable { mutableStateOf("") }
    val saveMutex = remember { Mutex() }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("随时自动保存") }
    var draftLoaded by remember { mutableStateOf(false) }
    var restoreAttempt by remember { mutableStateOf(0) }

    LaunchedEffect(restoreAttempt) {
      draftLoaded = false
      try {
        val records = repository.listMemories()
        if (records.any { it.id == memoryId && it.state == "SEALED" }) { step = "done"; draftLoaded = true; return@LaunchedEffect }
        val drafts = records.filter { it.state == "DRAFT" }
        val draft = if (memoryId != null) drafts.firstOrNull { it.id == memoryId } else drafts.maxByOrNull { it.updatedAt }
        if (draft != null) {
            val restored = restoredDraftId == draft.id
            val restoredContent = if (restored) content else repository.readOriginal(draft.id).orEmpty()
            val restoredCues = repository.cues(draft.id)
            val restoredPolicy = repository.policy(draft.id)
            // Commit UI restoration only after all reads succeeded; retries cannot skip a failed read.
            if (!restored) {
                title = draft.userTitle.orEmpty()
                content = restoredContent
            }
            suggestions = restoredCues.map { CueSuggestion(it.type, it.value, if (it.source == "AI_SUGGESTED") "AI_SUGGESTED" else "手动添加") }
            adoptedCues = restoredCues
            restoredPolicy?.takeIf { it.dateSource == "USER" }?.fixedDate?.let {
                fixedDateText = java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().toString()
            }
            if (!restored) { policy = draft.policy; step = "draft" }
            memoryId = draft.id
            restoredDraftId = draft.id
            message = "草稿已经保存"
        }
        draftLoaded = true
      } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
      catch (_: Exception) { message = "草稿恢复失败，尚未修改内容。" }
    }
    LaunchedEffect(content, draftLoaded) {
        if (!draftLoaded) return@LaunchedEffect
        if (content.isBlank()) suggestions = emptyList() else {
            delay(550)
            suggestions = aiProvider.suggest(content)
        }
    }
    LaunchedEffect(title, content, policy, draftLoaded) {
        if (draftLoaded && content.isNotBlank() && step == "compose") {
            delay(1_000)
            runCatching {
                saveMutex.withLock {
                    memoryId = repository.saveDraft(memoryId, title, content, System.currentTimeMillis(), policy, titleEdited = titleEdited)
                    restoredDraftId = memoryId
                }
                message = "已自动保存草稿"
            }
        }
    }

    suspend fun persist(seal: Boolean = false): Boolean {
        if (content.isBlank() || busy || !draftLoaded) return false
        busy = true
        val result = runCatching {
            saveMutex.withLock {
                val due = if (policy == "FIXED_DATE" && fixedDateText.isNotBlank())
                    LocalDate.parse(fixedDateText).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() else null
                memoryId = repository.saveCapture(memoryId, title, content, System.currentTimeMillis(), policy, due, adoptedCues, seal, titleEdited)
                restoredDraftId = memoryId
            }
            message = if (seal) "已封存，之后会在合适的时机回来" else "已保存草稿"
        }.onFailure { message = "暂时无法保存，请稍后再试" }
        busy = false
        return result.isSuccess
    }

    if (!draftLoaded) {
        PanelScaffold("留下", color = LeaveColor, onBack = onBack) {
            Text(message, color = Muted)
            TextAction("重试恢复") { restoreAttempt++ }
        }
        return
    }
    when (step) {
        "compose" -> PanelScaffold("留下", color = LeaveColor, onBack = onBack) {
            PanelTitle("想留下些什么？")
            PanelCopy("不需要完整。只留下现在不想失去的部分。写下的内容会自动保存。")
            PrototypeTextField(content, { content = it }, "记忆内容", "从此刻开始写……", minHeight = 180.dp)
            PrototypeTextField(title, { title = it; titleEdited = true }, "给它一个名字（可选）", "例如：夏天的晚饭", singleLine = true)
            Text(message, modifier = Modifier.padding(top = 18.dp), color = Muted, fontSize = 12.sp)
            ActionRow {
                PrimaryAction("下一步：添加线索", enabled = !busy && content.isNotBlank()) {
                    scope.launch { if (persist()) step = "cues" }
                }
                SecondaryAction("保存并退出", enabled = !busy && content.isNotBlank()) {
                    scope.launch { if (persist()) onBack() }
                }
            }
        }
        "draft" -> PanelScaffold("留下", color = LeaveColor, onBack = onBack) {
            val hairline = Hairline
            PanelTitle("继续完成这段记忆。")
            PanelCopy("草稿已经保存。你可以继续修改，或从线索开始整理它。")
            Column(modifier = Modifier.fillMaxWidth().padding(top = 48.dp).drawBehind { drawLine(hairline, Offset(0f, 0f), Offset(size.width, 0f), 1f); drawLine(hairline, Offset(0f, size.height), Offset(size.width, size.height), 1f) }.padding(vertical = 22.dp)) {
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
                    restoredDraftId = null
                    adoptedCues = emptyList()
                    fixedDateText = ""
                    titleEdited = false
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
                PrimaryAction("确认线索并继续", enabled = !busy) { scope.launch {
                    adoptedCues = suggestions.filter { it.value.isNotBlank() }.map { repository.cue(UUID.randomUUID().toString(), it.type, it.value, it.source, confirmed = true) }
                    if (persist()) step = "policy"
                } }
                TextAction("不采用线索") { scope.launch { adoptedCues = emptyList(); if (persist()) step = "policy" } }
                TextAction("稍后再说") { step = "draft" }
            }
        }
        "policy" -> PanelScaffold("留下", "浮现方式", color = LeaveColor, onBack = onBack) {
            PanelTitle("它以后要怎么回来？")
            ChoiceCard("静静保存", "只在你主动寻找时出现", policy == "QUIET") { policy = "QUIET" }
            ChoiceCard("以后偶尔出现", "在不可预测的某一天回来", policy == "SERENDIPITY") { policy = "SERENDIPITY" }
            ChoiceCard("某一天回来", "指定一个未来日期", policy == "FIXED_DATE") { policy = "FIXED_DATE" }
            if (policy == "FIXED_DATE") PrototypeTextField(fixedDateText, { fixedDateText = it }, "回来日期", "YYYY-MM-DD", singleLine = true)
            ActionRow { PrimaryAction("查看封存确认") { step = "review" } }
        }
        "review" -> PanelScaffold("留下", "确认封存", color = LeaveColor, onBack = onBack) {
            PanelTitle("准备好把它交给未来了吗？")
            PanelCopy("封存是一次明确的完成动作。确认后，Original 原文不会在浮现过程中被覆盖。")
            Column(modifier = Modifier.padding(top = 42.dp).fillMaxWidth().border(1.dp, Hairline).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ReviewRow("这段记忆", title.ifBlank { "未命名的一段" })
                ReviewRow("回来方式", policyLabel(policy))
                ReviewRow("浮现线索", if (adoptedCues.isEmpty()) "未设置" else "${adoptedCues.size} 条")
            }
            Text("之后仍可删除，但不能直接修改已经封存的原文。", modifier = Modifier.padding(top = 18.dp), color = Muted, fontSize = 12.sp)
            Text(message, color = Muted, fontSize = 12.sp)
            ActionRow {
                PrimaryAction("确认并封存", enabled = !busy && content.isNotBlank()) { scope.launch { if (persist(true)) step = "done" } }
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
    val hairline = Hairline
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp).drawBehind { drawLine(hairline, Offset(0f, 0f), Offset(size.width, 0f), 1f) }.padding(top = 14.dp, bottom = 8.dp)) {
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
internal fun EncounterPanel(
    repository: MemoryRepository,
    surface: String,
    onSurfaceChange: (String) -> Unit,
    onBack: () -> Unit,
    initialCue: CueEntity? = null,
    directRevealOnOpen: Boolean = false
) {
    val scope = rememberCoroutineScope()
    val persisted by repository.state.collectAsStateWithLifecycle()
    val encounter = persisted.encounter
    val session = encounter?.session
    var message by remember { mutableStateOf("今天可以什么都不想起。") }
    var busy by remember { mutableStateOf(false) }
    var pendingCue by remember(session?.id) { mutableStateOf(initialCue) }
    var recall by remember { mutableStateOf("") }
    var clarity by remember { mutableStateOf("PARTIAL") }
    var reflection by rememberSaveable { mutableStateOf("") }
    var original by remember { mutableStateOf<String?>(null) }
    var request by remember { mutableStateOf<ReadRequest?>(null) }
    var reflectionPage by rememberSaveable { mutableStateOf(false) }
    val inputMutex = remember { Mutex() }
    var inputLoaded by remember(session?.id) { mutableStateOf(false) }
    var inputRestoreFailed by remember(session?.id) { mutableStateOf(false) }
    var inputRestoreAttempt by remember(session?.id) { mutableStateOf(0) }
    fun command(action: suspend () -> Unit) {
        if (busy) return
        busy = true
        scope.launch {
            try { inputMutex.withLock {
                if (session != null && !session.snapshotLocked && !session.cueEngineStopped)
                    repository.saveRecallInput(session.id, recall, clarity)
                action()
            } } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { message = "操作未完成，请重试。" }
            finally { busy = false }
        }
    }
    LaunchedEffect(Unit) { repository.schedule() }
    LaunchedEffect(session?.id, inputRestoreAttempt) {
        inputLoaded = false
        inputRestoreFailed = false
        pendingCue = initialCue; original = null; request = null; reflectionPage = false
        try {
            val restored = session?.id?.let { repository.recallInput(it) }.orEmpty()
            recall = restored
            clarity = session?.clarity ?: "PARTIAL"
            inputLoaded = true
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { inputRestoreFailed = true }
    }
    LaunchedEffect(session?.id, session?.state, directRevealOnOpen) {
        val activeSession = session ?: return@LaunchedEffect
        if (directRevealOnOpen && activeSession.state !in listOf("REVEAL_REQUESTED", "REVEALED")) {
            runCatching { repository.requestReveal(activeSession.id) }
                .onFailure { message = "原文读取失败，可重试；尚未记录曝光。" }
            return@LaunchedEffect
        }
        if (session?.state in listOf("REVEAL_REQUESTED", "REVEALED")) {
            original = null; request = null
            try {
                val next = repository.requestReveal(session!!.id)
                val loaded = repository.readOriginal(next.memoryId)
                request = next; original = loaded
                if (loaded == null) message = "原文尚未读取成功。"
            } catch (_: Exception) { message = "原文读取失败，可重试；尚未记录曝光。" }
        }
    }
    if (encounter == null || session == null) {
        PanelScaffold("遇见过去", color = EncounterColor, onBack = onBack) {
            PanelTitle("今天很安静。")
            PanelCopy(message)
        }
        return
    }
    val sid = session.id
    if (!inputLoaded) {
        PanelScaffold("遇见过去", color = EncounterColor, onBack = onBack) {
            Text(if (inputRestoreFailed) "回忆读取失败，尚未修改记录。" else "正在恢复这次相遇…", color = Muted)
            if (inputRestoreFailed) TextAction("重试恢复") { inputRestoreAttempt++ }
        }
        return
    }
    val stopped = session.cueEngineStopped
    if (reflectionPage && session.state == "REVEALED") {
        PanelScaffold("回应", color = EncounterColor, onBack = onBack) {
            PanelTitle("现在呢？")
            PrototypeTextField(reflection, { reflection = it }, "现在的回应（可选）", "如果你愿意，可以写一点……", minHeight = 140.dp)
            Text(message, color = Muted, fontSize = 12.sp)
            ActionRow {
                PrimaryAction("完成这次相遇", enabled = !busy) { command { repository.complete(sid, reflection); onBack() } }
                TextAction("不写了") { command { repository.complete(sid, null); onBack() } }
            }
        }
    } else if (session.state in listOf("REVEAL_REQUESTED", "REVEALED")) {
        PanelScaffold("揭晓", color = EncounterColor, onBack = onBack) {
            PanelTitle("那时候的你，是这样留下的。")
            if (recall.isNotBlank()) CompareBlock("现在记得的", recall)
            val current = request; val loaded = original
            if (loaded != null && current != null) CompareBlock("当时留下的", loaded, current.actionId) { repository.originalVisible(current) }
            else Text(message, modifier = Modifier.padding(top = 32.dp), color = Muted)
            ActionRow {
                PrimaryAction("现在呢？", enabled = session.state == "REVEALED" && !busy) { reflectionPage = true }
                if (loaded == null) SecondaryAction("重试读取") { command {
                    request = repository.requestReveal(sid)
                    original = repository.readOriginal(encounter.memory.id)
                } }
            }
        }
    } else {
        PanelScaffold(if (stopped) "暂时停在这里" else "你还记得吗？", color = EncounterColor, onBack = onBack) {
            VisibleReceipt("shore:" + encounter.event.id, onVisible = { repository.shoreVisible(encounter.event.id) }) {
                PanelTitle(if (stopped) "不用勉强自己。" else "你想起什么了吗？")
            }
            val cue = pendingCue ?: encounter.shownCues.lastOrNull()
            if (cue != null && !stopped) CueBlock("线索", cue.value, "cue:$sid:${cue.id}") { repository.cueVisible(sid, cue.id) }
            if (!stopped) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 26.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    listOf("CLEAR" to "清楚", "PARTIAL" to "一部分", "VAGUE" to "模糊", "NONE" to "不记得").forEach { (value, label) ->
                        FilterChip(selected = clarity == value, onClick = { clarity = value }, label = { Text(label, fontSize = 11.sp) })
                    }
                }
                PrototypeTextField(recall, { value ->
                    recall = value
                    scope.launch(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
                        inputMutex.withLock { runCatching { repository.saveRecallInput(sid, value, clarity) }.onFailure { message = "输入尚未保存，请重试。" } }
                    }
                }, "现在记得的版本（可选）", "写下你现在记得的……", minHeight = 140.dp)
            }
            Text(message, color = Muted, fontSize = 12.sp)
            ActionRow {
                PrimaryAction(if (recall.isBlank() || stopped) "直接看原记录" else "收好回忆并揭晓", enabled = !busy) { command {
                    if (!stopped && recall.isNotBlank()) repository.answer(sid, recall, clarity)
                    repository.requestReveal(sid)
                } }
                if (!stopped && encounter.remainingCues.isNotEmpty()) SecondaryAction("再给一点") { command { pendingCue = repository.nextCue(sid) } }
            }
            if (!stopped) TextAction("停止回忆") { command { repository.stopRecall(sid) } }
            TextAction("再待七天") { command { repository.defer(encounter.event.id); onBack() } }
        }
    }
}

@Composable
private fun EmptyBlock(label: String, headline: String) {
    val hairline = Hairline
    Column(modifier = Modifier.fillMaxWidth().padding(top = 58.dp, bottom = 8.dp).drawBehind { drawLine(hairline, Offset(0f, 0f), Offset(size.width, 0f), 1f); drawLine(hairline, Offset(0f, size.height), Offset(size.width, size.height), 1f) }.padding(vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Text(headline, color = Ink, fontSize = 22.sp, lineHeight = 29.sp)
    }
}

@Composable
internal fun CueBlock(label: String, value: String, receiptId: String? = null, onVisible: suspend () -> Unit = {}) {
    val hairline = Hairline
    Column(modifier = Modifier.fillMaxWidth().padding(top = 62.dp, bottom = 8.dp).drawBehind { drawLine(hairline, Offset(0f, 0f), Offset(size.width, 0f), 1f); drawLine(hairline, Offset(0f, size.height), Offset(size.width, size.height), 1f) }.padding(vertical = 26.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        if (receiptId != null) VisibleReceipt(receiptId, onVisible = onVisible) { Text(value, color = Ink, fontSize = 27.sp, lineHeight = 34.sp) }
        else Text(value, color = Ink, fontSize = 27.sp, lineHeight = 34.sp)
    }
}

@Composable
private fun CompareBlock(label: String, value: String, receiptId: String? = null, onVisible: suspend () -> Unit = {}) {
    val hairline = Hairline
    Column(modifier = Modifier.fillMaxWidth().padding(top = 22.dp).drawBehind { drawLine(hairline, Offset(0f, 0f), Offset(size.width, 0f), 1f) }.padding(top = 20.dp)) {
        Text(label, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        if (receiptId != null) VisibleReceipt(receiptId, onVisible = onVisible) { Text(value, color = Ink, fontSize = 17.sp, lineHeight = 29.sp) }
        else Text(value, color = Ink, fontSize = 17.sp, lineHeight = 29.sp)
    }
}

@Composable
private fun PastPanel(repository: MemoryRepository, onBack: () -> Unit) {
    val hairline = Hairline
    val persisted by repository.state.collectAsStateWithLifecycle()
    val memories = persisted.memories.filter { it.state != "DRAFT" && it.state != "DELETED" }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = memories.firstOrNull { it.id == selectedId }
    if (selected == null) {
        PanelScaffold("很久以前", color = PastColor, onBack = onBack) {
            PanelTitle("你留下的过去。")
            PanelCopy("按年份寻找。这里不会主动替你解释任何一段记忆。")
            PrototypeTextField(query, { query = it }, "搜索过去", "日期或明确标题", singleLine = true)
            val visible = memories.filter { query.isBlank() || V1Rules.displayLabel(it).contains(query, ignoreCase = true) }
            if (visible.isEmpty()) Text("还没有留下任何记忆。", modifier = Modifier.padding(top = 40.dp), color = Muted, fontSize = 13.sp)
            else Column(modifier = Modifier.fillMaxWidth().padding(top = 40.dp)) {
                visible.groupBy { SimpleDateFormat("yyyy", Locale.CHINA).format(Date(it.occurredAt)) }.toSortedMap(compareByDescending { it }).forEach { (year, entries) ->
                    Row(modifier = Modifier.fillMaxWidth().height(84.dp).drawBehind { drawLine(hairline, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(year, color = Ink, fontSize = 30.sp); Text("${entries.size} 段", color = Muted, fontSize = 13.sp)
                    }
                    entries.forEach { memory ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp).noRippleClickable { selectedId = memory.id }.drawBehind { drawLine(hairline, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1f) }, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(V1Rules.displayLabel(memory), color = Ink, fontSize = 15.sp)
                            Text(formatDate(memory.occurredAt), color = Muted, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    } else key(selected.id) {
        var original by remember { mutableStateOf<String?>(null) }
        var photos by remember { mutableStateOf<List<CapturePhoto>>(emptyList()) }
        var mediaFailed by remember { mutableStateOf(false) }
        val context = LocalContext.current.applicationContext
        val captureGateway = remember(repository) { LocalCaptureGateway(context, repository) }
        var failed by remember { mutableStateOf(false) }
        var historyCount by remember { mutableStateOf(0) }
        var retry by remember { mutableStateOf(0) }
        val actionId = rememberSaveable { UUID.randomUUID().toString() }
        LaunchedEffect(selected.id, retry) {
            original = null; failed = false; photos = emptyList(); mediaFailed = false
            try {
                original = repository.readOriginal(selected.id)
                historyCount = repository.recallHistory(selected.id).size
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { failed = true }
            try { photos = captureGateway.loadDraft(selected.id)?.photos.orEmpty() }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (_: Exception) { mediaFailed = true }
        }
        PanelScaffold(SimpleDateFormat("yyyy", Locale.CHINA).format(Date(selected.occurredAt)), color = PastColor, onBack = { selectedId = null }) {
            PanelTitle(V1Rules.displayLabel(selected))
            PanelCopy("一段已经封存的记录。")
            val loaded = original
            if (!loaded.isNullOrEmpty()) CompareBlock("当时留下", loaded, actionId) { repository.originalVisible(repository.archiveRequest(selected.id, actionId)) }
            else if (loaded == null) Text(if (failed) "读取失败，尚未记录曝光。" else "正在读取原文…", color = Muted)
            else Text("这段没有文字正文。", color = Muted, fontSize = 13.sp)
            PrivatePhotoReadback(photos, captureGateway, actionId) { photo ->
                repository.originalVisible(repository.archiveRequest(selected.id, actionId), "PHOTO", listOf(photo.id))
            }
            if (mediaFailed) Text("照片读取失败，正文仍可查看；尚未记录照片曝光。", color = Muted, fontSize = 13.sp)
            CompareBlock("会话次数", "$historyCount 次")
            ActionRow {
                PrimaryAction("回到过去") { selectedId = null }
                if (failed || mediaFailed) SecondaryAction("重试") { retry += 1 }
            }
        }
    }
}

@Composable
private fun SettingsPanel(
    repository: MemoryRepository,
    aiProvider: CueSuggestionProvider,
    displaySettings: DisplaySettings,
    onDisplaySettingsChange: (DisplaySettings) -> Unit,
    onBack: () -> Unit
) {
    val hairline = Hairline
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
    val persisted by repository.state.collectAsStateWithLifecycle()
    val proactive = persisted.randomEnabled
    var notification by rememberSaveable { mutableStateOf(true) }
    var status by remember { mutableStateOf("配置只保存在本机，API Key 使用 Android Keystore 加密") }

    PanelScaffold("设置", color = CardKind.Settings.color, onBack = onBack) {
        PanelTitle("让它保持私人。")
        PanelCopy("你可以随时暂停主动浮现，也可以删除已经封存的记忆。")
        SettingRow(
            "夜间外观",
            "开启后使用夜色；关闭后跟随当地时间",
            displaySettings.forceNight,
            "夜色",
            offLabel = "跟随时间"
        ) { onDisplaySettingsChange(displaySettings.copy(forceNight = !displaySettings.forceNight)) }
        SettingRow(
            "降低透明度",
            "使用纯色控件，减少玻璃效果",
            displaySettings.reduceTransparency,
            "纯色",
            offLabel = "玻璃"
        ) { onDisplaySettingsChange(displaySettings.copy(reduceTransparency = !displaySettings.reduceTransparency)) }
        SettingRow("主动浮现", "允许被随机选中", proactive, "开") { scope.launch { repository.setRandomEnabled(!proactive) } }
        SettingRow("浮现通知", "只提醒“有一页回来”", notification, "开") { notification = !notification }
        SettingRow("本地锁", "打开 App 时再确认一次", lockEnabled, if (lockEnabled) "开" else "关") { lockEnabled = !lockEnabled }
        Column(modifier = Modifier.padding(top = 40.dp).drawBehind { drawLine(hairline, Offset(0f, 0f), Offset(size.width, 0f), 1f) }.padding(top = 18.dp)) {
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
private fun SettingRow(
    title: String,
    detail: String,
    enabled: Boolean,
    value: String,
    offLabel: String = "关",
    onClick: () -> Unit
) {
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
        Text(if (enabled) value else offLabel, color = if (enabled) Accent else Muted, fontSize = 12.sp)
    }
}

private fun formatDate(timestamp: Long): String = SimpleDateFormat("yyyy/MM/dd", Locale.CHINA).format(Date(timestamp))
