package me.xydesu.chatconduit.redis;

import com.google.gson.Gson;

/**
 * 跨伺服器聊天訊息封包模型
 *
 * @author xydesu
 */
public class ChatMessagePacket {

    private static final Gson GSON = new Gson();

    private String senderUuid;
    private String senderName;
    private String channelName;
    private String rawMessage;
    private String serverId;
    private long timestamp;
    private String chatFormat;
    private String messageJson;

    public ChatMessagePacket() {
    }

    public ChatMessagePacket(String senderUuid, String senderName, String channelName, String rawMessage, String serverId, long timestamp) {
        this(senderUuid, senderName, channelName, rawMessage, serverId, timestamp, null, null);
    }

    public ChatMessagePacket(String senderUuid, String senderName, String channelName, String rawMessage, String serverId, long timestamp, String chatFormat) {
        this(senderUuid, senderName, channelName, rawMessage, serverId, timestamp, chatFormat, null);
    }

    public ChatMessagePacket(String senderUuid, String senderName, String channelName, String rawMessage, String serverId, long timestamp, String chatFormat, String messageJson) {
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.channelName = channelName;
        this.rawMessage = rawMessage;
        this.serverId = serverId;
        this.timestamp = timestamp;
        this.chatFormat = chatFormat;
        this.messageJson = messageJson;
    }

    public String getSenderUuid() {
        return senderUuid;
    }

    public void setSenderUuid(String senderUuid) {
        this.senderUuid = senderUuid;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getChatFormat() {
        return chatFormat;
    }

    public void setChatFormat(String chatFormat) {
        this.chatFormat = chatFormat;
    }

    public String getMessageJson() {
        return messageJson;
    }

    public void setMessageJson(String messageJson) {
        this.messageJson = messageJson;
    }

    /**
     * 物件序列化為 JSON 字串
     */
    public String toJson() {
        return GSON.toJson(this);
    }

    /**
     * 從 JSON 字串反序列化為 ChatMessagePacket 物件
     */
    public static ChatMessagePacket fromJson(String json) {
        return GSON.fromJson(json, ChatMessagePacket.class);
    }
}
