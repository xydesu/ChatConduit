package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PendingInvitesGUI {

    public static void open(Player player) {
        String titleStr = "<green><bold>頻道邀請列表</bold></green>";
        Component titleComponent = ChatUtils.parse(player, titleStr);

        GUIHolder holder = new GUIHolder(GUIHolder.GUIType.PENDING_INVITES);
        Inventory inv = Bukkit.createInventory(holder, 27, titleComponent);

        // 裝飾
        ItemStack glassFiller = createItem(Material.GRAY_STAINED_GLASS_PANE, "<gray> ");
        for (int i = 0; i < 9; i++) inv.setItem(i, glassFiller);
        for (int i = 18; i < 27; i++) inv.setItem(i, glassFiller);

        int slot = 9;
        for (PlayerChannelManager.CustomChannel custChan : PlayerChannelManager.getCustomChannels().values()) {
            if (slot >= 18) break;
            if (!custChan.getPendingInvites().contains(player.getUniqueId())) continue;

            OfflinePlayer ownerP = Bukkit.getOfflinePlayer(custChan.getOwner());
            String ownerName = ownerP.getName() != null ? ownerP.getName() : custChan.getOwner().toString();

            String itemName = "<gradient:#a8c0ff:#3f2b96><bold>" + custChan.getDisplayName() + "</bold></gradient>";
            List<String> lore = new ArrayList<>();
            lore.add("<gray>邀請者: <yellow>" + ownerName);
            lore.add("<gray>目前成員數: <yellow>" + custChan.getMembers().size() + " 人");
            lore.add("");
            lore.add("<green>▶ 左鍵點擊: 接受邀請並加入頻道</green>");
            lore.add("<red>▶ 右鍵點擊: 拒絕此邀請</red>");

            ItemStack item = createItem(Material.PAPER, itemName, lore, custChan.getId());
            inv.setItem(slot++, item);
        }

        // Slot 22: 返回
        inv.setItem(22, createItem(Material.ARROW, "<yellow><bold>← 返回頻道大廳</bold>", List.of("<gray>回到頻道選擇選單"), null));

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material material, String name, List<String> lore, String channelId) {
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
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createItem(Material material, String name) {
        return createItem(material, name, null, null);
    }
}
