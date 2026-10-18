package com.unfinished.app.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.security.KeyStore

/** Dedicated databases only; the ordinary target app's unfinished.db is never touched. */
@RunWith(AndroidJUnit4::class)
class CaptureRuntimeTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private suspend fun fixture(fault: (String) -> Unit = {}, block: suspend (MemoryRepository) -> Unit) {
        val name = "capture-fixture-" + UUID.randomUUID() + ".db"
        val db = Room.databaseBuilder(context, UnfinishedDatabase::class.java, name)
            .addMigrations(UnfinishedDatabase.MIGRATION_5_6).build()
        val alias = "unfinished.test.text." + UUID.randomUUID()
        val repo = MemoryRepository(db, LocalCipher(alias), UUID.randomUUID().toString(), fault = fault)
        try { block(repo) } finally {
            repo.closeObservations()
            db.close()
            context.deleteDatabase(name)
            KeyStore.getInstance("AndroidKeyStore").apply { load(null); if (containsAlias(alias)) deleteEntry(alias) }
        }
    }

    @Test fun fullDraftAndOnlyAdoptedCuesSurviveReadbackWithoutExposure() = runBlocking {
        fixture { repo ->
            val draft = CaptureDraft(text = "synthetic immutable memory", occurredDate = "2024-01-02",
                place = "synthetic place", people = "synthetic person", source = "manual",
                preference = "light", fixedDateText = "2099-13-", cues = listOf(CaptureCue(value = "chosen fragment")),
                importErrors = listOf("one synthetic attachment failed"))
            repo.saveCaptureDraft(draft)
            val loaded = repo.captureDraft(draft.id)!!
            assertEquals(draft.text, loaded.text)
            assertEquals(draft.place, loaded.place)
            assertEquals(draft.people, loaded.people)
            assertEquals(draft.preference, loaded.preference)
            assertEquals("2099-13-", loaded.fixedDateText)
            assertEquals(listOf("chosen fragment"), loaded.cues.map { it.value })
            assertEquals(draft.importErrors, loaded.importErrors)
            assertFalse(loaded.sealed)
            assertTrue(repo.current().encounter == null)
            assertTrue(repo.pendingSyncOperations().all { it.state == "PENDING" })
        }
    }

    @Test fun sealRetryUsesSameIdentityAndCannotChangeOriginal() = runBlocking {
        fixture { repo ->
            val draft = CaptureDraft(text = "first immutable content", policy = "QUIET")
            repo.saveCaptureDraft(draft)
            val first = repo.sealCaptureDraft(draft)
            val retry = repo.sealCaptureDraft(draft.copy(text = "must not overwrite"))
            assertFalse(first.alreadySealed)
            assertTrue(retry.alreadySealed)
            assertEquals(first.memoryId, retry.memoryId)
            assertEquals(1, repo.listMemories().count { it.state == "SEALED" })
            assertEquals("first immutable content", repo.readOriginal(draft.id))
        }
    }

    @Test fun failedSealLeavesPreviousCompleteDraftAndCanRetry() = runBlocking {
        var inject = false
        fixture(fault = { if (inject && it == "seal-before-commit") error("synthetic interruption") }) { repo ->
            val draft = CaptureDraft(text = "durable draft")
            repo.saveCaptureDraft(draft)
            inject = true
            assertTrue(runCatching { repo.sealCaptureDraft(draft) }.isFailure)
            assertFalse(repo.captureDraft(draft.id)!!.sealed)
            assertEquals("durable draft", repo.readOriginal(draft.id))
            inject = false
            assertFalse(repo.sealCaptureDraft(draft).alreadySealed)
        }
    }

    @Test fun failedImportsAndInvalidDatesCannotProduceSealedRecord() = runBlocking {
        fixture { repo ->
            val draft = CaptureDraft(text = "retain successful input", importErrors = listOf("failed attachment"))
            repo.saveCaptureDraft(draft)
            assertTrue(runCatching { repo.sealCaptureDraft(draft) }.isFailure)
            assertTrue(runCatching { repo.sealCaptureDraft(draft.copy(importErrors = emptyList(), policy = "FIXED_DATE")) }.isFailure)
            assertFalse(repo.captureDraft(draft.id)!!.sealed)
            assertEquals(draft.text, repo.readOriginal(draft.id))
            assertFalse(repo.sealCaptureDraft(draft.copy(importErrors = emptyList(), policy = "QUIET")).alreadySealed)
        }
    }

    @Test fun stableAssetIdentityRetainsOrderAndAuthorizationAfterReorderAndRemoval() = runBlocking {
        fixture { repo ->
            val draft = CaptureDraft(text = "")
            repo.saveCaptureDraft(draft)
            val a = UUID.randomUUID().toString()
            val b = UUID.randomUUID().toString()
            val c = UUID.randomUUID().toString()
            for (id in listOf(a, b, c)) {
                repo.attachPrivateMedia(MediaAssetEntity(id, draft.id, null, null, "synthetic-cipher", "LOCAL", null,
                    originalPath = "$id-original.ufmi", previewPath = "$id-preview.ufmi", mimeType = "image/png",
                    width = 2, height = 3, originalBytes = 32, mediaState = "READY"))
            }
            val photos = repo.captureDraft(draft.id)!!.photos.associateBy { it.id }
            repo.saveCaptureDraft(draft.copy(photos = listOf(photos.getValue(c).copy(cueAllowed = true),
                photos.getValue(a), photos.getValue(b).copy(cueAllowed = true))))
            val reordered = repo.captureDraft(draft.id)!!
            assertEquals(listOf(c, a, b), reordered.photos.map { it.id })
            repo.saveCaptureDraft(reordered.copy(photos = reordered.photos.filter { it.id != b }))
            val restored = repo.captureDraft(draft.id)!!
            assertEquals(listOf(c, a), restored.photos.map { it.id })
            assertTrue(restored.photos[0].cueAllowed)
            assertFalse(restored.photos[1].cueAllowed)
            assertTrue(restored.hasContent)
            assertTrue(repo.referencedMediaPaths().none { it.startsWith(b) })
        }
    }

    @Test fun twentiethAssetAcceptedAndTwentyFirstCannotSilentlyReplaceAny() = runBlocking {
        fixture { repo ->
            val draft = CaptureDraft(text = "keep this")
            repo.saveCaptureDraft(draft)
            repeat(20) {
                val id = UUID.randomUUID().toString()
                repo.attachPrivateMedia(MediaAssetEntity(id, draft.id, null, null, "synthetic-cipher", "LOCAL", null,
                    originalPath = "$id-original.ufmi", previewPath = "$id-preview.ufmi", mediaState = "READY"))
            }
            val ids = repo.captureMedia(draft.id).map { it.id }
            val rejected = UUID.randomUUID().toString()
            assertTrue(runCatching { repo.attachPrivateMedia(MediaAssetEntity(rejected, draft.id, null, null,
                "synthetic-cipher", "LOCAL", null, originalPath = "$rejected-original.ufmi",
                previewPath = "$rejected-preview.ufmi", mediaState = "READY")) }.isFailure)
            assertEquals(ids, repo.captureMedia(draft.id).map { it.id })
            assertEquals("keep this", repo.readOriginal(draft.id))
        }
    }

    @Test fun incompleteSnapshotWriteRollsBackAllFieldsTogether() = runBlocking {
        var inject = false
        fixture(fault = { if (inject && it == "capture-draft-before-commit") error("synthetic write interruption") }) { repo ->
            val initial = CaptureDraft(text = "old whole snapshot", place = "old place")
            repo.saveCaptureDraft(initial)
            inject = true
            assertTrue(runCatching { repo.saveCaptureDraft(initial.copy(text = "new text", place = "new place",
                cues = listOf(CaptureCue(value = "new cue")))) }.isFailure)
            val restored = repo.captureDraft(initial.id)!!
            assertEquals("old whole snapshot", restored.text)
            assertEquals("old place", restored.place)
            assertTrue(restored.cues.isEmpty())
        }
    }

    @Test fun repeatedSceneWordCannotStealCueFromAnotherMemory() = runBlocking {
        fixture { repo ->
            val first = CaptureDraft(text = "first scene", cues = listOf(CaptureCue(id = "local:scene", value = "same chosen scene")))
            val second = CaptureDraft(text = "second scene", cues = listOf(CaptureCue(id = "local:scene", value = "same chosen scene")))
            repo.saveCaptureDraft(first)
            repo.saveCaptureDraft(second)
            assertEquals(listOf("same chosen scene"), repo.captureDraft(first.id)!!.cues.map { it.value })
            assertEquals(listOf("same chosen scene"), repo.captureDraft(second.id)!!.cues.map { it.value })
            assertNotEquals(repo.captureDraft(first.id)!!.cues.single().id, repo.captureDraft(second.id)!!.cues.single().id)
        }
    }

    @Test fun textReadWithMissingTestKeyDoesNotGenerateReplacement() {
        val originalAlias = "unfinished.test.text." + UUID.randomUUID()
        val missingAlias = "unfinished.test.text." + UUID.randomUUID()
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        try {
            val encrypted = LocalCipher(originalAlias).encrypt("synthetic secret")
            assertTrue(runCatching { LocalCipher(missingAlias).decrypt(encrypted) }.isFailure)
            assertFalse(store.containsAlias(missingAlias))
        } finally {
            store.deleteEntry(originalAlias)
            if (store.containsAlias(missingAlias)) store.deleteEntry(missingAlias)
        }
    }

    @Test fun loadedDraftCannotReplaceMissingKeyAndStrandOtherRecords(): Unit = runBlocking {
        val alias = "unfinished.test.text." + UUID.randomUUID()
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val name = "capture-key-fixture-" + UUID.randomUUID() + ".db"
        val db = Room.databaseBuilder(context, UnfinishedDatabase::class.java, name).build()
        val repo = MemoryRepository(db, LocalCipher(alias), UUID.randomUUID().toString())
        try {
            val draft = CaptureDraft(text = "loaded draft remains in UI")
            repo.saveCaptureDraft(draft)
            val other = CaptureDraft(text = "another encrypted record")
            repo.saveCaptureDraft(other)
            val loaded = repo.captureDraft(draft.id)!!
            store.deleteEntry(alias)
            assertTrue(runCatching { repo.saveCaptureDraft(loaded.copy(text = "new edit")) }.isFailure)
            assertFalse("A write must not replace a lost key", store.containsAlias(alias))
        } finally {
            repo.closeObservations(); db.close(); context.deleteDatabase(name)
            if (store.containsAlias(alias)) store.deleteEntry(alias)
        }
    }
}
