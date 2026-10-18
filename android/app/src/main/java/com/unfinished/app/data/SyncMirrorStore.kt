package com.unfinished.app.data

import androidx.room.withTransaction
import com.unfinished.app.sync.EncryptedSyncRecord
import java.security.MessageDigest
import java.util.UUID

/** Room boundary for an immutable retryable sync outbox and an encrypted inbound mirror. */
class SyncMirrorStore(
    private val database: UnfinishedDatabase,
    private val now: () -> Long = System::currentTimeMillis
) {
    private val dao = database.memoryDao()

    suspend fun enqueue(
        recordId: String,
        entityType: String,
        entityId: String,
        version: Long,
        ciphertext: String,
        deletedAt: Long? = null,
        operationId: String = UUID.randomUUID().toString()
    ): String {
        require(version > 0)
        require(entityType.isNotBlank() && entityId.isNotBlank() && ciphertext.isNotBlank())
        UUID.fromString(recordId)
        UUID.fromString(operationId)
        val hash = sha256(ciphertext)
        database.withTransaction {
            val inserted = dao.insertOutboxIfAbsent(
                SyncOutboxEntity(
                    operationId = operationId,
                    recordId = recordId,
                    entityType = entityType,
                    entityId = entityId,
                    version = version,
                    ciphertext = ciphertext,
                    contentHash = hash,
                    deletedAt = deletedAt,
                    state = "PENDING",
                    attempts = 0,
                    createdAt = now(),
                    lastAttemptAt = null
                )
            )
            if (inserted == -1L) {
                val existing = dao.findOutbox(operationId)
                require(existing != null && existing.recordId == recordId && existing.entityType == entityType &&
                    existing.entityId == entityId && existing.version == version && existing.ciphertext == ciphertext &&
                    existing.deletedAt == deletedAt) { "operationId already belongs to a different payload" }
            }
        }
        return operationId
    }

    suspend fun pending(limit: Int = 100): List<SyncOutboxEntity> {
        require(limit in 1..500)
        return dao.pendingOutbox(limit)
    }

    /** Only IDs captured in this batch can be acknowledged or marked failed. */
    suspend fun applyAcknowledgements(batchIds: Set<String>, outcomes: Map<String, String>) {
        require(outcomes.keys.all { it in batchIds }) { "server acknowledged an operation outside the sent batch" }
        database.withTransaction {
            val at = now()
            batchIds.forEach { id ->
                val status = outcomes[id]
                val state = when (status) {
                    "APPLIED", "DUPLICATE", "STALE" -> "SYNCED"
                    else -> "FAILED"
                }
                dao.updateOutboxState(id, state, at)
            }
        }
    }

    suspend fun markBatchFailed(batchIds: Set<String>) {
        database.withTransaction {
            val at = now()
            batchIds.forEach { dao.updateOutboxState(it, "FAILED", at) }
        }
    }

    /** Apply a complete pull page and its cursor atomically; conflicting equal versions abort the page. */
    suspend fun applyPage(records: List<EncryptedSyncRecord>, nextCursor: Long) {
        require(nextCursor >= 0)
        database.withTransaction {
            val oldCursor = dao.syncCursor(CURSOR_KEY) ?: 0L
            require(nextCursor >= oldCursor) { "sync cursor cannot move backwards" }
            records.forEach { incoming ->
                require(incoming.version > 0)
                require(incoming.contentHash == sha256(incoming.ciphertext)) { "ciphertext hash mismatch" }
                val current = dao.findSyncMirror(incoming.entityType, incoming.entityId)
                when {
                    current == null || incoming.version > current.version -> dao.upsertSyncMirror(incoming.toMirror())
                    incoming.version < current.version -> Unit
                    current.samePayload(incoming) -> Unit
                    else -> error("conflicting sync payload at equal entity version")
                }
            }
            dao.upsertSyncState(SyncStateEntity(CURSOR_KEY, nextCursor))
        }
    }

    suspend fun cursor(): Long = dao.syncCursor(CURSOR_KEY) ?: 0L

    suspend fun mirror(entityType: String, entityId: String): SyncMirrorEntity? =
        dao.findSyncMirror(entityType, entityId)

    private fun EncryptedSyncRecord.toMirror() = SyncMirrorEntity(
        recordId, entityType, entityId, version, ciphertext, contentHash, deletedAt, changeSequence
    )

    private fun SyncMirrorEntity.samePayload(record: EncryptedSyncRecord) =
        recordId == record.recordId && ciphertext == record.ciphertext && contentHash == record.contentHash &&
            deletedAt == record.deletedAt

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }

    companion object { private const val CURSOR_KEY = "remote-vault" }
}
