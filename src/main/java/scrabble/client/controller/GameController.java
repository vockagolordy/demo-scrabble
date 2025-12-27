package scrabble.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Point2D;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
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
    @FXML
    private Label roomNameLabel;
    @FXML
    private Label turnLabel;
    @FXML
    private HBox playersContainer;
    @FXML
    private Label timerLabel;
    @FXML
    private Label scoreLabel;
    @FXML
    private Button surrenderButton;
    @FXML
    private Button leaveButton;
    @FXML
    private Button exchangeButton;
    @FXML
    private Button skipButton;
    @FXML
    private Button submitButton;
    @FXML
    private BoardCanvas boardCanvas;
    @FXML
    private RackView rackView;
    @FXML
    private TableView<PlayerScore> scoreTable;
    @FXML
    private TableColumn<PlayerScore, String> playerColumn;
    @FXML
    private TableColumn<PlayerScore, Integer> scoreColumn;
    @FXML
    private ListView<String> movesHistory;
    @FXML
    private TextArea gameChatArea;
    @FXML
    private TextField gameChatInput;
    @FXML
    private Button sendGameChatButton;
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressIndicator thinkingIndicator;
    @FXML
    private Label tilesLeftLabel;

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

        public String getPlayerName() {
            return playerName;
        }

        public int getScore() {
            return score;
        }
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

    public void setBoardCanvasClickHandler() {
        boardCanvas.addEventHandler(BoardCanvas.TileClickedEvent.TILE_CLICKED, this::handleBoardTileClick);
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
                roomNameLabel.setText("Room: " + roomName);
            } else {
                roomNameLabel.setText("Room: not selected");
            }


            if (gameState.getCurrentPlayer() != null) {
                isMyTurn = gameState.getCurrentPlayer().getId().equals(model.getPlayerId());
                turnLabel.setText(isMyTurn ? "Your turn!" :
                        "Turn: " + gameState.getCurrentPlayer().getName());
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


            tilesLeftLabel.setText("Tiles left: " + model.getTileBag().remainingTiles());


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

            Label scoreLabel = new Label("Score: " + player.getScore());
            scoreLabel.getStyleClass().add("player-score");


            if (player.isCurrentTurn()) {
                playerBox.getStyleClass().add("current-turn");
            }


            if (player.getId().equals(model.getPlayerId())) {
                nameLabel.getStyleClass().add("current-user");
                scoreLabel.getStyleClass().add("current-user-score");
            }


            if (!model.getGameState().isGameStarted() && player.isReady()) {
                Label readyLabel = new Label("✓ Ready");
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
            scoreLabel.setText("Your score: " + currentPlayer.getScore());
        }
    }

    private void updateMovesHistory(List<String> chatMessages) {

        List<String> moves = new ArrayList<>();
        for (String msg : chatMessages) {
            if (msg.contains("placed") || msg.contains("got") || msg.contains("turn")) {
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
            timerLabel.setText(String.format("Time: %02d:%02d", minutes, seconds));


            if (elapsedSeconds > 120) {
                timerLabel.setTextFill(Color.RED);
                statusLabel.setText("Attention! You have little time left for the turn!");
            } else if (elapsedSeconds > 60) {
                timerLabel.setTextFill(Color.ORANGE);
            } else {
                timerLabel.setTextFill(Color.WHITE);
            }
        } else {
            timerLabel.setText("Time: --:--");
            timerLabel.setTextFill(Color.WHITE);
        }
    }

    private void setupButtons() {

        sendGameChatButton.setOnAction(event -> handleGameChat());
        gameChatInput.setOnAction(event -> handleGameChat());


        exchangeButton.setOnAction(event -> handleExchangeTiles());


        skipButton.setOnAction(event -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Skip Turn");
            confirm.setHeaderText("Are you sure you want to skip the turn?");
            confirm.setContentText("You will not be able to make a move in this round.");

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                handleSkipTurn();
            }
        });
    }

    @FXML
    private void handleSubmitMove() {
        if (placedTiles.isEmpty()) {
            statusLabel.setText("First place tiles on the board!");
            showAlert("Error", "Tiles not placed", "Please place at least one tile on the board.");
            return;
        }


        if (!validateTilePlacement()) {
            statusLabel.setText("Tiles must be placed in a straight line!");
            showAlert("Error", "Invalid placement",
                    "Tiles must be placed in a horizontal or vertical line.");
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
            statusLabel.setText("Move sent for verification...");
            thinkingIndicator.setVisible(true);
            submitButton.setDisable(true);
            skipButton.setDisable(true);
            exchangeButton.setDisable(true);

            clearBoardAfterMove();
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
                if (positions.get(i)[1] != positions.get(i - 1)[1] + 1) {
                    return false;
                }
            }
        } else {

            positions.sort((a, b) -> Integer.compare(a[0], b[0]));
            for (int i = 1; i < positions.size(); i++) {
                if (positions.get(i)[0] != positions.get(i - 1)[0] + 1) {
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
            statusLabel.setText("Turn skipped");
            thinkingIndicator.setVisible(true);
            skipButton.setDisable(true);
            submitButton.setDisable(true);
            exchangeButton.setDisable(true);
            
            // Clear any placed tiles from the board
            clearBoardAfterMove();
        }
    }

    @FXML
    private void handleExchangeTiles() {
        if (selectedTilesForExchange.isEmpty()) {
            statusLabel.setText("Select tiles for exchange");
            showAlert("Attention", "Tiles not selected",
                    "Please select tiles for exchange by clicking on them.");
            return;
        }

        if (selectedTilesForExchange.size() > model.getTileBag().remainingTiles()) {
            statusLabel.setText("Not enough tiles in the bag for exchange!");
            showAlert("Error", "Not enough tiles",
                    "There are not enough tiles in the bag for such an exchange.");
            return;
        }

        List<String> tileIds = new ArrayList<>(selectedTilesForExchange);

        Message exchangeMsg = ProtocolParser.createTilesExchangeMessage(tileIds);

        if (networkHandler != null) {
            networkHandler.sendMessage(exchangeMsg);
            statusLabel.setText("Tile exchange request sent...");
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
            statusLabel.setText("Message sent");
        }
    }

    @FXML
    private void handleSurrender() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Surrender");
        confirm.setHeaderText("Are you sure you want to surrender?");
        confirm.setContentText("This action will end the game for you.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            Message surrenderMsg = ProtocolParser.createSurrenderMessage();

            if (networkHandler != null) {
                networkHandler.sendMessage(surrenderMsg);
                statusLabel.setText("You have surrendered");


                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("Game finished");
                info.setHeaderText("You have surrendered");
                info.setContentText("Game finished. You can create a new room or join another one.");
                info.showAndWait();


                ((javafx.stage.Stage) surrenderButton.getScene().getWindow()).close();
            }
        }
    }

    @FXML
    private void handleLeaveGame() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Leave Game");
        confirm.setHeaderText("Are you sure you want to leave?");
        confirm.setContentText("You will leave the current game.");

        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            Message leaveMsg = ProtocolParser.createLeaveRoomMessage();

            if (networkHandler != null) {
                networkHandler.sendMessage(leaveMsg);
                statusLabel.setText("Leaving the room...");


                javafx.stage.Stage stage = (javafx.stage.Stage) leaveButton.getScene().getWindow();
                stage.close();
            }
        }
    }

    private void handleTileDrop(TileView.TileDropEvent event) {
        if (!isMyTurn) {
            statusLabel.setText("It's not your turn now!");
            return;
        }

        TileBag.Tile tile = event.getTile();


        Point2D sceneCoords = new Point2D(event.getSceneX(), event.getSceneY());
        Point2D canvasLocalCoords = boardCanvas.sceneToLocal(sceneCoords);
        if (canvasLocalCoords == null) {
            statusLabel.setText("Drop tile on the board!");
            return;
        }

        double cellX = canvasLocalCoords.getX();
        double cellY = canvasLocalCoords.getY();

        int col = (int) (cellX / 40);
        int row = (int) (cellY / 40);

        if (row >= 0 && row < 15 && col >= 0 && col < 15) {
            if (model.getGameState().getCell(row, col).hasTile()) {
                statusLabel.setText("Cell is already occupied!");
                showAlert("Error", "Cell occupied",
                        "The selected cell already contains a tile. Choose another cell.");
                return;
            }


            GameState gameState = model.getGameState();
            if (isFirstMove(gameState) && !(row == 7 && col == 7)) {
                statusLabel.setText("The first move must start from the central cell!");
                showAlert("Error", "Invalid placement",
                        "The first move must start from the central cell (H8).");
                return;
            }


            placedTiles.put(tile.getId(), new int[]{row, col});

            // Add tile to GameState board so it's displayed
            gameState.placeTile(row, col, tile);

            rackView.removeTile(tile.getId());

            // Redraw the board to show the placed tile
            boardCanvas.drawBoard();

            statusLabel.setText("Tile '" + Character.toUpperCase(tile.getLetter()) +
                    "' placed at [" + (char) ('A' + col) + "," + (row + 1) + "]");


            submitButton.setDisable(false);


            selectedTilesForExchange.remove(tile.getId());
        } else {
            statusLabel.setText("Tile '" + Character.toUpperCase(tile.getLetter()) + "' returned to the rack");
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
                statusLabel.setText("Tile removed from exchange");
            } else {

                if (!placedTiles.containsKey(tileId)) {
                    selectedTilesForExchange.add(tileId);
                    clickedTile.setSelected(true);
                    statusLabel.setText("Tile selected for exchange");
                } else {
                    statusLabel.setText("Tile is already placed on the board!");
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
            gameOverAlert.setTitle("Game finished");


            Player winner = gameState.getPlayers().stream()
                    .max(Comparator.comparingInt(Player::getScore))
                    .orElse(null);

            if (winner != null) {
                if (winner.getId().equals(model.getPlayerId())) {
                    gameOverAlert.setHeaderText("Congratulations! You won!");
                    gameOverAlert.setContentText("Your result: " + winner.getScore() + " points");
                } else {
                    gameOverAlert.setHeaderText("Game finished!");
                    gameOverAlert.setContentText("Winner: " + winner.getName() +
                            " with result " + winner.getScore() + " points\n" +
                            "Your result: " +
                            gameState.getPlayerById(model.getPlayerId()).getScore() + " points");
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
            confirm.setTitle("Cancel Move");
            confirm.setHeaderText("Are you sure you want to cancel the move?");
            confirm.setContentText("All placed tiles will be returned to the rack.");

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {

                for (Map.Entry<String, int[]> entry : placedTiles.entrySet()) {
                    String tileId = entry.getKey();
                    int[] pos = entry.getValue();
                    TileBag.Tile tile = getTileById(tileId);
                    if (tile != null) {
                        // Remove tile from GameState board
                        model.getGameState().placeTile(pos[0], pos[1], null);
                        rackView.addTile(tile);
                    }
                }

                placedTiles.clear();
                boardCanvas.drawBoard();
                statusLabel.setText("Move canceled. All tiles returned to the rack.");
                submitButton.setDisable(true);
            }
        }
    }

    private void handleBoardTileClick(BoardCanvas.TileClickedEvent event) {
        if (!isMyTurn) {
            statusLabel.setText("It's not your turn now!");
            return;
        }

        TileBag.Tile clickedTile = event.getTile();
        int row = event.getRow();
        int col = event.getCol();

        // Check if this tile was placed by the current player in this turn
        String tileId = clickedTile.getId();
        if (placedTiles.containsKey(tileId)) {
            // Remove tile from board and return to rack
            model.getGameState().placeTile(row, col, null);
            placedTiles.remove(tileId);
            rackView.addTile(clickedTile);
            boardCanvas.drawBoard();

            statusLabel.setText("Tile '" + Character.toUpperCase(clickedTile.getLetter()) +
                    "' returned to the rack");

            submitButton.setDisable(placedTiles.isEmpty());
        }
    }

    private void clearBoardAfterMove() {
        // Remove all placed tiles from the board
        for (Map.Entry<String, int[]> entry : placedTiles.entrySet()) {
            int[] pos = entry.getValue();
            model.getGameState().placeTile(pos[0], pos[1], null);
        }
        placedTiles.clear();
        boardCanvas.drawBoard();
    }
}