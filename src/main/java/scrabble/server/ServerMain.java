package scrabble.server;

import scrabble.server.controller.ServerController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;

public class ServerMain extends Application {
    private ServerController controller;
    private TextArea logArea;

    @Override
    public void start(Stage primaryStage) {
        controller = new ServerController();

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));

        Label title = new Label("Сервер Скрэббл");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button startButton = new Button("Запустить сервер");
        Button stopButton = new Button("Остановить сервер");
        stopButton.setDisable(true);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(400);

        TextField portField = new TextField("5555");
        portField.setPromptText("Порт");
        portField.setPrefWidth(100);

        startButton.setOnAction(e -> {
            try {
                int port = Integer.parseInt(portField.getText());
                controller.startServer(port);
                startButton.setDisable(true);
                stopButton.setDisable(false);
                log("Сервер запущен на порту " + port);
            } catch (Exception ex) {
                log("Ошибка: " + ex.getMessage());
            }
        });

        stopButton.setOnAction(e -> {
            controller.stopServer();
            startButton.setDisable(false);
            stopButton.setDisable(true);
            log("Сервер остановлен");
        });

        HBox controls = new HBox(10, new Label("Порт:"), portField, startButton, stopButton);
        controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        root.getChildren().addAll(title, controls, new Label("Лог сервера:"), logArea);

        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Сервер Скрэббл");
        primaryStage.setOnCloseRequest(e -> {
            if (controller.isRunning()) {
                controller.stopServer();
            }
        });
        primaryStage.show();
    }

    private void log(String message) {
        logArea.appendText(message + "\n");
    }

    public static void main(String[] args) {
        launch(args);
    }
}