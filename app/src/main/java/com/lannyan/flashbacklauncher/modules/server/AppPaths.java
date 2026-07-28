package com.lannyan.flashbacklauncher.modules.server;

import java.io.File;

public class AppPaths {

    private static final String APP_NAME = "flashback-launcher";

    public static File configDir() {
        String xdgConfig = System.getenv("XDG_CONFIG_HOME");
        File base = xdgConfig != null
                ? new File(xdgConfig)
                : new File(System.getProperty("user.home"), ".config");
        File dir = new File(base, APP_NAME);
        dir.mkdirs();
        return dir;
    }

    public static File dataDir() {
        String xdgData = System.getenv("XDG_DATA_HOME");
        File base = xdgData != null
                ? new File(xdgData)
                : new File(System.getProperty("user.home"), ".local/share");
        File dir = new File(base, APP_NAME);
        dir.mkdirs();
        return dir;
    }

    public static File configFile(String name) {
        return new File(configDir(), name);
    }

    public static File dataFile(String name) {
        return new File(dataDir(), name);
    }
}
