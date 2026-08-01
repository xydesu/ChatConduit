package me.xydesu.chatconduit.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 測試 ChatUtils 的 Legacy 顏色轉換與 Adventure Component 工具
 *
 * @author xydesu
 */
class ChatUtilsTest {

    @Test
    @DisplayName("測試 parseLegacy 舊版顏色碼解析")
    void testParseLegacy() {
        String legacyText = "&a[Global] &fHello &cWorld!";
        Component component = ChatUtils.parseLegacy(legacyText);

        assertNotNull(component);
        String plainText = PlainTextComponentSerializer.plainText().serialize(component);
        assertEquals("[Global] Hello World!", plainText);
    }

    @Test
    @DisplayName("測試 parseNoItalic 強制無斜體 Component 解析")
    void testParseNoItalic() {
        Component component = ChatUtils.parseNoItalic("<green>Test Message");
        assertNotNull(component);

        String plainText = PlainTextComponentSerializer.plainText().serialize(component);
        assertEquals("Test Message", plainText);
    }

    @Test
    @DisplayName("測試 translateLegacyToMiniMessage 顏色碼轉 MiniMessage")
    void testTranslateLegacyToMiniMessage() {
        String result1 = ChatUtils.translateLegacyToMiniMessage("[&c管理員]");
        assertEquals("[<red>管理員]", result1);

        String result2 = ChatUtils.translateLegacyToMiniMessage("[&a玩家]");
        assertEquals("[<green>玩家]", result2);

        String result3 = ChatUtils.translateLegacyToMiniMessage("&#FF5555Hex &x&1&2&3&4&5&6Color");
        assertEquals("<#FF5555>Hex <#123456>Color", result3);
    }

    @Test
    @DisplayName("測試 InteractiveChat 內部標籤淨化 (cleanInteractiveChatPlaceholders)")
    void testCleanInteractiveChatPlaceholders() {
        String input1 = "Look at this <chat=1234-5678:[item]:> is cool!";
        assertEquals("Look at this [\u200Bitem] is cool!", ChatUtils.cleanInteractiveChatPlaceholders(input1));

        String input2 = "Check my <chat=abcd-efgh:[inv]:> and <chat=9876-5432:[ender]:>!";
        assertEquals("Check my [\u200Binv] and [\u200Bender]!", ChatUtils.cleanInteractiveChatPlaceholders(input2));

        String input3 = "Ping: <chat=uuid-1234:ping:>";
        assertEquals("Ping: [\u200Bping]", ChatUtils.cleanInteractiveChatPlaceholders(input3));

        String input4 = "Look at <chat=1234-5678:[item]> without trailing colon";
        assertEquals("Look at [\u200Bitem] without trailing colon", ChatUtils.cleanInteractiveChatPlaceholders(input4));

        String input5 = "Check <ic=uuid-9999:item> and <interactivechat=uuid-8888:[ender]:>";
        assertEquals("Check [\u200Bitem] and [\u200Bender]", ChatUtils.cleanInteractiveChatPlaceholders(input5));

        String normal = "Normal chat message without IC tags.";
        assertEquals(normal, ChatUtils.cleanInteractiveChatPlaceholders(normal));
    }

    @Test
    @DisplayName("測試 cleanComponentInteractiveChatTags Component 內部標籤淨化")
    void testCleanComponentInteractiveChatTags() {
        Component comp = Component.text("Look at ").append(Component.text("<chat=1234-5678:[item]:>"));
        Component cleaned = ChatUtils.cleanComponentInteractiveChatTags(comp);

        assertNotNull(cleaned);
        String plainText = PlainTextComponentSerializer.plainText().serialize(cleaned);
        assertEquals("Look at [\u200Bitem]", plainText);
    }
}
