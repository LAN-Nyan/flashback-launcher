/*
Server backend script
7/26/26

Lannyan Github

*/

package com.lannyan.flashbacklauncher;

import com.lannyan.flashbacklauncher.modules.server.ServerCommands;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        boolean autoStart = args.length > 0 && args[0].equals("--auto-start");
        if (!autoStart) {
            autoStart = "true".equalsIgnoreCase(System.getenv("FLASHBACK_AUTO_START"));
        }

        boolean firstTimeSetupNeeded = !AppPaths.configFile("config.json").exists()
                || !AppPaths.configFile("users.json").exists();

        if (autoStart && firstTimeSetupNeeded) {
            System.err.println("FATAL: Auto-start requested, but first-time setup has not been completed.");
            System.err.println("Run the server interactively once (without --auto-start / FLASHBACK_AUTO_START) to finish setup:");
            System.err.println("  - config.json missing: " + !AppPaths.configFile("config.json").exists());
            System.err.println("  - users.json missing: " + !AppPaths.configFile("users.json").exists());
            System.exit(1);
        }

        if (autoStart) {
            System.out.println("Auto-start enabled, skipping interactive prompt.");
            ServerCommands.startServer();
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.println("Welcome! Would you like to start the server [Y/N] (default = Y):");
            String choice = scanner.nextLine();
            if (choice.equalsIgnoreCase("Y") || choice.isEmpty()) {
                ServerCommands.startServer();
            } else if (choice.equalsIgnoreCase("N")) {
                System.out.println("Okay, closing server.");
            }
            scanner.close();
        }
    }
}
