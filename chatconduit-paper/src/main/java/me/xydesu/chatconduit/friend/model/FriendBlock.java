package me.xydesu.chatconduit.friend.model;

import java.util.UUID;

/**
 * 黑名單資料模型
 *
 * @author xydesu
 */
public class FriendBlock {

    private final UUID playerUuid;
    private final UUID blockedUuid;
    private final long createdAt;

    public FriendBlock(UUID playerUuid, UUID blockedUuid, long createdAt) {
        this.playerUuid = playerUuid;
        this.blockedUuid = blockedUuid;
        this.createdAt = createdAt;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public UUID getBlockedUuid() {
        return blockedUuid;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
