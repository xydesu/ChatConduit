package me.xydesu.chatconduit.channel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 測試 PlayerChannelManager 的關鍵邏輯 (保留字比對、成員權限、CustomChannel 屬性)
 *
 * @author xydesu
 */
class PlayerChannelManagerTest {

    @Test
    @DisplayName("測試系統保留關鍵字檢測 (isReservedKeyword)")
    void testReservedKeywords() {
        assertTrue(PlayerChannelManager.isReservedKeyword("create"));
        assertTrue(PlayerChannelManager.isReservedKeyword("CREATE"));
        assertTrue(PlayerChannelManager.isReservedKeyword("global"));
        assertTrue(PlayerChannelManager.isReservedKeyword("admin"));
        assertTrue(PlayerChannelManager.isReservedKeyword("reload"));

        assertFalse(PlayerChannelManager.isReservedKeyword("myteam"));
        assertFalse(PlayerChannelManager.isReservedKeyword("pvp_squad"));
    }

    @Test
    @DisplayName("測試 CustomChannel 物件建立與成員管理")
    void testCustomChannelMembers() {
        UUID ownerUuid = UUID.randomUUID();
        PlayerChannelManager.CustomChannel channel = new PlayerChannelManager.CustomChannel(
                "squad1",
                "Squad One",
                ownerUuid,
                PlayerChannelManager.Mode.PRIVATE,
                "<gradient:#ff0000:#00ff00>"
        );

        assertEquals("squad1", channel.getId());
        assertEquals("Squad One", channel.getDisplayName());
        assertEquals(ownerUuid, channel.getOwner());
        assertEquals(PlayerChannelManager.Mode.PRIVATE, channel.getMode());
        assertTrue(channel.getMembers().contains(ownerUuid));

        UUID memberUuid = UUID.randomUUID();
        channel.getMembers().add(memberUuid);
        assertEquals(2, channel.getMembers().size());
        assertTrue(channel.getMembers().contains(memberUuid));

        channel.getMembers().remove(memberUuid);
        assertEquals(1, channel.getMembers().size());
        assertFalse(channel.getMembers().contains(memberUuid));
    }
}
