package scrabble.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import scrabble.client.model.ClientModel;
import scrabble.client.model.GameState;
import scrabble.client.model.Player;
import scrabble.client.network.ClientNetworkHandler;
import scrabble.client.view.components.BoardCanvas;
import scrabble.client.view.components.RackView;
import scrabble.client.view.components.TileView;
import scrabble.protocol.Message;
import scrabble.protocol.ProtocolParser;
import scrabble.utils.TileBag;
import java.util.*;

public class GameController {
    @FXML private Label roomNameLabel;
    @FXML private Label turnLabel;
    @FXML private HBox playersContainer;
    @FXML private Label timerLabel;
    @FXML private Label scoreLabel;
    @FXML private Button surrenderButton;
    @FXML private Button leaveButton;
    @FXML private Button exchangeButton;
    @FXML private Button skipButton;
    @FXML private Button submitButton;
    @FXML private BoardCanvas boardCanvas;
    @FXML private RackView rackView;
    @FXML private TableView<PlayerScore> scoreTable;
    @FXML private TableColumn<PlayerScore, String> playerColumn;
    @FXML private TableColumn<PlayerScore, Integer> scoreColumn;
    @FXML private ListView<String> movesHistory;
    @FXML private TextArea gameChatArea;
    @FXML private TextField gameChatInput;
    @FXML private Button sendGameChatButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator thinkingIndicator;
    @FXML private Label tilesLeftLabel;

    private ClientModel model;
    private ClientNetworkHandler networkHandler;
    private ObservableList<PlayerScore> scoreData;
    private Map<String, Label> playerScoreLabels;
    private Set<String> selectedTilesForExchange;
    private Map<String, int[]> placedTiles; 
    private javafx.animation.Timeline gameTimer;
    private long startTime;
    private boolean isMyTurn;
    private List<TileView> tileViews;

    private static class PlayerScore {
        private final String playerName;
        private final int score;

        public PlayerScore(String playerName, int score) {
            this.playerName = playerName;
            this.score = score;
        }

        public String getPlayerName() { return playerName; }
        public int getScore() { return score; }
    }

    @FXML
    public void initialize() {
        scoreData = FXCollections.observableArrayList();
        playerScoreLabels = new HashMap<>();
        selectedTilesForExchange = new HashSet<>();
        placedTiles = new HashMap<>();
        tileViews = new ArrayList<>();

        scoreTable.setItems(scoreData);
        playerColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().getPlayerName()));
        scoreColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleIntegerProperty(cell.getValue().getScore()).asObject());

        
        rackView.setOnTileDropped(this::handleTileDrop);

        
        rackView.setOnMouseClicked(this::handleRackClick);

        
        initializeTimer();

        
        setupButtons();
    }

    public void setModel(ClientModel model) {
        this.model = model;
        updateGameState();

        
        model.gameStateProperty().addListener((obs, oldState, newState) -> {
            updateGameState();
        });

        
        fillPlayerRack();
    }

    public void setNetworkHandler(ClientNetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
    }

    private void updateGameState() {
        GameState gameState = model.getGameState();

        if (gameState != null) {
            
            String roomId = gameState.getCurrentRoomId();
            if (roomId != null && !roomId.isEmpty()) {
                String roomName = roomId.split("_")[0];
                roomNameLabel.setText("Комната: " + roomName);
            } else {
                roomNameLabel.setText("Комната: не выбрана");
            }

            
            if (gameState.getCurrentPlayer() != null) {
                isMyTurn = gameState.getCurrentPlayer().getId().equals(model.getPlayerId());
                turnLabel.setText(isMyTurn ? "Ваш ход!" :
                        "Ход игрока: " + gameState.getCurrentPlayer().getName());
                turnLabel.getStyleClass().removeAll("my-turn", "opponent-turn");
                turnLabel.getStyleClass().add(isMyTurn ? "my-turn" : "opponent-turn");

                
                boolean canMakeMove = isMyTurn && !gameState.isGameFinished();
                submitButton.setDisable(!canMakeMove || placedTiles.isEmpty());
                skipButton.setDisable(!canMakeMove);
                exchangeButton.setDisable(!canMakeMove || selectedTilesForExchange.isEmpty());

                
                if (isMyTurn) {
                    startTimer();
                }
            }

            
            updatePlayersDisplay(gameState.getPlayers());

            
            updateScoreTable(gameState.getPlayers());

            
            updateMovesHistory(gameState.getChatMessages());

            
            updateGameChat(gameState.getChatMessages());

            
            tilesLeftLabel.setText("Фишек осталось: " + model.getTileBag().remainingTiles());

            
            boardCanvas.setGameState(gameState);

            
            if (gameState.isGameFinished()) {
                handleGameFinished();
            }
        }
    }

    private void updatePlayersDisplay(List<Player> players) {
        playersContainer.getChildren().clear();
        playerScoreLabels.clear();

        for (Player player : players) {
            VBox playerBox = new VBox(5);
            playerBox.setAlignment(javafx.geometry.Pos.CENTER);
            playerBox.getStyleClass().add("player-box");

            Label nameLabel = new Label(player.getName());
            nameLabel.getStyleClass().add("player-name");

            Label scoreLabel = new Label("Очки: " + player.getScore());
            scoreLabel.getStyleClass().add("player-score");

            
            if (player.isCurrentTurn()) {
                playerBox.getStyleClass().add("current-turn");
            }

            
            if (player.getId().equals(model.getPlayerId())) {
                nameLabel.getStyleClass().add("current-user");
                scoreLabel.getStyleClass().add("current-user-score");
            }

            
            if (!model.getGameState().isGameStarted() && player.isReady()) {
                Label readyLabel = new Label("✓ Готов");
                readyLabel.getStyleClass().add("ready-status");
                playerBox.getChildren().addAll(nameLabel, scoreLabel, readyLabel);
            } else {
                playerBox.getChildren().addAll(nameLabel, scoreLabel);
            }

            playersContainer.getChildren().add(playerBox);
            playerScoreLabels.put(player.getId(), scoreLabel);
        }
    }

    private void updateScoreTable(List<Player> players) {
        scoreData.clear();
        for (Player player : players) {
            scoreData.add(new PlayerScore(player.getName(), player.getScore()));
        }

        
        scoreData.sort((p1, p2) -> Integer.compare(p2.getScore(), p1.getScore()));

        
        Player currentPlayer = model.getGameState().getPlayers().stream()
                .filter(p -> p.getId().equals(model.getPlayerId()))
                .findFirst()
                .orElse(null);

        if (currentPlayer != null) {
            scoreLabel.setText("Ваши очки: " + currentPlayer.getScore());
        }
    }

    private void updateMovesHistory(List<String> chatMessages) {
        
        List<String> moves = new ArrayList<>();
        for (String msg : chatMessages) {
            if (msg.contains("выложил") || msg.contains("получил") || msg.contains("ход")) {
                moves.add(msg);
            }
        }

        
        if (movesHistory.getItems().size() != moves.size()) {
            movesHistory.getItems().setAll(moves);
            movesHistory.scrollTo(movesHistory.getItems().size() - 1);
        }
    }

    private void updateGameChat(List<String> chatMessages) {
        StringBuilder chatText = new StringBuilder();
        for (String msg : chatMessages) {
            chatText.append(msg).append("\n");
        }
        gameChatArea.setText(chatText.toString());
        gameChatArea.setScrollTop(Double.MAX_VALUE);
    }

    private void fillPlayerRack() {
        rackView.clearTiles();
        tileViews.clear();
        selectedTilesForExchange.clear();

        Player currentPlayer = model.getGameState().getPlayers().stream()
                .filter(p -> p.getId().equals(model.getPlayerId()))
                .findFirst()
                .orElse(null);

        if (currentPlayer != null) {
            for (TileBag.Tile tile : currentPlayer.getRack()) {
                rackView.addTile(tile);
            }
        }
    }

    private void initializeTimer() {
        startTime = System.currentTimeMillis();

        gameTimer = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), event -> {
                    updateTimer();
                })
        );
        gameTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);
        gameTimer.play();
    }

    private void startTimer() {
        startTime = System.currentTimeMillis();
    }

    private void updateTimer() {
        if (isMyTurn && model.getGameState().isGameStarted()) {
            long elapsedSeconds = (System.currentTimeMillis() - startTime) / 1000;
            long minutes = elapsedSeconds / 60;
            long seconds = elapsedSeconds % 60;
            timerLabel.setText(String.format("Время: %02d:%02d", minutes, seconds));

            
            if (elapsedSeconds > 120) {
                timerLabel.setTextFill(Color.RED);
                statusLabel.setText("Внимание! У вас осталось мало времени на ход!");
            } else if (elapsedSeconds > 60) {
                timerLabel.setTextFill(Color.ORANGE);
            } else {
                timerLabel.setTextFill(Color.WHITE);
            }
        } else {
            timerLabel.setText("Время: --:--");
            timerLabel.setTextFill(Color.WHITE);
        }
    }

    private void setupButtons() {
        
        sendGameChatButton.setOnAction(event -> handleGameChat());
        gameChatInput.setOnAction(event -> handleGameChat());

        
        exchangeButton.setOnAction(event -> handleExchangeTiles());

        
        skipButton.setOnAction(event -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Пропуск хода");
            confirm.setHeaderText("Вы уверены, что хотите пропустить ход?");
            confirm.setContentText("Вы не сможете сделать ход в этом раунде.");

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                handleSkipTurn();
            }
        });
    }

    @FXML
    private void handleSubmitMove() {
        if (placedTiles.isEmpty()) {
            statusLabel.setText("Сначала разместите фишки на доске!");
            showAlert("Ошибка", "Не размещены фишки", "Пожалуйста, разместите хотя бы одну фишку на доске.");
            return;
        }

        
        if (!validateTilePlacement()) {
            statusLabel.setText("Фишки должны быть размещены по прямой линии!");
            showAlert("Ошибка", "Неправильное размещение",
                    "Фишки должны быть размещены по горизонтальной или вертикальной линии.");
            return;
        }

        
        StringBuilder wordBuilder = new StringBuilder();
        List<String> tileIds = new ArrayList<>();

        List<Map.Entry<String, int[]>> sortedTiles = new ArrayList<>(placedTiles.entrySet());
        sortedTiles.sort((a, b) -> {
            int[] posA = a.getValue();
            int[] posB = b.getValue();
            if (posA[0] == posB[0]) {
                return Integer.compare(posA[1], posB[1]);
            }
            return Integer.compare(posA[0], posB[0]);
        });

        for (Map.Entry<String, int[]> entry : sortedTiles) {
            TileBag.Tile tile = getTileById(entry.getKey());
            if (tile != null) {
                wordBuilder.append(tile.getLetter());
                tileIds.add(tile.getId());
            }
        }

        String word = wordBuilder.toString();

        
        int[] firstPos = sortedTiles.get(0).getValue();
        int row = firstPos[0];
        int col = firstPos[1];

        
        boolean horizontal = true;
        if (placedTiles.size() > 1) {
            int[] secondPos = sortedTiles.get(1).getValue();
            horizontal = firstPos[0] == secondPos[0];
        }

        Message moveMsg = ProtocolParser.createPlayerMoveMessage(
                word.toLowerCase(), row, col, horizontal,
                tileIds.toArray(new String[0])
        );

        if (networkHandler != null) {
            networkHandler.sendMessage(moveMsg);
            statusLabel.setText("Ход отправлен на проверку...");
            thinkingIndicator.setVisible(true);
            submitButton.setDisable(true);
            skipButton.setDisable(true);
            exchangeButton.setDisable(true);

            placedTiles.clear();
            boardCanvas.setDraggedTile(null);
        }
    }

    private boolean validateTilePlacement() {
        if (placedTiles.size() < 2) {
            return true; 
        }

        List<int[]> positions = new ArrayList<>(placedTiles.values());

        
        boolean sameRow = true;
        int firstRow = positions.get(0)[0];
        for (int i = 1; i < positions.size(); i++) {
            if (positions.get(i)[0] != firstRow) {
                sameRow = false;
                break;
            }
        }

        
        boolean sameCol = true;
        int firstCol = positions.get(0)[1];
        for (int i = 1; i < positions.size(); i++) {
            if (positions.get(i)[1] != firstCol) {
                sameCol = false;
                break;
            }
        }

        if (!sameRow && !sameCol) {
            return false; 
        }

        
        if (sameRow) {
            
            positions.sort((a, b) -> Integer.compare(a[1], b[1]));
            for (int i = 1; i < positions.size(); i++) {
                if (positions.get(i)[1] != positions.get(i-1)[1] + 1) {
                    return false; 
                }
            }
        } else { 
            
            positions.sort((a, b) -> Integer.compare(a[0], b[0]));
            for (int i = 1; i < positions.size(); i++) {
                if (positions.get(i)[0] != positions.get(i-1)[0] + 1) {
                    return false; 
                }
            }
        }

        return true;
    }

    @FXML
    private void handleSkipTurn() {
        Message skipMsg = ProtocolParser.createSkipTurnMessage();

        if (networkHandler != null) {
            networkHandler.sendMessage(skipMsg);
            statusLabel.setText("Ход пропущен");
            thinkingIndicator.setVisible(true);
            skipButton.setDisable(true);
        }
    }

    @FXML
    private void handleExchangeTiles() {
        if (selectedTilesForExchange.isEmpty()) {
            statusLabel.setText("Выберите фишки для обмена");
            showAlert("Внимание", "Не выбраны фишки",
                    "Пожалуйста, выберите фишки для обмена, кликая по ним.");
            return;
        }

        if (selectedTilesForExchange.size() > model.getTileBag().remainingTiles()) {
            statusLabel.setText("Недостаточно фишек в мешке для обмена!");
            showAlert("Ошибка", "Недостаточно фишек",
                    "В мешке недостаточно фишек для обмена такого количества.");
            return;
        }

        List<String> tileIds = new ArrayList<>(selectedTilesForExchange);

        Message exchangeMsg = ProtocolParser.createTilesExchangeMessage(tileIds);

        if (networkHandler != null) {
            networkHandler.sendMessage(exchangeMsg);
            statusLabel.setText("Запрос на обмен фишек отправлен...");
            exchangeButton.setDisable(true);
            selectedTilesForExchange.clear();

            
            for (TileView tileView : tileViews) {
                tileView.setSelected(false);
            }
        }
    }

    @FXML
    private void handleGameChat() {
        String message = gameChatInput.getText().trim();
        if (!message.isEmpty() && networkHandler != null) {
            Message chatMsg = ProtocolParser.createChatMessage(message);
            networkHandler.sendMessage(chatMsg);
            gameChatInput.clear();
            statusLabel.setText("Сообщение отправлено");
        }
    }

    @FXML
    private void handleSurrender() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Сдача");
        confirm.setHeaderText("Вы уверены, что хотите сдаться?");
        confirm.setContentText("Это действие завершит игру для вас.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            Message surrenderMsg = ProtocolParser.createSurrenderMessage();

            if (networkHandler != null) {
                networkHandler.sendMessage(surrenderMsg);
                statusLabel.setText("Вы сдались");

                
                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Игра завершена");
                info.setHeaderText("Вы сдались");
                info.setContentText("Игра завершена. Вы можете создать новую комнату или присоединиться к другой.");
                info.showAndWait();

                
                ((javafx.stage.Stage) surrenderButton.getScene().getWindow()).close();
            }
        }
    }

    @FXML
    private void handleLeaveGame() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Выход из игры");
        confirm.setHeaderText("Вы уверены, что хотите выйти?");
        confirm.setContentText("Вы покинете текущую игру.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            Message leaveMsg = ProtocolParser.createLeaveRoomMessage();

            if (networkHandler != null) {
                networkHandler.sendMessage(leaveMsg);
                statusLabel.setText("Выход из комнаты...");

                
                javafx.stage.Stage stage = (javafx.stage.Stage) leaveButton.getScene().getWindow();
                stage.close();
            }
        }
    }

    private void handleTileDrop(TileView.TileDropEvent event) {
        if (!isMyTurn) {
            statusLabel.setText("Сейчас не ваш ход!");
            return;
        }

        TileBag.Tile tile = event.getTile();

        
        double cellX = event.getSceneX() - boardCanvas.getLayoutX();
        double cellY = event.getSceneY() - boardCanvas.getLayoutY();

        int col = (int) (cellX / 40);
        int row = (int) (cellY / 40);

        if (row >= 0 && row < 15 && col >= 0 && col < 15) {
            
            if (model.getGameState().getCell(row, col).hasTile()) {
                statusLabel.setText("Клетка уже занята!");
                showAlert("Ошибка", "Клетка занята",
                        "Выбранная клетка уже содержит фишку. Выберите другую клетку.");
                return;
            }

            
            GameState gameState = model.getGameState();
            if (isFirstMove(gameState) && !(row == 7 && col == 7)) {
                statusLabel.setText("Первый ход должен начинаться с центральной клетки!");
                showAlert("Ошибка", "Неправильное размещение",
                        "Первый ход должен начинаться с центральной клетки (H8).");
                return;
            }

            
            placedTiles.put(tile.getId(), new int[]{row, col});

            
            rackView.removeTile(tile.getId());

            
            boardCanvas.setDraggedTile(tile);

            statusLabel.setText("Фишка '" + Character.toUpperCase(tile.getLetter()) +
                    "' размещена в [" + (char)('A' + col) + "," + (row + 1) + "]");

            
            submitButton.setDisable(false);

            
            selectedTilesForExchange.remove(tile.getId());
        } else {
            statusLabel.setText("Фишка '" + Character.toUpperCase(tile.getLetter()) + "' возвращена на полку");
        }
    }

    private boolean isFirstMove(GameState gameState) {
        
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                if (gameState.getCell(i, j).hasTile()) {
                    return false;
                }
            }
        }
        return true;
    }

    private void handleRackClick(MouseEvent event) {
        if (!isMyTurn) {
            return;
        }

        
        double clickX = event.getX();
        double clickY = event.getY();

        
        TileView clickedTile = null;
        for (TileView tileView : tileViews) {
            double tileX = tileView.getLayoutX();
            double tileY = tileView.getLayoutY();
            double tileSize = 50; 

            if (clickX >= tileX && clickX <= tileX + tileSize &&
                    clickY >= tileY && clickY <= tileY + tileSize) {
                clickedTile = tileView;
                break;
            }
        }

        if (clickedTile != null && event.getClickCount() == 2) {
            
            String tileId = clickedTile.getTile().getId();
            if (selectedTilesForExchange.contains(tileId)) {
                selectedTilesForExchange.remove(tileId);
                clickedTile.setSelected(false);
                statusLabel.setText("Фишка снята с обмена");
            } else {
                
                if (!placedTiles.containsKey(tileId)) {
                    selectedTilesForExchange.add(tileId);
                    clickedTile.setSelected(true);
                    statusLabel.setText("Фишка выбрана для обмена");
                } else {
                    statusLabel.setText("Фишка уже размещена на доске!");
                }
            }

            
            exchangeButton.setDisable(selectedTilesForExchange.isEmpty());
        }
    }

    private TileBag.Tile getTileById(String tileId) {
        Player currentPlayer = model.getGameState().getPlayers().stream()
                .filter(p -> p.getId().equals(model.getPlayerId()))
                .findFirst()
                .orElse(null);

        if (currentPlayer != null) {
            return currentPlayer.getTileById(tileId);
        }
        return null;
    }

    private void handleGameFinished() {
        GameState gameState = model.getGameState();
        if (gameState.isGameFinished()) {
            
            submitButton.setDisable(true);
            skipButton.setDisable(true);
            exchangeButton.setDisable(true);
            surrenderButton.setDisable(true);

            
            if (gameTimer != null) {
                gameTimer.stop();
            }

            
            Alert gameOverAlert = new Alert(Alert.AlertType.INFORMATION);
            gameOverAlert.setTitle("Игра завершена");

            
            Player winner = gameState.getPlayers().stream()
                    .max(Comparator.comparingInt(Player::getScore))
                    .orElse(null);

            if (winner != null) {
                if (winner.getId().equals(model.getPlayerId())) {
                    gameOverAlert.setHeaderText("Поздравляем! Вы победили!");
                    gameOverAlert.setContentText("Ваш результат: " + winner.getScore() + " очков");
                } else {
                    gameOverAlert.setHeaderText("Игра завершена!");
                    gameOverAlert.setContentText("Победитель: " + winner.getName() +
                            " с результатом " + winner.getScore() + " очков\n" +
                            "Ваш результат: " +
                            gameState.getPlayerById(model.getPlayerId()).getScore() + " очков");
                }
            }

            gameOverAlert.showAndWait();
        }
    }

    private void showAlert(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    
    public void updateTileViews(List<TileView> views) {
        this.tileViews = views;
    }

    
    @FXML
    private void handleCancelMove() {
        if (!placedTiles.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Отмена хода");
            confirm.setHeaderText("Вы уверены, что хотите отменить ход?");
            confirm.setContentText("Все размещенные фишки вернутся на полку.");

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                
                for (String tileId : placedTiles.keySet()) {
                    TileBag.Tile tile = getTileById(tileId);
                    if (tile != null) {
                        rackView.addTile(tile);
                    }
                }

                placedTiles.clear();
                boardCanvas.setDraggedTile(null);
                statusLabel.setText("Ход отменен. Все фишки возвращены на полку.");
                submitButton.setDisable(true);
            }
        }
    }
}