package com.unfinished.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    // Includes deleted rows: surviving ciphertext must never be stranded by a replacement key.
    @Query("SELECT EXISTS(SELECT 1 FROM memories LIMIT 1)")
    suspend fun hasTextCiphertext(): Boolean

    @Query("SELECT * FROM memories WHERE state != 'DELETED' ORDER BY updatedAt DESC")
    fun observeMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM resurface_events ORDER BY scheduledAt, id")
    fun observeEvents(): Flow<List<ResurfaceEventEntity>>

    @Query("SELECT * FROM recall_sessions ORDER BY startedAt, id")
    fun observeSessions(): Flow<List<RecallSessionEntity>>

    @Query("SELECT * FROM exposures ORDER BY observedAt")
    fun observeExposures(): Flow<List<ExposureEntity>>

    @Query("SELECT * FROM vault_settings WHERE scopeId = :scopeId")
    fun observeSettings(scopeId: String): Flow<VaultSettingsEntity?>

    @Query("SELECT * FROM resurface_events ORDER BY scheduledAt, id")
    suspend fun allEvents(): List<ResurfaceEventEntity>

    @Query("SELECT * FROM resurface_events WHERE activeScope = :scopeId LIMIT 1")
    suspend fun activeEvent(scopeId: String): ResurfaceEventEntity?

    @Query("SELECT * FROM recall_sessions WHERE resurfaceEventId = :eventId ORDER BY startedAt, id LIMIT 1")
    suspend fun eventSession(eventId: String): RecallSessionEntity?

    @Query("SELECT * FROM daily_decisions WHERE scopeId = :scopeId AND localDateKey = :date LIMIT 1")
    suspend fun daily(scopeId: String, date: String): DailyDecisionEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertDaily(decision: DailyDecisionEntity)

    @androidx.room.Update
    suspend fun updateDaily(decision: DailyDecisionEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(event: ResurfaceEventEntity)

    @Query("SELECT * FROM exposures WHERE actionId = :actionId AND kind = :kind AND contentScope = :contentScope")
    suspend fun exposure(actionId: String, kind: String, contentScope: String): ExposureEntity?

    @Query("SELECT * FROM exposures WHERE sessionId = :sessionId ORDER BY observedAt, contentScope")
    suspend fun sessionExposures(sessionId: String): List<ExposureEntity>

    @Query("SELECT * FROM exposures WHERE memoryId = :memoryId")
    suspend fun memoryExposures(memoryId: String): List<ExposureEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExposure(exposure: ExposureEntity)

    @Query("SELECT * FROM recall_snapshots WHERE actionId = :actionId")
    suspend fun snapshot(actionId: String): RecallSnapshotEntity?

    @Query("SELECT * FROM recall_snapshots WHERE sessionId = :sessionId ORDER BY createdAt, actionId")
    suspend fun snapshots(sessionId: String): List<RecallSnapshotEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSnapshot(snapshot: RecallSnapshotEntity)

    @Query("SELECT * FROM vault_settings WHERE scopeId = :scopeId")
    suspend fun settings(scopeId: String): VaultSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: VaultSettingsEntity)

    @Query("SELECT * FROM memory_cues WHERE memoryId = :memoryId AND confirmed = 1 AND authorizationSource = 'USER_EXPLICIT' AND type != 'PHOTO' ORDER BY orderIndex")
    fun observeCues(memoryId: String): Flow<List<CueEntity>>

    @Query("SELECT * FROM memories WHERE state != 'DELETED' ORDER BY updatedAt DESC")
    suspend fun listMemories(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE id = :id LIMIT 1")
    suspend fun findMemory(id: String): MemoryEntity?

    @Query("SELECT * FROM memories WHERE state = 'SEALED' AND state != 'DELETED' ORDER BY updatedAt DESC")
    suspend fun listSealed(): List<MemoryEntity>

    @Query("SELECT * FROM memories WHERE state = 'DRAFT' ORDER BY updatedAt DESC")
    suspend fun listDrafts(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMemory(memory: MemoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOriginal(original: MemoryOriginalEntity)

    @Query("SELECT * FROM memory_originals WHERE memoryId = :memoryId LIMIT 1")
    suspend fun findOriginal(memoryId: String): MemoryOriginalEntity?

    @Query("UPDATE memory_originals SET sealedAt = :sealedAt WHERE memoryId = :memoryId")
    suspend fun markOriginalSealed(memoryId: String, sealedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPolicy(policy: ResurfacePolicyEntity)

    @Query("SELECT * FROM resurface_policies WHERE memoryId = :memoryId LIMIT 1")
    suspend fun findPolicy(memoryId: String): ResurfacePolicyEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMediaAsset(asset: MediaAssetEntity)

    @Query("SELECT * FROM media_assets WHERE memoryId = :memoryId AND deletedAt IS NULL ORDER BY orderIndex, id")
    suspend fun listMediaAssets(memoryId: String): List<MediaAssetEntity>

    @Query("SELECT * FROM media_assets WHERE deletedAt IS NULL")
    suspend fun referencedMediaAssets(): List<MediaAssetEntity>

    @Query("SELECT * FROM capture_drafts WHERE memoryId = :memoryId")
    suspend fun findCaptureDraft(memoryId: String): CaptureDraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCaptureDraft(snapshot: CaptureDraftEntity)

    @Query("UPDATE memories SET state = 'DELETED', updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteMemory(id: String, updatedAt: Long)

    @Query("UPDATE memories SET state = :state, sealedAt = :sealedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateState(id: String, state: String, sealedAt: Long?, updatedAt: Long)

    @Query("UPDATE memories SET lastResurfacedAt = :lastSeen, resurfaceCount = resurfaceCount + 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markResurfaced(id: String, lastSeen: Long, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCue(cue: CueEntity)

    @Query("SELECT * FROM memory_cues WHERE memoryId = :memoryId AND confirmed = 1 AND authorizationSource = 'USER_EXPLICIT' ORDER BY orderIndex")
    suspend fun listCues(memoryId: String): List<CueEntity>

    @Query("DELETE FROM memory_cues WHERE memoryId = :memoryId")
    suspend fun deleteCues(memoryId: String)

    @androidx.room.Upsert
    suspend fun upsertResurfaceEvent(event: ResurfaceEventEntity)

    @Query("SELECT * FROM resurface_events WHERE id = :id LIMIT 1")
    suspend fun findResurfaceEvent(id: String): ResurfaceEventEntity?

    @Query("SELECT * FROM resurface_events WHERE memoryId = :memoryId ORDER BY scheduledAt DESC LIMIT 1")
    suspend fun latestEvent(memoryId: String): ResurfaceEventEntity?

    @Query("UPDATE resurface_events SET state = :state, openedAt = :openedAt, deferredUntil = :deferredUntil, completedAt = :completedAt WHERE id = :id")
    suspend fun updateEvent(id: String, state: String, openedAt: Long?, deferredUntil: Long?, completedAt: Long?)

    @androidx.room.Upsert
    suspend fun upsertRecallSession(session: RecallSessionEntity)

    @Query("SELECT * FROM recall_sessions WHERE id = :id LIMIT 1")
    suspend fun findRecallSession(id: String): RecallSessionEntity?

    @Query("SELECT * FROM recall_sessions WHERE memoryId = :memoryId ORDER BY startedAt DESC")
    suspend fun listRecallSessions(memoryId: String): List<RecallSessionEntity>

    @Query("UPDATE recall_sessions SET state = :state, clarity = :clarity, recallContent = :recallContent, answeredAt = :answeredAt, revealedAt = :revealedAt, completedAt = :completedAt WHERE id = :id")
    suspend fun updateRecallSession(id: String, state: String, clarity: String?, recallContent: String?, answeredAt: Long?, revealedAt: Long?, completedAt: Long?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReflection(reflection: ReflectionEntity)

    @Query("SELECT * FROM reflections WHERE memoryId = :memoryId ORDER BY createdAt DESC")
    suspend fun listReflections(memoryId: String): List<ReflectionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncOperation(operation: SyncOperationEntity)

    @Query("SELECT * FROM sync_operations WHERE state = 'PENDING' OR (state = 'FAILED' AND attempts < 5) ORDER BY createdAt")
    suspend fun pendingSyncOperations(): List<SyncOperationEntity>

    @Query("UPDATE sync_operations SET state = :state, attempts = attempts + 1, lastAttemptAt = :attemptedAt WHERE id = :id")
    suspend fun updateSyncState(id: String, state: String, attemptedAt: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOutboxIfAbsent(operation: SyncOutboxEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOutbox(operation: SyncOutboxEntity)

    @Query("SELECT * FROM sync_outbox WHERE state = 'PENDING' OR (state = 'FAILED' AND attempts < 5) ORDER BY createdAt, operationId LIMIT :limit")
    suspend fun pendingOutbox(limit: Int): List<SyncOutboxEntity>

    @Query("SELECT * FROM sync_outbox WHERE operationId = :operationId")
    suspend fun findOutbox(operationId: String): SyncOutboxEntity?

    @Query("UPDATE sync_outbox SET state = :state, attempts = attempts + 1, lastAttemptAt = :attemptedAt WHERE operationId = :operationId AND state != 'SYNCED'")
    suspend fun updateOutboxState(operationId: String, state: String, attemptedAt: Long)

    @Query("SELECT * FROM sync_mirror WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun findSyncMirror(entityType: String, entityId: String): SyncMirrorEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncMirror(record: SyncMirrorEntity)

    @Query("SELECT cursor FROM sync_state WHERE \"key\" = :key")
    suspend fun syncCursor(key: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncState(state: SyncStateEntity)
}
