package me.xydesu.chatconduit.redis;

import com.google.gson.Gson;

/**
 * 跨伺服器私人訊息封包模型
 *
 * @author xydesu
 */
public class PrivateMessagePacket {

    private static final Gson GSON = new Gson();

    private String senderUuid;
    private String senderName;
    private String senderServerId;
    private String targetUuid;
    private String targetName;
    private String targetServerId;
    private String rawMessage;
    private long timestamp;
    private String messageJson;

    public PrivateMessagePacket() {
    }

    public PrivateMessagePacket(String senderUuid, String senderName, String senderServerId,
                                String targetUuid, String targetName, String targetServerId,
                                String rawMessage, long timestamp) {
        this(senderUuid, senderName, senderServerId, targetUuid, targetName, targetServerId, rawMessage, timestamp, null);
    }

    public PrivateMessagePacket(String senderUuid, String senderName, String senderServerId,
                                String targetUuid, String targetName, String targetServerId,
                                String rawMessage, long timestamp, String messageJson) {
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.senderServerId = senderServerId;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.targetServerId = targetServerId;
        this.rawMessage = rawMessage;
        this.timestamp = timestamp;
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

    public String getSenderServerId() {
        return senderServerId;
    }

    public void setSenderServerId(String senderServerId) {
        this.senderServerId = senderServerId;
    }

    public String getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid(String targetUuid) {
        this.targetUuid = targetUuid;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getTargetServerId() {
        return targetServerId;
    }

    public void setTargetServerId(String targetServerId) {
        this.targetServerId = targetServerId;
    }

    public String getRawMessage() {
        return rawMessage;
    }

    public void setRawMessage(String rawMessage) {
        this.rawMessage = rawMessage;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getMessageJson() {
        return messageJson;
    }

    public void setMessageJson(String messageJson) {
        this.messageJson = messageJson;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static PrivateMessagePacket fromJson(String json) {
        try {
            return GSON.fromJson(json, PrivateMessagePacket.class);
        } catch (Exception e) {
            return null;
        }
    }
}
