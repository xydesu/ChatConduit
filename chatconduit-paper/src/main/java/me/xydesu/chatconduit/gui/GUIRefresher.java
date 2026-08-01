package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryView;

import java.util.UUID;

/**
 * 即時動態刷新開啟中 GUI 視窗之管理器
 *
 * @author xydesu
 */
public class GUIRefresher {

    /**
     * 當好友連線/離線狀態改變時，為其線上好友刷新目前的 GUI 介面
     *
     * @param targetFriendUuid 狀態發生改變的好友 UUID
     */
    public static void refreshFriendGUIs(UUID targetFriendUuid) {
        if (targetFriendUuid == null) return;

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            me.xydesu.chatconduit.friend.FriendManager friendManager = me.xydesu.chatconduit.friend.FriendManager.getInstance();
            if (friendManager == null) return;

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (friendManager.isFriend(player.getUniqueId(), targetFriendUuid)) {
                    refreshForPlayer(player);
                }
            }
        });
    }

    /**
     * 檢查指定玩家目前開啟的視窗，若為好友社交 GUI 則即時重新渲染
     *
     * @param player 玩家
     */
    public static void refreshForPlayer(Player player) {
        if (player == null || !player.isOnline()) return;

        InventoryView view = player.getOpenInventory();
        if (view == null || view.getTopInventory() == null) return;

        if (view.getTopInventory().getHolder() instanceof GUIHolder holder) {
            if (holder.getGuiType() == GUIHolder.GUIType.FRIEND_LIST) {
                FriendListGUI.open(player, holder.getPage());
            } else if (holder.getGuiType() == GUIHolder.GUIType.FRIEND_MAIN) {
                FriendMainGUI.open(player);
            } else if (holder.getGuiType() == GUIHolder.GUIType.FRIEND_REQUESTS) {
                FriendRequestsGUI.open(player, holder.getPage());
            } else if (holder.getGuiType() == GUIHolder.GUIType.FRIEND_BLOCK) {
                FriendBlockGUI.open(player, holder.getPage());
            }
        }
    }
}
