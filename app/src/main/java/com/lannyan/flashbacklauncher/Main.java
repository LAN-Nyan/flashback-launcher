package com.lannyan.flashbacklauncher;
import java.util.Scanner;
/*
Server backend script
7/26/26

Lannyan Github

*/


public class Main {
   public static void main(String args[]) {
       Scanner scanner = new Scanner(System.in);

       System.out.println("Welcome! Would you like to start the server [Y/N] (default = Y):");

       String choice = scanner.nextLine();
       if (choice.equalsIgnoreCase("Y") || choice.isEmpty()) { // isEmpty() handles your "default = Y"
           ServerCommands.startServer();
       } else if (choice.equalsIgnoreCase("N")) {
           System.out.println("Okay, closing server.");
           return; // see #3
       }

       scanner.close();
   }
}
