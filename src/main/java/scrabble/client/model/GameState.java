package scrabble.client.model;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import scrabble.utils.TileBag.Tile;

public class GameState {
    public static class BoardCell {
        private Tile tile;
        private String multiplier; // "DW", "TW", "DL", "TL", null
        private int row;
        private int col;

        public BoardCell(int row, int col, String multiplier) {
            this.row = row;
            this.col = col;
            this.multiplier = multiplier;
        }

        public Tile getTile() { return tile; }
        public void setTile(Tile tile) { this.tile = tile; }
        public boolean hasTile() { return tile != null; }
        public String getMultiplier() { return multiplier; }
        public int getRow() { return row; }
        public int getCol() { return col; }
    }

    private BoardCell[][] board;
    private ObservableList<Player> players = FXCollections.observableArrayList();
    private ObjectProperty<Player> currentPlayer = new SimpleObjectProperty<>();
    private BooleanProperty gameStarted = new SimpleBooleanProperty(false);
    private BooleanProperty gameFinished = new SimpleBooleanProperty(false);
    private StringProperty currentRoomId = new SimpleStringProperty("");
    private ObservableList<String> chatMessages = FXCollections.observableArrayList();
    private Map<String, Object> gameSettings = new HashMap<>();

    public GameState() {
        initializeBoard();
        players = FXCollections.observableArrayList();
        chatMessages = FXCollections.observableArrayList();
        gameSettings = new HashMap<>();
        gameStarted.set(false);
        gameFinished.set(false);
    }

    public GameState(GameState gameState) {
        this();
        players = gameState.players;
        chatMessages = gameState.chatMessages;
        gameSettings = gameState.gameSettings;
        gameStarted = gameState.gameStarted;
        gameFinished = gameState.gameFinished;
    }

    private void initializeBoard() {
        board = new BoardCell[15][15];

        
        for (int i = 0; i < 15; i++) {
            for (int j = 0; j < 15; j++) {
                String multiplier = null;

                
                if ((i == 0 || i == 14) && (j == 0 || j == 14)) {
                    multiplier = "TW";
                }
                
                else if (i == 7 && j == 7) {
                    multiplier = "DW";
                }
                
                else if (i == j || i == 14 - j) {
                    multiplier = "DL";
                }

                board[i][j] = new BoardCell(i, j, multiplier);
            }
        }
    }

    public BoardCell[][] getBoard() { return board; }

    public ObservableList<Player> getPlayers() {
        return players;
    }

    public Player getCurrentPlayer() {
        return currentPlayer.get();
    }

    public ObjectProperty<Player> currentPlayerProperty() {
        return currentPlayer;
    }

    public boolean isGameStarted() {
        return gameStarted.get();
    }

    public BooleanProperty gameStartedProperty() {
        return gameStarted;
    }

    public boolean isGameFinished() {
        return gameFinished.get();
    }

    public BooleanProperty gameFinishedProperty() {
        return gameFinished;
    }

    public String getCurrentRoomId() {
        return currentRoomId.get();
    }

    public StringProperty currentRoomIdProperty() {
        return currentRoomId;
    }

    public List<String> getChatMessages() { return chatMessages; }
    public Map<String, Object> getGameSettings() { return gameSettings; }

    public void setBoard(BoardCell[][] board) { this.board = board; }
    public void setPlayers(List<Player> players) { this.players.addAll(players); }

    public void setCurrentPlayer(Player currentPlayer) {
        this.currentPlayer.set(currentPlayer);
    }

    public void setGameStarted(boolean gameStarted) {
        this.gameStarted.set(gameStarted);
    }

    public void setGameFinished(boolean gameFinished) {
        this.gameFinished.set(gameFinished);
    }

    public void setCurrentRoomId(String currentRoomId) {
        this.currentRoomId.set(currentRoomId);
    }

    public void addChatMessage(String message) {
        chatMessages.add(message);
        if (chatMessages.size() > 100) {
            chatMessages.remove(0);
        }
    }

    public void addPlayer(Player player) {
        players.removeIf(p -> p.getId().equals(player.getId()));
        players.add(player);
    }

    public void removePlayer(String playerId) {
        players.removeIf(p -> p.getId().equals(playerId));
    }

    public Player getPlayerById(String playerId) {
        return players.stream()
                .filter(p -> p.getId().equals(playerId))
                .findFirst()
                .orElse(null);
    }

    public BoardCell getCell(int row, int col) {
        if (row >= 0 && row < 15 && col >= 0 && col < 15) {
            return board[row][col];
        }
        return null;
    }

    public void placeTile(int row, int col, Tile tile) {
        if (row >= 0 && row < 15 && col >= 0 && col < 15) {
            board[row][col].setTile(tile);
        }
    }
}