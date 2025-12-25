package scrabble.server.network;

import scrabble.server.model.ServerModel;
import scrabble.server.model.GameRoom;
import scrabble.protocol.Message;
import scrabble.protocol.MessageType;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Map;

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

        Message response = new Message(MessageType.CONNECT);
        response.put("playerId", clientId);
        response.put("status", "connected");
        sendMessage(response);

        // Отправляем список доступных комнат
        sendRoomList();
    }

    private void handleCreateRoom(Message message) {
        String roomName = (String) message.get("roomName");
        int maxPlayers = ((Double) message.get("maxPlayers")).intValue();

        GameRoom room = model.createRoom(roomName, maxPlayers, clientId);
        currentRoomId = room.getId();

        Message response = new Message(MessageType.CREATE_ROOM);
        response.put("roomId", room.getId());
        response.put("roomName", room.getName());
        sendMessage(response);

        // Уведомляем всех о новом списке комнат
        broadcastRoomListUpdate();
    }

    private void handleJoinRoom(Message message) {
        String roomId = (String) message.get("roomId");

        if (model.joinRoom(roomId, clientId)) {
            currentRoomId = roomId;
            GameRoom room = model.getRoom(roomId);

            Message response = new Message(MessageType.JOIN_ROOM);
            response.put("roomId", roomId);
            response.put("roomName", room.getName());
            response.put("players", room.getPlayerIds());
            sendMessage(response);

            // Уведомляем других игроков в комнате
            Message notification = new Message(MessageType.PLAYER_JOINED);
            notification.put("playerId", clientId);
            notification.put("playerName", playerName);
            broadcastToRoom(notification, clientId);
        } else {
            sendErrorMessage("Не удалось присоединиться к комнате");
        }
    }

    private void handleLeaveRoom(Message message) {
        if (currentRoomId != null) {
            model.leaveRoom(currentRoomId, clientId);

            Message notification = new Message(MessageType.PLAYER_LEFT);
            notification.put("playerId", clientId);
            broadcastToRoom(notification, clientId);

            currentRoomId = null;
        }
    }

    private void handlePlayerReady(Message message) {
        if (currentRoomId != null) {
            GameRoom room = model.getRoom(currentRoomId);
            room.playerReady(clientId);

            Message notification = new Message(MessageType.PLAYER_READY);
            notification.put("playerId", clientId);
            broadcastToRoom(notification, null);

            // Если все готовы, уведомляем создателя
            if (room.allPlayersReady() && room.getCreatorId().equals(clientId)) {
                Message readyNotification = new Message(MessageType.ALL_PLAYERS_READY);
                sendMessage(readyNotification);
            }
        }
    }

    private void handleGameStart(Message message) {
        if (currentRoomId != null) {
            GameRoom room = model.getRoom(currentRoomId);
            if (room.getCreatorId().equals(clientId) && room.startGame()) {
                // Отправляем начальное состояние игры всем игрокам
                Message gameStartMsg = new Message(MessageType.GAME_START);
                gameStartMsg.put("currentPlayer", room.getCurrentPlayerId());
                broadcastToRoom(gameStartMsg, null);
            }
        }
    }

    private void handlePlayerMove(Message message) {
        if (currentRoomId != null) {
            GameRoom room = model.getRoom(currentRoomId);

            // Проверяем, что это ход текущего игрока
            if (clientId.equals(room.getCurrentPlayerId())) {
                // Здесь должна быть проверка хода
                // Пока просто передаем ход следующему игроку

                // Рассчитываем очки
                String word = (String) message.get("word");
                int score = word.length() * 10; // Упрощенный расчет

                Message moveResult = new Message(MessageType.PLAYER_MOVE);
                moveResult.put("playerId", clientId);
                moveResult.put("word", word);
                moveResult.put("score", score);
                moveResult.put("row", message.get("row"));
                moveResult.put("col", message.get("col"));
                moveResult.put("horizontal", message.get("horizontal"));
                broadcastToRoom(moveResult, null);

                // Передаем ход
                room.nextTurn();

                Message nextTurnMsg = new Message(MessageType.GAME_STATE);
                nextTurnMsg.put("currentPlayer", room.getCurrentPlayerId());
                broadcastToRoom(nextTurnMsg, null);
            } else {
                sendErrorMessage("Сейчас не ваш ход");
            }
        }
    }

    private void handleChatMessage(Message message) {
        if (currentRoomId != null) {
            String content = (String) message.get("content");
            Message chatMsg = new Message(MessageType.CHAT_MESSAGE);
            chatMsg.put("content", playerName + ": " + content);
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
        Message message = new Message(MessageType.ROOM_LIST);
        message.put("rooms", model.getAvailableRooms());
        sendMessage(message);
    }

    private void broadcastRoomListUpdate() {
        Message message = new Message(MessageType.ROOM_LIST);
        message.put("rooms", model.getAvailableRooms());

        // Рассылаем всем подключенным клиентам
        for (ClientHandler handler : model.getAllClientHandlers()) {
            if (handler != this) {
                handler.sendMessage(message);
            }
        }
    }

    public void sendMessage(Message message) {
        try {
            String json = message.toJson() + "\n";
            ByteBuffer buffer = ByteBuffer.wrap(json.getBytes(StandardCharsets.UTF_8));

            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        } catch (IOException e) {
            System.err.println("Ошибка отправки сообщения клиенту " + clientId + ": " + e.getMessage());
            disconnect();
        }
    }

    private void sendErrorMessage(String error) {
        Message errorMsg = new Message(MessageType.ERROR);
        errorMsg.put("error", error);
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