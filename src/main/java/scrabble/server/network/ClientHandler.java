package scrabble.server.network;

import scrabble.client.model.GameState;
import scrabble.protocol.ProtocolParser;
import scrabble.server.model.ServerModel;
import scrabble.server.model.GameRoom;
import scrabble.protocol.Message;
import scrabble.server.model.WordChecker;
import scrabble.utils.TileBag;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ClientHandler {
    private final SocketChannel channel;
    private final String clientId;
    private final ServerModel model;
    private String playerName;
    private String currentRoomId;

    public ClientHandler(SocketChannel channel, String clientId, ServerModel model) {
        this.channel = channel;
        this.clientId = clientId;
        this.model = model;
    }

    public void processMessage(String jsonMessage) {
        try {
            Message message = Message.fromJson(jsonMessage);
            message.setSender(clientId);

            switch (message.getType()) {
                case CONNECT:
                    handleConnect(message);
                    break;
                case CREATE_ROOM:
                    handleCreateRoom(message);
                    break;
                case JOIN_ROOM:
                    handleJoinRoom(message);
                    break;
                case LEAVE_ROOM:
                    handleLeaveRoom(message);
                    break;
                case PLAYER_READY:
                    handlePlayerReady(message);
                    break;
                case GAME_START:
                    handleGameStart(message);
                    break;
                case PLAYER_MOVE:
                    handlePlayerMove(message);
                    break;
                case CHAT_MESSAGE:
                    handleChatMessage(message);
                    break;
                case DISCONNECT:
                    handleDisconnect(message);
                    break;
                default:
                    sendErrorMessage("Неизвестный тип сообщения");
            }
        } catch (Exception e) {
            sendErrorMessage("Ошибка обработки сообщения: " + e.getMessage());
        }
    }

    private void handleConnect(Message message) {
        this.playerName = (String) message.get("playerName");

        Message response = ProtocolParser.createConnectResponseMessage(clientId, "connected");
        sendMessage(response);

        
        sendRoomList();
    }

    private void handleCreateRoom(Message message) {
        String roomName = (String) message.get("roomName");
        int maxPlayers = ((Double) message.get("maxPlayers")).intValue();

        GameRoom room = model.createRoom(roomName, maxPlayers, clientId);
        currentRoomId = room.getId();

        Message response = ProtocolParser.createCreateRoomResponseMessage(room.getId(), room.getName());
        sendMessage(response);

        
        sendRoomList();
        broadcastRoomListUpdate();
    }

    private void handleJoinRoom(Message message) {
        String roomId = (String) message.get("roomId");

        if (model.joinRoom(roomId, clientId)) {
            currentRoomId = roomId;
            GameRoom room = model.getRoom(roomId);

            Message response = ProtocolParser.createJoinRoomResponseMessage(roomId, room.getName(), new ArrayList<>(room.getPlayerIds()));
            sendMessage(response);

            
            Message notification = ProtocolParser.createPlayerJoinedMessage(clientId, playerName);
            broadcastToRoom(notification, clientId);
        } else {
            sendErrorMessage("Не удалось присоединиться к комнате");
        }
    }

    private void handleLeaveRoom(Message message) {
        if (currentRoomId != null) {
            model.leaveRoom(currentRoomId, clientId);

            Message notification = ProtocolParser.createPlayerLeftMessage(clientId);
            broadcastToRoom(notification, clientId);

            currentRoomId = null;
        }
    }

    private void handlePlayerReady(Message message) {
        if (currentRoomId != null) {
            GameRoom room = model.getRoom(currentRoomId);
            room.playerReady(clientId);

            Message notification = ProtocolParser.createPlayerReadyNotificationMessage(clientId);
            broadcastToRoom(notification, null);

            
            if (room.allPlayersReady() && room.getCreatorId().equals(clientId)) {
                Message readyNotification = ProtocolParser.createAllPlayersReadyMessage();
                sendMessage(readyNotification);
            }
        }
    }

    private void handleGameStart(Message message) {
        if (currentRoomId != null) {
            GameRoom room = model.getRoom(currentRoomId);
            if (room.getCreatorId().equals(clientId) && room.startGame()) {
                
                Message gameStartMsg = ProtocolParser.createGameStartResponseMessage(room.getCurrentPlayerId());
                broadcastToRoom(gameStartMsg, null);
            }
        }
    }

    private void handlePlayerMove(Message message) {
        if (currentRoomId != null) {
            GameRoom room = model.getRoom(currentRoomId);

            if (clientId.equals(room.getCurrentPlayerId())) {
                String word = (String) message.get("word");
                int row = ((Double) message.get("row")).intValue();
                int col = ((Double) message.get("col")).intValue();
                boolean horizontal = (Boolean) message.get("horizontal");
                List<String> tileIds = Arrays.asList((String[]) message.get("tileIds"));

                
                WordChecker.ValidationResult result = model.getWordChecker().validateMove(
                        word, row, col, horizontal,
                        getCurrentBoardState(room), tileIds, clientId, room
                );

                if (result.isValid()) {
                    
                    model.getWordChecker().updateBoard(
                            getCurrentBoardState(room), word, row, col,
                            horizontal, getPlayerTiles(tileIds)
                    );

                    Message moveResult = ProtocolParser.createPlayerMoveResultMessage(
                            clientId, word, result.getScore(), row, col, horizontal
                    );
                    broadcastToRoom(moveResult, null);

                    room.nextTurn();
                    Map<String, Object> gameData = new HashMap<>();
                    Message nextTurnMsg = ProtocolParser.createGameStateMessage(room.getCurrentPlayerId(), gameData);
                    broadcastToRoom(nextTurnMsg, null);
                } else {
                    sendErrorMessage(result.getMessage());
                }
            } else {
                sendErrorMessage("Сейчас не ваш ход");
            }
        }
    }

    private GameState.BoardCell[][] getCurrentBoardState(GameRoom room) {
        
        
        GameState gameState = new GameState();
        return gameState.getBoard();
    }

    private List<TileBag.Tile> getPlayerTiles(List<String> tileIds) {
        
        List<scrabble.utils.TileBag.Tile> tiles = new ArrayList<>();
        for (String id : tileIds) {
            tiles.add(new scrabble.utils.TileBag.Tile(id.charAt(0), 1));
        }
        return tiles;
    }


    private void handleChatMessage(Message message) {
        if (currentRoomId != null) {
            String content = (String) message.get("content");
            Message chatMsg = ProtocolParser.createChatMessage(playerName + ": " + content);
            chatMsg.setSender(clientId);
            broadcastToRoom(chatMsg, null);
        }
    }

    private void handleDisconnect(Message message) {
        disconnect();
    }

    private void broadcastToRoom(Message message, String excludeClientId) {
        GameRoom room = model.getRoom(currentRoomId);
        if (room != null) {
            for (String playerId : room.getPlayerIds()) {
                if (!playerId.equals(excludeClientId)) {
                    ClientHandler handler = model.getClientHandler(playerId);
                    if (handler != null) {
                        handler.sendMessage(message);
                    }
                }
            }
        }
    }

    private void sendRoomList() {
        Message message = ProtocolParser.createRoomListMessage(model.getAvailableRooms());
        sendMessage(message);
    }

    private void broadcastRoomListUpdate() {
        Message message = ProtocolParser.createRoomListMessage(model.getAvailableRooms());

        
        for (ClientHandler handler : model.getAllClientHandlers()) {
            if (handler != this) {
                handler.sendMessage(message);
            }
        }
    }

    public synchronized void sendMessage(Message message) {
        try {
            String json = message.toJson() + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));

            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }

            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

        } catch (IOException e) {
            System.err.println("Ошибка отправки сообщения клиенту " + clientId + ": " + e.getMessage());
            disconnect();
        }
    }

    private void sendErrorMessage(String error) {
        Message errorMsg = ProtocolParser.createErrorMessage(error);
        sendMessage(errorMsg);
    }

    public void disconnect() {
        try {
            if (currentRoomId != null) {
                handleLeaveRoom(null);
            }

            model.unregisterClient(clientId);

            if (channel != null && channel.isOpen()) {
                channel.close();
            }

            System.out.println("Клиент отключен: " + clientId);
        } catch (IOException e) {
            System.err.println("Ошибка при отключении клиента: " + e.getMessage());
        }
    }

    public String getClientId() {
        return clientId;
    }

    public String getPlayerName() {
        return playerName;
    }
}