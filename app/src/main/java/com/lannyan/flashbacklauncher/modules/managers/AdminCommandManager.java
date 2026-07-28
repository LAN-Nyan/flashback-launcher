package com.lannyan.flashbacklauncher.modules.managers;

/*
I know for a fact i'll regret NOT saying this so,
this is the handler for running commands executed trhough the web UI for the ADMIN
panel, i know for a fact some idiot is going to think this is for the desktop launcher.
NO IT IS NOT.
*/

import com.lannyan.flashbacklauncher.modules.server.GameEntry;
import com.lannyan.flashbacklauncher.modules.server.ServerCommands;
import com.lannyan.flashbacklauncher.modules.server.ServerConfig;
import com.lannyan.flashbacklauncher.modules.providers.SteamGridDbProvider;
import com.lannyan.flashbacklauncher.modules.providers.SteamGridDbGridResult;

import java.util.List;
import java.util.Map;

/**
 * Executes commands issued from the browser-based admin panel
 * (rescan library, kick a connected client, push a theme, list art, set cover art).
 *
 * This is NOT the command path used by the desktop/client launcher
 * itself - client socket lifecycle (connect/auth/stream) is handled
 * directly in NetworkManager. Anything routed through here originated
 * from an admin HTTP request, not from a game client.
 *
 * Everything here is static, matching the rest of the managers/
 * package (FileManager, MetadataManager, ServerCommands) - there's
 * no instance state to justify constructing this.
 */
public class AdminCommandManager {

    public static AdminCommandResult execute(AdminCommand command, ServerConfig config, Map<String, String> params) {
        if (command == null) {
            return AdminCommandResult.rejected("Null command supplied.");
        }

        switch (command) {
            case RESCAN_LIBRARY:
                return rescanLibrary(config);
            case KICK_CLIENT:
                return kickClient(params != null ? params.get("clientId") : null);
            case PUSH_THEME:
                return pushClientTheme(
                    params != null ? params.get("cssUrl") : null,
                    params != null ? params.get("inlineCss") : null,
                    params != null && Boolean.parseBoolean(params.get("force"))
                );
                case LIST_ART_CANDIDATES:
                    return listArtCandidates(
                        params != null ? params.get("gameTitle") : null,
                        params != null ? params.get("console") : null,
                        config
                    );
            case SET_COVER_ART:
                return setCoverArt(
                    params != null ? params.get("gameLocation") : null,
                    params != null ? params.get("artUrl") : null,
                    config
                );
            default:
                return AdminCommandResult.rejected("Unrecognized admin command: " + command);
        }
    }

    private static AdminCommandResult rescanLibrary(ServerConfig config) {
        List<GameEntry> games = ServerCommands.loadGamesList();
        games = FileManager.fetchFileList(config, games);

        MetadataManager.updateMetadata(games, config);
        ServerCommands.saveGamesList(games);

        return AdminCommandResult.ok("Rescanned library: " + games.size() + " entries indexed.");
    }

    private static AdminCommandResult kickClient(String clientId) {
        if (clientId == null || clientId.isBlank()) {
            return AdminCommandResult.rejected("kickClient requires a clientId.");
        }
        NetworkManager.disConnectClient(clientId);
        return AdminCommandResult.ok("Disconnected client: " + clientId);
    }

    private static AdminCommandResult pushClientTheme(String cssUrl, String inlineCss, boolean force) {
        System.out.println("Theme push requested. cssUrl=" + cssUrl + " force=" + force);
        return AdminCommandResult.ok("Theme push acknowledged (not yet distributed to clients).");
    }

    private static AdminCommandResult listArtCandidates(String gameTitle, String consoleCode, ServerConfig config) {
        if (gameTitle == null || gameTitle.isBlank()) {
            return AdminCommandResult.rejected("listArtCandidates requires a gameTitle.");
        }

        List<GameEntry> games = ServerCommands.loadGamesList();
        GameEntry match = games.stream()
                .filter(g -> gameTitle.equals(g.commonName) && (consoleCode == null || consoleCode.equals(g.console)))
                .findFirst()
                .orElse(null);

        if (match == null) {
            return AdminCommandResult.rejected("Game not found: " + gameTitle);
        }

        String key = config.apiKeys.getOrDefault("STEAMGRIDDB", "");
        SteamGridDbProvider provider = new SteamGridDbProvider(key);
        SteamGridDbGridResult result = provider.searchGridImages(match);

        if (result == null || !result.success || result.data.isEmpty()) {
            return AdminCommandResult.rejected("No art candidates found for " + gameTitle);
        }

        List<String> candidateUrls = result.data.stream().map(img -> img.url).toList();
        return AdminCommandResult.ok("Found " + candidateUrls.size() + " candidates for " + gameTitle, candidateUrls);
    }

    private static AdminCommandResult setCoverArt(String gameLocation, String artUrl, ServerConfig config) {
        if (gameLocation == null || artUrl == null || artUrl.isBlank()) {
            return AdminCommandResult.rejected("setCoverArt requires gameLocation and artUrl.");
        }

        List<GameEntry> games = ServerCommands.loadGamesList();
        boolean found = false;

        for (GameEntry game : games) {
            if (gameLocation.equals(game.fileLocation)) {
                game.coverArt = artUrl;
                found = true;
                break;
            }
        }

        if (found) {
            ServerCommands.saveGamesList(games);
            return AdminCommandResult.ok("Updated cover art for " + gameLocation);
        }

        return AdminCommandResult.rejected("Game not found with location: " + gameLocation);
    }

    /**
     * Result wrapper so the admin panel gets a clear
     * ok/rejected + message back.
     */
    public static class AdminCommandResult {
        public final boolean success;
        public final String message;
        public final List<String> candidates;

        private AdminCommandResult(boolean success, String message, List<String> candidates) {
            this.success = success;
            this.message = message;
            this.candidates = candidates;
        }

        public static AdminCommandResult ok(String message) {
            return new AdminCommandResult(true, message, null);
        }

        public static AdminCommandResult ok(String message, List<String> candidates) {
            return new AdminCommandResult(true, message, candidates);
        }

        public static AdminCommandResult rejected(String message) {
            return new AdminCommandResult(false, message, null);
        }
    }
}
