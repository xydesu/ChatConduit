package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.friend.FriendManager;
import me.xydesu.chatconduit.friend.model.FriendRequest;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 好友社交系統 GUI 主選單
 *
 * @author xydesu
 */
public class FriendMainGUI {

    public static void open(Player player) {
        if (player == null || !player.isOnline()) return;

        UUID playerUuid = player.getUniqueId();
        FriendManager friendManager = FriendManager.getInstance();

        // 異步查詢未讀申請，完成後回主執行緒開啟 Inventory
        friendManager.getIncomingRequestsAsync(playerUuid).thenAccept(requests -> {
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (!player.isOnline()) return;

                FileConfiguration config = GUIManager.getConfig("friend_main");
                String titleStr = GUIManager.getTitle("friend_main", "<gradient:#4158D0:#C850C0><bold>好友社交系統主介面</bold></gradient>");
                Component titleComponent = ChatUtils.parse(player, titleStr);

                int size = GUIManager.getSize("friend_main", 45);
                GUIHolder holder = new GUIHolder(GUIHolder.GUIType.FRIEND_MAIN);
                Inventory inv = Bukkit.createInventory(holder, size, titleComponent);

                Set<UUID> friends = friendManager.getFriends(playerUuid);
                int friendCount = friends.size();
                int requestCount = requests != null ? requests.size() : 0;
                
                // 計算黑名單數量
                int blockCount = 0;
                try {
                    blockCount = me.xydesu.chatconduit.database.dao.FriendBlockDAO.getBlockedPlayers(playerUuid).size();
                } catch (Exception ignored) {}

                Map<String, String> replacements = new HashMap<>();
                replacements.put("<player_name>", player.getName());
                replacements.put("<friend_count>", String.valueOf(friendCount));
                replacements.put("<request_count>", String.valueOf(requestCount));
                replacements.put("<block_count>", String.valueOf(blockCount));

                // 填充邊框
                ItemStack filler = GUIManager.createItem(config, "filler-glass", Material.GRAY_STAINED_GLASS_PANE, replacements);
                int[] fillerSlots = GUIManager.getSlots(config, "items.filler-glass.slots", new int[]{
                        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 41, 42, 43, 44
                });
                for (int s : fillerSlots) {
                    if (s < size) inv.setItem(s, filler);
                }

                // Slot 13: 個人資訊頭顱
                int profileSlot = GUIManager.getSlot(config, "player-profile", 13);
                if (profileSlot < size) {
                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    if (meta != null) {
                        meta.setPlayerProfile(player.getPlayerProfile());
                        meta.displayName(ChatUtils.parseNoItalic("<yellow><bold>👤 個人好友資訊</bold></yellow>"));
                        List<Component> lore = List.of(
                                ChatUtils.parseNoItalic("<gray>玩家名稱: <white>" + player.getName() + "</white>"),
                                ChatUtils.parseNoItalic("<gray>目前好友數: <green>" + friendCount + "</green> 人"),
                                ChatUtils.parseNoItalic("<gray>待處理申請: <gold>" + requestCount + "</gold> 個"),
                                ChatUtils.parseNoItalic("<gray>黑名單人數: <red>" + blockCount + "</red> 人")
                        );
                        meta.lore(lore);
                        head.setItemMeta(meta);
                    }
                    inv.setItem(profileSlot, head);
                }

                // Slot 20: 好友列表
                int listSlot = GUIManager.getSlot(config, "friend-list", 20);
                if (listSlot < size) {
                    inv.setItem(listSlot, GUIManager.createItem(config, "friend-list", Material.BOOK, replacements));
                }

                // Slot 22: 待處理申請
                int reqSlot = GUIManager.getSlot(config, "friend-requests", 22);
                if (reqSlot < size) {
                    inv.setItem(reqSlot, GUIManager.createItem(config, "friend-requests", Material.WRITABLE_BOOK, replacements));
                }

                // Slot 24: 黑名單管理
                int blockSlot = GUIManager.getSlot(config, "block-list", 24);
                if (blockSlot < size) {
                    inv.setItem(blockSlot, GUIManager.createItem(config, "block-list", Material.REDSTONE, replacements));
                }

                // Slot 40: 關閉按鈕
                int closeSlot = GUIManager.getSlot(config, "close-button", 40);
                if (closeSlot < size) {
                    inv.setItem(closeSlot, GUIManager.createItem(config, "close-button", Material.BARRIER, replacements));
                }

                player.openInventory(inv);
            });
        });
    }
}
