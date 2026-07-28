package com.lannyan.flashbacklauncher.modules.managers;

import com.lannyan.flashbacklauncher.modules.server.GameEntry;
import com.lannyan.flashbacklauncher.modules.server.ServerConfig;
import com.lannyan.flashbacklauncher.modules.server.AppPaths;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

public class FileManager {

    // Matches optional (id: XXX), common name, and optional [GameCode]
    // Example 1: (id: 38627) Luigi's Mansion [GLMP01] -> id: 38627, name: Luigi's Mansion, code: GLMP01
    // Example 2: Super Mario Galaxy [RM8E01]          -> id: null,  name: Super Mario Galaxy, code: RM8E01
    // Example 3: Donkey Kong Country                  -> id: null,  name: Donkey Kong Country, code: null
    private static final Pattern NAME_PATTERN =
            Pattern.compile("^(?:\\(id:\\s*(\\S+)\\)\\s*)?(.+?)(?:\\s*\\[(\\w+)\\])?$");

    /**
     * Scans config.gamesDirectory for console subfolders, finds game files,
     * and appends any not already present in existingGames (matched by fileLocation).
     * Returns the same list, mutated in place, for convenience.
     */
    public static List<GameEntry> fetchFileList(ServerConfig config, List<GameEntry> existingGames) {
        File gamesRoot = new File(config.gamesDirectory);
        if (!gamesRoot.exists()) {
            gamesRoot.mkdirs();
            System.out.println("Created games directory at " + gamesRoot.getAbsolutePath());
            return existingGames;
        }

        File[] platformDirs = gamesRoot.listFiles();
        if (platformDirs == null) {
            System.out.println("Could not read games directory.");
            return existingGames;
        }

        for (File platformDir : platformDirs) {
            if (!platformDir.isDirectory()) continue;

            String consoleCode = platformDir.getName();
            System.out.println("Scanning platform folder: " + consoleCode);

            File[] gameFiles = platformDir.listFiles();
            if (gameFiles == null) continue;

            for (File gameFile : gameFiles) {
                if (gameFile.isDirectory()) continue; // skip save-data folders etc. for now

                String path = gameFile.getAbsolutePath();
                boolean alreadyKnown = existingGames.stream()
                        .anyMatch(g -> path.equals(g.fileLocation));
                if (alreadyKnown) continue;

                GameEntry entry = parseGameFile(gameFile, consoleCode, config);
                existingGames.add(entry);
                System.out.println("  Indexed: " + entry.commonName + " (" + consoleCode + ")");
            }
        }

        return existingGames;
    }

    private static GameEntry parseGameFile(File gameFile, String consoleCode, ServerConfig config) {
        String filename = gameFile.getName();
        String nameNoExt = stripExtension(filename);

        GameEntry entry = new GameEntry();
        entry.console = consoleCode;
        entry.fileLocation = gameFile.getAbsolutePath();
        entry.fileSize = gameFile.length();
        entry.preferredMetadataProvider = config.defaultMetadataProvider;
        entry.providerIds = new HashMap<>();
        entry.emulator = EmulatorRegistry.getEmulatorFor(consoleCode);

        Matcher m = NAME_PATTERN.matcher(nameNoExt);
        if (m.find()) {
            String providerId = m.group(1); // May be null for clean filenames
            String commonName = m.group(2) != null ? m.group(2).trim() : nameNoExt;
            String code = m.group(3);       // May be null (e.g. GameCube/Wii ID like RM8E01)

            entry.commonName = commonName;
            entry.gameName = code;

            if (providerId != null) {
                entry.providerIds.put(config.defaultMetadataProvider, providerId);
            }
        } else {
            entry.commonName = nameNoExt;
        }

        return entry;
    }

    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    public static List<GameEntry> pruneMissingFiles(List<GameEntry> games) {
        List<GameEntry> stillValid = new ArrayList<>();
        for (GameEntry game : games) {
            File f = new File(game.fileLocation);
            if (f.exists()) {
                stillValid.add(game);
            } else {
                System.out.println("Removing entry for missing file: " + game.commonName + " (" + game.fileLocation + ")");
            }
        }
        return stillValid;
    }
    public static File getGameDataDir(GameEntry game, ServerConfig config) {
        String safeName = game.commonName.replaceAll("[^a-zA-Z0-9 ]", "").trim();
        File dir = new File(AppPaths.dataDir(), "game_data" + File.separator + game.console + File.separator + safeName);
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

}
