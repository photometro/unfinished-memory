package com.unfinished.server.extension;

import java.util.List;

/** Feature registry used to keep disabled future settings out of V1 UI. */
public interface SettingsFeatureRegistry {
    List<String> enabledFeatures();
}
