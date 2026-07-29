package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class PendingInvitesGUI {

    public static void open(Player player) {
        FileConfiguration config = GUIManager.getConfig("pending_invites");

        String titleStr = GUIManager.getTitle("pending_invites", "<green><bold>頻道邀請列表</bold></green>");
        Component titleComponent = ChatUtils.parse(player, titleStr);

        int size = GUIManager.getSize("pending_invites", 27);
        GUIHolder holder = new GUIHolder(GUIHolder.GUIType.PENDING_INVITES);
        Inventory inv = Bukkit.createInventory(holder, size, titleComponent);

        // 裝飾邊框
        ItemStack glassFiller = GUIManager.createItem(config, "filler-glass", Material.GRAY_STAINED_GLASS_PANE, null);
        int[] fillerSlots = GUIManager.getSlots(config, "items.filler-glass.slots", new int[]{0,1,2,3,4,5,6,7,8,18,19,20,21,23,24,25,26});
        for (int s : fillerSlots) {
            if (s < size) inv.setItem(s, glassFiller);
        }

        int[] paperSlots = GUIManager.getSlots(config, "slots.invite-papers", new int[]{9,10,11,12,13,14,15,16,17});
        int paperIdx = 0;

        for (PlayerChannelManager.CustomChannel custChan : PlayerChannelManager.getCustomChannels().values()) {
            if (paperIdx >= paperSlots.length) break;
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

            ItemStack item = createItem(Material.PAPER, itemName, lore);
            int slot = paperSlots[paperIdx++];
            if (slot < size) inv.setItem(slot, item);
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
