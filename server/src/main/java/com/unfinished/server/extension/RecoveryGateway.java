package com.unfinished.server.extension;

/** Future recovery-code or QR based device migration boundary. */
public interface RecoveryGateway {
    String createRecoveryCode(String vaultId);
}
