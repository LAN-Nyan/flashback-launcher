package com.lannyan.flashbacklauncher.modules.server;


import java.util.Map;

public class ServerConfig {
    public String gamesDirectory;
    public int port;
    public boolean useHttps;
    public boolean isFirstBoot;
    public String defaultMetadataProvider;
    public Map<String, String> apiKeys;
    public String pathToHttpsCredentials;
}
