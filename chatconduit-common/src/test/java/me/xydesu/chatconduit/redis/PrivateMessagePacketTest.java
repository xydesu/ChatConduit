package me.xydesu.chatconduit.redis;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 測試 PrivateMessagePacket 跨服私訊封包 JSON 序列化與反序列化
 *
 * @author xydesu
 */
class PrivateMessagePacketTest {

    @Test
    @DisplayName("測試 PrivateMessagePacket 序列化與反序列化正確性")
    void testPrivateMessagePacketSerialization() {
        String senderUuid = UUID.randomUUID().toString();
        String targetUuid = UUID.randomUUID().toString();

        PrivateMessagePacket original = new PrivateMessagePacket(
                senderUuid,
                "xydesu",
                "survival-1",
                targetUuid,
                "TargetPlayer",
                "lobby-1",
                "Hello cross-server PM!",
                System.currentTimeMillis()
        );

        String json = original.toJson();
        assertNotNull(json);
        assertTrue(json.contains("\"senderName\":\"xydesu\""));
        assertTrue(json.contains("\"targetName\":\"TargetPlayer\""));
        assertTrue(json.contains("\"rawMessage\":\"Hello cross-server PM!\""));

        PrivateMessagePacket deserialized = PrivateMessagePacket.fromJson(json);
        assertNotNull(deserialized);
        assertEquals(original.getSenderUuid(), deserialized.getSenderUuid());
        assertEquals(original.getSenderName(), deserialized.getSenderName());
        assertEquals(original.getSenderServerId(), deserialized.getSenderServerId());
        assertEquals(original.getTargetUuid(), deserialized.getTargetUuid());
        assertEquals(original.getTargetName(), deserialized.getTargetName());
        assertEquals(original.getTargetServerId(), deserialized.getTargetServerId());
        assertEquals(original.getRawMessage(), deserialized.getRawMessage());
    }
}
