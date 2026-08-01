package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.friend.FriendManager;
import me.xydesu.chatconduit.friend.model.FriendRequest;
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

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * 好友社交系統 GUI 待處理申請視窗
 *
 * @author xydesu
 */
public class FriendRequestsGUI {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static void open(Player player, int page) {
        if (player == null || !player.isOnline()) return;

        FriendManager.getInstance().getIncomingRequestsAsync(player.getUniqueId()).thenAccept(requests -> {
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (!player.isOnline()) return;

                FileConfiguration config = GUIManager.getConfig("friend_requests");
                String titleStr = GUIManager.getTitle("friend_requests", "<gold><bold>待處理好友申請 - 第 <page> 頁</bold></gold>")
                        .replace("<page>", String.valueOf(page));
                Component titleComponent = ChatUtils.parse(player, titleStr);

                int size = GUIManager.getSize("friend_requests", 54);
                GUIHolder holder = new GUIHolder(GUIHolder.GUIType.FRIEND_REQUESTS, null, page);
                Inventory inv = Bukkit.createInventory(holder, size, titleComponent);

                // 邊框填充
                ItemStack filler = GUIManager.createItem(config, "filler-glass", Material.GRAY_STAINED_GLASS_PANE, null);
                int[] fillerSlots = GUIManager.getSlots(config, "items.filler-glass.slots", new int[]{
                        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 46, 47, 49, 51, 52, 53
                });
                for (int s : fillerSlots) {
                    if (s < size) inv.setItem(s, filler);
                }

                int[] headSlots = GUIManager.getSlots(config, "slots.request-heads", new int[]{
                        10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34
                });

                List<FriendRequest> reqList = requests != null ? requests : new ArrayList<>();
                int itemsPerPage = headSlots.length;
                int totalItems = reqList.size();
                int totalPages = Math.max(1, (int) Math.ceil((double) totalItems / itemsPerPage));
                int curPage = Math.min(Math.max(1, page), totalPages);

                int startIndex = (curPage - 1) * itemsPerPage;
                int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

                int slotIdx = 0;
                for (int i = startIndex; i < endIndex; i++) {
                    if (slotIdx >= headSlots.length) break;

                    FriendRequest req = reqList.get(i);
                    OfflinePlayer senderP = Bukkit.getOfflinePlayer(req.getSenderUuid());
                    String name = senderP.getName() != null ? senderP.getName() : req.getSenderUuid().toString().substring(0, 8);
                    String timeStr = req.getTimestamp() > 0 ? DATE_FORMAT.format(new Timestamp(req.getTimestamp())) : "未知時間";

                    ItemStack head = new ItemStack(Material.PLAYER_HEAD);
                    SkullMeta meta = (SkullMeta) head.getItemMeta();
                    if (meta != null) {
                        try {
                            meta.setPlayerProfile(Bukkit.createProfile(req.getSenderUuid(), name));
                        } catch (Exception ignored) {}

                        meta.displayName(ChatUtils.parseNoItalic("<white><bold>" + name + "</bold>"));
                        List<Component> lore = new ArrayList<>();
                        lore.add(ChatUtils.parseNoItalic("<gray>申請時間: <yellow>" + timeStr + "</yellow>"));
                        lore.add(ChatUtils.parseNoItalic(""));
                        lore.add(ChatUtils.parseNoItalic("<green>▶ 左鍵：接受好友申請</green>"));
                        lore.add(ChatUtils.parseNoItalic("<red>▶ 右鍵：拒絕好友申請</red>"));
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
}
