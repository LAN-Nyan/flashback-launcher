package com.lannyan.flashbacklauncher.modules.providers;

import com.lannyan.flashbacklauncher.modules.server.GameEntry;
import com.google.gson.Gson;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

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
                SteamGridDbGame match = result.data.get(0); // auto-pick first result for now
                System.out.println("Matched: " + match.name + " (id: " + match.id + ")");

                if (game.providerIds == null) {
                    game.providerIds = new java.util.HashMap<>();
                }
                game.providerIds.put("STEAMGRIDDB", String.valueOf(match.id));
            } else {
                System.out.println("No SteamGridDB match found for " + game.commonName);
            }

        } catch (Exception e) {
            System.out.println("Failed to fetch art for " + game.commonName + ": " + e.getMessage());
        }
    }

    /**
     * Auto-pick path: fetches candidates and immediately saves the first one.
     * Used during normal metadata refresh (MetadataManager.updateMetadata).
     */
    public void fetchArt(GameEntry game, File gameDataDir) {
        if (game.coverArt != null && new File(game.coverArt).exists()) {
            System.out.println("Cover art already exists for " + game.commonName + ", skipping...");
            return;
        }
        SteamGridDbGridResult result = searchGridImages(game);
        if (result == null || !result.success || result.data.isEmpty()) {
            System.out.println("No grid images found for " + game.commonName);
            return;
        }

        SteamGridDbGridImage image = result.data.get(0); // auto-pick first, admin can override later
        try {
            File savedFile = downloadImage(image.url, gameDataDir, "cover");
            game.coverArt = savedFile.getAbsolutePath();
            System.out.println("Saved cover art for " + game.commonName + " -> " + savedFile.getName());
        } catch (Exception e) {
            System.out.println("Failed to download cover art for " + game.commonName + ": " + e.getMessage());
        }
    }

    /**
     * Returns every candidate grid image for a game, so the admin panel
     * can show a picker instead of just accepting the auto-pick.
     * Does NOT download or save anything itself.
     */
    public SteamGridDbGridResult searchGridImages(GameEntry game) {
        String id = game.providerIds != null ? game.providerIds.get("STEAMGRIDDB") : null;
        if (id == null) return null;

        try {
            String url = "https://www.steamgriddb.com/api/v2/grids/game/" + id;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return new Gson().fromJson(response.body(), SteamGridDbGridResult.class);
        } catch (Exception e) {
            System.out.println("Failed to search grid images for " + game.commonName + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Downloads a specific chosen image URL and saves it into the game's
     * data directory. Used both by the auto-pick path (fetchArt) and by
     * an admin's manual selection (once wired up in AdminCommandManager).
     */
    public File downloadImage(String imageUrl, File destDir, String baseName) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(imageUrl)).GET().build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        String extension = imageUrl.substring(imageUrl.lastIndexOf('.'));
        File outFile = new File(destDir, baseName + extension);
        Files.write(outFile.toPath(), response.body());
        return outFile;
    }
}
