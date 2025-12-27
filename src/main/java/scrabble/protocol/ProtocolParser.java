package scrabble.protocol;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class ProtocolParser {
    private static final Gson gson = new Gson();

    public static Message createConnectMessage(String playerName) {
        Message msg = new Message(MessageType.CONNECT);
        msg.put("playerName", playerName);
        return msg;
    }

    public static Message createCreateRoomMessage(String roomName, int maxPlayers) {
        Message msg = new Message(MessageType.CREATE_ROOM);
        msg.put("roomName", roomName);
        msg.put("maxPlayers", maxPlayers);
        return msg;
    }

    public static Message createJoinRoomMessage(String roomId) {
        Message msg = new Message(MessageType.JOIN_ROOM);
        msg.put("roomId", roomId);
        return msg;
    }

    public static Message createLeaveRoomMessage() {
        return new Message(MessageType.LEAVE_ROOM);
    }

    public static Message createPlayerReadyMessage() {
        return new Message(MessageType.PLAYER_READY);
    }

    public static Message createGameStartMessage() {
        return new Message(MessageType.GAME_START);
    }

    public static Message createPlayerMoveMessage(String word, int row, int col,
                                                  boolean horizontal, String[] tileIds) {
        Message msg = new Message(MessageType.PLAYER_MOVE);
        msg.put("word", word);
        msg.put("row", row);
        msg.put("col", col);
        msg.put("horizontal", horizontal);
        msg.put("tileIds", tileIds);
        return msg;
    }

    public static Message createSkipTurnMessage() {
        Message msg = new Message(MessageType.PLAYER_MOVE);
        msg.put("action", "skip");
        return msg;
    }

    public static Message createTilesExchangeMessage(List<String> tileIds) {
        Message msg = new Message(MessageType.TILES_EXCHANGE);
        msg.put("tiles", tileIds);
        return msg;
    }

    public static Message createChatMessage(String content) {
        Message msg = new Message(MessageType.CHAT_MESSAGE);
        msg.put("content", content);
        return msg;
    }

    public static Message createErrorMessage(String error) {
        Message msg = new Message(MessageType.ERROR);
        msg.put("error", error);
        return msg;
    }

    public static Message createGameOverMessage(String winnerId, Map<String, Integer> finalScores) {
        Message msg = new Message(MessageType.GAME_OVER);
        msg.put("winnerId", winnerId);
        msg.put("finalScores", finalScores);
        return msg;
    }

    public static Message createRoomListMessage(List<String> rooms) {
        Message msg = new Message(MessageType.ROOM_LIST);
        msg.put("rooms", rooms);
        return msg;
    }

    public static Message createGameStateMessage(String currentPlayerId, Map<String, Object> gameData) {
        Message msg = new Message(MessageType.GAME_STATE);
        msg.put("currentPlayer", currentPlayerId);
        if (gameData != null) {
            msg.getData().putAll(gameData);
        }
        return msg;
    }

    public static Message createSurrenderMessage() {
        Message msg = new Message(MessageType.DISCONNECT);
        msg.put("action", "surrender");
        return msg;
    }

    public static Message createConnectResponseMessage(String playerId, String status) {
        Message msg = new Message(MessageType.CONNECT);
        msg.put("playerId", playerId);
        msg.put("status", status);
        return msg;
    }

    public static Message createCreateRoomResponseMessage(String roomId, String roomName) {
        Message msg = new Message(MessageType.CREATE_ROOM);
        msg.put("roomId", roomId);
        msg.put("roomName", roomName);
        return msg;
    }

    public static Message createJoinRoomResponseMessage(String roomId, String roomName, List<String> players) {
        Message msg = new Message(MessageType.JOIN_ROOM);
        msg.put("roomId", roomId);
        msg.put("roomName", roomName);
        msg.put("players", players);
        return msg;
    }

    public static Message createPlayerJoinedMessage(String playerId, String playerName) {
        Message msg = new Message(MessageType.PLAYER_JOINED);
        msg.put("playerId", playerId);
        msg.put("playerName", playerName);
        return msg;
    }

    public static Message createPlayerLeftMessage(String playerId) {
        Message msg = new Message(MessageType.PLAYER_LEFT);
        msg.put("playerId", playerId);
        return msg;
    }

    public static Message createPlayerReadyNotificationMessage(String playerId) {
        Message msg = new Message(MessageType.PLAYER_READY);
        msg.put("playerId", playerId);
        return msg;
    }

    public static Message createAllPlayersReadyMessage() {
        return new Message(MessageType.ALL_PLAYERS_READY);
    }

    public static Message createGameStartResponseMessage(String currentPlayerId) {
        Message msg = new Message(MessageType.GAME_START);
        msg.put("currentPlayer", currentPlayerId);
        return msg;
    }

    public static Message createPlayerMoveResultMessage(String playerId, String word, int score, int row, int col, boolean horizontal) {
        Message msg = new Message(MessageType.PLAYER_MOVE);
        msg.put("playerId", playerId);
        msg.put("word", word);
        msg.put("score", score);
        msg.put("row", row);
        msg.put("col", col);
        msg.put("horizontal", horizontal);
        return msg;
    }

    public static String toJson(Message message) {
        return message.toJson();
    }

    public static Message fromJson(String json) {
        return Message.fromJson(json);
    }

    public static <T> T parseData(Object data, Class<T> clazz) {
        String json = gson.toJson(data);
        return gson.fromJson(json, clazz);
    }

    public static <T> List<T> parseListData(Object data, Class<T> clazz) {
        String json = gson.toJson(data);
        Type type = TypeToken.getParameterized(List.class, clazz).getType();
        return gson.fromJson(json, type);
    }

    public static <K, V> Map<K, V> parseMapData(Object data, Class<K> keyClass, Class<V> valueClass) {
        String json = gson.toJson(data);
        Type type = TypeToken.getParameterized(Map.class, keyClass, valueClass).getType();
        return gson.fromJson(json, type);
    }
}