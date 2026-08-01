package me.xydesu.chatconduit.friend.model;

import java.util.UUID;

/**
 * 好友關係資料模型
 *
 * @author xydesu
 */
public class FriendRelation {

    private final UUID playerUuid;
    private final UUID friendUuid;
    private final long createdAt;

    public FriendRelation(UUID playerUuid, UUID friendUuid, long createdAt) {
        this.playerUuid = playerUuid;
        this.friendUuid = friendUuid;
        this.createdAt = createdAt;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public UUID getFriendUuid() {
        return friendUuid;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
