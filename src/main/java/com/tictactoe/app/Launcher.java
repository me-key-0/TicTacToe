package com.tictactoe.app;

import java.net.UnknownHostException;
import java.util.Scanner;

public class Launcher {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Welcome to Tic-Tac-Toe!");
        System.out.println("1. Run as Server and Client (Host Game)");
        System.out.println("2. Run as Client Only (Join Game)");
        System.out.print("Choose an option (1/2): ");

        int choice = scanner.nextInt();

        if (choice == 1) {
            System.out.println("Starting the server...");
            Thread serverThread = new Thread(() -> {
                try {
                    new TicTacToeServerStandalone().startServer();
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                }
            });
            serverThread.start();

            // Delay to ensure the server is initialized
            try {
                Thread.sleep(2000); // Wait 2 seconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println("Starting the client...");
            TicTacToeClient.launchClient(args);
        } else if (choice == 2) {
            System.out.println("Starting as a client...");
            TicTacToeClient.launchClient(args);
        } else {
            System.out.println("Invalid choice. Exiting...");
        }

        scanner.close();
    }
}



























//public class Launcher {
//    public static void main(String[] args) {
//        Scanner scanner = new Scanner(System.in);
//        System.out.println("Welcome to Tic-Tac-Toe!");
//        System.out.println("1. Run as Server and Client (Host Game)");
//        System.out.println("2. Run as Client Only (Join Game)");
//        System.out.print("Choose an option (1/2): ");
//
//        int choice = scanner.nextInt();
//
//        if (choice == 1) {
//            System.out.println("Starting the server...");
//            Thread serverThread = new Thread(() -> TicTacToeServer.launchServer(args));
//            serverThread.start();
//
//            // Delay to ensure server starts first
//            try {
//                Thread.sleep(2000); // Wait 2 seconds for server initialization
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//            System.out.println("Starting the client...");
//            TicTacToeClient.launchClient(args);
//        } else if (choice == 2) {
//            System.out.println("Starting as a client...");
//            TicTacToeClient.launchClient(args);
//        } else {
//            System.out.println("Invalid choice. Exiting...");
//        }
//
//        scanner.close();
//    }
//}
