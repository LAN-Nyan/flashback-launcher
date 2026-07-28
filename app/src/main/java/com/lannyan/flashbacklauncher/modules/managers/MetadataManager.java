package com.lannyan.flashbacklauncher.modules.managers;

import java.util.List;
import java.io.File;

import com.lannyan.flashbacklauncher.modules.server.GameEntry;
import com.lannyan.flashbacklauncher.modules.server.ServerConfig;
import com.lannyan.flashbacklauncher.modules.providers.MetadataProvider;
import com.lannyan.flashbacklauncher.modules.providers.SteamGridDbProvider;
import com.lannyan.flashbacklauncher.modules.providers.GameTdbProvider;
import com.lannyan.flashbacklauncher.modules.providers.TheGamesDbProvider;

public class MetadataManager {
    public static void updateMetadata(List<GameEntry> games, ServerConfig config) {
        for (GameEntry game : games) {
            System.out.println("Checking metadata for: " + game.commonName);
            MetadataProvider provider = getProviderFor(game.preferredMetadataProvider, config);
            provider.fetchMetadata(game);

            if (provider instanceof SteamGridDbProvider sgdb) {
                File gameDataDir = FileManager.getGameDataDir(game, config);
                sgdb.fetchArt(game, gameDataDir);
            }
        }
        System.out.println("Metadata is up to date.");
    }

    private static MetadataProvider getProviderFor(String providerName, ServerConfig config) {
        String key = config.apiKeys.getOrDefault(providerName, "");
        switch (providerName) {
            case "THEGAMESDB": return new TheGamesDbProvider(key);
            case "STEAMGRIDDB": return new SteamGridDbProvider(key);
            case "GAMETDB": return new GameTdbProvider();
            default:
                System.out.println("Unknown provider: " + providerName + ", skipping.");
                return new GameTdbProvider();

        }
    }
}
