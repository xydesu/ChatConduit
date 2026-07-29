package me.xydesu.chatconduit.mute;

import me.xydesu.chatconduit.command.MuteCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 禁言管理器與指令時間解析單元測試
 *
 * @author xydesu
 */
class MuteManagerTest {

    @Test
    @DisplayName("測試時間字串解析 (parseDuration)")
    void testParseDuration() {
        assertEquals(30_000L, MuteCommand.parseDuration("30s"));
        assertEquals(300_000L, MuteCommand.parseDuration("5m"));
        assertEquals(7_200_000L, MuteCommand.parseDuration("2h"));
        assertEquals(86_400_000L, MuteCommand.parseDuration("1d"));
        assertEquals(-1L, MuteCommand.parseDuration("perm"));
        assertEquals(-1L, MuteCommand.parseDuration("permanent"));
        assertEquals(-2L, MuteCommand.parseDuration("invalid_time"));
    }

    @Test
    @DisplayName("測試禁言條目 (MuteEntry) 倒數與過期狀態計算")
    void testMuteEntryExpiration() {
        UUID uuid = UUID.randomUUID();
        long now = System.currentTimeMillis();

        // 永久禁言
        MuteManager.MuteEntry permEntry = new MuteManager.MuteEntry(uuid, "testPlayer", "reason", now, -1, "admin");
        assertTrue(permEntry.isPermanent());
        assertFalse(permEntry.isExpired());
        assertEquals(-1L, permEntry.getRemainingMillis());

        // 已過期禁言
        MuteManager.MuteEntry expiredEntry = new MuteManager.MuteEntry(uuid, "testPlayer", "reason", now - 10000, now - 1000, "admin");
        assertFalse(expiredEntry.isPermanent());
        assertTrue(expiredEntry.isExpired());
        assertEquals(0L, expiredEntry.getRemainingMillis());

        // 有效未過期禁言
        MuteManager.MuteEntry activeEntry = new MuteManager.MuteEntry(uuid, "testPlayer", "reason", now, now + 60000, "admin");
        assertFalse(activeEntry.isPermanent());
        assertFalse(activeEntry.isExpired());
        assertTrue(activeEntry.getRemainingMillis() > 0);
    }
}
