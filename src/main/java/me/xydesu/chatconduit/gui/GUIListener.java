package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class GUIListener implements Listener {

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getHolder() instanceof GUIHolder holder) {
            event.setCancelled(true);

            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            // 播放點擊音效
            try {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            } catch (Exception ignored) {}

            int slot = event.getSlot();

            switch (holder.getGuiType()) {
                case CHANNEL_SELECT -> handleChannelSelectClick(player, slot, clickedItem);
                case PLAYER_CHANNEL_MANAGE -> handleManageClick(player, holder.getExtraData(), slot, clickedItem, event.getClick());
                case PENDING_INVITES -> handlePendingInvitesClick(player, slot, clickedItem, event.getClick());
            }
        }
    }

    private void handleChannelSelectClick(Player player, int slot, ItemStack clickedItem) {
        if (slot == 48) {
            PlayerChannelManageGUI.open(player);
            return;
        }
        if (slot == 49) {
            player.closeInventory();
            return;
        }
        if (slot == 50) {
            PendingInvitesGUI.open(player);
            return;
        }

        // 點擊系統公用頻道 (Row 2 ~ 3)
        if ((slot >= 10 && slot <= 16) || (slot >= 19 && slot <= 25)) {
            int sysIndex = 10;
            for (ChannelManager.Channel sysChan : ChannelManager.getChannels().values()) {
                if (sysIndex == slot) {
                    if (!sysChan.permission().isEmpty() && !player.hasPermission(sysChan.permission())) {
                        ChatUtils.sendMessage(player, Main.getInstance().getLanguageConfig().getString("channel.no-permission", "<red>你沒有權限進入此頻道！"));
                        return;
                    }
                    ChannelManager.setPlayerChannel(player, sysChan.key());
                    String switchMsg = Main.getInstance().getLanguageConfig().getString("channel.switched", "<green>已切換預設發言頻道至：<yellow><channel_name>").replace("<channel_name>", sysChan.name());
                    ChatUtils.sendMessage(player, switchMsg);
                    ChannelSelectGUI.open(player);
                    return;
                }
                sysIndex++;
                if (sysIndex % 9 == 8) sysIndex += 2;
            }
        }

        // 點擊玩家自訂群組頻道 (Row 5)
        if (slot >= 37 && slot <= 43) {
            int custIndex = 37;
            for (PlayerChannelManager.CustomChannel custChan : PlayerChannelManager.getCustomChannels().values()) {
                if (!custChan.getMembers().contains(player.getUniqueId())) continue;
                if (custIndex == slot) {
                    ChannelManager.setPlayerChannel(player, custChan.getId());
                    String switchMsg = Main.getInstance().getLanguageConfig().getString("channel.switched", "<green>已切換預設發言頻道至：<yellow><channel_name>").replace("<channel_name>", custChan.getDisplayName());
                    ChatUtils.sendMessage(player, switchMsg);
                    ChannelSelectGUI.open(player);
                    return;
                }
                custIndex++;
            }
        }
    }

    private void handleManageClick(Player player, String channelId, int slot, ItemStack clickedItem, ClickType clickType) {
        PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(channelId);
        if (customChan == null) {
            ChannelSelectGUI.open(player);
            return;
        }

        boolean isOwner = customChan.getOwner().equals(player.getUniqueId());

        // Slot 4: 頻道模式切換
        if (slot == 4) {
            if (isOwner) {
                PlayerChannelManager.Mode newMode = customChan.getMode() == PlayerChannelManager.Mode.PUBLIC ? PlayerChannelManager.Mode.PRIVATE : PlayerChannelManager.Mode.PUBLIC;
                // 利用全頻道重建模式
                PlayerChannelManager.deleteChannel(customChan.getId());
                PlayerChannelManager.createChannel(customChan.getDisplayName(), player);
                PlayerChannelManager.CustomChannel updated = PlayerChannelManager.getChannel(customChan.getId());
                if (updated != null) {
                    updated.getMembers().addAll(customChan.getMembers());
                    updated.getPendingInvites().addAll(customChan.getPendingInvites());
                    PlayerChannelManager.save();
                    PlayerChannelManageGUI.openForChannel(player, updated);
                }
            } else {
                ChatUtils.sendMessage(player, "<red>僅有頻道隊長可以修改頻道設定！");
            }
            return;
        }

        // Slot 45: 返回主選單
        if (slot == 45) {
            ChannelSelectGUI.open(player);
            return;
        }

        // Slot 53: 解散頻道
        if (slot == 53 && isOwner) {
            String delName = customChan.getDisplayName();
            PlayerChannelManager.deleteChannel(customChan.getId());
            ChatUtils.sendMessage(player, "<red>已成功解散群組頻道 <yellow>" + delName + "</yellow>。");
            ChannelSelectGUI.open(player);
            return;
        }

        // 點擊成員頭顱進行踢人/轉讓隊長
        if (clickedItem.getType() == Material.PLAYER_HEAD && isOwner) {
            SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
            if (meta != null && meta.getOwningPlayer() != null) {
                OfflinePlayer targetP = meta.getOwningPlayer();
                UUID targetUuid = targetP.getUniqueId();

                if (targetUuid.equals(player.getUniqueId())) return; // 不可操作自己

                if (clickType.isLeftClick()) {
                    // 左鍵踢人
                    customChan.getMembers().remove(targetUuid);
                    PlayerChannelManager.save();
                    String pName = targetP.getName() != null ? targetP.getName() : targetUuid.toString();
                    ChatUtils.sendMessage(player, "<green>已將 <yellow>" + pName + "</yellow> 踢出群組頻道。");

                    if (targetP.isOnline() && targetP.getPlayer() != null) {
                        ChatUtils.sendMessage(targetP.getPlayer(), "<red>你已被踢出頻道 <yellow>" + customChan.getDisplayName() + "</yellow>。");
                        if (ChannelManager.getPlayerSelectedKey(targetP.getPlayer()).equalsIgnoreCase(customChan.getId())) {
                            ChannelManager.setPlayerChannel(targetP.getPlayer(), "global");
                        }
                    }
                    PlayerChannelManageGUI.openForChannel(player, customChan);
                } else if (clickType.isRightClick()) {
                    // 右鍵轉讓隊長
                    if (targetP.isOnline() && targetP.getPlayer() != null) {
                        customChan.setOwner(targetUuid);
                        PlayerChannelManager.save();
                        String pName = targetP.getName();
                        ChatUtils.sendMessage(player, "<green>已成功將頻道隊長轉讓給 <yellow>" + pName + "</yellow>。");
                        ChatUtils.sendMessage(targetP.getPlayer(), "<green>你現在是頻道 <yellow>" + customChan.getDisplayName() + "</yellow> 的新隊長！");
                        PlayerChannelManageGUI.openForChannel(player, customChan);
                    } else {
                        ChatUtils.sendMessage(player, "<red>目標玩家必須在線才能接收隊長職位！");
                    }
                }
            }
        }
    }

    private void handlePendingInvitesClick(Player player, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 22) {
            ChannelSelectGUI.open(player);
            return;
        }

        if (clickedItem.getType() == Material.PAPER) {
            int currentSlot = 9;
            for (PlayerChannelManager.CustomChannel custChan : PlayerChannelManager.getCustomChannels().values()) {
                if (!custChan.getPendingInvites().contains(player.getUniqueId())) continue;
                if (currentSlot == slot) {
                    if (clickType.isLeftClick()) {
                        // 接受邀請
                        custChan.getPendingInvites().remove(player.getUniqueId());
                        custChan.getMembers().add(player.getUniqueId());
                        PlayerChannelManager.save();
                        ChannelManager.setPlayerChannel(player, custChan.getId());

                        ChatUtils.sendMessage(player, "<green>成功加入群組頻道 <yellow>" + custChan.getDisplayName() + "</yellow>！");
                        ChannelSelectGUI.open(player);
                    } else if (clickType.isRightClick()) {
                        // 拒絕邀請
                        custChan.getPendingInvites().remove(player.getUniqueId());
                        PlayerChannelManager.save();

                        ChatUtils.sendMessage(player, "<gray>已拒絕頻道 <yellow>" + custChan.getDisplayName() + "</yellow> 的邀請。");
                        PendingInvitesGUI.open(player);
                    }
                    return;
                }
                currentSlot++;
            }
        }
    }
}
