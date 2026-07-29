package me.xydesu.chatconduit.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 測試 Redis 通訊封包 (ChatMessagePacket 與 ChannelInvitePacket) 序列化與反序列化
 *
 * @author xydesu
 */
class RedisPacketTest {

    @Test
    @DisplayName("測試 ChatMessagePacket JSON 序列化與反序列化")
    void testChatMessagePacketJsonSerialization() {
        String uuidStr = UUID.randomUUID().toString();
        ChatMessagePacket originalPacket = new ChatMessagePacket(
                uuidStr,
                "xydesu",
                "global",
                "Hello Redis multi-server!",
                "survival-1",
                1700000000000L
        );

        String json = originalPacket.toJson();
        assertNotNull(json);
        assertTrue(json.contains("\"senderUuid\":\"" + uuidStr + "\""));
        assertTrue(json.contains("\"senderName\":\"xydesu\""));

        ChatMessagePacket deserialized = ChatMessagePacket.fromJson(json);
        assertNotNull(deserialized);
        assertEquals(originalPacket.getSenderUuid(), deserialized.getSenderUuid());
        assertEquals(originalPacket.getSenderName(), deserialized.getSenderName());
        assertEquals(originalPacket.getChannelName(), deserialized.getChannelName());
        assertEquals(originalPacket.getRawMessage(), deserialized.getRawMessage());
        assertEquals(originalPacket.getServerId(), deserialized.getServerId());
        assertEquals(originalPacket.getTimestamp(), deserialized.getTimestamp());
    }

    @Test
    @DisplayName("測試 ChannelInvitePacket JSON 序列化與反序列化")
    void testChannelInvitePacketJsonSerialization() {
        String senderUuid = UUID.randomUUID().toString();
        ChannelInvitePacket originalPacket = new ChannelInvitePacket(
                ChannelInvitePacket.Action.INVITE,
                senderUuid,
                "xydesu",
                "PlayerB",
                "group1",
                "Group One",
                "lobby-1",
                1700000005000L
        );

        String json = originalPacket.toJson();
        assertNotNull(json);

        ChannelInvitePacket deserialized = ChannelInvitePacket.fromJson(json);
        assertNotNull(deserialized);
        assertEquals(ChannelInvitePacket.Action.INVITE, deserialized.getAction());
        assertEquals(senderUuid, deserialized.getSenderUuid());
        assertEquals("xydesu", deserialized.getSenderName());
        assertEquals("PlayerB", deserialized.getTargetPlayerName());
        assertEquals("group1", deserialized.getChannelId());
        assertEquals("Group One", deserialized.getChannelDisplayName());
        assertEquals("lobby-1", deserialized.getOriginServerId());
        assertEquals(1700000005000L, deserialized.getTimestamp());
    }
}
