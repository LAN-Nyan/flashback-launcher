package com.lannyan.flashbacklauncher.modules.providers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class TheGamesDbPlatformResolver {

    private static Map<String, Integer> platformNameToId = new HashMap<>();
    private static boolean loaded = false;

    public static void loadPlatforms(String apiKey) {
        try {
            String url = "https://api.thegamesdb.net/v1/Platforms?apikey=" + apiKey;
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonObject platforms = root.getAsJsonObject("data").getAsJsonObject("platforms");

            for (String key : platforms.keySet()) {
                JsonObject platform = platforms.getAsJsonObject(key);
                String name = platform.get("name").getAsString();
                int id = Integer.parseInt(key);
                platformNameToId.put(name, id);
            }

            loaded = true;
            System.out.println("Loaded " + platformNameToId.size() + " platforms from TheGamesDB.");
        } catch (Exception e) {
            System.out.println("Failed to load TheGamesDB platforms: " + e.getMessage());
        }
    }

    public static Integer getIdForName(String name) {
        return platformNameToId.get(name);
    }
}
