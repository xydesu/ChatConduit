package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelSettingsGUI {

    public static void open(Player player, PlayerChannelManager.CustomChannel customChan) {
        FileConfiguration config = GUIManager.getConfig("channel_settings");

        String titleStr = GUIManager.getTitle("channel_settings", "<gradient:#00d2ff:#3a7bd5><bold>頻道詳細設定 - <channel_name></bold></gradient>")
                .replace("<channel_name>", customChan.getDisplayName());
        Component titleComponent = ChatUtils.parse(player, titleStr);

        int size = GUIManager.getSize("channel_settings", 27);
        GUIHolder holder = new GUIHolder(GUIHolder.GUIType.CHANNEL_SETTINGS, customChan.getId());
        Inventory inv = Bukkit.createInventory(holder, size, titleComponent);

        // 裝飾邊框
        ItemStack glassFiller = GUIManager.createItem(config, "filler-glass", Material.GRAY_STAINED_GLASS_PANE, null);
        int[] fillerSlots = GUIManager.getSlots(config, "items.filler-glass.slots", new int[]{0,1,2,3,4,5,6,7,8,9,15,18,19,20,21,23,24,25,26});
        for (int s : fillerSlots) {
            if (s < size) inv.setItem(s, glassFiller);
        }

        // Slot 10: 存取模式切換
        boolean isPublic = customChan.getMode() == PlayerChannelManager.Mode.PUBLIC;
        Material modeMat = isPublic ? Material.OAK_DOOR : Material.IRON_DOOR;
        Map<String, String> modeReplacements = Map.of(
                "<mode_status>", isPublic ? "<green>公共 (PUBLIC)</green>" : "<red>私人 (PRIVATE)</red>",
                "<mode_desc>", isPublic ? "所有人可在選單自由加入" : "僅限隊長邀請加入"
        );
        int modeSlot = GUIManager.getSlot(config, "access-mode", 10);
        if (modeSlot < size) {
            inv.setItem(modeSlot, GUIManager.createItem(config, "access-mode", modeMat, modeReplacements));
        }

        // Slot 11: 頻道顯示名稱重命名
        int renameSlot = GUIManager.getSlot(config, "rename-channel", 11);
        if (renameSlot < size) {
            Map<String, String> renameReplacements = Map.of(
                    "<current_name>", customChan.getColorTheme() + customChan.getDisplayName() + "</gradient>"
            );
            inv.setItem(renameSlot, GUIManager.createItem(config, "rename-channel", Material.NAME_TAG, renameReplacements));
        }

        // Slot 12: 頻道簡介說明設定
        int descSlot = GUIManager.getSlot(config, "channel-description", 12);
        if (descSlot < size) {
            Map<String, String> descReplacements = Map.of(
                    "<current_desc>", customChan.getDescription()
            );
            inv.setItem(descSlot, GUIManager.createItem(config, "channel-description", Material.BOOK, descReplacements));
        }

        // Slot 13: 頻道規則守則設定
        int rulesSlot = GUIManager.getSlot(config, "channel-rules", 13);
        if (rulesSlot < size) {
            Map<String, String> rulesReplacements = Map.of(
                    "<current_rules>", customChan.getRules()
            );
            inv.setItem(rulesSlot, GUIManager.createItem(config, "channel-rules", Material.PAPER, rulesReplacements));
        }

        // Slot 14: 頻道色彩主題切換
        int colorSlot = GUIManager.getSlot(config, "color-theme", 14);
        if (colorSlot < size) {
            Map<String, String> colorReplacements = Map.of(
                    "<theme_preview>", customChan.getColorTheme() + "樣式預覽 [頻道名稱]</gradient>"
            );
            inv.setItem(colorSlot, GUIManager.createItem(config, "color-theme", Material.CYAN_DYE, colorReplacements));
        }

        // Slot 16: 專屬 Webhook 網址綁定
        String webhookUrl = customChan.getWebhookUrl();
        boolean hasWebhook = webhookUrl != null && !webhookUrl.trim().isEmpty();
        int hookSlot = GUIManager.getSlot(config, "webhook-setting", 16);
        if (hookSlot < size) {
            Map<String, String> hookReplacements = Map.of(
                    "<webhook_status>", hasWebhook ? "<green>✓ 已綁定 Webhook</green>" : "<red>✗ 未綁定 (單向純遊戲內頻道)</red>",
                    "<webhook_preview>", hasWebhook ? (webhookUrl.length() > 30 ? webhookUrl.substring(0, 27) + "..." : webhookUrl) : "無"
            );
            inv.setItem(hookSlot, GUIManager.createItem(config, "webhook-setting", Material.PLAYER_HEAD, hookReplacements));
        }

        // Slot 17: 測試 Webhook 連線
        int testSlot = GUIManager.getSlot(config, "test-webhook", 17);
        if (testSlot < size) {
            Material testMat = hasWebhook ? Material.TARGET : Material.GRAY_DYE;
            String testTitle = "<gold><bold>7. 測試 Webhook 連線</bold>";
            List<String> testLore = hasWebhook ? List.of(
                    "<gray>狀態: <green>已綁定，可進行測試",
                    "<gray>說明: 發送測試 Payload 驗證 Discord 接收狀態",
                    "",
                    "<yellow>▶ 點擊發送測試訊息至 Discord 頻道</yellow>"
            ) : List.of(
                    "<gray>狀態: <red>未綁定 Webhook</red>",
                    "<gray>說明: 必須先設定 Webhook 網址後才能發送測試訊息",
                    "",
                    "<red>✕ 請點擊左側圖示輸入 URL 綁定</red>"
            );
            inv.setItem(testSlot, createItem(testMat, testTitle, testLore));
        }

        // Slot 22: 返回
        int backSlot = GUIManager.getSlot(config, "back-button", 22);
        if (backSlot < size) {
            inv.setItem(backSlot, GUIManager.createItem(config, "back-button", Material.ARROW, null));
        }

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ChatUtils.parseNoItalic(name));
            if (lore != null && !lore.isEmpty()) {
                List<Component> parsedLore = new ArrayList<>();
                for (String line : lore) {
                    parsedLore.add(ChatUtils.parseNoItalic(line));
                }
                meta.lore(parsedLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }
}
