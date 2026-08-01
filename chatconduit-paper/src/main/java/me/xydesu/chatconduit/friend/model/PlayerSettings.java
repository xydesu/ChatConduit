package me.xydesu.chatconduit.friend.model;

import java.util.UUID;

/**
 * 玩家個人偏好設定模型
 *
 * @author xydesu
 */
public class PlayerSettings {

    private final UUID uuid;
    private boolean allowFriendRequests;
    private boolean allowTeleport;
    private boolean allowPrivateMessages;

    public PlayerSettings(UUID uuid, boolean allowFriendRequests, boolean allowTeleport, boolean allowPrivateMessages) {
        this.uuid = uuid;
        this.allowFriendRequests = allowFriendRequests;
        this.allowTeleport = allowTeleport;
        this.allowPrivateMessages = allowPrivateMessages;
    }

    public static PlayerSettings createDefault(UUID uuid) {
        return new PlayerSettings(uuid, true, true, true);
    }

    public UUID getUuid() {
        return uuid;
    }

    public boolean isAllowFriendRequests() {
        return allowFriendRequests;
    }

    public void setAllowFriendRequests(boolean allowFriendRequests) {
        this.allowFriendRequests = allowFriendRequests;
    }

    public boolean isAllowTeleport() {
        return allowTeleport;
    }

    public void setAllowTeleport(boolean allowTeleport) {
        this.allowTeleport = allowTeleport;
    }

    public boolean isAllowPrivateMessages() {
        return allowPrivateMessages;
    }

    public void setAllowPrivateMessages(boolean allowPrivateMessages) {
        this.allowPrivateMessages = allowPrivateMessages;
    }
}
