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
        confirm.setTitle("Выход");
        confirm.setHeaderText("Завершить работу программы?");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            primaryStage.close();
        }
    }

    @FXML
    private void handleAbout() {
        Alert about = new Alert(Alert.AlertType.INFORMATION);
        about.setTitle("О программе");
        about.setHeaderText("Сетевой Скрэббл");
        about.setContentText("Версия 1.0\n\n" +
                "Семестровая работа по Основам разработки ИС\n" +
                "Сетевое приложение с оконным интерфейсом\n\n" +
                "Игра Скрэббл с поддержкой сети, комнат и чата.\n" +
                "Реализовано на JavaFX с использованием Java NIO.");
        about.showAndWait();
    }
}