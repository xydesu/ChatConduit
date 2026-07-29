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
}
