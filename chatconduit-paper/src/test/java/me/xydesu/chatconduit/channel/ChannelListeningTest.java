package me.xydesu.chatconduit.channel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 測試 ChannelManager 的頻道訂閱 (Listening) 功能
 *
 * @author xydesu
 */
class ChannelListeningTest {

    @Test
    @DisplayName("測試預設訂閱狀態與單一頻道切換")
    void testChannelListening() {
        UUID testUuid = UUID.randomUUID();

        // 預設情況下應訂閱所有頻道
        assertTrue(ChannelManager.isChannelListening(testUuid, "global"));
        assertTrue(ChannelManager.isChannelListening(testUuid, "trade"));

        // 設定取消訂閱 trade 頻道
        ChannelManager.setChannelListening(testUuid, "trade", false);
        assertFalse(ChannelManager.isChannelListening(testUuid, "trade"));

        // 重新訂閱 trade 頻道
        ChannelManager.setChannelListening(testUuid, "trade", true);
        assertTrue(ChannelManager.isChannelListening(testUuid, "trade"));
    }
}
