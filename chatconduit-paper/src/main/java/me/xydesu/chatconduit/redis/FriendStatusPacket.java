package me.xydesu.chatconduit.redis;

import com.google.gson.Gson;

/**
 * 跨服好友線上狀態廣播封包 (登入/登出/切換伺服器)
 *
 * @author xydesu
 */
public class FriendStatusPacket {

    private static final Gson GSON = new Gson();

    public enum Action {
        JOIN,
        QUIT
    }

    private String playerUuid;
    private String playerName;
    private String serverId;
    private Action action;
    private long timestamp;

    public FriendStatusPacket() {}

    public FriendStatusPacket(String playerUuid, String playerName, String serverId, Action action, long timestamp) {
        this.playerUuid = playerUuid;
        this.playerName = playerName;
        this.serverId = serverId;
        this.action = action;
        this.timestamp = timestamp;
    }

    public String getPlayerUuid() { return playerUuid; }
    public String getPlayerName() { return playerName; }
    public String getServerId() { return serverId; }
    public Action getAction() { return action; }
    public long getTimestamp() { return timestamp; }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static FriendStatusPacket fromJson(String json) {
        try {
            return GSON.fromJson(json, FriendStatusPacket.class);
        } catch (Exception e) {
            return null;
        }
    }
}
