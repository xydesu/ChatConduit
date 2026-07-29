package me.xydesu.chatconduit.gui;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 測試 GUIManager 之配置解析與工具方法
 *
 * @author xydesu
 */
class GUIManagerTest {

    @Test
    @DisplayName("測試 GUIManager Slot 與 Slots 讀取解析")
    void testGetSlotAndSlots() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("title", "<gradient:#00d2ff:#3a7bd5><bold>頻道選擇</bold></gradient>");
        config.set("size", 54);
        config.set("slots.system-channels", java.util.List.of(10, 11, 12, 13));
        config.set("items.create-channel.slot", 47);

        int slot = GUIManager.getSlot(config, "create-channel", 0);
        assertEquals(47, slot);

        int missingSlot = GUIManager.getSlot(config, "non-existent", 99);
        assertEquals(99, missingSlot);

        int[] sysSlots = GUIManager.getSlots(config, "slots.system-channels", new int[]{});
        assertArrayEquals(new int[]{10, 11, 12, 13}, sysSlots);

        int[] defaultSlots = GUIManager.getSlots(config, "slots.missing", new int[]{1, 2, 3});
        assertArrayEquals(new int[]{1, 2, 3}, defaultSlots);
    }
}
