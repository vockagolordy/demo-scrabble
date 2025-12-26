// scrabble/client/network/ClientNetworkHandler.java

package scrabble.client.network;

import scrabble.protocol.Message;
import scrabble.protocol.MessageType;
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
import com.google.gson.reflect.TypeToken;
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
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(host, port));

            // Ждем подключения
            int attempts = 0;
            while (!socketChannel.finishConnect() && attempts < 50) {
                Thread.sleep(100);
                attempts++;
            }

            if (!socketChannel.isConnected()) {
                Platform.runLater(() ->
                        model.setStatusMessage("Не удалось подключиться к серверу"));
                return false;
            }

            running = true;
            startNetworkThreads();

            // Отправляем сообщение о подключении
            Message connectMsg = ProtocolParser.createConnectMessage(playerName);
            sendMessage(connectMsg);

            Platform.runLater(() -> {
                model.setStatusMessage("Подключено к серверу");
                model.setConnectedToServer(true);
            });

            return true;

        } catch (Exception e) {
            Platform.runLater(() ->
                    model.setStatusMessage("Ошибка подключения: " + e.getMessage()));
            return false;
        }
    }

    private void startNetworkThreads() {
        // Поток для отправки сообщений
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

        // Поток для приема сообщений
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

                        // Обработка JSON сообщений
                        String data = messageBuilder.toString();
                        if (data.contains("\n")) {
                            String[] messages = data.split("\n");
                            for (int i = 0; i < messages.length - 1; i++) {
                                processIncomingMessage(messages[i].trim());
                            }
                            messageBuilder = new StringBuilder(messages[messages.length - 1]);
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
            System.err.println("Ошибка обработки сообщения: " + e.getMessage());
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
                System.out.println("Получено сообщение типа: " + message.getType());
        }
    }

    private void handleConnectResponse(Message message) {
        String playerId = (String) message.get("playerId");
        model.setPlayerId(playerId);
        model.setStatusMessage("Идентифицирован как: " + model.getPlayerName());
    }

    private void handleRoomList(Message message) {
        List<String> rooms = (List<String>) message.get("rooms");
        if (rooms != null) {
            model.clearAvailableRooms();
            for (String room : rooms) {
                model.addAvailableRoom(room);
            }
        }
    }

    private void handleCreateRoomResponse(Message message) {
        String roomId = (String) message.get("roomId");
        String roomName = (String) message.get("roomName");
        model.setCurrentRoomId(roomId);
        model.setStatusMessage("Создана комната: " + roomName);
    }

    private void handleJoinRoomResponse(Message message) {
        String roomId = (String) message.get("roomId");
        String roomName = (String) message.get("roomName");
        List<String> playerIds = (List<String>) message.get("players");

        model.setCurrentRoomId(roomId);
        model.setStatusMessage("Присоединились к комнате: " + roomName);

        // Обновляем состояние игры
        GameState gameState = model.getGameState();
        gameState.setCurrentRoomId(roomId);

        // Добавляем игроков
        if (playerIds != null) {
            for (String playerId : playerIds) {
                if (!playerId.equals(model.getPlayerId())) {
                    Player player = new Player(playerId, "Игрок " + playerId.substring(0, 4));
                    gameState.addPlayer(player);
                }
            }
        }

        // Добавляем себя
        Player self = new Player(model.getPlayerId(), model.getPlayerName());
        gameState.addPlayer(self);

        model.setGameState(gameState);
    }

    private void handlePlayerJoined(Message message) {
        String playerId = (String) message.get("playerId");
        String playerName = (String) message.get("playerName");

        GameState gameState = model.getGameState();
        Player player = new Player(playerId, playerName);
        gameState.addPlayer(player);
        gameState.addChatMessage(playerName + " присоединился к игре");
        model.setGameState(gameState);

        model.setStatusMessage(playerName + " присоединился к комнате");
    }

    private void handlePlayerLeft(Message message) {
        String playerId = (String) message.get("playerId");

        GameState gameState = model.getGameState();
        Player player = gameState.getPlayerById(playerId);
        if (player != null) {
            gameState.removePlayer(playerId);
            gameState.addChatMessage(player.getName() + " покинул игру");
            model.setGameState(gameState);
            model.setStatusMessage(player.getName() + " покинул комнату");
        }
    }

    private void handlePlayerReady(Message message) {
        String playerId = (String) message.get("playerId");

        GameState gameState = model.getGameState();
        Player player = gameState.getPlayerById(playerId);
        if (player != null) {
            player.setReady(true);
            gameState.addChatMessage(player.getName() + " готов к игре");
            model.setGameState(gameState);
        }
    }

    private void handleAllPlayersReady(Message message) {
        GameState gameState = model.getGameState();
        gameState.addChatMessage("Все игроки готовы! Ожидайте начала игры...");
        model.setGameState(gameState);
        model.setStatusMessage("Все игроки готовы");
    }

    private void handleGameStart(Message message) {
        String currentPlayerId = (String) message.get("currentPlayer");

        GameState gameState = model.getGameState();
        gameState.setGameStarted(true);

        // Устанавливаем текущего игрока
        for (Player player : gameState.getPlayers()) {
            player.setCurrentTurn(player.getId().equals(currentPlayerId));
            if (player.getId().equals(currentPlayerId)) {
                gameState.setCurrentPlayer(player);
            }
        }

        gameState.addChatMessage("Игра началась!");
        gameState.addChatMessage("Первым ходит: " + gameState.getCurrentPlayer().getName());

        // Заполняем полки фишками
        fillPlayerRacks();

        model.setGameState(gameState);
        model.setStatusMessage("Игра началась!");
    }

    private void handleGameState(Message message) {
        String currentPlayerId = (String) message.get("currentPlayer");

        GameState gameState = model.getGameState();

        // Обновляем текущего игрока
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

        GameState gameState = model.getGameState();
        Player player = gameState.getPlayerById(playerId);

        if (player != null) {
            player.addScore(score);
            gameState.addChatMessage(player.getName() + " выложил(а) слово '" + word +
                    "' и получил(а) " + score + " очков");

            // Обновляем очки на интерфейсе
            if (playerId.equals(model.getPlayerId())) {
                model.setStatusMessage("Вы получили " + score + " очков за слово '" + word + "'");
            }
        }

        model.setGameState(gameState);
    }

    private void handleChatMessage(Message message) {
        String content = (String) message.get("content");
        String sender = message.getSender();

        GameState gameState = model.getGameState();
        gameState.addChatMessage(content);
        model.setGameState(gameState);
    }

    private void handleGameOver(Message message) {
        String winnerId = (String) message.get("winnerId");
        Map<String, Integer> finalScores = (Map<String, Integer>) message.get("finalScores");

        GameState gameState = model.getGameState();
        gameState.setGameFinished(true);

        Player winner = gameState.getPlayerById(winnerId);
        if (winner != null) {
            gameState.addChatMessage("Игра окончена! Победитель: " + winner.getName());
        } else {
            gameState.addChatMessage("Игра окончена!");
        }

        model.setGameState(gameState);
        model.setStatusMessage("Игра завершена");
    }

    private void handleErrorMessage(Message message) {
        String error = (String) message.get("error");
        model.setStatusMessage("Ошибка: " + error);
    }

    private void fillPlayerRacks() {
        GameState gameState = model.getGameState();

        for (Player player : gameState.getPlayers()) {
            // Заполняем полку 7 фишками
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
            // Игнорируем ошибку закрытия
        }

        Platform.runLater(() -> {
            model.setStatusMessage("Отключено от сервера");
            model.setConnectedToServer(false);
            model.setCurrentRoomId("");
            model.clearAvailableRooms();
        });
    }
}