// scrabble/client/model/ClientModel.java

package scrabble.client.model;

import javafx.beans.property.*;
import javafx.collections.ObservableList;
import java.util.List;
import java.util.ArrayList;
import scrabble.utils.TileBag;

public class ClientModel {
    private final ObjectProperty<GameState> gameState;
    private final StringProperty playerName;
    private final StringProperty statusMessage;
    private final ListProperty<String> availableRooms;
    private final BooleanProperty connectedToServer;
    private TileBag tileBag;
    private String playerId;
    private String currentRoomId;

    public ClientModel() {
        this.gameState = new SimpleObjectProperty<>(new GameState());
        this.playerName = new SimpleStringProperty("");
        this.statusMessage = new SimpleStringProperty("Не подключено");
        this.availableRooms = new SimpleListProperty<>(javafx.collections.FXCollections.observableArrayList());
        this.connectedToServer = new SimpleBooleanProperty(false);
        this.tileBag = new TileBag();
        this.playerId = "";
        this.currentRoomId = "";
    }

    public ObjectProperty<GameState> gameStateProperty() { return gameState; }
    public StringProperty playerNameProperty() { return playerName; }
    public StringProperty statusMessageProperty() { return statusMessage; }
    public ListProperty<String> availableRoomsProperty() { return availableRooms; }
    public BooleanProperty connectedToServerProperty() { return connectedToServer; }

    public GameState getGameState() { return gameState.get(); }
    public void setGameState(GameState gameState) { this.gameState.set(gameState); }

    public String getPlayerName() { return playerName.get(); }
    public void setPlayerName(String playerName) { this.playerName.set(playerName); }

    public String getStatusMessage() { return statusMessage.get(); }
    public void setStatusMessage(String statusMessage) { this.statusMessage.set(statusMessage); }

    public ObservableList<String> getAvailableRooms() { return availableRooms.get(); }
    public void setAvailableRooms(List<String> rooms) {
        availableRooms.setAll(rooms);
    }

    public boolean isConnectedToServer() { return connectedToServer.get(); }
    public void setConnectedToServer(boolean connected) { this.connectedToServer.set(connected); }

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }

    public String getCurrentRoomId() { return currentRoomId; }
    public void setCurrentRoomId(String currentRoomId) { this.currentRoomId = currentRoomId; }

    public TileBag getTileBag() { return tileBag; }

    public void addAvailableRoom(String roomInfo) {
        if (!availableRooms.contains(roomInfo)) {
            availableRooms.add(roomInfo);
        }
    }

    public void clearAvailableRooms() {
        availableRooms.clear();
    }
}