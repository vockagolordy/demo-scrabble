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

        Label title = new Label("Server of Word-Pot");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button startButton = new Button("Start server");
        Button stopButton = new Button("Stop server");
        stopButton.setDisable(true);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(400);

        TextField portField = new TextField("5555");
        portField.setPromptText("Port");
        portField.setPrefWidth(100);

        startButton.setOnAction(e -> {
            try {
                int port = Integer.parseInt(portField.getText());
                controller.startServer(port);
                startButton.setDisable(true);
                stopButton.setDisable(false);
                log("Server started in a port  " + port);
            } catch (Exception ex) {
                log("Error: " + ex.getMessage());
            }
        });

        stopButton.setOnAction(e -> {
            controller.stopServer();
            startButton.setDisable(false);
            stopButton.setDisable(true);
            log("Server stopped");
        });

        HBox controls = new HBox(10, new Label("Port:"), portField, startButton, stopButton);
        controls.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        root.getChildren().addAll(title, controls, new Label("Server's log:"), logArea);

        Scene scene = new Scene(root, 600, 500);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Server of Word-Post");
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