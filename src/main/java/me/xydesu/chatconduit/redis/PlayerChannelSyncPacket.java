package me.xydesu.chatconduit.redis;

import com.google.gson.Gson;
import me.xydesu.chatconduit.channel.PlayerChannelManager;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * 跨伺服器玩家頻道同步與狀態變動封包模型
 *
 * @author xydesu
 */
public class PlayerChannelSyncPacket {

    private static final Gson GSON = new Gson();

    public enum Action {
        CREATE,         // 建立頻道
        UPDATE,         // 更新頻道屬性
        DELETE,         // 解散/刪除頻道
        MEMBER_JOIN,    // 成員加入
        MEMBER_LEAVE,   // 成員離開
        MEMBER_KICK,    // 成員被踢出
        TRANSFER_OWNER  // 轉讓隊長
    }

    private Action action;
    private String channelId;
    private String displayName;
    private String ownerUuid;
    private String mode;
    private String colorTheme;
    private String webhookUrl;
    private String description;
    private String rules;
    private Set<String> members = new HashSet<>();
    private Set<String> pendingInvites = new HashSet<>();
    private String originServerId;
    private String targetUuid;
    private String targetName;
    private long timestamp;

    public PlayerChannelSyncPacket() {
    }

    public PlayerChannelSyncPacket(Action action, String channelId, String originServerId) {
        this.action = action;
        this.channelId = channelId;
        this.originServerId = originServerId;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 從 CustomChannel 複製完整數據進行封包構建
     */
    public static PlayerChannelSyncPacket fromChannel(Action action, PlayerChannelManager.CustomChannel channel, String originServerId) {
        PlayerChannelSyncPacket packet = new PlayerChannelSyncPacket(action, channel.getId(), originServerId);
        packet.setDisplayName(channel.getDisplayName());
        packet.setOwnerUuid(channel.getOwner() != null ? channel.getOwner().toString() : null);
        packet.setMode(channel.getMode() != null ? channel.getMode().name() : "PRIVATE");
        packet.setColorTheme(channel.getColorTheme());
        packet.setWebhookUrl(channel.getWebhookUrl());
        packet.setDescription(channel.getDescription());
        packet.setRules(channel.getRules());

        if (channel.getMembers() != null) {
            for (UUID uuid : channel.getMembers()) {
                packet.getMembers().add(uuid.toString());
            }
        }
        if (channel.getPendingInvites() != null) {
            for (UUID uuid : channel.getPendingInvites()) {
                packet.getPendingInvites().add(uuid.toString());
            }
        }
        return packet;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getOwnerUuid() {
        return ownerUuid;
    }

    public void setOwnerUuid(String ownerUuid) {
        this.ownerUuid = ownerUuid;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getColorTheme() {
        return colorTheme;
    }

    public void setColorTheme(String colorTheme) {
        this.colorTheme = colorTheme;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRules() {
        return rules;
    }

    public void setRules(String rules) {
        this.rules = rules;
    }

    public Set<String> getMembers() {
        return members;
    }

    public void setMembers(Set<String> members) {
        this.members = members;
    }

    public Set<String> getPendingInvites() {
        return pendingInvites;
    }

    public void setPendingInvites(Set<String> pendingInvites) {
        this.pendingInvites = pendingInvites;
    }

    public String getOriginServerId() {
        return originServerId;
    }

    public void setOriginServerId(String originServerId) {
        this.originServerId = originServerId;
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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static PlayerChannelSyncPacket fromJson(String json) {
        return GSON.fromJson(json, PlayerChannelSyncPacket.class);
    }
}
