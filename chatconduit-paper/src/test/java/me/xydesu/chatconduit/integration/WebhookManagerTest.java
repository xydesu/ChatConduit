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
    @DisplayName("測試 InteractiveChat 內部標籤淨化 (cleanInteractiveChatPlaceholders)")
    void testCleanInteractiveChatPlaceholders() {
        String input1 = "Check this item: <chat=123e4567-e89b-12d3-a456-426614174000:[item]:>!";
        String cleaned1 = WebhookManager.cleanInteractiveChatPlaceholders(input1);
        assertEquals("Check this item: [item]!", cleaned1);

        String input2 = "Player ping: <chat=abc-def:[ping]:>";
        String cleaned2 = WebhookManager.cleanInteractiveChatPlaceholders(input2);
        assertEquals("Player ping: [ping]", cleaned2);

        String inputNormal = "Hello world! Normal message";
        assertEquals("Hello world! Normal message", WebhookManager.cleanInteractiveChatPlaceholders(inputNormal));
    }

    @Test
    @DisplayName("測試 removeCooldown 不會引發 Exception")
    void testRemoveCooldown() {
        assertDoesNotThrow(() -> WebhookManager.removeCooldown("test-channel"));
        assertDoesNotThrow(() -> WebhookManager.removeCooldown(null));
    }
}
