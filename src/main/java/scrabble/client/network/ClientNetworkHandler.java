package scrabble.client.network;

import scrabble.protocol.Message;
import scrabble.client.model.ClientModel;
import scrabble.client.model.GameState;
import scrabble.client.model.Player;
import javafx.application.Platform;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import scrabble.protocol.ProtocolParser;

public class ClientNetworkHandler {
    private SocketChannel socketChannel;
    private ClientModel model;
    private ExecutorService executor;
    private BlockingQueue<Message> outgoingMessages;
    private volatile boolean running;

    public ClientNetworkHandler(ClientModel model) {
        this.model = model;
        this.executor = Executors.newFixedThreadPool(2);
        this.outgoingMessages = new ArrayBlockingQueue<>(100);
        this.running = false;
    }

    public boolean connect(String host, int port, String playerName) {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(true);
            socketChannel.connect(new InetSocketAddress(host, port));

            running = true;
            startNetworkThreads();

            Message connectMsg = ProtocolParser.createConnectMessage(playerName);
            sendMessage(connectMsg);

            Platform.runLater(() -> {
                model.setStatusMessage("Connected to server");
                model.setConnectedToServer(true);
            });

            return true;

        } catch (Exception e) {
            model.setStatusMessage("Connecting error: " + e.getMessage());
            return false;
        }
    }

    private void startNetworkThreads() {
        
        executor.submit(() -> {
            try {
                while (running) {
                    Message message = outgoingMessages.take();
                    sendMessageInternal(message);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                disconnect();
            }
        });

        
        executor.submit(() -> {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            StringBuilder messageBuilder = new StringBuilder();

            try {
                while (running) {
                    buffer.clear();
                    int bytesRead = socketChannel.read(buffer);

                    if (bytesRead == -1) {
                        disconnect();
                        break;
                    }

                    if (bytesRead > 0) {
                        buffer.flip();
                        String chunk = StandardCharsets.UTF_8.decode(buffer).toString();
                        messageBuilder.append(chunk);

                        
                        String data = messageBuilder.toString();
                        int processed = 0;
                        int start = 0;

                        while (true) {
                            int jsonStart = data.indexOf('{', start);
                            if (jsonStart == -1) break;

                            
                            int braceCount = 0;
                            boolean inString = false;
                            int jsonEnd = -1;

                            for (int i = jsonStart; i < data.length(); i++) {
                                char c = data.charAt(i);

                                if (c == '"' && (i == 0 || data.charAt(i-1) != '\\')) {
                                    inString = !inString;
                                } else if (!inString) {
                                    if (c == '{') braceCount++;
                                    else if (c == '}') {
                                        braceCount--;
                                        if (braceCount == 0) {
                                            jsonEnd = i + 1;
                                            break;
                                        }
                                    }
                                }
                            }

                            if (jsonEnd != -1) {
                                String json = data.substring(jsonStart, jsonEnd);
                                processIncomingMessage(json);
                                processed++;
                                start = jsonEnd;
                            } else {
                                break; 
                            }
                        }

                        if (start < data.length()) {
                            String remaining = data.substring(start);
                            messageBuilder = new StringBuilder(remaining);
                        } else {
                            messageBuilder = new StringBuilder();
                        }
                    }

                    Thread.sleep(10);
                }
            } catch (Exception e) {
                disconnect();
            }
        });
    }

    private void sendMessageInternal(Message message) {
        try {
            String json = message.toJson() + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));

            while (buffer.hasRemaining()) {
                socketChannel.write(buffer);
            }

        } catch (IOException e) {
            disconnect();
        }
    }

    public void sendMessage(Message message) {
        if (running) {
            try {
                outgoingMessages.put(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void processIncomingMessage(String json) {
        try {
            Message message = Message.fromJson(json);
            Platform.runLater(() -> handleMessage(message));
        } catch (Exception e) {
            System.err.println("Error while processing message: " + e.getMessage());
        }
    }

    private void handleMessage(Message message) {
        switch (message.getType()) {
            case CONNECT:
                handleConnectResponse(message);
                break;
            case ROOM_LIST:
                handleRoomList(message);
                break;
            case CREATE_ROOM:
                handleCreateRoomResponse(message);
                break;
            case JOIN_ROOM:
                handleJoinRoomResponse(message);
                break;
            case PLAYER_JOINED:
                handlePlayerJoined(message);
                break;
            case PLAYER_LEFT:
                handlePlayerLeft(message);
                break;
            case PLAYER_READY:
                handlePlayerReady(message);
                break;
            case ALL_PLAYERS_READY:
                handleAllPlayersReady(message);
                break;
            case GAME_START:
                handleGameStart(message);
                break;
            case GAME_STATE:
                handleGameState(message);
                break;
            case PLAYER_MOVE:
                handlePlayerMove(message);
                break;
            case CHAT_MESSAGE:
                handleChatMessage(message);
                break;
            case GAME_OVER:
                handleGameOver(message);
                break;
            case ERROR:
                handleErrorMessage(message);
                break;
            default:
                System.out.println("Received a message with type: " + message.getType());
        }
    }

    private void handleConnectResponse(Message message) {
        String playerId = (String) message.get("playerId");
        model.setPlayerId(playerId);
        model.setStatusMessage("Identified as: " + model.getPlayerName());
    }

    private void handleRoomList(Message message) {
        List<String> rooms = (List<String>) message.get("rooms");
        if (rooms != null) {
            Platform.runLater(() -> {
                model.clearAvailableRooms();
                for (String room : rooms) {
                    model.addAvailableRoom(room);
                }
            });
        }
    }

    private void handleCreateRoomResponse(Message message) {
        String roomId = (String) message.get("roomId");
        String roomName = (String) message.get("roomName");
        model.setCurrentRoomId(roomId);
        model.setStatusMessage("Room created: " + roomName);

        GameState gameState = new GameState();
        gameState.setCurrentRoomId(roomId);

        Player self = new Player(model.getPlayerId(), model.getPlayerName());
        gameState.addPlayer(self);

        model.setGameState(gameState);
    }

    private void handleJoinRoomResponse(Message message) {
        String roomId = (String) message.get("roomId");
        String roomName = (String) message.get("roomName");
        List<String> playerIds = (List<String>) message.get("players");

        model.setCurrentRoomId(roomId);
        model.setStatusMessage("Joined to room: " + roomName);

        GameState gameState = new GameState();
        gameState.setCurrentRoomId(roomId);

        if (playerIds != null) {
            for (String playerId : playerIds) {
                if (!playerId.equals(model.getPlayerId())) {
                    Player player = new Player(playerId, "Player " + playerId.substring(0, 4));
                    gameState.addPlayer(player);
                }
            }
        }

        Player self = new Player(model.getPlayerId(), model.getPlayerName());
        gameState.addPlayer(self);

        model.setGameState(gameState);
    }

    private void handlePlayerJoined(Message message) {
        String playerId = (String) message.get("playerId");
        String playerName = (String) message.get("playerName");

        GameState gameState = new GameState(new GameState(model.getGameState()));
        Player player = new Player(playerId, playerName);
        gameState.addPlayer(player);
        gameState.addChatMessage(playerName + " joined the game");
        model.setGameState(gameState);

        model.setStatusMessage(playerName + " joined the room");
    }

    private void handlePlayerLeft(Message message) {
        String playerId = (String) message.get("playerId");

        GameState gameState = new GameState(new GameState(model.getGameState()));
        Player player = gameState.getPlayerById(playerId);
        if (player != null) {
            gameState.removePlayer(playerId);
            gameState.addChatMessage(player.getName() + " leaved the game");
            model.setGameState(gameState);
            model.setStatusMessage(player.getName() + " leaved the room");
        }
    }

    private void handlePlayerReady(Message message) {
        String playerId = (String) message.get("playerId");

        GameState gameState = new GameState(new GameState(model.getGameState()));
        Player player = gameState.getPlayerById(playerId);
        if (player != null) {
            player.setReady(true);
            gameState.addChatMessage(player.getName() + " ready to the game");
            model.setGameState(gameState);
        }
    }

    private void handleAllPlayersReady(Message message) {
        GameState gameState = new GameState(new GameState(model.getGameState()));
        gameState.addChatMessage("All players are ready! Wait for the game to start...");
        model.setGameState(gameState);
        model.setStatusMessage("All players are ready");
    }

    private void handleGameStart(Message message) {
        System.out.println("Received a message GAME_START");

        String currentPlayerId = (String) message.get("currentPlayer");
        System.out.println("Current player: " + currentPlayerId);

        GameState gameState = new GameState(model.getGameState());

        gameState.setGameStarted(true);
        Player currentPlayer = null;
        for (Player player : gameState.getPlayers()) {
            if (player.getId().equals(currentPlayerId)) {
                player.setCurrentTurn(true);
                currentPlayer = player;
                gameState.setCurrentPlayer(player);
                System.out.println("Current player has been found: " + player.getName());
            } else {
                player.setCurrentTurn(false);
            }
        }

        gameState.addChatMessage("Game has started!");
        if (currentPlayer != null) {
            gameState.addChatMessage("Goes first: " + currentPlayer.getName());
        }

        fillPlayerRacks();
        model.setGameState(gameState);
    }

    private void handleGameState(Message message) {
        String currentPlayerId = (String) message.get("currentPlayer");

        GameState gameState = new GameState(model.getGameState());

        for (Player player : gameState.getPlayers()) {
            player.setCurrentTurn(player.getId().equals(currentPlayerId));
            if (player.getId().equals(currentPlayerId)) {
                gameState.setCurrentPlayer(player);
            }
        }

        model.setGameState(gameState);
    }

    private void handlePlayerMove(Message message) {
        String playerId = (String) message.get("playerId");
        String word = (String) message.get("word");
        int score = ((Double) message.get("score")).intValue();
        int row = ((Double) message.get("row")).intValue();
        int col = ((Double) message.get("col")).intValue();
        boolean horizontal = (Boolean) message.get("horizontal");

        GameState gameState = new GameState(model.getGameState());
        Player player = gameState.getPlayerById(playerId);

        if (player != null) {
            player.addScore(score);
            gameState.addChatMessage(player.getName() + " placed a word '" + word +
                    "' and received " + score + " scores");

            if (playerId.equals(model.getPlayerId())) {
                model.setStatusMessage("You received " + score + " scores for the word '" + word + "'");
            }
        }

        model.setGameState(gameState);
    }

    private void handleChatMessage(Message message) {
        String content = (String) message.get("content");
        String sender = message.getSender();

        GameState gameState = new GameState(model.getGameState());
        gameState.addChatMessage(content);
        model.setGameState(gameState);
    }

    private void handleGameOver(Message message) {
        String winnerId = (String) message.get("winnerId");
        Map<String, Integer> finalScores = (Map<String, Integer>) message.get("finalScores");

        GameState gameState = new GameState(model.getGameState());
        gameState.setGameFinished(true);

        Player winner = gameState.getPlayerById(winnerId);
        if (winner != null) {
            gameState.addChatMessage("The game is over! Winner: " + winner.getName());
        } else {
            gameState.addChatMessage("The game is over!");
        }

        model.setGameState(gameState);
        model.setStatusMessage("The game is over");
    }

    private void handleErrorMessage(Message message) {
        String error = (String) message.get("error");
        model.setStatusMessage("Error: " + error);
    }

    private void fillPlayerRacks() {
        GameState gameState = new GameState(model.getGameState());

        for (Player player : gameState.getPlayers()) {
            for (int i = 0; i < 7; i++) {
                scrabble.utils.TileBag.Tile tile = model.getTileBag().drawTile();
                if (tile != null) {
                    player.addToRack(tile);
                }
            }
        }

        model.setGameState(gameState);
    }

    public void disconnect() {
        running = false;
        executor.shutdownNow();

        try {
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close();
            }
        } catch (IOException e) {
            
        }

        Platform.runLater(() -> {
            model.setStatusMessage("Disconnected from the server");
            model.setConnectedToServer(false);
            model.setCurrentRoomId("");
            model.clearAvailableRooms();
        });
    }
}