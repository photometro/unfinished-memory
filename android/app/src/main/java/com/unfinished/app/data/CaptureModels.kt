package com.unfinished.app.data

import android.graphics.Bitmap
import android.net.Uri
import java.util.UUID

/** A durable editable snapshot; identity is allocated before any async work. */
data class CaptureDraft(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val title: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val occurredDate: String = "",
    val place: String = "",
    val people: String = "",
    val source: String = "",
    val provenance: String = "now",
    val preference: String = "inherit",
    val policy: String = "SERENDIPITY",
    val fixedDate: Long? = null,
    val fixedDateText: String = "",
    val cues: List<CaptureCue> = emptyList(),
    val photos: List<CapturePhoto> = emptyList(),
    val importErrors: List<String> = emptyList(),
    val sealed: Boolean = false,
    val maintenanceWarning: String = ""
) {
    val hasContent: Boolean get() = text.isNotBlank() || photos.isNotEmpty()
}

data class CaptureCue(
    val id: String = UUID.randomUUID().toString(),
    val value: String,
    val type: String = "KEYWORD",
    val source: String = "USER_WRITTEN"
)

data class CapturePhoto(
    val id: String,
    val memoryId: String,
    val originalPath: String?,
    val previewPath: String?,
    val mimeType: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val originalBytes: Long = 0,
    val cueAllowed: Boolean = false,
    val available: Boolean = true
)

data class SealReceipt(val memoryId: String, val alreadySealed: Boolean)

/** Pre-agreed public seams from item12 R4; no backend or AI dependency. */
interface CaptureGateway {
    suspend fun latestDraft(): CaptureDraft?
    suspend fun loadDraft(id: String): CaptureDraft?
    suspend fun saveDraft(draft: CaptureDraft): CaptureDraft
    suspend fun importPhoto(memoryId: String, uri: Uri): CapturePhoto
    suspend fun seal(draft: CaptureDraft): SealReceipt
    suspend fun preview(photo: CapturePhoto): Bitmap
}

internal fun MediaAssetEntity.toCapturePhoto() = CapturePhoto(
    id = id, memoryId = memoryId, originalPath = originalPath, previewPath = previewPath,
    mimeType = mimeType, width = width, height = height, originalBytes = originalBytes,
    cueAllowed = cueAllowed, available = mediaState == "READY"
)
