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
        Scanner scanner = new Scanner(System.in);

        System.out.println("Welcome! Would you like to start the server [Y/N] (default = Y):");

        String choice = scanner.nextLine().trim();
         // Caused first time setup isues

        if (choice.equalsIgnoreCase("Y") || choice.isEmpty()) {
            ServerCommands.startServer();
        } else if (choice.equalsIgnoreCase("N")) {
            System.out.println("Okay, closing server.");
            return;
        } else {
            System.out.println("Unrecognized choice, exiting.");
            return;
        }

        // Keep the main thread alive so the HttpsServer running on background threads doesn't exit
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            System.out.println("Server shutting down...");
            Thread.currentThread().interrupt();
        }
    }
}
