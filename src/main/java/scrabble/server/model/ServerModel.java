package scrabble.server.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import scrabble.server.network.ClientHandler;
import scrabble.utils.DictionaryLoader;

public class ServerModel {
    private final Map<String, GameRoom> rooms;
    private final Map<String, ClientHandler> connectedClients;
    private final Set<String> dictionary;

    public ServerModel() {
        this.rooms = new ConcurrentHashMap<>();
        this.connectedClients = new ConcurrentHashMap<>();
        this.dictionary = DictionaryLoader.loadDictionary();
    }

    public synchronized GameRoom createRoom(String roomName, int maxPlayers, String creatorId) {
        String roomId = generateRoomId(roomName);
        GameRoom room = new GameRoom(roomId, roomName, maxPlayers, creatorId);
        rooms.put(roomId, room);
        return room;
    }

    public synchronized boolean joinRoom(String roomId, String playerId) {
        GameRoom room = rooms.get(roomId);
        if (room != null && room.canJoin()) {
            return room.addPlayer(playerId);
        }
        return false;
    }

    public synchronized void leaveRoom(String roomId, String playerId) {
        GameRoom room = rooms.get(roomId);
        if (room != null) {
            room.removePlayer(playerId);
            if (room.isEmpty()) {
                rooms.remove(roomId);
            }
        }
    }

    public synchronized List<String> getAvailableRooms() {
        List<String> available = new ArrayList<>();
        for (GameRoom room : rooms.values()) {
            if (room.canJoin()) {
                available.add(room.getId() + " - " + room.getName() +
                        " (" + room.getPlayerCount() + "/" + room.getMaxPlayers() + ")");
            }
        }
        return available;
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public void registerClient(String clientId, ClientHandler handler) {
        connectedClients.put(clientId, handler);
    }

    public void unregisterClient(String clientId) {
        connectedClients.remove(clientId);
        // Удаляем игрока из всех комнат
        for (GameRoom room : rooms.values()) {
            room.removePlayer(clientId);
        }
    }

    public ClientHandler getClientHandler(String clientId) {
        return connectedClients.get(clientId);
    }

    public Collection<ClientHandler> getAllClientHandlers() {
        return connectedClients.values();
    }

    public boolean isValidWord(String word) {
        return dictionary.contains(word.toLowerCase());
    }

    private String generateRoomId(String roomName) {
        return roomName.replaceAll("\\s+", "_") + "_" + System.currentTimeMillis();
    }
}