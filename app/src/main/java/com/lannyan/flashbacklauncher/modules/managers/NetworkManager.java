package com.lannyan.flashbacklauncher.modules.managers;

import com.lannyan.flashbacklauncher.modules.server.ServerConfig;

import com.google.gson.Gson;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class NetworkManager {

    public static void fetchCreds(ServerConfig config) {
        // Prevent NullPointerException if the path wasn't set in the JSON
        if (config.pathToHttpsCredentials == null) {
            System.err.println("Error: pathToHttpsCredentials is not set in config.");
            return;
        }

        // Renamed variable to 'credsPath' to avoid conflict with the 'config' parameter
        Path credsPath = Paths.get(config.pathToHttpsCredentials);

        try (Stream<Path> stream = Files.walk(credsPath)) {
            stream.filter(Files::isRegularFile)
                  .filter(path -> path.toString().endsWith(".pem"))
                  // Fixed syntax: using '::' instead of '.'
                  .forEach(System.out::println);
        } catch (IOException e) {
            System.err.println("Error scanning for credentials: " + e.getMessage());
            e.printStackTrace();
        }


    }


    public static void connectClient() {

    }

    public static void disConnectClient() {

    }

    public static void authenticateClient() {

    }

    public static void rejectClient() {

    }

    public static void startClientDownload() {

    }

    public static void startClientUpload() {

    }

    public static void authAdmin() {

    }

    public static void rejectAdmin() {

    }





}
