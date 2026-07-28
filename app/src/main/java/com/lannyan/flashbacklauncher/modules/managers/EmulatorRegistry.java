package com.lannyan.flashbacklauncher.modules.managers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class EmulatorRegistry {

    private static Map<String, String> emulatorMap = new HashMap<>();

    public static void load() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader("emulators.json")) {
            Type mapType = new TypeToken<Map<String, String>>(){}.getType();
            emulatorMap = gson.fromJson(reader, mapType);
            System.out.println("Loaded " + emulatorMap.size() + " emulator mappings from emulators.json");
        } catch (IOException e) {
            System.out.println("Could not read emulators.json: " + e.getMessage());
            emulatorMap = new HashMap<>();
        }
    }

    public static String getEmulatorFor(String consoleCode) {
        return emulatorMap.getOrDefault(consoleCode, null);
    }
}
