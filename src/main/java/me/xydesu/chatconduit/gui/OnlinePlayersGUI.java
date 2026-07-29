package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class OnlinePlayersGUI {

    public static void open(Player inviter, PlayerChannelManager.CustomChannel customChan) {
        String titleStr = "<green><bold>選擇邀請玩家 - " + customChan.getDisplayName() + "</bold></green>";
        Component titleComponent = ChatUtils.parse(inviter, titleStr);

        GUIHolder holder = new GUIHolder(GUIHolder.GUIType.ONLINE_PLAYERS_SELECT, customChan.getId());
        Inventory inv = Bukkit.createInventory(holder, 54, titleComponent);

        // 裝飾邊框
        ItemStack glassFiller = createItem(Material.GRAY_STAINED_GLASS_PANE, "<gray> ");
        for (int i = 0; i < 9; i++) inv.setItem(i, glassFiller);
        for (int i = 45; i < 54; i++) inv.setItem(i, glassFiller);

        int slot = 9;
        java.util.Set<UUID> addedUuids = new java.util.HashSet<>();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (slot >= 45) break;

            // 過濾已是成員的玩家
            if (customChan.getMembers().contains(onlinePlayer.getUniqueId())) continue;
            addedUuids.add(onlinePlayer.getUniqueId());

            boolean isInvited = customChan.getPendingInvites().contains(onlinePlayer.getUniqueId());
            String curChanKey = ChannelManager.getPlayerSelectedKey(onlinePlayer);

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setPlayerProfile(onlinePlayer.getPlayerProfile());
                meta.displayName(ChatUtils.parseNoItalic("<white><bold>" + onlinePlayer.getName() + "</bold>"));

                List<Component> lore = new ArrayList<>();
                lore.add(ChatUtils.parseNoItalic("<gray>連線狀態: <green>● 本服線上 (" + onlinePlayer.getWorld().getName() + ")</green>"));
                lore.add(ChatUtils.parseNoItalic("<gray>當前頻道: <yellow>" + curChanKey + "</yellow>"));
                lore.add(ChatUtils.parseNoItalic("<gray>連線延遲: <yellow>" + onlinePlayer.getPing() + " ms</yellow>"));
                lore.add(ChatUtils.parseNoItalic(""));
                if (isInvited) {
                    lore.add(ChatUtils.parseNoItalic("<gold>✉ 已發送邀請 (等待對方接受)</gold>"));
                } else {
                    lore.add(ChatUtils.parseNoItalic("<yellow>▶ 點擊發送邀請給此玩家</yellow>"));
                }
                meta.lore(lore);
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);
        }

        // 補載入跨服/已知玩家頭顱
        if (slot < 45) {
            List<me.xydesu.chatconduit.database.dao.PlayerDAO.PlayerData> knownPlayers = me.xydesu.chatconduit.database.dao.PlayerDAO.getAllKnownPlayers();
            for (me.xydesu.chatconduit.database.dao.PlayerDAO.PlayerData known : knownPlayers) {
                if (slot >= 45) break;
                if (known.uuid() == null || known.uuid().equals(inviter.getUniqueId())) continue;
                if (addedUuids.contains(known.uuid())) continue;
                if (customChan.getMembers().contains(known.uuid())) continue;

                addedUuids.add(known.uuid());
                boolean isInvited = customChan.getPendingInvites().contains(known.uuid());

                ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                SkullMeta meta = (SkullMeta) head.getItemMeta();
                if (meta != null) {
                    org.bukkit.OfflinePlayer offP = Bukkit.getOfflinePlayer(known.uuid());
                    meta.setOwningPlayer(offP);

                    String pName = known.playerName() != null ? known.playerName() : offP.getName();
                    if (pName == null) pName = "跨服玩家";
                    meta.displayName(ChatUtils.parseNoItalic("<white><bold>" + pName + "</bold>"));

                    List<Component> lore = new ArrayList<>();
                    lore.add(ChatUtils.parseNoItalic("<gray>連線狀態: <yellow>● 跨服/已知玩家</yellow>"));
                    lore.add(ChatUtils.parseNoItalic("<gray>記錄頻道: <yellow>" + (known.currentChannel() != null ? known.currentChannel() : "無") + "</yellow>"));
                    lore.add(ChatUtils.parseNoItalic(""));
                    if (isInvited) {
                        lore.add(ChatUtils.parseNoItalic("<gold>✉ 已發送邀請 (等待對方接受)</gold>"));
                    } else {
                        lore.add(ChatUtils.parseNoItalic("<yellow>▶ 點擊發送跨服邀請給此玩家</yellow>"));
                    }
                    meta.lore(lore);
                    head.setItemMeta(meta);
                }
                inv.setItem(slot++, head);
            }
        }

        // Slot 45: 返回
        inv.setItem(45, createItem(Material.ARROW, "<yellow><bold>← 返回頻道管理</bold>", List.of("<gray>回到頻道管理頁面")));

        // Slot 53: 手動輸入玩家 ID 進行邀請
        inv.setItem(53, createItem(Material.NAME_TAG, "<green><bold>⌨ 手動輸入玩家 ID 進行邀請</bold>", List.of(
                "<gray>可在對話框手動輸入目標玩家名稱進行邀請",
                "",
                "<yellow>▶ 點擊開啟手動輸入對話框</yellow>"
        )));

        inviter.openInventory(inv);
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
