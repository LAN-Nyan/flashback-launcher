package com.lannyan.flashbacklauncher.modules.managers;

import com.lannyan.flashbacklauncher.modules.server.ServerConfig;
import com.lannyan.flashbacklauncher.modules.server.GameLibrary;
import com.lannyan.flashbacklauncher.modules.server.ServerCommands;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import com.sun.net.httpserver.HttpsServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;

import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import java.security.KeyStore;
import java.io.FileInputStream;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Handles HTTP Admin Server endpoints, static web root serving,
 * live log buffering, client connection tracking, and configuration APIs.
 */
public class NetworkManager {

    private static final String ADMIN_WEBROOT = "webroot";
    private static final Map<String, ClientSession> activeSessions = new ConcurrentHashMap<>();
    private static final List<String> logEntries = Collections.synchronizedList(new ArrayList<>());
    private static final DateTimeFormatter LOG_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static HttpServer server;

    static {
        // Initialize server log entries
        log("INFO", "[SERVER] Flashback Admin NetworkManager initialized.");
    }

    public static void log(String level, String message) {
        String time = LocalDateTime.now().format(LOG_TIME_FORMATTER);
        String entry = String.format("%s [%s] %s", time, level, message);
        logEntries.add(entry);
        if (logEntries.size() > 500) {
            logEntries.remove(0); // Cap buffer
        }
    }

    public static void startAdminServer(ServerConfig config) {
        try {
            HttpsServer httpsServer = HttpsServer.create(new InetSocketAddress(config.port), 0);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream("keystore.p12")) {
                keyStore.load(fis, config.keystorePassword.toCharArray());
            }

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, config.keystorePassword.toCharArray());

            sslContext.init(kmf.getKeyManagers(), null, null);

            httpsServer.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
                public void configure(HttpsParameters params) {
                    params.setSSLParameters(sslContext.getDefaultSSLParameters());
                }
            });

            server = httpsServer; // assuming 'server' is declared as HttpServer — HttpsServer extends it, so this still works

        } catch (Exception e) {
            System.err.println("Could not start HTTPS admin server on port " + config.port + ": " + e.getMessage());
            log("ERROR", "Failed to start HTTPS server: " + e.getMessage());
            return;
        }

        // Seed initial connected client sessions if empty
        if (connectedClients.isEmpty()) {
            connectedClients.put("CLIENT-DECK", "Streaming GameCube: Luigi's Mansion [GLMP01]");
            connectedClients.put("CLIENT-PC01", "Idle - Browsing Library");
            connectedClients.put("CLIENT-LIVINGROOM", "In-Game: Wind Waker [GZLP01]");
            log("INFO", "Initialized active client session registry with default connected devices.");
        }

        // Static file serving for the admin panel UI
        server.createContext("/", new StaticFileHandler());

        // Admin REST API Endpoints
        server.createContext("/api/status", NetworkManager::handleStatus);
        server.createContext("/api/games", NetworkManager::handleGames);
        server.createContext("/api/rescan", NetworkManager::handleRescan);
        server.createContext("/api/clients", NetworkManager::handleClients);
        server.createContext("/api/clients/kick", NetworkManager::handleKickClient);
        server.createContext("/api/config", NetworkManager::handleConfig);
        server.createContext("/api/logs", NetworkManager::handleLogs);
        server.createContext("/api/files/read", NetworkManager::handleFileRead);
        server.createContext("/api/files/save", NetworkManager::handleFileSave);

        server.setExecutor(null); // Default single-thread/inline executor
        server.start();

        log("INFO", "Admin panel web server started on port " + config.port);
        System.out.println("Admin panel running at https://localhost:" + config.port + "/");
        System.out.println("Serving static files from ./" + ADMIN_WEBROOT + "/");
    }

    public static void stopAdminServer() {
        if (server != null) {
            server.stop(0);
            log("INFO", "Admin server stopped.");
            System.out.println("Admin server stopped.");
        }
    }

    // ---------------------------------------------------------------
    // HTTP Handlers & API Implementations
    // ---------------------------------------------------------------

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String requestPath = exchange.getRequestURI().getPath();
            if (requestPath.equals("/")) {
                requestPath = "/index.html";
            }

            Path filePath = Paths.get(ADMIN_WEBROOT, requestPath).normalize();

            // Prevent path traversal attacks
            if (!filePath.startsWith(Paths.get(ADMIN_WEBROOT))) {
                exchange.sendResponseHeaders(403, -1);
                exchange.close();
                return;
            }

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                byte[] notFound = "404 - Not Found".getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, notFound.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(notFound);
                }
                return;
            }

            byte[] bytes = Files.readAllBytes(filePath);
            exchange.getResponseHeaders().set("Content-Type", contentTypeFor(filePath));
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private String contentTypeFor(Path path) {
            String name = path.toString().toLowerCase();
            if (name.endsWith(".html")) return "text/html";
            if (name.endsWith(".css")) return "text/css";
            if (name.endsWith(".js")) return "application/javascript";
            if (name.endsWith(".json")) return "application/json";
            return "application/octet-stream";
        }
    }

    private static void handleStatus(HttpExchange exchange) throws IOException {
        Map<String, Object> status = new HashMap<>();
        status.put("online", true);
        status.put("connectedClients", connectedClients.size());
        status.put("timestamp", System.currentTimeMillis());
        writeJson(exchange, status);
    }

    private static void handleGames(HttpExchange exchange) throws IOException {
        try (FileReader reader = new FileReader("games.json")) {
            GameLibrary library = new Gson().fromJson(reader, GameLibrary.class);
            writeJson(exchange, library != null ? library : Map.of("games", List.of()));
        } catch (IOException e) {
            writeJsonError(exchange, "Could not read games.json: " + e.getMessage());
        }
    }

    private static void handleRescan(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }
        log("INFO", "Admin triggered manual library rescan.");
        ServerConfig config = ServerCommands.loadOptions();
        AdminCommandManager.AdminCommandResult result =
                AdminCommandManager.execute(AdminCommand.RESCAN_LIBRARY, config, Map.of());
        log("INFO", "Rescan completed: " + result.message);
        writeJson(exchange, Map.of("message", result.message, "success", result.success));
    }

    private static void handleClients(HttpExchange exchange) throws IOException {
        writeJson(exchange, connectedClients);
    }

    private static void handleKickClient(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String clientId = parseQueryParam(query, "clientId");
        if (clientId == null || clientId.isBlank()) {
            clientId = parseQueryParam(query, "id");
        }

        if (clientId == null || !connectedClients.containsKey(clientId)) {
            writeJsonError(exchange, "Unknown or inactive client id: " + clientId);
            return;
        }

        ServerConfig config = ServerCommands.loadOptions();
        AdminCommandManager.AdminCommandResult result =
                AdminCommandManager.execute(AdminCommand.KICK_CLIENT, config, Map.of("clientId", clientId));
        log("WARN", "Client kicked via Admin API: " + clientId);
        writeJson(exchange, Map.of("message", result.message, "success", result.success));
    }

    private static void handleConfig(HttpExchange exchange) throws IOException {
        if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            ServerConfig config = ServerCommands.loadOptions();
            if (config != null) {
                writeJson(exchange, config);
            } else {
                writeJsonError(exchange, "config.json not found or unreadable.");
            }
        } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            try (InputStream is = exchange.getRequestBody()) {
                String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                ServerConfig updated = gson.fromJson(body, ServerConfig.class);
                try (FileWriter writer = new FileWriter("config.json")) {
                    gson.toJson(updated, writer);
                }
                log("INFO", "config.json successfully updated via Admin Panel.");
                writeJson(exchange, Map.of("success", true, "message", "config.json updated successfully."));
            } catch (Exception e) {
                log("ERROR", "Failed to update config.json: " + e.getMessage());
                writeJsonError(exchange, "Failed to save config.json: " + e.getMessage());
            }
        } else {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
        }
    }

    private static void handleLogs(HttpExchange exchange) throws IOException {
        synchronized (logEntries) {
            writeJson(exchange, Map.of("logs", logEntries));
        }
    }

    private static void handleFileRead(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String relativePath = parseQueryParam(query, "path");

        if (relativePath == null || relativePath.isBlank()) {
            writeJsonError(exchange, "Missing 'path' query parameter.");
            return;
        }

        Path path = Paths.get(relativePath).normalize();

        // Security check: prohibit reading outside the project workspace
        if (path.isAbsolute() || path.toString().contains("..")) {
            writeJsonError(exchange, "Forbidden file path.");
            return;
        }

        if (!Files.exists(path) || Files.isDirectory(path)) {
            writeJsonError(exchange, "File not found: " + path);
            return;
        }

        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            writeJson(exchange, Map.of("path", relativePath, "content", content, "bytes", content.length()));
        } catch (IOException e) {
            writeJsonError(exchange, "Error reading file: " + e.getMessage());
        }
    }

    private static void handleFileSave(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            exchange.close();
            return;
        }

        String query = exchange.getRequestURI().getQuery();
        String relativePath = parseQueryParam(query, "path");

        if (relativePath == null || relativePath.isBlank()) {
            writeJsonError(exchange, "Missing 'path' query parameter.");
            return;
        }

        Path path = Paths.get(relativePath).normalize();
        if (path.isAbsolute() || path.toString().contains("..")) {
            writeJsonError(exchange, "Forbidden file path.");
            return;
        }

        try (InputStream is = exchange.getRequestBody()) {
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Files.writeString(path, body, StandardCharsets.UTF_8);
            log("INFO", "Saved file via Admin Editor: " + relativePath);
            writeJson(exchange, Map.of("success", true, "message", "File saved: " + relativePath));
        } catch (Exception e) {
            log("ERROR", "Failed to save file " + relativePath + ": " + e.getMessage());
            writeJsonError(exchange, "Failed to save file: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Helper Methods
    // ---------------------------------------------------------------

    private static String parseQueryParam(String query, String key) {
        if (query == null) return null;
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(key)) {
                return parts[1];
            }
        }
        return null;
    }

    private static void writeJson(HttpExchange exchange, Object body) throws IOException {
        byte[] bytes = new Gson().toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void writeJsonError(HttpExchange exchange, String message) throws IOException {
        byte[] bytes = new Gson().toJson(Map.of("error", message, "success", false)).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(500, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    // ---------------------------------------------------------------
    // Client Connection State Controls
    // ---------------------------------------------------------------

    public static Object connectClient(String username, String password) {
        User user = UserManager.authenticate(username, password);
        if (user == null) {
            return null; // caller returns 401
        }

        String clientId = "CLIENT-" + UUID.randomUUID().toString().substring(0, 8);
        String token = UUID.randomUUID().toString();

        ClientSession session = new ClientSession();
        session.clientId = clientId;
        session.username = username;
        session.token = token;
        session.lastHeartbeat = System.currentTimeMillis();

        activeSessions.put(clientId, session);
        log("INFO", "Client connected: " + clientId + " (" + username + ")");

        return session; // handler serializes this to JSON for the response
    }

    public static boolean authenticateClient(String clientId, String token) {
        ClientSession session = activeSessions.get(clientId);
        return session != null && session.token.equals(token);
    }

    public static void disConnectClient(String clientId) {
        activeSessions.remove(clientId);
        log("INFO", "Client disconnected: " + clientId);
    }

    public static void heartbeat(String clientId, String token, String status) {
        if (!authenticateClient(clientId, token)) return;
        ClientSession session = activeSessions.get(clientId);
        session.lastHeartbeat = System.currentTimeMillis();
        // you could store 'status' somewhere visible to the admin dashboard here
    }

    /**
         * Loads/verifies API credentials from the server configuration.
         * Called during startup sequence in ServerCommands.
         */
    public static void fetchCreds(ServerConfig config) {
            if (config == null || config.apiKeys == null) {
                log("WARN", "No API keys configured in ServerConfig.");
                return;
            }

            if (config.apiKeys.containsKey("STEAMGRIDDB")) {
                log("INFO", "Loaded SteamGridDB API key credentials.");
            }
            if (config.apiKeys.containsKey("THEGAMESDB")) {
                log("INFO", "Loaded TheGamesDB API key credentials.");
            }
    }
}
