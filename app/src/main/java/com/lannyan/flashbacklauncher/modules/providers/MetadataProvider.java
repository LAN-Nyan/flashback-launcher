package com.lannyan.flashbacklauncher.modules.providers;

import com.lannyan.flashbacklauncher.modules.server.GameEntry;

public interface MetadataProvider {
    void fetchMetadata(GameEntry game);
}
