package com.lannyan.flashbacklauncher.modules.providers;

import com.lannyan.flashbacklauncher.modules.server.GameEntry;

import java.net.URI;
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
        try {
            String url = "https://api.thegamesdb.net/v1/Games/ByGameID?id="
                    + game.gameName + "&apikey=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("Response status: " + response.statusCode());
            System.out.println("Raw response: " + response.body());

            // next step: parse response.body() with Gson into fields on 'game'

        } catch (Exception e) {
            System.out.println("Failed to fetch metadata for " + game.commonName + ": " + e.getMessage());
        }
    }
}
