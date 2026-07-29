package me.xydesu.chatconduit.redis;

import com.google.gson.Gson;

/**
 * 跨伺服器禁言封包模型
 *
 * @author xydesu
 */
public class MutePacket {

    private static final Gson GSON = new Gson();

    public enum Action {
        MUTE,
        UNMUTE
    }

    private Action action;
    private String uuid;
    private String playerName;
    private String reason;
    private long mutedAt;
    private long expireAt;
    private String mutedBy;
    private String serverId;

    public MutePacket() {
    }

    public MutePacket(Action action, String uuid, String playerName, String reason, long mutedAt, long expireAt, String mutedBy, String serverId) {
        this.action = action;
        this.uuid = uuid;
        this.playerName = playerName;
        this.reason = reason;
        this.mutedAt = mutedAt;
        this.expireAt = expireAt;
        this.mutedBy = mutedBy;
        this.serverId = serverId;
    }

    public Action action() { return action; }
    public Action getAction() { return action; }

    public String uuid() { return uuid; }
    public String getUuid() { return uuid; }

    public String playerName() { return playerName; }
    public String getPlayerName() { return playerName; }

    public String reason() { return reason; }
    public String getReason() { return reason; }

    public long mutedAt() { return mutedAt; }
    public long getMutedAt() { return mutedAt; }

    public long expireAt() { return expireAt; }
    public long getExpireAt() { return expireAt; }

    public String mutedBy() { return mutedBy; }
    public String getMutedBy() { return mutedBy; }

    public String serverId() { return serverId; }
    public String getServerId() { return serverId; }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static MutePacket fromJson(String json) {
        return GSON.fromJson(json, MutePacket.class);
    }
}
