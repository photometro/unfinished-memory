package com.unfinished.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "memories", indices = [Index("state"), Index("updatedAt")])
data class MemoryEntity(
    @PrimaryKey val id: String,
    val title: String,
    val encryptedOriginal: String,
    val occurredAt: Long,
    val state: String,
    val policy: String,
    val sealedAt: Long?,
    val lastResurfacedAt: Long?,
    val resurfaceCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val userTitle: String? = null,
    @ColumnInfo(defaultValue = "'LEGACY_UNKNOWN'") val titleSource: String = "USER_OR_NONE",
    @ColumnInfo(defaultValue = "'LEGACY_UNKNOWN'") val exposureHistory: String = "KNOWN",
    val lastOriginalExposureAt: Long? = null,
    val cooldownUntil: Long? = null,
    val deferUntil: Long? = null
)

@Entity(tableName = "memory_originals")
data class MemoryOriginalEntity(
    @PrimaryKey val memoryId: String,
    val encryptedContent: String,
    val contentHash: String,
    val sealedAt: Long?
)

@Entity(tableName = "resurface_policies")
data class ResurfacePolicyEntity(
    @PrimaryKey val memoryId: String,
    val mode: String,
    val minDormantDays: Int,
    val fixedDate: Long?,
    val notificationEnabled: Boolean,
    @ColumnInfo(defaultValue = "1") val randomAllowed: Boolean = true,
    val planId: String? = null,
    @ColumnInfo(defaultValue = "'LEGACY_UNKNOWN'") val dateSource: String = "USER_OR_NONE",
    val planCompletedAt: Long? = null,
    @ColumnInfo(defaultValue = "0") val allowSerendipityAfterFixedDate: Boolean = false
)

@Entity(tableName = "memory_cues", indices = [Index("memoryId")])
data class CueEntity(
    @PrimaryKey val id: String,
    val memoryId: String,
    val type: String,
    val value: String,
    val source: String,
    val confirmed: Boolean,
    val orderIndex: Int,
    @ColumnInfo(defaultValue = "'LEGACY_UNKNOWN'") val authorizationSource: String = "USER_EXPLICIT"
)

@Entity(tableName = "resurface_events", indices = [Index("memoryId"), Index("state"), Index(value = ["activeScope"], unique = true), Index(value = ["fixedPlanId"], unique = true)])
data class ResurfaceEventEntity(
    @PrimaryKey val id: String,
    val memoryId: String,
    val state: String,
    val scheduledAt: Long,
    val openedAt: Long?,
    val deferredUntil: Long?,
    val completedAt: Long?,
    @ColumnInfo(defaultValue = "'LEGACY_UNKNOWN'") val source: String = "LEGACY_UNKNOWN",
    @ColumnInfo(defaultValue = "0") val createdAt: Long = scheduledAt,
    val dueAt: Long? = null,
    val fixedPlanId: String? = null,
    val activeScope: String? = null,
    val localDateKey: String? = null,
    val zoneId: String? = null,
    val legacyState: String? = null
)

@Entity(tableName = "recall_sessions", indices = [Index("memoryId"), Index("state")])
data class RecallSessionEntity(
    @PrimaryKey val id: String,
    val memoryId: String,
    val resurfaceEventId: String?,
    val state: String,
    val clarity: String?,
    val recallContent: String?,
    val startedAt: Long,
    val answeredAt: Long?,
    val revealedAt: Long?,
    val completedAt: Long?,
    @ColumnInfo(defaultValue = "'LEGACY_UNKNOWN'") val source: String = "LEGACY_UNKNOWN",
    @ColumnInfo(defaultValue = "'LEGACY_UNKNOWN'") val recallMode: String = "LEGACY_UNKNOWN",
    @ColumnInfo(defaultValue = "0") val cueEngineStopped: Boolean = false,
    @ColumnInfo(defaultValue = "0") val snapshotLocked: Boolean = false,
    val revealActionId: String? = null,
    val recallOutcome: String? = null,
    val legacyState: String? = null,
    @ColumnInfo(defaultValue = "'LEGACY_UNKNOWN'") val inputRecallMode: String = "LEGACY_UNKNOWN"
)

@Entity(tableName = "reflections", indices = [Index("memoryId")])
data class ReflectionEntity(
    @PrimaryKey val id: String,
    val memoryId: String,
    val recallSessionId: String,
    val content: String,
    val createdAt: Long
)

@Entity(tableName = "media_assets", indices = [Index("memoryId")])
data class MediaAssetEntity(
    @PrimaryKey val id: String,
    val memoryId: String,
    val localUri: String?,
    val remoteObjectId: String?,
    val sha256: String,
    val uploadState: String,
    val deletedAt: Long?,
    @ColumnInfo(defaultValue = "0") val cueAllowed: Boolean = false,
    val originalPath: String? = null,
    val previewPath: String? = null,
    @ColumnInfo(defaultValue = "''") val mimeType: String = "",
    @ColumnInfo(defaultValue = "0") val width: Int = 0,
    @ColumnInfo(defaultValue = "0") val height: Int = 0,
    @ColumnInfo(defaultValue = "0") val originalBytes: Long = 0,
    @ColumnInfo(defaultValue = "''") val previewCipherHash: String = "",
    @ColumnInfo(defaultValue = "0") val orderIndex: Int = 0,
    @ColumnInfo(defaultValue = "'LEGACY'") val mediaState: String = "LEGACY"
)

/** Encrypted editable metadata; finalized together with the immutable Original. */
@Entity(tableName = "capture_drafts")
data class CaptureDraftEntity(
    @PrimaryKey val memoryId: String,
    val encryptedSnapshot: String,
    val updatedAt: Long
)

/** One row owns both the once-daily auto decision and the independent shared quota. */
@Entity(tableName = "daily_decisions", primaryKeys = ["scopeId", "localDateKey"])
data class DailyDecisionEntity(
    val scopeId: String, val localDateKey: String, val zoneId: String,
    val evaluatedAt: Long?, val autoResult: String?, val chosenMemoryId: String?,
    val quotaEventId: String?, val quotaConsumedAt: Long?, val ruleVersion: String
)

@Entity(tableName = "exposures", primaryKeys = ["actionId", "kind", "contentScope"], indices = [Index("memoryId"), Index("sessionId")])
data class ExposureEntity(
    val actionId: String, val kind: String, val contentScope: String,
    val memoryId: String, val sessionId: String?, val source: String,
    val observedAt: Long, val localDateKey: String, val zoneId: String
)

@Entity(tableName = "recall_snapshots", indices = [Index("sessionId")])
data class RecallSnapshotEntity(
    @PrimaryKey val actionId: String, val sessionId: String, val encryptedContent: String,
    val recallMode: String, val shownCueIds: String, val createdAt: Long
)

@Entity(tableName = "vault_settings")
data class VaultSettingsEntity(@PrimaryKey val scopeId: String, val randomEnabled: Boolean)

@Entity(tableName = "sync_operations", indices = [Index("state")])
data class SyncOperationEntity(
    @PrimaryKey val id: String,
    val entityType: String,
    val entityId: String,
    val operationType: String,
    val payloadHash: String,
    val state: String,
    val attempts: Int,
    val createdAt: Long,
    val lastAttemptAt: Long?
)

/** Immutable, encrypted payloads that can safely be retried with the same operation ID. */
@Entity(tableName = "sync_outbox", indices = [Index("state"), Index(value = ["entityType", "entityId"])])
data class SyncOutboxEntity(
    @PrimaryKey val operationId: String,
    val recordId: String,
    val entityType: String,
    val entityId: String,
    val version: Long,
    val ciphertext: String,
    val contentHash: String,
    val deletedAt: Long?,
    val state: String,
    val attempts: Int,
    val createdAt: Long,
    val lastAttemptAt: Long?
)

/** Server ciphertext mirror; domain decryption/merge remains a separate client operation. */
@Entity(tableName = "sync_mirror", primaryKeys = ["entityType", "entityId"])
data class SyncMirrorEntity(
    val recordId: String,
    val entityType: String,
    val entityId: String,
    val version: Long,
    val ciphertext: String,
    val contentHash: String,
    val deletedAt: Long?,
    val changeSequence: Long
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(@PrimaryKey val key: String, val cursor: Long)
