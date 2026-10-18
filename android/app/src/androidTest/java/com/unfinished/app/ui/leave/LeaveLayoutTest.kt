package com.unfinished.app.ui.leave

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.unfinished.app.data.*
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Synthetic gateway only; no user database or gallery. Actual Compose controls. */
@RunWith(AndroidJUnit4::class)
class LeaveLayoutTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val stores = mutableListOf<ViewModelStore>()
    @After fun dispose() { compose.runOnIdle { stores.forEach { it.clear() } } }

    private class FakeGateway(initial: CaptureDraft? = null) : CaptureGateway {
        var stored = initial
        var failSeal = false
        var seals = 0
        var savedBeforeImport = false
        var imports = 0
        var secondImport: CompletableDeferred<Unit>? = null
        var secondPreview: CompletableDeferred<Unit>? = null
        var previewRequested: String? = null
        override suspend fun latestDraft() = stored
        override suspend fun loadDraft(id: String) = stored?.takeIf { it.id == id }
        override suspend fun saveDraft(draft: CaptureDraft): CaptureDraft { stored = draft; return draft }
        override suspend fun importPhoto(memoryId: String, uri: Uri): CapturePhoto {
            check(stored?.id == memoryId) { "A pure-photo draft needs a durable owner first" }
            savedBeforeImport = true
            imports++
            if (imports == 2) secondImport?.await()
            return CapturePhoto("synthetic-photo-$imports", memoryId, "original", "preview")
        }
        override suspend fun seal(draft: CaptureDraft): SealReceipt {
            if (failSeal) error("synthetic disk failure")
            seals++; stored = draft.copy(sealed = true); return SealReceipt(draft.id, false)
        }
        override suspend fun preview(photo: CapturePhoto): Bitmap {
            previewRequested = photo.id
            if (photo.id == "second") secondPreview?.await()
            return Bitmap.createBitmap(4, 4, Bitmap.Config.ARGB_8888)
        }
    }

    private fun show(gateway: FakeGateway, height: Float = 640f, scale: Float = 1f,
                     reduced: androidx.compose.runtime.MutableState<Boolean> = mutableStateOf(true),
                     changingHeight: androidx.compose.runtime.MutableState<Float> = mutableStateOf(height)): LeaveController {
        lateinit var controller: LeaveController
        compose.runOnUiThread {
            controller = LeaveController(gateway, SavedStateHandle())
            ViewModelStore().also { it.put("leave", controller); stores += it }
        }
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                Box(Modifier.fillMaxSize()) {
                    Box(Modifier.size(360.dp, changingHeight.value.dp)) {
                        NativeLeaveContent(controller, gateway, {}, false, reduced.value)
                    }
                }
            }
        }
        compose.waitUntil(5000) { controller.state.value.ready }
        return controller
    }

    @Test fun textNoCueSealsOnceAndReducedMotionShowsStaticCompletion() {
        val gateway = FakeGateway()
        val controller = show(gateway)
        compose.onNodeWithTag("leave.text").performTextInput("synthetic plain record")
        compose.onNodeWithTag("leave.next").performClick()
        compose.onNodeWithTag("leave.seal").performScrollTo().performClick()
        compose.onNodeWithTag("leave.journey").assertExists()
        compose.runOnIdle {
            assertEquals(1, gateway.seals)
            assertTrue(gateway.stored!!.cues.isEmpty())
            assertEquals(LeavePage.COMPLETE, controller.state.value.page)
        }
    }

    @Test fun failedSealKeepsWholeInputEditableAndNeverStartsCeremony() {
        val gateway = FakeGateway(CaptureDraft(text = "keep whole input")).apply { failSeal = true }
        val controller = show(gateway)
        compose.onNodeWithTag("leave.next").performClick()
        compose.onNodeWithTag("leave.seal").performScrollTo().performClick()
        compose.onNodeWithTag("leave.error").assertTextContains("synthetic disk failure")
        compose.onNodeWithTag("leave.journey").assertDoesNotExist()
        compose.onNodeWithTag("leave.edit.original").performScrollTo().performClick()
        compose.onNodeWithTag("leave.text").assertTextContains("keep whole input")
        compose.runOnIdle { assertFalse(controller.state.value.draft.sealed) }
    }

    @Test fun purePhotoHasOwnerBeforeImportAndAuthorizationDefaultsOff() {
        val gateway = FakeGateway()
        val controller = show(gateway)
        compose.runOnIdle { controller.importPhotos(listOf(Uri.EMPTY)) }
        compose.waitUntil(5000) { !controller.state.value.importing }
        compose.runOnIdle {
            assertTrue(gateway.savedBeforeImport)
            assertEquals("", controller.state.value.draft.text)
            assertFalse(controller.state.value.draft.photos.single().cueAllowed)
            assertTrue(controller.state.value.draft.cues.isEmpty())
        }
        compose.onNodeWithTag("leave.next").performClick()
        compose.onNodeWithTag("leave.seal").performScrollTo().performClick()
        compose.onNodeWithTag("leave.journey").assertExists()
    }

    @Test fun smallerReselectionDoesNotSilentlyDiscardOtherFailedPhotos() {
        val controller = show(FakeGateway(CaptureDraft(text = "keep", importErrors = listOf("failed one", "failed two", "failed three"))))
        compose.runOnIdle { controller.importPhotos(listOf(Uri.EMPTY), replaceFailedItems = true) }
        compose.waitUntil(5000) { !controller.state.value.importing }
        compose.runOnIdle {
            assertEquals(listOf("failed two", "failed three"), controller.state.value.draft.importErrors)
            assertNotNull(sealValidation(controller.state.value.draft))
        }
    }

    @Test fun overQuotaReselectionDiscardsNoPreviousFailedItems() {
        val draft = CaptureDraft(text = "keep", importErrors = listOf("failed one", "failed two", "failed three"))
        val photos = (1..19).map { CapturePhoto("photo-$it", draft.id, "original", "preview") }
        val controller = show(FakeGateway(draft.copy(photos = photos)))
        compose.runOnIdle { controller.importPhotos(listOf(Uri.EMPTY, Uri.EMPTY), replaceFailedItems = true) }
        compose.waitUntil(5000) { !controller.state.value.importing }
        compose.runOnIdle {
            assertEquals(draft.importErrors, controller.state.value.draft.importErrors.take(3))
            assertEquals(19, controller.state.value.draft.photos.size)
        }
    }

    @Test fun interruptedBatchKeepsUnattemptedFailuresInDurableSnapshot() {
        val old = listOf("failed one", "failed two", "failed three")
        val gateway = FakeGateway(CaptureDraft(text = "keep", importErrors = old)).apply { secondImport = CompletableDeferred() }
        val controller = show(gateway)
        compose.runOnIdle { controller.importPhotos(listOf(Uri.EMPTY, Uri.EMPTY, Uri.EMPTY), replaceFailedItems = true) }
        compose.waitUntil(5000) { gateway.imports == 2 }
        compose.runOnIdle {
            assertEquals(1, gateway.stored!!.photos.size)
            assertEquals(listOf("failed two", "failed three"), gateway.stored!!.importErrors)
            assertTrue(controller.state.value.importing)
        }
        // Disposal cancels the pending second import, like losing this owner.
    }

    @Test fun readbackNeverExposesSecondAssetWhileShowingFirstAssetsOldBitmap() {
        val gateway = FakeGateway().apply { secondPreview = CompletableDeferred() }
        val photos = listOf("first", "second").map { CapturePhoto(it, "memory", "original", "preview") }
        val visible = mutableListOf<String>()
        compose.setContent {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                PrivatePhotoReadback(photos, gateway, "synthetic-action") { visible += it.id }
            }
        }
        compose.waitUntil(5000) { visible == listOf("first") }
        compose.onNodeWithTag("past.photo.next").performScrollTo().performClick()
        compose.waitUntil(5000) { gateway.previewRequested == "second" }
        compose.onNodeWithTag("past.photo.image").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf("first"), visible); gateway.secondPreview!!.complete(Unit) }
        compose.waitUntil(5000) { visible == listOf("first", "second") }
    }

    @Test fun unavailableReadbackNeverPretendsThatAPhotoWasVisible() {
        val gateway = FakeGateway()
        var receipts = 0
        compose.setContent {
            PrivatePhotoReadback(listOf(CapturePhoto("unavailable", "memory", null, null,
                available = false)), gateway, "failed-action") { receipts++ }
        }
        compose.onNodeWithTag("past.photo.pending").assertTextContains("尚未记录照片曝光", substring = true)
        compose.onNodeWithTag("past.photo.image").assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, receipts); assertNull(gateway.previewRequested) }
    }

    @Test fun clippedReadbackWaitsUntilTheWholePhotoIsInTheViewport() {
        val gateway = FakeGateway()
        val visible = mutableListOf<String>()
        compose.setContent {
            Column(Modifier.size(360.dp, 480.dp).verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(300.dp))
                PrivatePhotoReadback(listOf(CapturePhoto("clipped", "memory", "original", "preview")),
                    gateway, "clipped-action") { visible += it.id }
            }
        }
        compose.waitUntil(5000) { gateway.previewRequested == "clipped" }
        compose.onNodeWithTag("past.photo.image").assertExists()
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle { assertTrue("Prefetch and a cropped image are not exposure", visible.isEmpty()) }
        compose.onNodeWithTag("past.photo.image").performScrollTo()
        compose.waitUntil(5000) { visible == listOf("clipped") }
    }

    @Test fun sameWritingCompositionKeepsFooterAtBottomAcrossShortAndTallWindows() {
        val height = mutableStateOf(620f)
        show(FakeGateway(), changingHeight = height)
        fun assertFooter() {
            val viewport = compose.onNodeWithTag("leave.safe.viewport").fetchSemanticsNode().boundsInRoot
            val footer = compose.onNodeWithTag("leave.footer").fetchSemanticsNode().boundsInRoot
            assertTrue("Fitting footer belongs to window bottom: viewport=$viewport footer=$footer", viewport.bottom - footer.bottom < 90f)
            assertTrue("Footer not clipped: viewport=$viewport footer=$footer", footer.bottom <= viewport.bottom + 2f)
        }
        assertFooter()
        compose.runOnIdle { height.value = 380f }
        assertFooter()
        compose.runOnIdle { height.value = 620f }
        assertFooter()
    }

    @Test fun largeTextShortReviewScrollsToSealWithoutOverlayingContent() {
        show(FakeGateway(CaptureDraft(text = "synthetic long original\n".repeat(45))), height = 300f, scale = 1.5f)
        compose.onNodeWithTag("leave.next").performClick()
        compose.onNodeWithTag("leave.seal").performScrollTo().assertIsDisplayed()
        val footer = compose.onNodeWithTag("leave.footer").fetchSemanticsNode().boundsInRoot
        val original = compose.onNodeWithTag("leave.review.original").fetchSemanticsNode().boundsInRoot
        assertTrue("Overflow scrolls, never overlays original", footer.top > original.bottom)
    }

    @Test fun twentyPhotosRemainEditableAndSealableInAnExtremelyShortWindow() {
        val draft = CaptureDraft(text = "short window with twenty photos")
        val photos = (1..20).map { CapturePhoto("short-$it", draft.id, "original", "preview") }
        val gateway = FakeGateway(draft.copy(photos = photos))
        val controller = show(gateway, height = 180f, scale = 1.3f)
        compose.waitUntil(5000) { !controller.state.value.operationPending }
        // Scroll the vertical parent, not just the nested horizontal LazyRow.
        compose.onNodeWithTag("leave.photos").performScrollTo()
        compose.onNodeWithTag("leave.photo.open.short-1").performClick()
        compose.onNodeWithTag("leave.photo.authorize").performScrollTo().performClick()
        compose.onNodeWithTag("leave.back").performScrollTo().performClick()
        compose.onNodeWithTag("leave.next").performScrollTo().performClick()
        compose.onNodeWithTag("leave.seal").performScrollTo().assertIsDisplayed().performClick()
        compose.onNodeWithTag("leave.journey").assertExists()
        compose.runOnIdle {
            assertEquals(20, gateway.stored!!.photos.size)
            assertTrue(gateway.stored!!.photos.first().cueAllowed)
            assertEquals(1, gateway.seals)
        }
    }

    @Test fun invalidOptionalDateCanReturnWithoutForcingCompletion() {
        val controller = show(FakeGateway(CaptureDraft(text = "keep optional input")))
        compose.onNodeWithTag("leave.next").performClick()
        compose.onNodeWithTag("leave.policy").performScrollTo().performClick()
        compose.runOnIdle {
            controller.edit { it.copy(policy = "FIXED_DATE") }; controller.setDate("2099-13-")
        }
        compose.onNodeWithTag("leave.back").performClick()
        compose.runOnIdle {
            assertEquals(LeavePage.REVIEW, controller.state.value.page)
            assertEquals("2099-13-", controller.state.value.draft.fixedDateText)
        }
    }

    @Test fun reducedMotionChangeDuringReceiptCaptureCannotStrandSealedReview() {
        val reduced = mutableStateOf(false)
        val controller = show(FakeGateway(CaptureDraft(text = "real review capture")), reduced = reduced)
        compose.onNodeWithTag("leave.next").performClick()
        compose.mainClock.autoAdvance = false
        compose.runOnUiThread { controller.seal() }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnUiThread {
            assertNotNull(controller.state.value.receipt)
            assertEquals("Change during capture, not after it", LeavePage.REVIEW, controller.state.value.page)
            reduced.value = true
        }
        compose.mainClock.advanceTimeBy(64)
        compose.mainClock.autoAdvance = true
        compose.waitUntil(5000) { controller.state.value.page == LeavePage.COMPLETE }
        compose.onNodeWithTag("leave.journey").assertExists()
    }
}
