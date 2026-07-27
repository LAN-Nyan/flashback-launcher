package com.lannyan.flashbacklauncher.modules.providers;

import com.lannyan.flashbacklauncher.modules.server.GameEntry;

public class GameTdbProvider implements MetadataProvider {
    @Override
    public void fetchMetadata(GameEntry game) {
        System.out.println("GameTDB uses bulk XML downloads, not per-game lookups — not implemented yet.");
    }
}
