package me.xydesu.chatconduit.redis;

import com.google.gson.Gson;

/**
 * 跨伺服器玩家頻道邀請與同步封包模型
 *
 * @author xydesu
 */
public class ChannelInvitePacket {

    private static final Gson GSON = new Gson();

    public enum Action {
        INVITE,     // 發起邀請
        ACCEPT,     // 接受邀請
        REJECT,     // 拒絕邀請
        SYNC_MEMBER // 同步成員異動
    }

    private Action action;
    private String senderUuid;
    private String senderName;
    private String targetPlayerName;
    private String channelId;
    private String channelDisplayName;
    private String originServerId;
    private long timestamp;

    public ChannelInvitePacket() {
    }

    public ChannelInvitePacket(Action action, String senderUuid, String senderName, String targetPlayerName, String channelId, String channelDisplayName, String originServerId, long timestamp) {
        this.action = action;
        this.senderUuid = senderUuid;
        this.senderName = senderName;
        this.targetPlayerName = targetPlayerName;
        this.channelId = channelId;
        this.channelDisplayName = channelDisplayName;
        this.originServerId = originServerId;
        this.timestamp = timestamp;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
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

    public String getTargetPlayerName() {
        return targetPlayerName;
    }

    public void setTargetPlayerName(String targetPlayerName) {
        this.targetPlayerName = targetPlayerName;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelDisplayName() {
        return channelDisplayName;
    }

    public void setChannelDisplayName(String channelDisplayName) {
        this.channelDisplayName = channelDisplayName;
    }

    public String getOriginServerId() {
        return originServerId;
    }

    public void setOriginServerId(String originServerId) {
        this.originServerId = originServerId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static ChannelInvitePacket fromJson(String json) {
        return GSON.fromJson(json, ChannelInvitePacket.class);
    }
}
