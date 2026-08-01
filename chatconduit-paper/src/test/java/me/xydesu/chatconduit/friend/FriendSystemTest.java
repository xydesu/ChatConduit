package me.xydesu.chatconduit.friend;

import me.xydesu.chatconduit.friend.model.FriendBlock;
import me.xydesu.chatconduit.friend.model.FriendRelation;
import me.xydesu.chatconduit.friend.model.FriendRequest;
import me.xydesu.chatconduit.friend.model.PlayerSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 好友系統模型與狀態測試
 *
 * @author xydesu
 */
class FriendSystemTest {

    @Test
    @DisplayName("測試 FriendRelation 模型屬性")
    void testFriendRelationModel() {
        UUID p1 = UUID.randomUUID();
        UUID p2 = UUID.randomUUID();
        long now = System.currentTimeMillis();

        FriendRelation relation = new FriendRelation(p1, p2, now);
        assertEquals(p1, relation.getPlayerUuid());
        assertEquals(p2, relation.getFriendUuid());
        assertEquals(now, relation.getCreatedAt());
    }

    @Test
    @DisplayName("測試 FriendRequest 模型屬性")
    void testFriendRequestModel() {
        UUID sender = UUID.randomUUID();
        UUID receiver = UUID.randomUUID();
        long timestamp = System.currentTimeMillis();

        FriendRequest request = new FriendRequest(sender, receiver, timestamp);
        assertEquals(sender, request.getSenderUuid());
        assertEquals(receiver, request.getReceiverUuid());
        assertEquals(timestamp, request.getTimestamp());
    }

    @Test
    @DisplayName("測試 FriendBlock 模型屬性")
    void testFriendBlockModel() {
        UUID player = UUID.randomUUID();
        UUID blocked = UUID.randomUUID();
        long now = System.currentTimeMillis();

        FriendBlock block = new FriendBlock(player, blocked, now);
        assertEquals(player, block.getPlayerUuid());
        assertEquals(blocked, block.getBlockedUuid());
        assertEquals(now, block.getCreatedAt());
    }

    @Test
    @DisplayName("測試 PlayerSettings 模型預設值與修改")
    void testPlayerSettingsModel() {
        UUID uuid = UUID.randomUUID();
        PlayerSettings settings = PlayerSettings.createDefault(uuid);

        assertEquals(uuid, settings.getUuid());
        assertTrue(settings.isAllowFriendRequests());
        assertTrue(settings.isAllowTeleport());
        assertTrue(settings.isAllowPrivateMessages());

        settings.setAllowFriendRequests(false);
        settings.setAllowTeleport(false);
        settings.setAllowPrivateMessages(false);

        assertFalse(settings.isAllowFriendRequests());
        assertFalse(settings.isAllowTeleport());
        assertFalse(settings.isAllowPrivateMessages());
    }
}
