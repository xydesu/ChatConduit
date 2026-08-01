package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class OnlinePlayersGUI {

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.database.dao.PlayerDAO;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class OnlinePlayersGUI {

    public static void open(Player inviter, PlayerChannelManager.CustomChannel customChan) {
        if (inviter == null || !inviter.isOnline() || customChan == null) return;

        Bukkit.getAsyncScheduler().runNow(Main.getInstance(), task -> {
            List<PlayerDAO.PlayerData> knownPlayers = PlayerDAO.getAllKnownPlayers();

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (!inviter.isOnline()) return;

                FileConfiguration config = GUIManager.getConfig("online_players");

                String titleStr = GUIManager.getTitle("online_players", "<green><bold>選擇邀請玩家 - <channel_name></bold></green>")
                        .replace("<channel_name>", customChan.getDisplayName());
                Component titleComponent = ChatUtils.parse(inviter, titleStr);

                int size = GUIManager.getSize("online_players", 54);
                GUIHolder holder = new GUIHolder(GUIHolder.GUIType.ONLINE_PLAYERS_SELECT, customChan.getId());
                Inventory inv = Bukkit.createInventory(holder, size, titleComponent);

                // 裝飾邊框
                ItemStack glassFiller = GUIManager.createItem(config, "filler-glass", Material.GRAY_STAINED_GLASS_PANE, null);
                int[] fillerSlots = GUIManager.getSlots(config, "items.filler-glass.slots", new int[]{0,1,2,3,4,5,6,7,8,46,47,48,49,50,51,52});
                for (int s : fillerSlots) {
                    if (s < size) inv.setItem(s, glassFiller);
                }

                int[] headSlots = GUIManager.getSlots(config, "slots.player-heads", new int[]{
                        9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
                        27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
                });

                int headIdx = 0;
                Set<UUID> addedUuids = new HashSet<>();

                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (headIdx >= headSlots.length) break;

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
                    int slot = headSlots[headIdx++];
                    if (slot < size) inv.setItem(slot, head);
                }

                // 補載入跨服/已知玩家頭顱 (使用非阻塞 createProfile)
                if (headIdx < headSlots.length && knownPlayers != null) {
                    for (PlayerDAO.PlayerData known : knownPlayers) {
                        if (headIdx >= headSlots.length) break;
                        if (known.uuid() == null || known.uuid().equals(inviter.getUniqueId())) continue;
                        if (addedUuids.contains(known.uuid())) continue;
                        if (customChan.getMembers().contains(known.uuid())) continue;

                        addedUuids.add(known.uuid());
                        boolean isInvited = customChan.getPendingInvites().contains(known.uuid());

                        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                        SkullMeta meta = (SkullMeta) head.getItemMeta();
                        if (meta != null) {
                            String pName = known.playerName() != null ? known.playerName() : "跨服玩家";
                            try {
                                meta.setPlayerProfile(Bukkit.createProfile(known.uuid(), pName));
                            } catch (Exception ignored) {}

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
                        int slot = headSlots[headIdx++];
                        if (slot < size) inv.setItem(slot, head);
                    }
                }

                // Slot 45: 返回
                int backSlot = GUIManager.getSlot(config, "back-button", 45);
                if (backSlot < size) {
                    inv.setItem(backSlot, GUIManager.createItem(config, "back-button", Material.ARROW, null));
                }

                // Slot 53: 手動輸入玩家 ID 進行邀請
                int manualSlot = GUIManager.getSlot(config, "manual-invite", 53);
                if (manualSlot < size) {
                    inv.setItem(manualSlot, GUIManager.createItem(config, "manual-invite", Material.NAME_TAG, null));
                }

                inviter.openInventory(inv);
            });
        });
    }
}
}
