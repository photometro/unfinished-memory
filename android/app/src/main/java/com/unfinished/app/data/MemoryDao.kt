package com.unfinished.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MemoryDao {
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

    @Query("SELECT * FROM media_assets WHERE memoryId = :memoryId AND deletedAt IS NULL ORDER BY id")
    suspend fun listMediaAssets(memoryId: String): List<MediaAssetEntity>

    @Query("UPDATE memories SET state = 'DELETED', updatedAt = :updatedAt WHERE id = :id")
    suspend fun softDeleteMemory(id: String, updatedAt: Long)

    @Query("UPDATE memories SET state = :state, sealedAt = :sealedAt, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateState(id: String, state: String, sealedAt: Long?, updatedAt: Long)

    @Query("UPDATE memories SET lastResurfacedAt = :lastSeen, resurfaceCount = resurfaceCount + 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markResurfaced(id: String, lastSeen: Long, updatedAt: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCue(cue: CueEntity)

    @Query("SELECT * FROM memory_cues WHERE memoryId = :memoryId AND confirmed = 1 ORDER BY orderIndex")
    suspend fun listCues(memoryId: String): List<CueEntity>

    @Query("DELETE FROM memory_cues WHERE memoryId = :memoryId")
    suspend fun deleteCues(memoryId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertResurfaceEvent(event: ResurfaceEventEntity)

    @Query("SELECT * FROM resurface_events WHERE id = :id LIMIT 1")
    suspend fun findResurfaceEvent(id: String): ResurfaceEventEntity?

    @Query("SELECT * FROM resurface_events WHERE memoryId = :memoryId ORDER BY scheduledAt DESC LIMIT 1")
    suspend fun latestEvent(memoryId: String): ResurfaceEventEntity?

    @Query("UPDATE resurface_events SET state = :state, openedAt = :openedAt, deferredUntil = :deferredUntil, completedAt = :completedAt WHERE id = :id")
    suspend fun updateEvent(id: String, state: String, openedAt: Long?, deferredUntil: Long?, completedAt: Long?)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
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
}
