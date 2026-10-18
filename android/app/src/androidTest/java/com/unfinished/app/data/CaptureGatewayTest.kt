package com.unfinished.app.data

import android.graphics.Bitmap
import android.net.Uri
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.unfinished.app.data.media.LocalMediaStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.security.KeyStore
import java.util.UUID

/** Real Room + private cipher files; every fixture has its own root, DB and aliases. */
@RunWith(AndroidJUnit4::class)
class CaptureGatewayTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private class Fixture(val repo: MemoryRepository, val media: LocalMediaStore, val root: File, val source: File, val mediaAlias: String)
    private suspend fun fixture(repoFault: (String) -> Unit = {}, block: suspend (Fixture) -> Unit) {
        val token = UUID.randomUUID().toString()
        val name = "gateway-fixture-$token.db"
        val textAlias = "unfinished.test.text.$token"
        val mediaAlias = "unfinished.test.media.$token"
        val root = File(context.cacheDir, "gateway-fixture-$token").apply { mkdirs() }
        val mediaRoot = File(root, "media")
        val source = File(root, "source.png")
        Bitmap.createBitmap(80, 60, Bitmap.Config.ARGB_8888).also { image ->
            source.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }; image.recycle()
        }
        val db = Room.databaseBuilder(context, UnfinishedDatabase::class.java, name).build()
        val repo = MemoryRepository(db, LocalCipher(textAlias), token, fault = repoFault)
        val media = LocalMediaStore(context, token, mediaRoot, keyAlias = mediaAlias)
        try { block(Fixture(repo, media, mediaRoot, source, mediaAlias)) } finally {
            repo.closeObservations(); db.close(); context.deleteDatabase(name)
            // Only the known fixture root; never production noBackupFilesDir/media.
            mediaRoot.listFiles().orEmpty().forEach { it.delete() }; mediaRoot.delete(); source.delete(); root.delete()
            val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            listOf(textAlias, mediaAlias).forEach { if (store.containsAlias(it)) store.deleteEntry(it) }
        }
    }

    @Test fun lostAttachmentReplyReturnsCommittedAssetAndNextSavePreservesPrivateFiles(): Unit = runBlocking {
        fixture { f ->
            val gateway = LocalCaptureGateway(context, f.repo, f.media) { if (it == "after-media-register") error("lost reply") }
            val draft = gateway.saveDraft(CaptureDraft(text = "successful text"))
            val photo = gateway.importPhoto(draft.id, Uri.fromFile(f.source))
            assertEquals(listOf(photo.id), gateway.loadDraft(draft.id)!!.photos.map { it.id })
            val saved = gateway.saveDraft(draft.copy(photos = listOf(photo)))
            assertEquals(photo.id, saved.photos.single().id)
            assertTrue(File(f.root, photo.originalPath!!).isFile)
            assertTrue(File(f.root, photo.previewPath!!).isFile)
            gateway.preview(photo).also { assertEquals(80, it.width); it.recycle() }
        }
    }

    @Test fun failedRegistrationCleansOnlyNewFilesAndPreservesPreviousAttachment(): Unit = runBlocking {
        var inject = false
        fixture(repoFault = { if (inject && it == "media-register-before-commit") error("Room rollback") }) { f ->
            val gateway = LocalCaptureGateway(context, f.repo, f.media)
            val draft = gateway.saveDraft(CaptureDraft(text = "keep previous"))
            val first = gateway.importPhoto(draft.id, Uri.fromFile(f.source))
            gateway.saveDraft(draft.copy(photos = listOf(first)))
            val knownPaths = f.root.listFiles()!!.map { it.name }.toSet()
            inject = true
            assertTrue(runCatching { gateway.importPhoto(draft.id, Uri.fromFile(f.source)) }.isFailure)
            assertEquals(knownPaths, f.root.listFiles()!!.map { it.name }.toSet())
            assertEquals(listOf(first.id), gateway.loadDraft(draft.id)!!.photos.map { it.id })
            gateway.preview(first).recycle()
        }
    }

    @Test fun sourceRemovalAndGatewayRecreationRetainPhotosWithoutExposure(): Unit = runBlocking {
        fixture { f ->
            val gateway = LocalCaptureGateway(context, f.repo, f.media)
            val draft = gateway.saveDraft(CaptureDraft())
            val photo = gateway.importPhoto(draft.id, Uri.fromFile(f.source))
            gateway.saveDraft(draft.copy(photos = listOf(photo)))
            assertTrue(f.source.delete())
            val recreated = LocalCaptureGateway(context, f.repo, f.media)
            val restored = recreated.loadDraft(draft.id)!!
            assertTrue(restored.hasContent); assertEquals("", restored.text)
            assertFalse(restored.photos.single().cueAllowed)
            recreated.preview(restored.photos.single()).recycle()
            assertTrue(f.repo.database.memoryDao().memoryExposures(draft.id).isEmpty())
            assertFalse(recreated.seal(restored).alreadySealed)
            assertTrue(recreated.seal(restored).alreadySealed)
            // Creation, preview and the ceremony never expose the Original.
            val dao = f.repo.database.memoryDao()
            assertTrue(dao.memoryExposures(draft.id).isEmpty())
            val visibleAt = System.currentTimeMillis()
            val read = f.repo.archiveRequest(draft.id, "photo-readback-${draft.id}")
            f.repo.originalVisible(read, "PHOTO", listOf(photo.id))
            f.repo.originalVisible(read, "PHOTO", listOf(photo.id))
            val exposures = dao.memoryExposures(draft.id)
            assertEquals(2, exposures.size)
            assertTrue(exposures.all { it.source == "ARCHIVE_OPEN" && it.sessionId == null })
            assertEquals(setOf("ORIGINAL" to "PHOTO", "ORIGINAL_MEDIA" to photo.id),
                exposures.map { it.kind to it.contentScope }.toSet())
            assertTrue(dao.allEvents().isEmpty())
            assertTrue(dao.findMemory(draft.id)!!.cooldownUntil!! >= visibleAt + 30 * V1Rules.DAY_MS)
            assertEquals("", f.repo.readOriginal(draft.id))
            assertFalse(recreated.loadDraft(draft.id)!!.photos.single().cueAllowed)
        }
    }

    @Test fun sealAuthenticatesActualRoomPathsNotForgedCallerPaths(): Unit = runBlocking {
        fixture { f ->
            val gateway = LocalCaptureGateway(context, f.repo, f.media)
            val draft = gateway.saveDraft(CaptureDraft(text = "original stays editable"))
            val first = gateway.importPhoto(draft.id, Uri.fromFile(f.source))
            val second = gateway.importPhoto(draft.id, Uri.fromFile(f.source))
            val saved = gateway.saveDraft(draft.copy(photos = listOf(first, second)))
            val original = File(f.root, first.originalPath!!)
            val bytes = original.readBytes().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
            original.writeBytes(bytes)
            val forged = saved.copy(photos = listOf(first.copy(originalPath = second.originalPath,
                previewPath = second.previewPath), second))
            assertTrue(runCatching { gateway.seal(forged) }.isFailure)
            assertFalse(gateway.loadDraft(draft.id)!!.sealed)
            assertEquals("original stays editable", gateway.loadDraft(draft.id)!!.text)
            assertEquals(2, gateway.loadDraft(draft.id)!!.photos.size)
        }
    }

    @Test fun lostSealReplyCanBeReconciledAsOneImmutableRecord(): Unit = runBlocking {
        fixture { f ->
            val gateway = LocalCaptureGateway(context, f.repo, f.media) { if (it == "after-seal-commit") error("lost reply") }
            val draft = gateway.saveDraft(CaptureDraft(text = "immutable original"))
            assertTrue(runCatching { gateway.seal(draft) }.isFailure)
            val committed = gateway.loadDraft(draft.id)!!
            assertTrue(committed.sealed)
            assertTrue(gateway.seal(draft.copy(text = "must not overwrite")).alreadySealed)
            assertEquals("immutable original", f.repo.readOriginal(draft.id))
            assertEquals(1, f.repo.listMemories().size)
        }
    }

    @Test fun temporaryLegacyProbeIoFailureReturnsWholeDraftAndCanRetry(): Unit = runBlocking {
        fixture { f ->
            val original = LocalCaptureGateway(context, f.repo, f.media).saveDraft(CaptureDraft(text = "keep whole legacy draft"))
            val uri = Uri.fromFile(f.source).toString()
            val assetId = f.repo.saveMediaAsset(original.id, uri)
            var fail = true
            val gateway = LocalCaptureGateway(context, f.repo, f.media) {
                if (fail && it == "legacy-source-probe") throw java.io.IOException("temporary cloud read/close failure")
            }
            val loaded = gateway.loadDraft(original.id)!!
            assertEquals(original.text, loaded.text)
            assertTrue(loaded.importErrors.isNotEmpty())
            val unchanged = f.repo.captureMedia(original.id).single()
            assertEquals(assetId, unchanged.id); assertEquals("LEGACY", unchanged.mediaState)
            assertEquals(uri, unchanged.localUri)
            fail = false
            val retried = gateway.loadDraft(original.id)!!
            assertTrue(retried.importErrors.isEmpty()); assertTrue(retried.photos.single().available)
            gateway.preview(retried.photos.single()).recycle()
            assertEquals(original.text, retried.text)
        }
    }

    @Test fun newSizeGuardDoesNotPermanentlyDiscardALegacySource(): Unit = runBlocking {
        fixture { f ->
          for (limits in listOf(com.unfinished.app.data.media.MediaLimits(maxOriginalBytes = 16),
              com.unfinished.app.data.media.MediaLimits(maxInputPixels = 1))) {
            val owner = LocalCaptureGateway(context, f.repo, f.media).saveDraft(CaptureDraft(text = "legacy stays"))
            val uri = Uri.fromFile(f.source).toString()
            val asset = f.repo.saveMediaAsset(owner.id, uri)
            f.repo.authorizePhotoCue(owner.id, asset, true)
            val limitedMedia = LocalMediaStore(context, f.repo.scopeId, f.root,
                limits, keyAlias = f.mediaAlias)
            val limited = LocalCaptureGateway(context, f.repo, limitedMedia).loadDraft(owner.id)!!
            assertEquals("legacy stays", limited.text); assertTrue(limited.importErrors.isNotEmpty())
            val retained = f.repo.captureMedia(owner.id).single()
            assertEquals(asset, retained.id); assertEquals(uri, retained.localUri)
            assertEquals("LEGACY", retained.mediaState)
            assertTrue(retained.cueAllowed)
            val retry = LocalCaptureGateway(context, f.repo, f.media).loadDraft(owner.id)!!
            assertTrue(retry.photos.single().available); assertTrue(retry.importErrors.isEmpty())
            assertTrue(retry.photos.single().cueAllowed)
          }
        }
    }
}
