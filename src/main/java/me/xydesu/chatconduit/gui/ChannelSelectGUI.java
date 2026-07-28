package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ChannelSelectGUI {

    public static void open(Player player) {
        String titleStr = Main.getInstance().getLanguageConfig().getString(
                "gui.title-channel-select",
                "<gradient:#00d2ff:#3a7bd5><bold>聊天頻道控制台</bold></gradient>"
        );
        Component titleComponent = ChatUtils.parse(player, titleStr);

        GUIHolder holder = new GUIHolder(GUIHolder.GUIType.CHANNEL_SELECT);
        Inventory inv = Bukkit.createInventory(holder, 54, titleComponent);

        String currentChannelKey = ChannelManager.getPlayerSelectedKey(player);

        // 邊框與背景裝飾
        ItemStack glassFiller = createItem(Material.GRAY_STAINED_GLASS_PANE, "<gray> ");
        for (int i = 0; i < 9; i++) {
            inv.setItem(i, glassFiller);
            inv.setItem(i + 45, glassFiller);
        }
        for (int row = 1; row < 5; row++) {
            inv.setItem(row * 9, glassFiller);
            inv.setItem(row * 9 + 8, glassFiller);
        }

        // 系統公用頻道圖示映射表
        int sysIndex = 10; // 從 2 行開始擺放
        for (ChannelManager.Channel sysChan : ChannelManager.getChannels().values()) {
            if (sysIndex >= 26) break; // 避免超過區域

            boolean isSelected = currentChannelKey.equalsIgnoreCase(sysChan.key());
            boolean hasPermission = sysChan.permission().isEmpty() || player.hasPermission(sysChan.permission());

            Material material = getMaterialForSystemChannel(sysChan.key());
            String displayName = sysChan.color() + "<bold>" + sysChan.name() + " 頻道</bold>";

            List<String> lore = new ArrayList<>();
            lore.add("<gray>頻道 Key: <yellow>" + sysChan.key());
            lore.add("<gray>快速對話字首: <gold>" + (sysChan.prefixKey().isEmpty() ? "無" : sysChan.prefixKey()));
            lore.add("<gray>進入權限: " + (hasPermission ? "<green>✓ 擁有" : "<red>✗ 無權限"));
            lore.add("");
            if (isSelected) {
                lore.add("<green><bold>✓ 目前選取的發言頻道</bold>");
            } else if (hasPermission) {
                lore.add("<yellow>▶ 點擊切換至此頻道</yellow>");
            } else {
                lore.add("<red>❌ 無權限切換至此頻道</red>");
            }

            ItemStack item = createItem(material, displayName, lore, isSelected);
            inv.setItem(sysIndex++, item);

            // 跳過邊界 slot
            if (sysIndex % 9 == 8) sysIndex += 2;
        }

        // 分隔線
        ItemStack lineFiller = createItem(Material.BLUE_STAINED_GLASS_PANE, "<blue> ");
        for (int i = 27; i < 36; i++) {
            inv.setItem(i, lineFiller);
        }

        // 玩家自訂群組頻道 (底層)
        int custIndex = 37;
        for (PlayerChannelManager.CustomChannel custChan : PlayerChannelManager.getCustomChannels().values()) {
            if (custIndex >= 44) break;
            if (!custChan.getMembers().contains(player.getUniqueId())) continue;

            boolean isSelected = currentChannelKey.equalsIgnoreCase(custChan.getId());
            String displayName = "<gradient:#a8c0ff:#3f2b96><bold>" + custChan.getDisplayName() + "</bold></gradient>";

            List<String> lore = new ArrayList<>();
            lore.add("<gray>類型: <gold>玩家群組頻道");
            lore.add("<gray>成員人數: <yellow>" + custChan.getMembers().size() + " 人");
            lore.add("");
            if (isSelected) {
                lore.add("<green><bold>✓ 目前選取的發言頻道</bold>");
            } else {
                lore.add("<yellow>▶ 點擊切換至此群組頻道</yellow>");
            }

            ItemStack item = createItem(Material.BOOKSHELF, displayName, lore, isSelected);
            inv.setItem(custIndex++, item);
        }

        // 底部功能按鈕
        // Slot 48: 群組頻道管理
        ItemStack manageItem = createItem(Material.NAME_TAG, "<gold><bold>⚙ 群組頻道管理</bold>", List.of(
                "<gray>查看與管理我的群組頻道",
                "<gray>包含成員列表、踢人與模式切換",
                "",
                "<yellow>▶ 點擊開啟管理面板</yellow>"
        ), false);
        inv.setItem(48, manageItem);

        // Slot 49: 關閉選單
        ItemStack closeItem = createItem(Material.BARRIER, "<red><bold>✖ 關閉選單</bold>", List.of("<gray>點擊關閉此介面"), false);
        inv.setItem(49, closeItem);

        // Slot 50: 待處理邀請
        int pendingCount = 0;
        for (PlayerChannelManager.CustomChannel c : PlayerChannelManager.getCustomChannels().values()) {
            if (c.getPendingInvites().contains(player.getUniqueId())) {
                pendingCount++;
            }
        }
        String inviteName = "<green><bold>✉ 頻道邀請</bold> " + (pendingCount > 0 ? "<red>(" + pendingCount + ")" : "");
        ItemStack inviteItem = createItem(Material.WRITABLE_BOOK, inviteName, List.of(
                "<gray>查看收到的群組頻道邀請",
                "<gray>當前未處理邀請: <yellow>" + pendingCount + " 個",
                "",
                "<yellow>▶ 點擊開啟邀請選單</yellow>"
        ), pendingCount > 0);
        inv.setItem(50, inviteItem);

        player.openInventory(inv);
    }

    private static Material getMaterialForSystemChannel(String key) {
        return switch (key.toLowerCase()) {
            case "global" -> Material.BEACON;
            case "trade" -> Material.GOLD_INGOT;
            case "party" -> Material.GREEN_BANNER;
            case "ask" -> Material.BOOK;
            case "chitchat" -> Material.FEATHER;
            case "facility" -> Material.COMPASS;
            case "lottery" -> Material.NETHER_STAR;
            default -> Material.PAPER;
        };
    }

    private static ItemStack createItem(Material material, String name, List<String> lore, boolean glow) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ChatUtils.parse(null, name));
            if (lore != null && !lore.isEmpty()) {
                List<Component> parsedLore = new ArrayList<>();
                for (String line : lore) {
                    parsedLore.add(ChatUtils.parse(null, line));
                }
                meta.lore(parsedLore);
            }
            if (glow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createItem(Material material, String name) {
        return createItem(material, name, null, false);
    }
}
