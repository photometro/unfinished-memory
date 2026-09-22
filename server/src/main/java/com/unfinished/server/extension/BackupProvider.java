package com.unfinished.server.extension;

/** Storage abstraction for encrypted snapshots. */
public interface BackupProvider {
    String put(String vaultId, String ciphertext, String contentHash);
}
