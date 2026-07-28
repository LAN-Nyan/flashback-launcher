package com.lannyan.flashbacklauncher.modules.server;

public class User {
    public String username;
    public String passwordHash; // "salt:hash", both Base64
    public boolean isAdmin;
}
