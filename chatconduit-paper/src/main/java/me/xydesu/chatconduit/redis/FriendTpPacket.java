package me.xydesu.chatconduit.redis;

import com.google.gson.Gson;

/**
 * 跨服好友傳送對接與請求封包
 *
 * @author xydesu
 */
public class FriendTpPacket {

    private static final Gson GSON = new Gson();

    public enum Action {
        REQUEST,
        ACCEPT,
        DENY
    }

    private String senderUuid;
    private String senderName;
    private String targetUuid;
    private String targetName;
    private String originServerId;
    private Action action;
    private long timestamp;

    public FriendTpPacket() {}

    public FriendTpPacket(String senderUuid, String senderName, String targetUuid, String targetName, String originServerId, Action action, long timestamp) {
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
        this.originServerId = originServerId;
        this.action = action;
        this.timestamp = timestamp;
    }

    public String getSenderUuid() { return senderUuid; }
    public String getSenderName() { return senderName; }
    public String getTargetUuid() { return targetUuid; }
    public String getTargetName() { return targetName; }
    public String getOriginServerId() { return originServerId; }
    public Action getAction() { return action; }
    public long getTimestamp() { return timestamp; }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static FriendTpPacket fromJson(String json) {
        try {
            return GSON.fromJson(json, FriendTpPacket.class);
        } catch (Exception e) {
            return null;
        }
    }
}
