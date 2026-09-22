package com.unfinished.app.sync

import com.unfinished.app.data.MemoryRepository

/**
 * Small, retry-safe coordinator. A foreground call can flush the encrypted records;
 * a WorkManager worker can be attached later without changing the repository contract.
 */
class OutboxSyncCoordinator(
    private val repository: MemoryRepository,
    private val gateway: SyncGateway
) {
    suspend fun flush(records: List<EncryptedSyncRecord>): Long {
        if (records.isEmpty()) return 0L
        return runCatching {
            val cursor = gateway.push(records)
            repository.pendingSyncOperations().forEach { repository.markSyncOperation(it.id, "SYNCED") }
            cursor
        }.getOrElse {
            repository.pendingSyncOperations().forEach { repository.markSyncOperation(it.id, "FAILED") }
            throw it
        }
    }
}
