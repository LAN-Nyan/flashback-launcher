package com.lannyan.flashbacklauncher.modules.providers;

import com.lannyan.flashbacklauncher.modules.server.GameEntry;

import java.net.URI;
import com.google.gson.Gson;
import java.util.HashMap;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class TheGamesDbProvider implements MetadataProvider {

    private final String apiKey;
    private static final HttpClient client = HttpClient.newHttpClient();

    public TheGamesDbProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void fetchMetadata(GameEntry game) {
        if (game.providerIds != null && game.providerIds.containsKey("THEGAMESDB")) {
            System.out.println("Already matched on TheGamesDB, skipping search for " + game.commonName);
            return;
        }

        try {
            String encodedName = URLEncoder.encode(game.commonName, StandardCharsets.UTF_8);
            String url = "https://api.thegamesdb.net/v1/Games/ByGameName?name=" + encodedName + "&apikey=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            TheGamesDbSearchResult result = new Gson().fromJson(response.body(), TheGamesDbSearchResult.class);

            if (result == null || result.data == null || result.data.games.isEmpty()) {
                System.out.println("No TheGamesDB match found for " + game.commonName);
                return;
            }

            TheGamesDbGame match = result.data.games.get(0); // auto-pick first result for now, same caveat as SteamGridDB
            System.out.println("Matched on TheGamesDB: " + match.game_title + " (id: " + match.id + ")");

            if (game.providerIds == null) game.providerIds = new HashMap<>();
            game.providerIds.put("THEGAMESDB", String.valueOf(match.id));

            if (game.date == null || game.date.isBlank()) {
                game.date = match.release_date;
            }

        } catch (Exception e) {
            System.out.println("Failed to fetch TheGamesDB metadata for " + game.commonName + ": " + e.getMessage());
        }
    }
}
