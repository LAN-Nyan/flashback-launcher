package com.lannyan.flashbacklauncher.modules.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.lannyan.flashbacklauncher.modules.server.User;
import com.lannyan.flashbacklauncher.modules.server.AppPaths;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class UserManager {

    public static List<User> loadUsers() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(AppPaths.configFile("users.json"))) {
            Type listType = new TypeToken<List<User>>(){}.getType();
            List<User> users = gson.fromJson(reader, listType);
            return users != null ? users : new ArrayList<>();
        } catch (IOException e) {
            System.out.println("Could not read users.json: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void saveUsers(List<User> users) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(AppPaths.configFile("users.json"))) {
            gson.toJson(users, writer);
        } catch (IOException e) {
            System.out.println("Could not write users.json: " + e.getMessage());
        }
    }

    public static User authenticate(String username, String password) {
        if (username == null || password == null) return null;

        List<User> users = loadUsers();
        for (User u : users) {
            if (u.username != null && u.passwordHash != null) {
                if (u.username.equalsIgnoreCase(username.trim()) && PasswordUtil.verify(password.trim(), u.passwordHash)) {
                    return u;
                }
            }
        }
        return null;
    }
}
