// scrabble/client/controller/LobbyController.java

package scrabble.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import scrabble.client.model.ClientModel;
import scrabble.client.network.ClientNetworkHandler;
import scrabble.protocol.Message;
import scrabble.protocol.MessageType;
import scrabble.protocol.ProtocolParser;

import java.io.IOException;

public class LobbyController {
    @FXML private TextField playerNameField;
    @FXML private TextField serverAddressField;
    @FXML private TextField serverPortField;
    @FXML private Button connectButton;
    @FXML private Button disconnectButton;
    @FXML private Label statusLabel;
    @FXML private TextField newRoomNameField;
    @FXML private ComboBox<String> maxPlayersCombo;
    @FXML private Button createRoomButton;
    @FXML private ListView<String> roomsListView;
    @FXML private Button joinRoomButton;
    @FXML private Label currentRoomLabel;
    @FXML private ListView<String> playersListView;
    @FXML private Button readyButton;
    @FXML private Button leaveRoomButton;
    @FXML private Button startGameButton;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInputField;
    @FXML private Button sendChatButton;

    private ClientModel model;
    private ClientNetworkHandler networkHandler;
    private Stage primaryStage;

    @FXML
    public void initialize() {
        model = new ClientModel();

        statusLabel.textProperty().bind(model.statusMessageProperty());
        roomsListView.setItems(model.getAvailableRooms());

        model.connectedToServerProperty().addListener((obs, oldVal, newVal) -> {
            connectButton.setDisable(newVal);
            disconnectButton.setDisable(!newVal);
            createRoomButton.setDisable(!newVal);
            newRoomNameField.setDisable(!newVal);
            maxPlayersCombo.setDisable(!newVal);
        });

        model.gameStateProperty().addListener((obs, oldState, newState) -> {
            updateRoomInfo();

            if (newState.isGameStarted()) {
                Platform.runLater(() -> openGameWindow());
            }
        });

        maxPlayersCombo.getItems().addAll("2", "3", "4");
        maxPlayersCombo.getSelectionModel().selectFirst();

        roomsListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    joinRoomButton.setDisable(newVal == null);
                });
    }

    @FXML
    private void handleConnect() {
        String playerName = playerNameField.getText().trim();
        String address = serverAddressField.getText().trim();
        String portText = serverPortField.getText().trim();

        if (playerName.isEmpty()) {
            showAlert("Введите имя игрока");
            return;
        }

        if (address.isEmpty()) {
            showAlert("Введите адрес сервера");
            return;
        }

        try {
            int port = Integer.parseInt(portText);
            model.setPlayerName(playerName);

            networkHandler = new ClientNetworkHandler(model);
            if (networkHandler.connect(address, port, playerName)) {
                model.setStatusMessage("Подключение...");
            }
        } catch (NumberFormatException e) {
            showAlert("Некорректный номер порта");
        }
    }

    @FXML
    private void handleDisconnect() {
        if (networkHandler != null) {
            networkHandler.disconnect();
        }
    }

    @FXML
    private void handleCreateRoom() {
        String roomName = newRoomNameField.getText().trim();
        String maxPlayers = maxPlayersCombo.getValue();

        if (roomName.isEmpty()) {
            showAlert("Введите название комнаты");
            return;
        }

        Message message = ProtocolParser.createCreateRoomMessage(roomName, Integer.parseInt(maxPlayers));

        if (networkHandler != null) {
            networkHandler.sendMessage(message);
            newRoomNameField.clear();
        }
    }

    @FXML
    private void handleJoinRoom() {
        String selectedRoom = roomsListView.getSelectionModel().getSelectedItem();
        if (selectedRoom != null) {
            // Извлекаем ID комнаты из строки
            String[] parts = selectedRoom.split(" - ");
            if (parts.length > 0) {
                String roomId = parts[0];

                Message message = ProtocolParser.createJoinRoomMessage(roomId);

                if (networkHandler != null) {
                    networkHandler.sendMessage(message);
                    leaveRoomButton.setDisable(false);
                    readyButton.setDisable(false);
                    startGameButton.setDisable(false);
                }
            }
        }
    }

    @FXML
    private void handleReady() {
        Message message = ProtocolParser.createPlayerReadyMessage();
        if (networkHandler != null) {
            networkHandler.sendMessage(message);
            readyButton.setDisable(true);
        }
    }

    @FXML
    private void handleLeaveRoom() {
        Message message = ProtocolParser.createLeaveRoomMessage();
        if (networkHandler != null) {
            networkHandler.sendMessage(message);
            currentRoomLabel.setText("Не в комнате");
            playersListView.getItems().clear();
            leaveRoomButton.setDisable(true);
            readyButton.setDisable(true);
            startGameButton.setDisable(true);
            chatArea.clear();
        }
    }

    @FXML
    private void handleStartGame() {
        Message message = ProtocolParser.createGameStartMessage();
        if (networkHandler != null) {
            networkHandler.sendMessage(message);
        }
    }

    @FXML
    private void handleSendChat() {
        String messageText = chatInputField.getText().trim();
        if (!messageText.isEmpty() && networkHandler != null) {
            Message message = ProtocolParser.createChatMessage(messageText);
            networkHandler.sendMessage(message);
            chatInputField.clear();
        }
    }

    private void updateRoomInfo() {
        String roomId = model.getCurrentRoomId();
        if (!roomId.isEmpty()) {
            currentRoomLabel.setText("Комната: " + roomId);

            playersListView.getItems().clear();
            for (scrabble.client.model.Player player : model.getGameState().getPlayers()) {
                String playerStatus = player.getName();
                if (player.isReady()) playerStatus += " ✓";
                if (player.isCurrentTurn()) playerStatus += " *";
                playersListView.getItems().add(playerStatus);
            }

            chatArea.clear();
            for (String message : model.getGameState().getChatMessages()) {
                chatArea.appendText(message + "\n");
            }

            boolean isCreator = model.getGameState().getPlayers().stream()
                    .filter(p -> p.getId().equals(model.getPlayerId()))
                    .findFirst()
                    .map(p -> model.getGameState().getPlayers().indexOf(p) == 0)
                    .orElse(false);

            startGameButton.setVisible(isCreator &&
                    model.getGameState().getPlayers().size() >= 2);
        }
    }

    private void openGameWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/game.fxml"));
            Parent gameRoot = loader.load();

            GameController gameController = loader.getController();
            gameController.setModel(model);
            gameController.setNetworkHandler(networkHandler);

            Stage gameStage = new Stage();
            gameStage.setTitle("Скрэббл - " + model.getPlayerName());
            gameStage.setScene(new Scene(gameRoot, 1200, 800));
            gameStage.setOnCloseRequest(event -> {
                handleLeaveRoom();
            });

            if (primaryStage != null) {
                primaryStage.close();
            }

            gameStage.show();

            System.out.println("Окно игры открыто");

        } catch (Exception e) {
            System.err.println("Ошибка открытия окна игры: " + e.getMessage());
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Ошибка");
            alert.setHeaderText("Не удалось открыть окно игры");
            alert.setContentText("Файл game.fxml не найден или поврежден");
            alert.showAndWait();
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Внимание");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }
}