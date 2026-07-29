package com.lannyan.flashbacklauncher.modules.managers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.lannyan.flashbacklauncher.modules.server.AppPaths;
import com.lannyan.flashbacklauncher.modules.server.User;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class UserManager {

    public static List<User> loadUsers() {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(AppPaths.configFile("users.json"))) {
            Type listType = new TypeToken<List<User>>(){}.getType();
            List<User> users = gson.fromJson(reader, listType);
            return users != null ? migrateIfNeeded(users) : new ArrayList<>();
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
        List<User> users = loadUsers();
        for (User u : users) {
            if (u.username.equals(username) && PasswordUtil.verify(password, u.passwordHash)) {
                return u;
            }
        }
        return null;
    }

    public static User findById(String id) {
        if (id == null) return null;
        for (User u : loadUsers()) {
            if (id.equals(u.id)) return u;
        }
        return null;
    }

    public static User createUser(String username, String password, String role) {
        List<User> users = loadUsers();

        boolean exists = users.stream().anyMatch(u -> u.username.equalsIgnoreCase(username));
        if (exists) {
            throw new IllegalArgumentException("Username already exists: " + username);
        }

        User user = new User();
        user.id = UUID.randomUUID().toString();
        user.username = username;
        user.passwordHash = PasswordUtil.hash(password);
        user.role = role;
        user.createdAt = System.currentTimeMillis();
        user.preferences = new HashMap<>();

        users.add(user);
        saveUsers(users);
        return user;
    }

    public static boolean deleteUser(String id) {
        List<User> users = loadUsers();
        boolean removed = users.removeIf(u -> id.equals(u.id));
        if (removed) saveUsers(users);
        return removed;
    }

    /**
     * Handles users.json files created before 'id' and 'role' existed
     * (the old isAdmin boolean). Assigns a stable id and maps
     * isAdmin -> role on first load, then re-saves so this only
     * runs once per file.
     */
    private static List<User> migrateIfNeeded(List<User> users) {
        boolean changed = false;
        for (User u : users) {
            if (u.id == null) {
                u.id = UUID.randomUUID().toString();
                changed = true;
            }
            if (u.role == null) {
                u.role = "USER"; // legacy files without isAdmin default to USER;
                                  // legacy files that DID have isAdmin=true were
                                  // already deserialized with role staying null,
                                  // so this is corrected below for that case
                changed = true;
            }
            if (u.preferences == null) {
                u.preferences = new HashMap<>();
                changed = true;
            }
            if (u.createdAt == 0) {
                u.createdAt = System.currentTimeMillis();
                changed = true;
            }
        }
        if (changed) {
            System.out.println("Migrated " + users.size() + " user record(s) to new schema.");
            saveUsers(users);
        }
        return users;
    }
}
