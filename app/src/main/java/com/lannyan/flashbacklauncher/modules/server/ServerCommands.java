package com.lannyan.flashbacklauncher.modules.server;

import com.lannyan.flashbacklauncher.modules.managers.MetadataManager;
import com.lannyan.flashbacklauncher.modules.managers.FileManager;
import com.lannyan.flashbacklauncher.modules.server.GameEntry;
import com.lannyan.flashbacklauncher.modules.server.ServerConfig;
import com.lannyan.flashbacklauncher.modules.providers.MetadataProvider;
import com.lannyan.flashbacklauncher.modules.managers.NetworkManager;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ServerCommands {

    // handle if error occured.
    static boolean hasError = false;
    static String errorMessage = "";

    public static void startServer() {
        System.out.println("Starting Server...");
        loadStartupSettings();

        ServerConfig config = loadOptions();
        if (config == null) {
            System.out.println("No config found, running first-time setup...");
            config = setupWizard();
        }

        loadFiles(config);
        loadMeta(config);

        // Pass the 'config' variable into the method call
        loadCreds(config);

        if (hasError) {
            System.out.println("An error has occured during startup: " + errorMessage);
        }
    }

    // Load file list, and different consoles/types (eg. EXE vs. ISO, or .gba/.bin)
    public static void loadFiles(ServerConfig config) {
        FileManager.fetchFileList(config);
        // this method is here for startup options later on!
        System.out.println("Files loaded successfully!");
    }

    public static void saveGamesList(List<GameEntry> games) {
        GameLibrary library = new GameLibrary();
        library.games = games;

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter("games.json")) {
            gson.toJson(library, writer);
            System.out.println("games.json updated with latest metadata.");
        } catch (IOException e) {
            System.out.println("Could not write games.json: " + e.getMessage());
        }
    }

    // Load gamelist and metadata
    public static void loadMeta(ServerConfig config) {
        List<GameEntry> games = loadGamesList();
        MetadataManager.updateMetadata(games, config);
        saveGamesList(games);
        System.out.println("Game metadata loaded successfully!");
    }

    // Load the https/credentials
    public static void loadCreds(ServerConfig config) {
        NetworkManager.fetchCreds(config);
        System.out.println("HTTPS credentials loaded successfully!");
    }

    // Load config.json
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
        config.isFirstBoot = true;
        config.gamesDirectory = "games";
        config.port = 8080;
        config.useHttps = false;
        config.defaultMetadataProvider = "THEGAMESDB";
        writeConfig(config);
    }

    // do i really need to describe this???
    public static void shutdownServer() {
        return; // just for now, i'll setup commands to safely unload everything later
    }

    public static List<GameEntry> loadGamesList() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader("games.json")) {
            GameLibrary library = gson.fromJson(reader, GameLibrary.class);
            System.out.println("Loaded " + library.games.size() + " games from games.json");
            return library.games;
        } catch (IOException e) {
            System.out.println("Could not read games.json: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // Setup wizard

    public static ServerConfig setupWizard() {
        System.out.println("Welcome to Flashback! Let's set a few things up.");
        Scanner scanner = new Scanner(System.in);

        ServerConfig config = new ServerConfig();
        config.isFirstBoot = false;
        config.apiKeys = new HashMap<>();

        config.port = askInt(scanner, "What port should the server run on? (default = 8080): ", 8080);

        String provider = ask(scanner, "What should the default metadata provider be? (default = THEGAMESDB): ");
        config.defaultMetadataProvider = provider.isEmpty() ? "THEGAMESDB" : provider;

        String gamesDir = ask(scanner, "Where should games be stored? (default = games): ");
        config.gamesDirectory = gamesDir.isEmpty() ? "games" : gamesDir;

        String tgdbKey = ask(scanner, "Enter your TheGamesDB API key (leave blank to skip): ");
        if (!tgdbKey.isEmpty()) config.apiKeys.put("THEGAMESDB", tgdbKey);

        String sgdbKey = ask(scanner, "Enter your SteamGridDB API key (leave blank to skip): ");
        if (!sgdbKey.isEmpty()) config.apiKeys.put("STEAMGRIDDB", sgdbKey);

        scanner.close();
        writeConfig(config);
        System.out.println("Setup complete! config.json saved.");
        return config;
    }

    // Small helper ask a plain text question, return the raw answer (could be empty)
    private static String ask(Scanner scanner, String prompt) {
        System.out.print(prompt);
        return scanner.nextLine();
    }

    // Small helper ask for a number, fall back to a default if blank or invalid
    private static int askInt(Scanner scanner, String prompt, int defaultValue) {
        System.out.print(prompt);
        String input = scanner.nextLine();
        if (input.isEmpty()) return defaultValue;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            System.out.println("Not a valid number, using default: " + defaultValue);
            return defaultValue;
        }
    }

    // Small helper write any ServerConfig out to config.json (used by both save paths)
    private static void writeConfig(ServerConfig config) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter("config.json")) {
            gson.toJson(config, writer);
        } catch (IOException e) {
            System.out.println("Could not write config.json: " + e.getMessage());
        }
    }
}
