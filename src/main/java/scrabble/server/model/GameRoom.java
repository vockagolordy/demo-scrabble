package scrabble.server.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameRoom {
    private final String id;
    private final String name;
    private final int maxPlayers;
    private final Set<String> playerIds;
    private final Set<String> readyPlayers;
    private final String creatorId;
    private boolean gameStarted;
    private String currentPlayerId;

    public GameRoom(String id, String name, int maxPlayers, String creatorId) {
        this.id = id;
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.creatorId = creatorId;
        this.playerIds = ConcurrentHashMap.newKeySet();
        this.readyPlayers = ConcurrentHashMap.newKeySet();
        this.gameStarted = false;
        this.playerIds.add(creatorId);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getMaxPlayers() { return maxPlayers; }
    public String getCreatorId() { return creatorId; }
    public boolean isGameStarted() { return gameStarted; }
    public String getCurrentPlayerId() { return currentPlayerId; }

    public Set<String> getPlayerIds() {
        return new HashSet<>(playerIds);
    }

    public int getPlayerCount() {
        return playerIds.size();
    }

    public boolean canJoin() {
        return !gameStarted && playerIds.size() < maxPlayers;
    }

    public boolean isEmpty() {
        return playerIds.isEmpty();
    }

    public synchronized boolean addPlayer(String playerId) {
        if (canJoin()) {
            playerIds.add(playerId);
            return true;
        }
        return false;
    }

    public synchronized void removePlayer(String playerId) {
        playerIds.remove(playerId);
        readyPlayers.remove(playerId);

        if (playerId.equals(creatorId) && !playerIds.isEmpty()) {
            
            String newCreator = playerIds.iterator().next();
            
        }
    }

    public synchronized void playerReady(String playerId) {
        readyPlayers.add(playerId);
    }

    public synchronized boolean allPlayersReady() {
        return !playerIds.isEmpty() && readyPlayers.size() == playerIds.size();
    }

    public synchronized boolean startGame() {
        if (playerIds.size() >= 2 && allPlayersReady() && !gameStarted) {
            gameStarted = true;
            List<String> playersList = new ArrayList<>(playerIds);
            currentPlayerId = playersList.get(new Random().nextInt(playersList.size()));
            return true;
        }
        return false;
    }

    public synchronized void nextTurn() {
        List<String> playersList = new ArrayList<>(playerIds);
        int currentIndex = playersList.indexOf(currentPlayerId);
        int nextIndex = (currentIndex + 1) % playersList.size();
        currentPlayerId = playersList.get(nextIndex);
    }

    public boolean isPlayerReady(String playerId) {
        return readyPlayers.contains(playerId);
    }
}