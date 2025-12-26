// scrabble/protocol/Message.java

package scrabble.protocol;

import com.google.gson.Gson;
import java.util.Map;
import java.util.HashMap;

public class Message {
    private MessageType type;
    private Map<String, Object> data;
    private String sender;
    private long timestamp;

    private static final Gson gson = new Gson();

    public Message(MessageType type) {
        this.type = type;
        this.data = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void put(String key, Object value) {
        data.put(key, value);
    }

    public Object get(String key) {
        return data.get(key);
    }

    public String toJson() {
        Map<String, Object> messageMap = new HashMap<>();
        messageMap.put("type", type.toString());
        messageMap.put("data", data);
        messageMap.put("sender", sender);
        messageMap.put("timestamp", timestamp);
        return gson.toJson(messageMap);
    }

    public static Message fromJson(String json) {
        Map<String, Object> messageMap = gson.fromJson(json, Map.class);
        Message message = new Message(MessageType.valueOf((String) messageMap.get("type")));
        message.setData((Map<String, Object>) messageMap.get("data"));
        message.setSender((String) messageMap.get("sender"));
        message.timestamp = ((Double) messageMap.get("timestamp")).longValue();
        return message;
    }
}