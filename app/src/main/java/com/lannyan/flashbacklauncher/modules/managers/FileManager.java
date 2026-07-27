package com.lannyan.flashbacklauncher.modules.managers;

import java.io.File;
import com.lannyan.flashbacklauncher.modules.server.ServerConfig;

public class FileManager {
    public static void fetchFileList(ServerConfig config) {
        File gamesRoot = new File(config.gamesDirectory);
        if (!gamesRoot.exists()) {
            gamesRoot.mkdirs();
            System.out.println("Created games directory at " + gamesRoot.getAbsolutePath());
            return;
        }

        File[] platformDirs = gamesRoot.listFiles();
        if (platformDirs == null) {
            System.out.println("Could not read games directory.");
            return;
        }

        for (File platformDir : platformDirs) {
            if (platformDir.isDirectory()) {
                System.out.println("Scanning platform folder: " + platformDir.getName());
                File[] games = platformDir.listFiles();
                if (games == null) continue;
                for (File game : games) {
                    System.out.println("  Found: " + game.getName());
                }
            }
        }
    }
}
