package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ChannelSettingsGUI {

    public static void open(Player player, PlayerChannelManager.CustomChannel customChan) {
        String titleStr = "<gradient:#00d2ff:#3a7bd5><bold>頻道設定 - " + customChan.getDisplayName() + "</bold></gradient>";
        Component titleComponent = ChatUtils.parse(player, titleStr);

        GUIHolder holder = new GUIHolder(GUIHolder.GUIType.CHANNEL_SETTINGS, customChan.getId());
        Inventory inv = Bukkit.createInventory(holder, 27, titleComponent);

        // 裝飾
        ItemStack glassFiller = createItem(Material.GRAY_STAINED_GLASS_PANE, "<gray> ");
        for (int i = 0; i < 9; i++) inv.setItem(i, glassFiller);
        for (int i = 18; i < 27; i++) inv.setItem(i, glassFiller);

        // Slot 11: 存取模式切換
        boolean isPublic = customChan.getMode() == PlayerChannelManager.Mode.PUBLIC;
        Material modeMat = isPublic ? Material.OAK_DOOR : Material.IRON_DOOR;
        String modeTitle = "<gold><bold>1. 存取權限模式</bold>";
        List<String> modeLore = List.of(
                "<gray>當前模式: " + (isPublic ? "<green>公共 (PUBLIC)" : "<red>私人 (PRIVATE)"),
                "<gray>說明: " + (isPublic ? "所有人可在選單自由加入" : "僅限隊長邀請加入"),
                "",
                "<yellow>▶ 點擊切換模式 (PUBLIC / PRIVATE)</yellow>"
        );
        inv.setItem(11, createItem(modeMat, modeTitle, modeLore));

        // Slot 13: 頻道顯示名稱重命名
        String nameTitle = "<gold><bold>2. 修改頻道顯示名稱</bold>";
        List<String> nameLore = List.of(
                "<gray>當前顯示名稱: " + customChan.getColorTheme() + customChan.getDisplayName() + "</gradient>",
                "<gray>說明: 在聊天面板呈現的頻道抬頭名稱",
                "",
                "<yellow>▶ 點擊開啟對話框輸入新名稱</yellow>"
        );
        inv.setItem(13, createItem(Material.NAME_TAG, nameTitle, nameLore));

        // Slot 15: 頻道色彩主題切換
        String colorTitle = "<gold><bold>3. 頻道色彩主題樣式</bold>";
        List<String> colorLore = List.of(
                "<gray>當前色彩預設: " + customChan.getColorTheme() + "樣式預覽 [頻道名稱]</gradient>",
                "<gray>說明: 改變該群組在聊天欄顯示的色彩主題",
                "",
                "<yellow>▶ 點擊切換下一個色彩主題</yellow>"
        );
        inv.setItem(15, createItem(Material.CYAN_DYE, colorTitle, colorLore));

        // Slot 22: 返回
        inv.setItem(22, createItem(Material.ARROW, "<yellow><bold>← 返回群組管理</bold>", List.of("<gray>回到頻道管理頁面")));

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

    private static ItemStack createItem(Material material, String name) {
        return createItem(material, name, null);
    }
}
