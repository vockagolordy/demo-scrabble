package scrabble.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

public class MainController {
    private Stage primaryStage;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    private void handleExit() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Exit");
        confirm.setHeaderText("End the program?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            primaryStage.close();
        }
    }

    @FXML
    private void handleAbout() {
        Alert about = new Alert(Alert.AlertType.INFORMATION);
        about.setTitle("About the program");
        about.setHeaderText("Word-Pot");
        about.setContentText("Version demo-1.0\n\n" +
                "Semester work for the ORIS\n" +
                "A network application with a windowed interface\n\n" +
                "Scrabble-like game with the chat and rooms\n" +
                "Implemented on JavaFX with use of Java NIO.");
        about.showAndWait();
    }
}