package com.unfinished.app.sync

import com.unfinished.app.data.SyncMirrorStore
import kotlinx.coroutines.CancellationException

/** Pushes a captured immutable batch and persists its exact per-operation outcome. */
class OutboxSyncCoordinator(
    private val store: SyncMirrorStore,
    private val gateway: SyncGateway
) {
    suspend fun flush(limit: Int = 100): Long {
        val captured = store.pending(limit)
        if (captured.isEmpty()) return 0L
        val ids = captured.mapTo(linkedSetOf()) { it.operationId }
        val records = captured.map {
            EncryptedSyncRecord(
                recordId = it.recordId,
                operationId = it.operationId,
                entityType = it.entityType,
                entityId = it.entityId,
                version = it.version,
                ciphertext = it.ciphertext,
                contentHash = it.contentHash,
                deletedAt = it.deletedAt
            )
        }
        return try {
            val response = gateway.push(records)
            val outcomes = response.acknowledgements.associate { it.operationId to it.outcome }
            store.applyAcknowledgements(ids, outcomes)
            response.cursor
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            store.markBatchFailed(ids)
            throw error
        }
    }

    suspend fun pullAndApply(limit: Int = 200): PullBatch {
        val batch = gateway.pull(store.cursor(), limit)
        store.applyPage(batch.records, batch.nextCursor)
        return batch
    }
}
