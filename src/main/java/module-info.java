module scrabble {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.desktop;
    requires java.naming;
    requires com.google.gson;

    exports scrabble;
    exports scrabble.client.controller;
    exports scrabble.server;
    exports scrabble.client.view.components to javafx.fxml;

    opens scrabble to javafx.graphics, com.google.gson;
    opens scrabble.client.controller to javafx.fxml;
    opens scrabble.client.model to com.google.gson;
    opens scrabble.server.model to com.google.gson;
    opens scrabble.protocol to com.google.gson;

    opens scrabble.client.view.components to javafx.graphics;
}