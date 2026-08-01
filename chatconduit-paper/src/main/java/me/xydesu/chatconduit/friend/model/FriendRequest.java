package me.xydesu.chatconduit.friend.model;

import java.util.UUID;

/**
 * 好友申請資料模型
 *
 * @author xydesu
 */
public class FriendRequest {

    private final UUID senderUuid;
    private final UUID receiverUuid;
    private final long timestamp;

    public FriendRequest(UUID senderUuid, UUID receiverUuid, long timestamp) {
        this.senderUuid = senderUuid;
        this.receiverUuid = receiverUuid;
        this.timestamp = timestamp;
    }

    public UUID getSenderUuid() {
        return senderUuid;
    }

    public UUID getReceiverUuid() {
        return receiverUuid;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
