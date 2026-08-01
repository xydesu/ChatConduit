package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.database.dao.FriendBlockDAO;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 好友社交系統 GUI 黑名單管理視窗
 *
 * @author xydesu
 */
public class FriendBlockGUI {

    public static void open(Player player, int page) {
        if (player == null || !player.isOnline()) return;

        Bukkit.getAsyncScheduler().runNow(Main.getInstance(), task -> {
            Set<UUID> blockedUuids = FriendBlockDAO.getBlockedPlayers(player.getUniqueId());

            List<BlockedItem> blockedList = new ArrayList<>();
            for (UUID bUuid : blockedUuids) {
                OfflinePlayer offP = Bukkit.getOfflinePlayer(bUuid);
                String name = offP.getName() != null ? offP.getName() : bUuid.toString().substring(0, 8);
                blockedList.add(new BlockedItem(bUuid, name));
            }

            blockedList.sort((a, b) -> a.name.compareToIgnoreCase(b.name));

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (!player.isOnline()) return;

                FileConfiguration config = GUIManager.getConfig("friend_block");
                String titleStr = GUIManager.getTitle("friend_block", "<red><bold>黑名單管理 - 第 <page> 頁</bold></red>")
                        .replace("<page>", String.valueOf(page));
                Component titleComponent = ChatUtils.parse(player, titleStr);

                int size = GUIManager.getSize("friend_block", 54);
                GUIHolder holder = new GUIHolder(GUIHolder.GUIType.FRIEND_BLOCK, null, page);
                Inventory inv = Bukkit.createInventory(holder, size, titleComponent);

                // 邊框填充
                ItemStack filler = GUIManager.createItem(config, "filler-glass", Material.GRAY_STAINED_GLASS_PANE, null);
                int[] fillerSlots = GUIManager.getSlots(config, "items.filler-glass.slots", new int[]{
                        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 46, 47, 49, 51, 52, 53
                });
                for (int s : fillerSlots) {
                    if (s < size) inv.setItem(s, filler);
                }

                int[] headSlots = GUIManager.getSlots(config, "slots.blocked-heads", new int[]{
                        10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34
                });

                int itemsPerPage = headSlots.length;
                int totalItems = blockedList.size();
                int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / itemsPerPage));
                int curPage = Math.min(Math.max(1, page), totalPages);

                int startIndex = (curPage - 1) * itemsPerPage;
                int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

                int slotIdx = 0;
                for (int i = startIndex; i < endIndex; i++) {
                    if (slotIdx >= headSlots.length) break;

                    BlockedItem item = blockedList.get(i);
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    if (meta != null) {
                        try {
                            meta.setPlayerProfile(Bukkit.createProfile(item.uuid, item.name));
                        } catch (Exception ignored) {}

                        meta.displayName(ChatUtils.parseNoItalic("<red><bold>" + item.name + "</bold></red>"));
                        List<Component> lore = new ArrayList<>();
                        lore.add(ChatUtils.parseNoItalic("<gray>狀態: <red>已加入黑名單</red>"));
                        lore.add(ChatUtils.parseNoItalic(""));
                        lore.add(ChatUtils.parseNoItalic("<green>▶ 點擊：解除黑名單封鎖</green>"));
                        meta.lore(lore);
                        head.setItemMeta(meta);
                    }

                    int s = headSlots[slotIdx++];
                    if (s < size) inv.setItem(s, head);
                }

                // 按鈕
                int backSlot = GUIManager.getSlot(config, "back-button", 45);
                if (backSlot < size) inv.setItem(backSlot, GUIManager.createItem(config, "back-button", Material.ARROW, null));

                if (curPage > 1) {
                    int prevSlot = GUIManager.getSlot(config, "prev-page", 48);
                    if (prevSlot < size) inv.setItem(prevSlot, GUIManager.createItem(config, "prev-page", Material.PAPER, null));
                }

                if (curPage < totalPages) {
                    int nextSlot = GUIManager.getSlot(config, "next-page", 50);
                    if (nextSlot < size) inv.setItem(nextSlot, GUIManager.createItem(config, "next-page", Material.PAPER, null));
                }

                player.openInventory(inv);
            });
        });
    }

    public static record BlockedItem(UUID uuid, String name) {}
}
