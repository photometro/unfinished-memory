package com.unfinished.server.extension;

/** Future migration from anonymous vault to an account. */
public interface AccountBindingGateway {
    String createBindingChallenge(String vaultId);
}
