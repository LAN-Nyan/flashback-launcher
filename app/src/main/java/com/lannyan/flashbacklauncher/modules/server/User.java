package com.lannyan.flashbacklauncher.modules.server;

import java.util.Map;

public class User {
    public String id;
    public String username;
    public String passwordHash;
    public String role;        // "ADMIN" or "USER"
    public long createdAt;
    public Map<String, Object> preferences;

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
