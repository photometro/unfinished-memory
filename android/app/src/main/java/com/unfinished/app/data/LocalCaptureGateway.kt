package com.unfinished.app.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.unfinished.app.data.media.LocalMediaStore
import com.unfinished.app.data.media.StoredMedia
import com.unfinished.app.data.media.MediaStoreException
import com.unfinished.app.data.media.MediaLimitException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Coordinates file completion with Room references. The lock covers import's
 * return-to-registration gap and reference collection-to-recovery gap as well.
 */
class LocalCaptureGateway(
    context: Context,
    private val repository: MemoryRepository,
    suppliedMediaStore: LocalMediaStore? = null,
    private val fault: (String) -> Unit = {}
) : CaptureGateway {
    private val appContext = context.applicationContext
    // media/v1 is shared by this app's vault namespaces; reference cleanup is
    // global, so coordination cannot be keyed only by the current vault ID.
    private val lock = locks.computeIfAbsent(appContext.noBackupFilesDir.absolutePath) { Mutex() }
    private val media by lazy { suppliedMediaStore ?: LocalMediaStore(appContext, repository.scopeId) }
    private var recovered = false
    private var cleanupWarning = ""

    private suspend fun recoverLocked() {
        if (!recovered) {
            cleanupLocked()
        }
    }

    private suspend fun cleanupLocked() {
        try {
            media.recover(repository.referencedMediaPaths())
            recovered = true
            cleanupWarning = ""
        } catch (failure: MediaStoreException) {
            recovered = false
            cleanupWarning = "内容已保存；暂存文件清理待重试。"
        }
    }

    override suspend fun latestDraft(): CaptureDraft? = withContext(Dispatchers.IO) {
        lock.withLock {
            recoverLocked()
            repository.latestCaptureDraft()?.let { loadLocked(it.id) }
        }
    }

    override suspend fun loadDraft(id: String): CaptureDraft? = withContext(Dispatchers.IO) {
        lock.withLock { recoverLocked(); loadLocked(id) }
    }

    private suspend fun loadLocked(id: String): CaptureDraft? {
        val legacyIssues = mutableListOf<String>()
        repository.captureMedia(id).filter { it.mediaState == "LEGACY" }.forEach { asset ->
            val uri = asset.localUri?.let(Uri::parse)
            val accessible = if (uri == null) false else try {
                fault("legacy-source-probe")
                appContext.contentResolver.openInputStream(uri)?.use { it.read(); true } ?: false
            } catch (_: SecurityException) {
                false
            } catch (_: FileNotFoundException) {
                false
            } catch (_: IOException) {
                legacyIssues += "有旧照片来源暂时无法读取，原文和其他照片已保留；请稍后重试。"
                return@forEach
            }
            if (!accessible) {
                repository.upgradeLegacyMedia(asset.copy(mediaState = "UNAVAILABLE", localUri = null))
            } else {
                val existingBytes = repository.captureMedia(id).sumOf { it.originalBytes }
                try {
                    val stored = media.importPhoto(id, asset.id, uri!!, existingBytes)
                    repository.upgradeLegacyMedia(asset.withStored(stored))
                } catch (failure: MediaStoreException) {
                    // A newly calibrated resource/size guard does not mean an
                    // old source is corrupt. Keep its URI for a later retry.
                    if (failure.invalidInput && failure.cause !is MediaLimitException) {
                        repository.upgradeLegacyMedia(asset.copy(mediaState = "UNAVAILABLE", localUri = null))
                    } else {
                        legacyIssues += failure.message ?: "有旧照片暂不能转换，请重试。"
                    }
                } catch (failure: Throwable) {
                    withContext(NonCancellable) { cleanupLocked() }
                    throw failure
                }
            }
        }
        return repository.captureDraft(id)?.let {
            it.copy(importErrors = it.importErrors + legacyIssues, maintenanceWarning = cleanupWarning)
        }
    }

    override suspend fun saveDraft(draft: CaptureDraft): CaptureDraft = withContext(Dispatchers.IO) {
        lock.withLock {
            recoverLocked()
            val saved = repository.saveCaptureDraft(draft)
            // Query fresh references only after commit; never delete a rolled-back asset.
            cleanupLocked()
            saved.copy(maintenanceWarning = cleanupWarning)
        }
    }

    override suspend fun importPhoto(memoryId: String, uri: Uri): CapturePhoto = withContext(Dispatchers.IO) {
        lock.withLock {
            recoverLocked()
            val draft = repository.captureDraft(memoryId) ?: error("请先保存当前草稿。")
            check(!draft.sealed) { "已封存的照片不能编辑。" }
            val existing = repository.captureMedia(memoryId)
            require(existing.size < 20) { "这一段最多留下20张照片。" }
            val assetId = UUID.randomUUID().toString()
            val stored = media.importPhoto(memoryId, assetId, uri, existing.sumOf { it.originalBytes })
            try {
                val entity = MediaAssetEntity(assetId, memoryId, null, null, stored.cipherHash, "LOCAL", null)
                    .withStored(stored)
                repository.attachPrivateMedia(entity)
                fault("after-media-register")
                entity.toCapturePhoto()
            } catch (failure: Throwable) {
                // A Room commit may precede a lost reply. Return the actual
                // registered ID so the next draft snapshot cannot delete it
                // as an apparently unselected attachment. Cancellation is
                // still propagated; re-entry loads the durable snapshot.
                val registered = withContext(NonCancellable) {
                    cleanupLocked()
                    repository.captureMedia(memoryId).firstOrNull { it.id == assetId && it.mediaState == "READY" }
                }
                if (failure !is kotlinx.coroutines.CancellationException && registered != null) {
                    return@withLock registered.toCapturePhoto()
                }
                throw failure
            }
        }
    }

    override suspend fun seal(draft: CaptureDraft): SealReceipt = withContext(Dispatchers.IO) {
        lock.withLock {
            recoverLocked()
            val existing = repository.captureDraft(draft.id)
            if (existing?.sealed == true) return@withLock SealReceipt(draft.id, true)
            require(draft.importErrors.isEmpty()) { "请重试或明确移除未导入的照片。" }
            val actualAssets = repository.captureMedia(draft.id).associateBy { it.id }
            draft.photos.forEach { photo ->
                val actual = actualAssets[photo.id]
                require(actual != null && actual.memoryId == draft.id && actual.mediaState == "READY") { "照片尚未完成私有保存。" }
                media.verify(draft.id, actual.id, requireNotNull(actual.originalPath), requireNotNull(actual.previewPath))
            }
            val receipt = repository.sealCaptureDraft(draft)
            fault("after-seal-commit")
            receipt
        }
    }

    override suspend fun preview(photo: CapturePhoto): Bitmap = withContext(Dispatchers.IO) {
        lock.withLock {
            require(photo.available) { "旧照片来源已失效，请重新选择来源。" }
            media.preview(photo.memoryId, photo.id, requireNotNull(photo.previewPath))
        }
    }

    private fun MediaAssetEntity.withStored(stored: StoredMedia) = copy(
        localUri = null, originalPath = stored.originalPath, previewPath = stored.previewPath,
        mimeType = stored.mimeType, width = stored.width, height = stored.height,
        originalBytes = stored.originalBytes, sha256 = stored.cipherHash,
        previewCipherHash = stored.previewCipherHash, mediaState = "READY"
    )

    private companion object { val locks = ConcurrentHashMap<String, Mutex>() }
}
