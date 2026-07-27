package com.lannyan.flashbacklauncher;
import java.io.File;
import java.util.Scanner;
import com.google.gson.Gson;
import java.io.FileReader;
import java.io.IOException;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;

public class ServerCommands {

    // handle if error occured.
    static boolean hasError = false;
    static String errorMessage = "";

    public static void startServer() {
        System.out.println("Starting Server...");
        loadStartupSettings();
        ServerConfig config = loadOptions();
        if (config == null) {
            System.out.println("No config found, creating default...");
            saveDefaultConfig();
            config = loadOptions(); // reload after creating it
        }
        loadFiles(config);
        loadMeta();
        loadCreds();
        if (hasError) {
            System.out.println("An error has occured during startup: " + errorMessage);
        }
    }
        // Load file list, and different consoles/types (eg. EXE vs. ISO, or .gba/.bin)
    public static void loadFiles(ServerConfig config) {
        File gamesRoot = new File(config.gamesDirectory);
        if (!gamesRoot.exists()) {
              gamesRoot.mkdirs();
              System.out.println("Created games directory at " + gamesRoot.getAbsolutePath());
             return; // nothing to scan yet, first run
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
         System.out.println("Files loaded successfully!");
        }

        // Load gamelist and metadata
    public static void loadMeta() {
        System.out.println("Game metadata loaded successfully!");
    }
        // Load the https/credentials
    public static void loadCreds() {
        System.out.println("HTTPS credentials loaded successfully!");
    }
        // Load config.json (i think that'll work in Java)
    public static ServerConfig loadOptions() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader("config.json")) {
            ServerConfig config = gson.fromJson(reader, ServerConfig.class);
            System.out.println("config.json loaded successfully!");
            return config;
        } catch (IOException e) {
            System.out.println("Could not read config.json: " + e.getMessage());
            return null;
        }
    }
        // Handle custom startup options (eg. RAM allocation, IPv6, etc.)
    public static void loadStartupSettings() {
        System.out.println("Startup flags loaded successfully!");
    }

    public static void errorHandler() {

    }

    public static void saveDefaultConfig() {
        ServerConfig config = new ServerConfig();
        config.gamesDirectory = "games";
        config.port = 8080;
        config.useHttps = false;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter("config.json")) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            System.out.println("Could not write config.json: " + e.getMessage());
        }
    }
        // do i really need to describe this???
    public static void shutdownServer() {
        return; // just for now, illl setup commands to safely unload everuything later
    }


}
