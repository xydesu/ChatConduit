package me.xydesu.chatconduit.channel;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 測試前綴符號 (prefix-key) 匹配邏輯
 * 確保單獨輸入與前綴符號相同的訊息時不觸發快捷切換，而是直接作為普通發言發送
 *
 * @author xydesu
 */
class PrefixKeyMatchingTest {

    /**
     * 模擬 ChatListener 與 DiscordSRVListener 的前綴匹配判斷邏輯
     */
    private boolean isPrefixMatched(String rawMessage, String prefixKey) {
        if (prefixKey == null || prefixKey.isEmpty()) {
            return false;
        }
        return rawMessage.startsWith(prefixKey) && !rawMessage.equals(prefixKey);
    }

    @Test
    @DisplayName("測試單獨輸入前綴符號時不應觸發前綴頻道快捷匹配")
    void testSinglePrefixSymbolNotMatched() {
        assertFalse(isPrefixMatched("!", "!"), "單獨輸入 ! 不應匹配快捷頻道");
        assertFalse(isPrefixMatched("$", "$"), "單獨輸入 $ 不應匹配快捷頻道");
        assertFalse(isPrefixMatched("+", "+"), "單獨輸入 + 不應匹配快捷頻道");
        assertFalse(isPrefixMatched("?", "?"), "單獨輸入 ? 不應匹配快捷頻道");
    }

    @Test
    @DisplayName("測試前綴符號後附帶內容時應正常匹配快捷頻道")
    void testPrefixWithContentMatched() {
        assertTrue(isPrefixMatched("!hello", "!"), "輸入 !hello 應匹配快捷頻道");
        assertTrue(isPrefixMatched("$100", "$"), "輸入 $100 應匹配快捷頻道");
        assertTrue(isPrefixMatched("+party", "+"), "輸入 +party 應匹配快捷頻道");
    }
}
