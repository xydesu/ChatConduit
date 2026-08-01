package me.xydesu.chatconduit.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 測試 WebhookManager 內部輔助功能 (InteractiveChat 佔位符淨化與冷卻快取管理)
 *
 * @author xydesu
 */
class WebhookManagerTest {

    @Test
    @DisplayName("測試 removeCooldown 不會引發 Exception")
    void testRemoveCooldown() {
        assertDoesNotThrow(() -> WebhookManager.removeCooldown("test-channel"));
        assertDoesNotThrow(() -> WebhookManager.removeCooldown(null));
    }
}
