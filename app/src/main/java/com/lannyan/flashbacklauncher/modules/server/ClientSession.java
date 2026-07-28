package com.lannyan.flashbacklauncher.modules.server;

public class ClientSession {
    public String clientId;
    public String username;
    public String token;
    public long lastHeartbeat; // System.currentTimeMillis()
}
