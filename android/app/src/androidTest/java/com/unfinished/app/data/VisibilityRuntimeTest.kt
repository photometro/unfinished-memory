package com.unfinished.app.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.*
import com.unfinished.app.EncounterPanel
import com.unfinished.app.CueBlock
import com.unfinished.app.LeavePanel
import com.unfinished.app.ai.AiProviderConfigStore
import com.unfinished.app.ai.CueSuggestionProvider
import androidx.lifecycle.Lifecycle
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class VisibilityRuntimeTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: UnfinishedDatabase
    private lateinit var repo: MemoryRepository
    private lateinit var memoryId: String
    @Before fun setup() = runBlocking {
        check(context.packageName == "com.unfinished.app.protocoltest")
        db = Room.inMemoryDatabaseBuilder(context, UnfinishedDatabase::class.java).build()
        repo = MemoryRepository(db, LocalCipher(), "visibility-scope")
        memoryId = repo.saveCapture(null, "", "synthetic visible original", Instant.now().toEpochMilli(), "QUIET", null, emptyList(), true)
    }
    @After fun close() { repo.closeObservations(); db.close() }
    private fun count() = runBlocking { db.memoryDao().memoryExposures(memoryId).size }

    @Test fun loadingOffscreenAndBackgroundDoNotExposeVisibleFrameDoesOnce() {
        val loaded = mutableStateOf(false)
        val shown = mutableStateOf(false)
        val recompose = mutableStateOf(0)
        val request = repo.archiveRequest(memoryId, "stable-read-action")
        compose.setContent {
            Column {
                Text("Loading ${recompose.value}")
                if (loaded.value) {
                    if (!shown.value) Spacer(Modifier.height(3000.dp))
                    VisibleReceipt(request.actionId, onVisible = { repo.originalVisible(request) }) { Text("synthetic visible original") }
                }
            }
        }
        compose.waitForIdle(); assertEquals(0, count())
        compose.runOnIdle { loaded.value = true }
        compose.waitForIdle(); assertEquals(0, count())
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.runOnUiThread { shown.value = true }
        android.os.SystemClock.sleep(200)
        assertEquals(0, count())
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(5000) { count() == 1 }
        compose.runOnIdle { recompose.value++ }
        compose.waitForIdle(); assertEquals(1, count())
        assertTrue(runBlocking { db.memoryDao().allEvents().isEmpty() })
        assertNotNull(runBlocking { db.memoryDao().findMemory(memoryId)!!.cooldownUntil })
    }

    @Test fun failedPhotoDecodeDoesNotExposeRetryUsesSameActionAndRecordsBothKinds() = runBlocking {
        val photoDb = Room.inMemoryDatabaseBuilder(context, UnfinishedDatabase::class.java).build()
        val photoRepo = MemoryRepository(photoDb, LocalCipher(), "photo-scope")
        val id = photoRepo.saveDraft(null, "", "", Instant.now().toEpochMilli(), "FIXED_DATE", 1L)
        val asset = photoRepo.saveMediaAsset(id, "synthetic://pixel")
        photoRepo.authorizePhotoCue(id, asset, true)
        photoRepo.seal(id)
        val sid = photoRepo.schedule().encounter!!.session.id
        val bytes = ByteArrayOutputStream().also {
            Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it)
        }.toByteArray()
        val photo = mutableStateOf<Bitmap?>(BitmapFactory.decodeByteArray(byteArrayOf(1, 2, 3), 0, 3))
        val updates = mutableStateOf(0)
        compose.setContent {
            Column {
                Text("decode: ${updates.value}")
                photo.value?.let { decoded ->
                    VisibleReceipt("stable-photo-display", fullyVisible = true,
                        onVisible = { photoRepo.photoCueVisible(sid, asset, "stable-photo-display") }) {
                        Image(decoded.asImageBitmap(), "synthetic photo", Modifier.size(32.dp))
                    }
                } ?: Text("failed image")
            }
        }
        compose.waitForIdle(); assertTrue(photoDb.memoryDao().sessionExposures(sid).isEmpty())
        compose.runOnIdle { photo.value = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
        compose.waitUntil(5000) { runBlocking { photoDb.memoryDao().sessionExposures(sid).size == 2 } }
        compose.runOnIdle { updates.value++ }
        compose.waitForIdle()
        assertEquals(2, photoDb.memoryDao().sessionExposures(sid).size)
        assertEquals("POST_EXPOSURE_RECALL", photoRepo.current().encounter!!.session.recallMode)
        photoRepo.closeObservations(); photoDb.close()
    }

    @Test fun realEncounterUiFlushesFastInputBeforeStopAndHasNoCueWhenStopped() = runBlocking {
        val draft = repo.saveDraft(null, "", "real synthetic original", Instant.now().toEpochMilli(), "FIXED_DATE", 1L)
        repo.saveCues(draft, listOf(repo.cue("ui-cue", "KEYWORD", "adopted cue", confirmed = true)))
        repo.seal(draft)
        val active = repo.schedule().encounter!!
        compose.setContent { EncounterPanel(repo, "returned", {}, {}) }
        compose.waitUntil(5000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction()).performTextInput("rapid real input")
        compose.onNodeWithText("停止回忆").performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { repo.current().encounter!!.session.cueEngineStopped } }
        assertEquals("rapid real input", repo.recallInput(active.session.id))
        assertEquals("rapid real input", LocalCipher().decrypt(db.memoryDao().snapshots(active.session.id).single().encryptedContent))
        compose.onNodeWithText("再给一点").assertDoesNotExist()
        assertEquals(active.session.id, repo.schedule().encounter!!.session.id)
    }
    @Test fun failedRecallRestoreKeepsInputClosedUntilSuccessfulRetry() = runBlocking {
        val draft = repo.saveDraft(null, "", "synthetic restore original", Instant.now().toEpochMilli(), "FIXED_DATE", 1L)
        repo.seal(draft)
        val session = repo.schedule().encounter!!.session
        db.memoryDao().upsertRecallSession(session.copy(recallContent = "invalid cipher"))
        compose.setContent { EncounterPanel(repo, "returned", {}, {}) }
        compose.waitUntil(5000) {
            compose.onAllNodesWithText("回忆读取失败，尚未修改记录。").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("停止回忆").assertDoesNotExist()
        assertEquals("invalid cipher", db.memoryDao().findRecallSession(session.id)!!.recallContent)
        assertTrue(db.memoryDao().sessionExposures(session.id).isEmpty())
        db.memoryDao().upsertRecallSession(session.copy(recallContent = LocalCipher().encrypt("recovered real input")))
        compose.onNodeWithText("重试恢复").performClick()
        compose.waitUntil(5000) {
            compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasSetTextAction()).assertTextContains("recovered real input")
        assertFalse(db.memoryDao().findRecallSession(session.id)!!.snapshotLocked)
    }

    @Test fun leaveRestoreFailureCannotUnlockEmptyDraftOnRetry() = runBlocking {
        val draft = repo.saveDraft(null, "", "saved synthetic draft", Instant.now().toEpochMilli(), "QUIET")
        val original = db.memoryDao().findOriginal(draft)!!
        db.memoryDao().upsertOriginal(original.copy(encryptedContent = "invalid cipher"))
        compose.setContent { LeavePanel(repo, CueSuggestionProvider(AiProviderConfigStore(context)), {}) }
        compose.waitUntil(5000) { compose.onAllNodesWithText("草稿恢复失败，尚未修改内容。").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("重试恢复").performClick()
        compose.waitForIdle()
        compose.onNode(hasSetTextAction()).assertDoesNotExist()
        compose.onNodeWithText("添加线索").assertDoesNotExist()
        assertEquals("invalid cipher", db.memoryDao().findOriginal(draft)!!.encryptedContent)
        db.memoryDao().upsertOriginal(original)
        compose.onNodeWithText("重试恢复").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("saved synthetic draft").fetchSemanticsNodes().isNotEmpty() }
        assertEquals("saved synthetic draft", repo.readOriginal(draft))
    }

    @Test fun newDraftKeepsUnsavedEditingAcrossSavedStateRestoration() = runBlocking {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { LeavePanel(repo, CueSuggestionProvider(AiProviderConfigStore(context)), {}) }
        compose.waitUntil(5000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().size == 2 }
        compose.onAllNodes(hasSetTextAction())[0].performTextInput("saved A")
        compose.waitUntil(5000) { runBlocking { repo.listMemories().any { it.state == "DRAFT" } } }
        val id = repo.listMemories().single { it.state == "DRAFT" }.id
        compose.waitUntil(5000) { runBlocking { repo.readOriginal(id) == "saved A" } }
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("unsaved B")
        assertEquals("saved A", repo.readOriginal(id))
        restoration.emulateSavedInstanceStateRestore()
        compose.waitUntil(5000) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasSetTextAction())[0].assertTextContains("unsaved B")
        Unit
    }

    @Test fun partiallyClippedPhotoDoesNotExposeUntilEntireImageIsVisible() {
        val receipts = AtomicInteger(0)
        val photo = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        lateinit var scroll: androidx.compose.foundation.ScrollState
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val stageHeight = maxHeight
                scroll = rememberScrollState()
                Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                    Spacer(Modifier.height(stageHeight - 32.dp))
                    VisibleReceipt("clipped-photo", fullyVisible = true, onVisible = { receipts.incrementAndGet() }) {
                        Image(photo.asImageBitmap(), "clipped synthetic photo", Modifier.size(64.dp))
                    }
                }
            }
        }
        compose.waitForIdle(); assertEquals(0, receipts.get())
        compose.runOnUiThread { runBlocking { scroll.scrollTo(300) } }
        compose.waitUntil(5000) { receipts.get() == 1 }
    }

    @Test fun cueLabelAloneAndBlockedRouteDoNotProduceVisibilityReceipt() {
        val receipts = AtomicInteger(0)
        val allowed = mutableStateOf(false)
        lateinit var scroll: androidx.compose.foundation.ScrollState
        compose.setContent {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val stageHeight = maxHeight
                scroll = rememberScrollState()
                CompositionLocalProvider(LocalReceiptAllowed provides allowed.value) {
                    Column(Modifier.fillMaxSize().verticalScroll(scroll)) {
                        Spacer(Modifier.height(stageHeight - 108.dp))
                        CueBlock("cue-label", "cue-body", "label-body-test") { receipts.incrementAndGet() }
                    }
                }
            }
        }
        compose.waitForIdle(); assertEquals(0, receipts.get())
        compose.runOnIdle { allowed.value = true }
        compose.waitForIdle(); assertEquals(0, receipts.get())
        compose.runOnUiThread { runBlocking { scroll.scrollTo(300) } }
        compose.waitUntil(5000) { receipts.get() == 1 }
        compose.waitForIdle(); assertEquals(1, receipts.get())
    }
}
