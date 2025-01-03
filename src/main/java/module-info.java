module com.tictactoe.app {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.tictactoe.app to javafx.fxml;
    exports com.tictactoe.app;
}