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

        open(player, 1);
    }

    public static void open(Player player, int page) {
        String titleStr = Main.getInstance().getLanguageConfig().getString(
                "gui.title-channel-select",
                "<gradient:#00d2ff:#3a7bd5><bold>聊天頻道控制台</bold></gradient>"
        );
        Component titleComponent = ChatUtils.parse(player, titleStr);

        GUIHolder holder = new GUIHolder(GUIHolder.GUIType.CHANNEL_SELECT, null, page);
        Inventory inv = Bukkit.createInventory(holder, 54, titleComponent);
        holder.setInventory(inv);

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

        // 系統公用頻道圖示 (Row 2 ~ 3)
        int sysSlotIdx = 0;
        for (ChannelManager.Channel sysChan : ChannelManager.getChannels().values()) {
            if (sysSlotIdx >= GUIHolder.SYS_SLOTS.length) break;

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
            inv.setItem(GUIHolder.SYS_SLOTS[sysSlotIdx++], item);
        }

        // 分隔線
        ItemStack lineFiller = createItem(Material.BLUE_STAINED_GLASS_PANE, "<blue> ");
        for (int i = 27; i < 36; i++) {
            inv.setItem(i, lineFiller);
        }

        // 玩家自訂與公開群組頻道 (Row 5 - 支援分頁)
        List<PlayerChannelManager.CustomChannel> availableChannels = new ArrayList<>();
        for (PlayerChannelManager.CustomChannel custChan : PlayerChannelManager.getCustomChannels().values()) {
            boolean isMember = custChan.getMembers().contains(player.getUniqueId());
            boolean isPublic = custChan.getMode() == PlayerChannelManager.Mode.PUBLIC;
            if (isMember || isPublic) {
                availableChannels.add(custChan);
            }
        }

        int totalPages = Math.max(1, (int) Math.ceil(availableChannels.size() / 7.0));
        int currentPage = Math.min(Math.max(1, page), totalPages);

        int startIndex = (currentPage - 1) * 7;
        int endIndex = Math.min(availableChannels.size(), currentPage * 7);

        int custSlotIdx = 0;
        for (int i = startIndex; i < endIndex; i++) {
            PlayerChannelManager.CustomChannel custChan = availableChannels.get(i);
            boolean isMember = custChan.getMembers().contains(player.getUniqueId());
            boolean isPublic = custChan.getMode() == PlayerChannelManager.Mode.PUBLIC;
            boolean isSelected = currentChannelKey.equalsIgnoreCase(custChan.getId());
            String displayName = "<gradient:#a8c0ff:#3f2b96><bold>" + custChan.getDisplayName() + "</bold></gradient>";

            List<String> lore = new ArrayList<>();
            lore.add("<gray>類型: <gold>玩家群組頻道 (" + (isPublic ? "<green>PUBLIC" : "<red>PRIVATE") + "<gray>)");
            lore.add("<gray>成員人數: <yellow>" + custChan.getMembers().size() + " 人");
            lore.add("");
            if (isSelected) {
                lore.add("<green><bold>✓ 目前選取的發言頻道</bold>");
            } else if (isMember) {
                lore.add("<yellow>▶ 左鍵點擊: 切換發言至此頻道</yellow>");
            } else {
                lore.add("<gold>▶ 左鍵點擊: 加入此公開頻道</gold>");
            }

            if (isMember) {
                lore.add("<gold>▶ 右鍵點擊: 開啟此頻道管理面板</gold>");
            }

            ItemStack item = createItem(Material.BOOKSHELF, displayName, lore, isSelected);
            inv.setItem(GUIHolder.CUST_SLOTS[custSlotIdx++], item);
        }


        // 分頁控制按鈕
        if (currentPage > 1) {
            ItemStack prevPage = createItem(Material.ARROW, "<yellow>◀ 上一頁 (第 " + (currentPage - 1) + " 頁)</yellow>");
            inv.setItem(46, prevPage);
        }
        if (currentPage < totalPages) {
            ItemStack nextPage = createItem(Material.ARROW, "<yellow>下一頁 ▶ (第 " + (currentPage + 1) + " 頁)</yellow>");
            inv.setItem(52, nextPage);
        }

        // 底部功能按鈕
        // Slot 47: ＋ 建立新頻道
        ItemStack createChannelItem = createItem(Material.EMERALD, "<green><bold>＋ 建立新群組頻道</bold>", List.of(
                "<gray>點擊即可輸入名稱建立自己的專屬頻道",
                "",
                "<yellow>▶ 點擊進行建立</yellow>"
        ), false);
        inv.setItem(47, createChannelItem);

        // Slot 48: 群組頻道管理說明/按鈕
        ItemStack manageItem = createItem(Material.NAME_TAG, "<gold><bold>⚙ 群組頻道管理</bold>", List.of(
                "<gray>提示：在上方自訂群組頻道圖示上",
                "<gold>右鍵點擊</gold> <gray>即可直接開啟該頻道管理面板！",
                "",
                "<yellow>▶ 點擊開啟目前所屬群組面板</yellow>"
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
            meta.displayName(ChatUtils.parseNoItalic(name));
            if (lore != null && !lore.isEmpty()) {
                List<Component> parsedLore = new ArrayList<>();
                for (String line : lore) {
                    parsedLore.add(ChatUtils.parseNoItalic(line));
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
