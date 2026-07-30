package me.xydesu.chatconduit.redis;

import me.xydesu.chatconduit.channel.PlayerChannelManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 測試 PlayerChannelSyncPacket 序列化與屬性轉換
 *
 * @author xydesu
 */
class PlayerChannelSyncPacketTest {

    @Test
    @DisplayName("測試 PlayerChannelSyncPacket JSON 序列化與反序列化 (CREATE 動作)")
    void testPlayerChannelSyncPacketCreateJson() {
        UUID owner = UUID.randomUUID();
        UUID member1 = UUID.randomUUID();

        PlayerChannelManager.CustomChannel customChannel = new PlayerChannelManager.CustomChannel(
                "vip_lounge",
                "VIP 貴賓廳",
                owner,
                PlayerChannelManager.Mode.PUBLIC,
                "<gradient:#ff9900:#ff5500>",
                "https://discord.com/api/webhooks/123/abc",
                "VIP 玩家專屬聊天室",
                "保持禮貌"
        );
        customChannel.getMembers().add(member1);

        PlayerChannelSyncPacket packet = new PlayerChannelSyncPacket(
                PlayerChannelSyncPacket.Action.CREATE,
                customChannel.getId(),
                "survival-1"
        );
        packet.setDisplayName(customChannel.getDisplayName());
        packet.setOwnerUuid(customChannel.getOwner().toString());
        packet.setMode(customChannel.getMode().name());
        packet.setColorTheme(customChannel.getColorTheme());
        packet.setWebhookUrl(customChannel.getWebhookUrl());
        packet.setDescription(customChannel.getDescription());
        packet.setRules(customChannel.getRules());
        packet.populateMembersFromUuids(customChannel.getMembers());
        packet.populatePendingInvitesFromUuids(customChannel.getPendingInvites());

        String json = packet.toJson();
        assertNotNull(json);
        assertTrue(json.contains("\"vip_lounge\""));
        assertTrue(json.contains("\"VIP 貴賓廳\""));

        PlayerChannelSyncPacket deserialized = PlayerChannelSyncPacket.fromJson(json);
        assertNotNull(deserialized);
        assertEquals(PlayerChannelSyncPacket.Action.CREATE, deserialized.getAction());
        assertEquals("vip_lounge", deserialized.getChannelId());
        assertEquals("VIP 貴賓廳", deserialized.getDisplayName());
        assertEquals(owner.toString(), deserialized.getOwnerUuid());
        assertEquals("PUBLIC", deserialized.getMode());
        assertEquals("<gradient:#ff9900:#ff5500>", deserialized.getColorTheme());
        assertEquals("https://discord.com/api/webhooks/123/abc", deserialized.getWebhookUrl());
        assertEquals("VIP 玩家專屬聊天室", deserialized.getDescription());
        assertEquals("保持禮貌", deserialized.getRules());
        assertEquals("survival-1", deserialized.getOriginServerId());
        assertTrue(deserialized.getMembers().contains(owner.toString()));
        assertTrue(deserialized.getMembers().contains(member1.toString()));
    }

    @Test
    @DisplayName("測試 PlayerChannelSyncPacket DELETE 與 MEMBER_KICK 動作屬性")
    void testPlayerChannelSyncPacketActions() {
        PlayerChannelSyncPacket deletePacket = new PlayerChannelSyncPacket(
                PlayerChannelSyncPacket.Action.DELETE,
                "my_channel",
                "survival-2"
        );
        String jsonDelete = deletePacket.toJson();
        PlayerChannelSyncPacket deserializedDelete = PlayerChannelSyncPacket.fromJson(jsonDelete);
        assertEquals(PlayerChannelSyncPacket.Action.DELETE, deserializedDelete.getAction());
        assertEquals("my_channel", deserializedDelete.getChannelId());
        assertEquals("survival-2", deserializedDelete.getOriginServerId());

        String targetUuidStr = UUID.randomUUID().toString();
        PlayerChannelSyncPacket kickPacket = new PlayerChannelSyncPacket(
                PlayerChannelSyncPacket.Action.MEMBER_KICK,
                "my_channel",
                "survival-1"
        );
        kickPacket.setTargetUuid(targetUuidStr);
        kickPacket.setTargetName("BadPlayer");

        String jsonKick = kickPacket.toJson();
        PlayerChannelSyncPacket deserializedKick = PlayerChannelSyncPacket.fromJson(jsonKick);
        assertEquals(PlayerChannelSyncPacket.Action.MEMBER_KICK, deserializedKick.getAction());
        assertEquals(targetUuidStr, deserializedKick.getTargetUuid());
        assertEquals("BadPlayer", deserializedKick.getTargetName());
    }
}
