package com.lannyan.flashbacklauncher.modules.providers;

import com.lannyan.flashbacklauncher.modules.server.GameEntry;
import com.google.gson.Gson;
import java.util.HashMap;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SteamGridDbProvider implements MetadataProvider {

    private final String apiKey;
    private static final HttpClient client = HttpClient.newHttpClient();

    public SteamGridDbProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public void fetchMetadata(GameEntry game) {
        if (game.providerIds != null && game.providerIds.containsKey("STEAMGRIDDB")) {
            System.out.println("Already matched on SteamGridDB, skipping search for " + game.commonName);
            return;
        }

        try {
            String encodedName = URLEncoder.encode(game.commonName, StandardCharsets.UTF_8);
            String url = "https://www.steamgriddb.com/api/v2/search/autocomplete/" + encodedName;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            Gson gson = new Gson();
            SteamGridDbSearchResult result = gson.fromJson(response.body(), SteamGridDbSearchResult.class);

            if (result.success && !result.data.isEmpty()) {
                SteamGridDbGame match = result.data.get(0); // just take the first result for now
                System.out.println("Matched: " + match.name + " (id: " + match.id + ")");

                if (game.providerIds == null) {
                    game.providerIds = new HashMap<>();
                }
                game.providerIds.put("STEAMGRIDDB", String.valueOf(match.id));
            } else {
                System.out.println("No SteamGridDB match found for " + game.commonName);
            }

        } catch (Exception e) {
            System.out.println("Failed to fetch art for " + game.commonName + ": " + e.getMessage());
        }
    }






}
