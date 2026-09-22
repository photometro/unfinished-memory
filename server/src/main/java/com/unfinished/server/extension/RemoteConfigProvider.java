package com.unfinished.server.extension;

/** Future remote feature/configuration boundary. */
public interface RemoteConfigProvider {
    String get(String key);
}
