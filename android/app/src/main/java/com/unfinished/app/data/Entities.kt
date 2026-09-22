package com.unfinished.app.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val updatedAt: Long
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
    val notificationEnabled: Boolean
)

@Entity(tableName = "memory_cues", indices = [Index("memoryId")])
data class CueEntity(
    @PrimaryKey val id: String,
    val memoryId: String,
    val type: String,
    val value: String,
    val source: String,
    val confirmed: Boolean,
    val orderIndex: Int
)

@Entity(tableName = "resurface_events", indices = [Index("memoryId"), Index("state")])
data class ResurfaceEventEntity(
    @PrimaryKey val id: String,
    val memoryId: String,
    val state: String,
    val scheduledAt: Long,
    val openedAt: Long?,
    val deferredUntil: Long?,
    val completedAt: Long?
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
    val completedAt: Long?
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
    val deletedAt: Long?
)

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
