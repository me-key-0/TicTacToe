package com.tictactoe.app;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

public class TicTacToeServerStandalone implements TicTacToeConstants {
    private int sessionNo = 1;


    public void startServer() throws UnknownHostException {
        System.out.println("[" + new Date() + "] Server starting...");
        InetAddress inetAddress = InetAddress.getLocalHost();
        System.out.println("Server IP Address: " + inetAddress.getHostAddress());

        try (
                ServerSocket serverSocket = new ServerSocket(8000, 50, InetAddress.getByName("0.0.0.0"));
        ) {
            System.out.println("[" + new Date() + "] Server started on port 8000");

            while (true) {
                System.out.println("[" + new Date() + "] Waiting for players to join session " + sessionNo);

                // Accept the first player
                Socket player1 = serverSocket.accept();
                System.out.println("[" + new Date() + "] Player 1 joined session " + sessionNo);
                System.out.println("Player 1's IP address: " + player1.getInetAddress().getHostAddress());
                new DataOutputStream(player1.getOutputStream()).writeInt(PLAYER1);

                // Accept the second player
                Socket player2 = serverSocket.accept();
                System.out.println("[" + new Date() + "] Player 2 joined session " + sessionNo);
                System.out.println("Player 2's IP address: " + player2.getInetAddress().getHostAddress());
                new DataOutputStream(player2.getOutputStream()).writeInt(PLAYER2);

                System.out.println("[" + new Date() + "] Starting a thread for session " + sessionNo++);
                new Thread(new HandleASession(player1, player2)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Handle a session between two players
    class HandleASession implements Runnable, TicTacToeConstants {
        private Socket player1;
        private Socket player2;
        private char[][] cell = new char[3][3];
        private boolean continueToPlay = true;

        public HandleASession(Socket player1, Socket player2) {
            this.player1 = player1;
            this.player2 = player2;

            // Initialize the game board
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    cell[i][j] = ' ';
        }

        private void resetGame() {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    cell[i][j] = ' '; // Reset to empty state
                }
            }
            System.out.println("Game reset on the server.");
        }

        public void run() {
            try {
                DataInputStream fromPlayer1 = new DataInputStream(player1.getInputStream());
                DataOutputStream toPlayer1 = new DataOutputStream(player1.getOutputStream());
                DataInputStream fromPlayer2 = new DataInputStream(player2.getInputStream());
                DataOutputStream toPlayer2 = new DataOutputStream(player2.getOutputStream());

                toPlayer1.writeInt(1); // Notify Player 1 to start

                while (true) {
                    int command = fromPlayer1.readInt();

                    if (command == RESTART) {
                        System.out.println("Restart signal received from a client.");

                        // Notify both players about the restart
                        toPlayer1.writeInt(RESTART);
                        toPlayer2.writeInt(RESTART);

                        // Reset the game state
                        resetGame();
                        continue;
                    }

                    int row = command;
                    int column = fromPlayer1.readInt();
                    cell[row][column] = 'X';

                    if (isWon('X')) {
                        toPlayer1.writeInt(PLAYER1_WON);
                        toPlayer2.writeInt(PLAYER1_WON);
                        sendMove(toPlayer2, row, column);
                        break;
                    } else if (isFull()) {
                        toPlayer1.writeInt(DRAW);
                        toPlayer2.writeInt(DRAW);
                        sendMove(toPlayer2, row, column);
                        break;
                    } else {
                        toPlayer2.writeInt(CONTINUE);
                        sendMove(toPlayer2, row, column);
                    }

                    row = fromPlayer2.readInt();
                    column = fromPlayer2.readInt();
                    cell[row][column] = 'O';

                    if (isWon('O')) {
                        toPlayer1.writeInt(PLAYER2_WON);
                        toPlayer2.writeInt(PLAYER2_WON);
                        sendMove(toPlayer1, row, column);
                        break;
                    } else {
                        toPlayer1.writeInt(CONTINUE);
                        sendMove(toPlayer1, row, column);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendMove(DataOutputStream out, int row, int column) throws IOException {
            out.writeInt(row);
            out.writeInt(column);
        }

        private boolean isFull() {
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    if (cell[i][j] == ' ')
                        return false;
            return true;
        }

        private boolean isWon(char token) {
            for (int i = 0; i < 3; i++)
                if ((cell[i][0] == token) && (cell[i][1] == token) && (cell[i][2] == token)) {
                    return true;
                }
            for (int j = 0; j < 3; j++)
                if ((cell[0][j] == token) && (cell[1][j] == token) && (cell[2][j] == token)) {
                    return true;
                }
            if ((cell[0][0] == token) && (cell[1][1] == token) && (cell[2][2] == token)) {
                return true;
            }

            if ((cell[0][2] == token) && (cell[1][1] == token) && (cell[2][0] == token)) {
                return true;
            }
            return false;

        }
    }
}
