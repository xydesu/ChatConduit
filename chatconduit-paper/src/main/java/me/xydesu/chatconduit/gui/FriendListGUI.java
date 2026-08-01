package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.friend.FriendManager;
import me.xydesu.chatconduit.redis.RedisManager;
import me.xydesu.chatconduit.redis.RedisPlayerRegistry;
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

import java.util.*;

/**
 * 好友社交系統 GUI 好友列表視窗
 *
 * @author xydesu
 */
public class FriendListGUI {

    public static void open(Player player, int page) {
        if (player == null || !player.isOnline()) return;

        Bukkit.getAsyncScheduler().runNow(Main.getInstance(), task -> {
            Set<UUID> friendUuids = FriendManager.getInstance().getFriends(player.getUniqueId());

            List<FriendItem> friendList = new ArrayList<>();
            for (UUID fUuid : friendUuids) {
                OfflinePlayer offP = Bukkit.getOfflinePlayer(fUuid);
                String name = offP.getName() != null ? offP.getName() : fUuid.toString().substring(0, 8);
                boolean isOnline = offP.isOnline();
                String serverName = "本服";

                if (!isOnline && RedisManager.isEnabled()) {
                    RedisPlayerRegistry.PlayerData redisData = RedisPlayerRegistry.getPlayerData(name);
                    if (redisData != null) {
                        isOnline = true;
                        serverName = redisData.getServerId();
                    }
                }

                friendList.add(new FriendItem(fUuid, name, isOnline, serverName));
            }

            // 排序：在線優先，其次字母
            friendList.sort((a, b) -> {
                if (a.isOnline != b.isOnline) {
                    return Boolean.compare(!a.isOnline, !b.isOnline);
                }
                return a.name.compareToIgnoreCase(b.name);
            });

            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (!player.isOnline()) return;

                FileConfiguration config = GUIManager.getConfig("friend_list");
                String titleStr = GUIManager.getTitle("friend_list", "<green><bold>好友列表 - 第 <page> 頁</bold></green>")
                        .replace("<page>", String.valueOf(page));
                Component titleComponent = ChatUtils.parse(player, titleStr);

                int size = GUIManager.getSize("friend_list", 54);
                GUIHolder holder = new GUIHolder(GUIHolder.GUIType.FRIEND_LIST, null, page);
                Inventory inv = Bukkit.createInventory(holder, size, titleComponent);

                // 邊框填充
                ItemStack filler = GUIManager.createItem(config, "filler-glass", Material.GRAY_STAINED_GLASS_PANE, null);
                int[] fillerSlots = GUIManager.getSlots(config, "items.filler-glass.slots", new int[]{
                        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 46, 47, 49, 51, 52
                });
                for (int s : fillerSlots) {
                    if (s < size) inv.setItem(s, filler);
                }

                int[] headSlots = GUIManager.getSlots(config, "slots.player-heads", new int[]{
                        10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34
                });

                int itemsPerPage = headSlots.length;
                int totalItems = friendList.size();
                int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / itemsPerPage));
                int curPage = Math.min(Math.max(1, page), totalPages);

                int startIndex = (curPage - 1) * itemsPerPage;
                int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

                int slotIdx = 0;
                for (int i = startIndex; i < endIndex; i++) {
                    if (slotIdx >= headSlots.length) break;

                    FriendItem item = friendList.get(i);
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    if (meta != null) {
                        try {
                            meta.setPlayerProfile(Bukkit.createProfile(item.uuid, item.name));
                        } catch (Exception ignored) {}

                        meta.displayName(ChatUtils.parseNoItalic("<white><bold>" + item.name + "</bold>"));
                        List<Component> lore = new ArrayList<>();
                        if (item.isOnline) {
                            lore.add(ChatUtils.parseNoItalic("<gray>連線狀態: <green>● 上線中 (" + item.serverName + ")</green>"));
                            lore.add(ChatUtils.parseNoItalic(""));
                            lore.add(ChatUtils.parseNoItalic("<yellow>▶ 左鍵：開啟發送私訊</yellow>"));
                            lore.add(ChatUtils.parseNoItalic("<red>▶ 右鍵：刪除好友關係</red>"));
                        } else {
                            lore.add(ChatUtils.parseNoItalic("<gray>連線狀態: <red>● 離線</red>"));
                            lore.add(ChatUtils.parseNoItalic(""));
                            lore.add(ChatUtils.parseNoItalic("<red>▶ 右鍵：刪除好友關係</red>"));
                        }
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

                int addSlot = GUIManager.getSlot(config, "add-friend", 53);
                if (addSlot < size) inv.setItem(addSlot, GUIManager.createItem(config, "add-friend", Material.ANVIL, null));

                player.openInventory(inv);
            });
        });
    }

    public static record FriendItem(UUID uuid, String name, boolean isOnline, String serverName) {}
}
