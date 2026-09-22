package com.unfinished.app.sync

interface BackupProvider {
    suspend fun createSnapshot(ciphertext: String, contentHash: String): String
    suspend fun restoreSnapshot(snapshotId: String): String
}
