package com.tictactoe.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class TicTacToeClient extends Application implements TicTacToeConstants{
    private boolean myTurn = false;
    private char myToken = ' ';
    private char otherToken = ' ';
    private Cell[][] cell = new Cell[3][3];
    private Label lbTitle = new Label();
    private Label lbStatus = new Label();
    private int rowSelected;
    private int columnSelected;
    private DataInputStream fromServer;
    private DataOutputStream toServer;
    private boolean continueToPlay = true;
    private boolean waiting = true;
//    private String host = "localhost";
    private String host = "192.168.229.133";

    @Override
    public void start(Stage stage){
        GridPane pane = new GridPane();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                pane.add(cell[i][j] = new Cell(i, j), j, i);
            }
        }

        // Create buttons
        Button btnRestart = new Button("Restart");
        Button btnClose = new Button("Close");

        // Create an HBox to hold the status label and buttons
        HBox bottomBox = new HBox(10); // Horizontal box with spacing
        bottomBox.setStyle("-fx-padding: 10; -fx-alignment: center-right;"); // Align to the right
        bottomBox.getChildren().addAll(lbStatus, btnRestart, btnClose); // Add status and buttons

        // Create the main layout
        BorderPane borderPane = new BorderPane();
        borderPane.setTop(lbTitle);
        borderPane.setCenter(pane);
        borderPane.setBottom(bottomBox); // Add the HBox to the bottom
        btnRestart.setStyle("-fx-background-color: lightblue; -fx-font-size: 14px;");
        btnClose.setStyle("-fx-background-color: lightcoral; -fx-font-size: 14px;");


        // Set up the scene
        Scene scene = new Scene(borderPane, 320, 350);
        stage.setTitle("Tic-Tac-Toe-Client");
        stage.setScene(scene);
        stage.show();

        connectToServer();

        // Button actions
        btnRestart.setOnAction(e -> {
            // Notify the server to reset the game
            try {
                toServer.writeInt(RESTART); // Send a reset signal to the server
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
//        btnClose.setOnAction(e -> Platform.exit());
        btnClose.setOnAction(e -> System.exit(0));
    }

    public static void launchClient(String[] args) {
        launch(args); // Calls the start() method of TicTacToeClient
    }

    private void restartGame() {
        // Clear the board
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                cell[i][j].setToken(' '); // Reset each cell's token
            }
        }

        // Reset game variables
        myTurn = (myToken == 'X'); // Player 1 starts first
        continueToPlay = true;
        waiting = true;
        lbStatus.setText("Game reset. Waiting for the first move...");
    }

    private void connectToServer(){
        try{
            Socket socket = new Socket(host, 8000);
            fromServer = new DataInputStream(socket.getInputStream());
            toServer = new DataOutputStream(socket.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        new Thread(() -> {
            try{
                int player = fromServer.readInt();
                if(player == PLAYER1){
                    myToken = 'X';
                    otherToken = '0';
                    Platform.runLater(() -> {
                        lbTitle.setText("Player 1 with token 'X'");
                        lbStatus.setText("Waiting for player 2 to join");
                    });
                    fromServer.readInt();
                    Platform.runLater(() ->
                            lbStatus.setText("Player 2 has joined. I start first"));
                    myTurn = true;
                }else if(player == PLAYER2){
                    myToken = '0';
                    otherToken = 'X';
                    Platform.runLater(() -> {
                        lbTitle.setText("Player 2 with token '0'");
                        lbStatus.setText("Waiting for player 1 to move");
                    });
                }
                while(continueToPlay){
                    if(player == PLAYER1){
                        waitForPlayerAction();
                        sendMove();
                        recieveInfoFromServer();
                    }else if(player == PLAYER2){
                        recieveInfoFromServer();
                        waitForPlayerAction();
                        sendMove();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
    private void waitForPlayerAction() throws InterruptedException {
        while(waiting){
            Thread.sleep(100);
        }
        waiting = true;
    }
    private void sendMove() throws IOException {
        toServer.writeInt(rowSelected);
        toServer.writeInt(columnSelected);
    }
    private void recieveInfoFromServer() throws IOException{
        int status = fromServer.readInt();

        if (status == RESTART) {
            // Handle restart
            Platform.runLater(() -> {
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        cell[i][j].setToken(' '); // Clear the board
                    }
                }
                lbStatus.setText("Game restarted. Waiting for the first move...");
            });
            myTurn = (myToken == 'X'); // Reset turn
            continueToPlay = true; // Allow new moves

            recieveMove();
            Platform.runLater(() -> lbStatus.setText("My turn"));
            myTurn = true;
        } else if(status == PLAYER1_WON){
            continueToPlay = false;
            if(myToken == 'X'){
                Platform.runLater(() -> lbStatus.setText("I won! (X)"));
            }
            else if(myToken == '0'){
                Platform.runLater(() -> lbStatus.setText("Player 1 (X) has won!"));
                recieveMove();
            }
        }else if(status == PLAYER2_WON){
            continueToPlay = false;
            if(myToken == 'X'){
                Platform.runLater(() -> lbStatus.setText("Player 2 (0) has won!"));
                recieveMove();
            }
            else if(myToken == '0'){
                Platform.runLater(() -> lbStatus.setText("I won! (X)"));
                recieveMove();
            }
        } else if (status == DRAW) {
            continueToPlay = false;
            Platform.runLater(() -> lbStatus.setText("Game is over, no winner!"));
            if(myToken == '0'){
                recieveMove();
            }
        } else{
            recieveMove();
            Platform.runLater(() -> lbStatus.setText("My turn"));
            myTurn = true;
        }
    }
    private void recieveMove() throws IOException{
        int row = fromServer.readInt();
        int column = fromServer.readInt();
        Platform.runLater(() -> cell[row][column].setToken(otherToken));
    }
    public class Cell extends Pane {
        private int row;
        private int column;
        private char token = ' ';
        public Cell(int row, int column){
            this.row = row;
            this.column = column;
            this.setPrefSize(2000, 2000);
            setStyle("-fx-border-color: black");
            this.setOnMouseClicked(e -> handleMouseClick());
        }
        public char getToken(){
            return token;
        }
        public void setToken(char c){
            token = c;
            repaint();
        }
//        protected void repaint(){
//            if(token == 'X'){
//                Line line1 = new Line(10, 10, this.getWidth() - 10, this.getHeight() - 10);
//                line1.endXProperty().bind(this.widthProperty().subtract(10));
//                line1.endYProperty().bind(this.heightProperty().subtract(10));
//
//                Line line2 = new Line(10, this.getHeight() - 10, this.getWidth() - 10, 10);
//                line2.startYProperty().bind(this.heightProperty().subtract(10));
//                line2.endXProperty().bind(this.widthProperty().subtract(10));
//
//                this.getChildren().addAll(line1, line2);
//            }else if(token == '0'){
//                Ellipse ellipse = new Ellipse(this.getWidth() / 2, this.getHeight() / 2,
//                        this.getWidth() / 2 - 10, this.getHeight() / 2 - 10);
//                ellipse.centerXProperty().bind(this.widthProperty().divide(2));
//                ellipse.centerYProperty().bind(this.heightProperty().divide(2));
//                ellipse.radiusXProperty().bind(this.widthProperty().divide(2).subtract(10));
//                ellipse.radiusYProperty().bind(this.heightProperty().divide(2).subtract(10));
//                ellipse.setStroke(Color.BLACK);
//                ellipse.setFill(Color.WHITE);
//
//                getChildren().add(ellipse);
//            } else {
//                // Implement the code to clear the cell's value
//                this.getChildren().clear();
//            }
//        }

        protected void repaint() {
            // Clear previous children to avoid layering
            this.getChildren().clear();

            if (token == 'X') {
                // Lighter red "X" with adjusted size for better UX
                Line line1 = new Line(30, 30, this.getWidth() - 30, this.getHeight() - 30);
                line1.setStroke(Color.rgb(255, 102, 102)); // Lighter red
                line1.setStrokeWidth(8); // Double the thickness
                line1.endXProperty().bind(this.widthProperty().subtract(30));
                line1.endYProperty().bind(this.heightProperty().subtract(30));

                Line line2 = new Line(30, this.getHeight() - 30, this.getWidth() - 30, 30);
                line2.setStroke(Color.rgb(227,59,91)); // Lighter red
                line2.setStrokeWidth(8); // Double the thickness
                line2.startYProperty().bind(this.heightProperty().subtract(30));
                line2.endXProperty().bind(this.widthProperty().subtract(30));

                this.getChildren().addAll(line1, line2);
            } else if (token == '0') {
                // Light blue "O" with smaller size and thicker stroke
                Ellipse ellipse = new Ellipse(this.getWidth() / 2, this.getHeight() / 2,
                        Math.min(this.getWidth(), this.getHeight()) / 4 - 10, Math.min(this.getWidth(), this.getHeight()) / 4 - 10);
                ellipse.setStroke(Color.rgb(0,156,247)); // Light blue
                ellipse.setStrokeWidth(8); // Double the thickness
                ellipse.setFill(Color.TRANSPARENT); // Hollow circle
                ellipse.centerXProperty().bind(this.widthProperty().divide(2));
                ellipse.centerYProperty().bind(this.heightProperty().divide(2));
                ellipse.radiusXProperty().bind(this.widthProperty().divide(4).subtract(10));
                ellipse.radiusYProperty().bind(this.heightProperty().divide(4).subtract(10));

                this.getChildren().add(ellipse);
            } else {
                // Clear the cell if no token
                this.getChildren().clear();
            }
        }


        private void handleMouseClick(){
            if(token == ' ' && myTurn){
                setToken(myToken);
                myTurn = false;
                rowSelected = row;
                columnSelected = column;
                lbStatus.setText("Waiting for the other player to move");
                waiting = false;
            }
        }
    }
}
