package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChannelSelectGUI {

    public static void open(Player player) {
        open(player, 1);
    }

    public static void open(Player player, int page) {
        FileConfiguration config = GUIManager.getConfig("channel_select");

        String titleStr = GUIManager.getTitle("channel_select", Main.getInstance().getLanguageConfig().getString(
                "gui.title-channel-select",
                "<gradient:#00d2ff:#3a7bd5><bold>聊天頻道控制台</bold></gradient>"
        ));
        Component titleComponent = ChatUtils.parse(player, titleStr);

        int size = GUIManager.getSize("channel_select", 54);
        GUIHolder holder = new GUIHolder(GUIHolder.GUIType.CHANNEL_SELECT, null, page);
        Inventory inv = Bukkit.createInventory(holder, size, titleComponent);
        holder.setInventory(inv);

        String currentChannelKey = ChannelManager.getPlayerSelectedKey(player);

        // 滿填灰色玻璃邊框與分隔線裝飾
        ItemStack glassFiller = GUIManager.createItem(config, "filler-glass", Material.GRAY_STAINED_GLASS_PANE, null);
        int[] fillerSlots = GUIManager.getSlots(config, "items.filler-glass.slots", new int[]{0,1,2,3,4,5,6,7,8,9,17,18,26,44,45,53});
        for (int s : fillerSlots) {
            if (s < size) inv.setItem(s, glassFiller);
        }

        ItemStack lineFiller = GUIManager.createItem(config, "divider-line", Material.BLUE_STAINED_GLASS_PANE, null);
        int[] lineSlots = GUIManager.getSlots(config, "items.divider-line.slots", new int[]{27,28,29,30,31,32,33,34,35});
        for (int s : lineSlots) {
            if (s < size) inv.setItem(s, lineFiller);
        }

        // 系統公用頻道圖示
        int[] sysSlots = GUIHolder.getSysSlots();
        int sysSlotIdx = 0;
        for (ChannelManager.Channel sysChan : ChannelManager.getChannels().values()) {
            if (sysSlotIdx >= sysSlots.length) break;

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
            int targetSlot = sysSlots[sysSlotIdx++];
            if (targetSlot < size) inv.setItem(targetSlot, item);
        }

        // 玩家自訂群組頻道
        List<PlayerChannelManager.CustomChannel> availableChannels = new ArrayList<>();
        for (PlayerChannelManager.CustomChannel custChan : PlayerChannelManager.getCustomChannels().values()) {
            boolean isMember = custChan.getMembers().contains(player.getUniqueId());
            boolean isPublic = custChan.getMode() == PlayerChannelManager.Mode.PUBLIC;
            if (isMember || isPublic) {
                availableChannels.add(custChan);
            }
        }

        int[] custSlots = GUIHolder.getCustSlots();
        int perPage = custSlots.length > 0 ? custSlots.length : 7;

        int totalPages = Math.max(1, (int) Math.ceil(availableChannels.size() / (double) perPage));
        int currentPage = Math.min(Math.max(1, page), totalPages);

        int startIndex = (currentPage - 1) * perPage;
        int endIndex = Math.min(availableChannels.size(), currentPage * perPage);

        int custSlotIdx = 0;
        for (int i = startIndex; i < endIndex; i++) {
            if (custSlotIdx >= custSlots.length) break;
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
            int targetSlot = custSlots[custSlotIdx++];
            if (targetSlot < size) inv.setItem(targetSlot, item);
        }

        // 分頁控制按鈕
        if (currentPage > 1) {
            int slot = GUIManager.getSlot(config, "prev-page", 46);
            ItemStack prevPage = GUIManager.createItem(config, "prev-page", Material.ARROW, Map.of("<page>", String.valueOf(currentPage - 1)));
            if (slot < size) inv.setItem(slot, prevPage);
        }

        if (currentPage < totalPages) {
            int slot = GUIManager.getSlot(config, "next-page", 52);
            ItemStack nextPage = GUIManager.createItem(config, "next-page", Material.ARROW, Map.of("<page>", String.valueOf(currentPage + 1)));
            if (slot < size) inv.setItem(slot, nextPage);
        }

        // 底部功能按鈕
        int createSlot = GUIManager.getSlot(config, "create-channel", 47);
        if (createSlot < size) {
            inv.setItem(createSlot, GUIManager.createItem(config, "create-channel", Material.EMERALD, null));
        }

        int manageSlot = GUIManager.getSlot(config, "manage-channel", 48);
        if (manageSlot < size) {
            inv.setItem(manageSlot, GUIManager.createItem(config, "manage-channel", Material.NAME_TAG, null));
        }

        int closeSlot = GUIManager.getSlot(config, "close-menu", 49);
        if (closeSlot < size) {
            inv.setItem(closeSlot, GUIManager.createItem(config, "close-menu", Material.BARRIER, null));
        }

        int pendingCount = 0;
        for (PlayerChannelManager.CustomChannel c : PlayerChannelManager.getCustomChannels().values()) {
            if (c.getPendingInvites().contains(player.getUniqueId())) {
                pendingCount++;
            }
        }
        int inviteSlot = GUIManager.getSlot(config, "pending-invites", 50);
        if (inviteSlot < size) {
            Map<String, String> replacements = new HashMap<>();
            replacements.put("<pending_badge>", pendingCount > 0 ? "<red>(" + pendingCount + ")" : "");
            replacements.put("<pending_count>", String.valueOf(pendingCount));
            inv.setItem(inviteSlot, GUIManager.createItem(config, "pending-invites", Material.WRITABLE_BOOK, replacements));
        }

        int msgSettingsSlot = GUIManager.getSlot(config, "message-settings", 51);
        if (msgSettingsSlot < size) {
            inv.setItem(msgSettingsSlot, GUIManager.createItem(config, "message-settings", Material.BELL, null));
        }

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
}
