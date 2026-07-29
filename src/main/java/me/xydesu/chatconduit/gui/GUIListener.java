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

import java.util.List;
import java.util.UUID;

public class GUIListener implements Listener {

    private static final List<String> COLOR_PRESETS = List.of(
            "<gradient:#a8c0ff:#3f2b96>",
            "<gradient:#ffb347:#ffcc33>",
            "<gradient:#a8ff78:#78ffd6>",
            "<gradient:#ee9ca7:#ffadc7>",
            "<gradient:#f78ca0:#fe9a8b>",
            "<gradient:#00d2ff:#3a7bd5>"
    );

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getInventory().getHolder() instanceof GUIHolder holder) {
            event.setCancelled(true);

            if (event.getClickedInventory() != event.getView().getTopInventory()) return;
            ItemStack clickedItem = event.getCurrentItem();
            if (clickedItem == null || clickedItem.getType() == Material.AIR) return;

            try {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.8f, 1.0f);
            } catch (Exception ignored) {}

            int slot = event.getSlot();

            switch (holder.getGuiType()) {
                case CHANNEL_SELECT -> handleChannelSelectClick(player, holder.getPage(), slot, clickedItem, event.getClick());
                case PLAYER_CHANNEL_MANAGE -> handleManageClick(player, holder.getExtraData(), slot, clickedItem, event.getClick());
                case PENDING_INVITES -> handlePendingInvitesClick(player, slot, clickedItem, event.getClick());
                case ONLINE_PLAYERS_SELECT -> handleOnlinePlayersSelectClick(player, holder.getExtraData(), slot, clickedItem);
                case CHANNEL_SETTINGS -> handleSettingsClick(player, holder.getExtraData(), slot, clickedItem);
                case MESSAGE_SETTINGS -> handleMessageSettingsClick(player, slot, clickedItem);
            }
        }
    }

    private void handleChannelSelectClick(Player player, int page, int slot, ItemStack clickedItem, ClickType clickType) {
        if (slot == 46) {
            ChannelSelectGUI.open(player, Math.max(1, page - 1));
            return;
        }
        if (slot == 52) {
            ChannelSelectGUI.open(player, page + 1);
            return;
        }
        if (slot == 47) {
            PlayerInputManager.expectInput(player, PlayerInputManager.InputType.CREATE_CHANNEL);
            return;
        }
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
        if (slot == 51) {
            MessageSettingsGUI.open(player);
            return;
        }

        if ((slot >= 10 && slot <= 16) || (slot >= 19 && slot <= 25)) {
            int sysSlotIdx = 0;
            for (ChannelManager.Channel sysChan : ChannelManager.getChannels().values()) {
                if (sysSlotIdx >= GUIHolder.SYS_SLOTS.length) break;
                int currentSlot = GUIHolder.SYS_SLOTS[sysSlotIdx++];
                if (currentSlot == slot) {
                    if (!sysChan.permission().isEmpty() && !player.hasPermission(sysChan.permission())) {
                        ChatUtils.sendMessage(player, Main.getInstance().getLanguageConfig().getString("channel.no-permission", "<red>你沒有權限進入此頻道！"));
                        return;
                    }
                    ChannelManager.setPlayerChannel(player, sysChan.key());
                    String switchMsg = Main.getInstance().getLanguageConfig().getString("channel.switched", "<green>已切換預設發言頻道至：<yellow><channel_name>").replace("<channel_name>", sysChan.name());
                    ChatUtils.sendMessage(player, switchMsg);
                    ChannelSelectGUI.open(player, page);
                    return;
                }
            }
        }

        if (slot >= 37 && slot <= 43) {
            List<PlayerChannelManager.CustomChannel> availableChannels = new java.util.ArrayList<>();
            for (PlayerChannelManager.CustomChannel custChan : PlayerChannelManager.getCustomChannels().values()) {
                boolean isMember = custChan.getMembers().contains(player.getUniqueId());
                boolean isPublic = custChan.getMode() == PlayerChannelManager.Mode.PUBLIC;
                if (isMember || isPublic) {
                    availableChannels.add(custChan);
                }
            }

            int custSlotIdx = -1;
            for (int i = 0; i < GUIHolder.CUST_SLOTS.length; i++) {
                if (GUIHolder.CUST_SLOTS[i] == slot) {
                    custSlotIdx = i;
                    break;
                }
            }

            if (custSlotIdx != -1) {
                int targetIndex = (page - 1) * 7 + custSlotIdx;
                if (targetIndex >= 0 && targetIndex < availableChannels.size()) {
                    PlayerChannelManager.CustomChannel custChan = availableChannels.get(targetIndex);
                    boolean isMember = custChan.getMembers().contains(player.getUniqueId());
                    boolean isPublic = custChan.getMode() == PlayerChannelManager.Mode.PUBLIC;

                    if (clickType.isRightClick()) {
                        if (isMember) {
                            PlayerChannelManageGUI.openForChannel(player, custChan);
                        } else {
                            String msg = Main.getInstance().getLanguageConfig().getString("channel.only-members-manage", "<red>你必須是該頻道的成員才能進行管理！");
                            ChatUtils.sendMessage(player, msg);
                        }
                        return;
                    }


                    if (!isMember && isPublic) {
                        custChan.getMembers().add(player.getUniqueId());
                        PlayerChannelManager.save();
                        ChatUtils.sendMessage(player, "<green>已成功加入公開頻道 <yellow>" + custChan.getDisplayName() + "</yellow>！");
                        PlayerChannelManager.broadcastToMembers(custChan, "<green>▶ 玩家 <yellow>" + player.getName() + "</yellow> 已加入公開頻道 <yellow>" + custChan.getDisplayName() + "</yellow>！", player.getUniqueId());
                    }
                    ChannelManager.setPlayerChannel(player, custChan.getId());
                    String switchMsg = Main.getInstance().getLanguageConfig().getString("channel.switched", "<green>已切換預設發言頻道至：<yellow><channel_name>").replace("<channel_name>", custChan.getDisplayName());
                    ChatUtils.sendMessage(player, switchMsg);
                    ChannelSelectGUI.open(player, page);
                    return;
                }
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

        // Slot 4: 打開頻道詳細設定面板
        if (slot == 4) {
            if (isOwner) {
                ChannelSettingsGUI.open(player, customChan);
            } else {
                ChatUtils.sendMessage(player, "<red>僅有頻道隊長可以修改頻道設定！");
            }
            return;
        }

        if (slot == 45) {
            ChannelSelectGUI.open(player);
            return;
        }

        if (slot == 49 && isOwner) {
            OnlinePlayersGUI.open(player, customChan);
            return;
        }

        if (slot == 51 && !isOwner) {
            customChan.getMembers().remove(player.getUniqueId());
            PlayerChannelManager.save();
            ChannelManager.setPlayerChannel(player, "global");
            ChatUtils.sendMessage(player, "<green>你已成功退出頻道 <yellow>" + customChan.getDisplayName() + "</yellow>。");
            PlayerChannelManager.broadcastToMembers(customChan, "<red>🚪 玩家 <yellow>" + player.getName() + "</yellow> 已退出群組頻道 <yellow>" + customChan.getDisplayName() + "</yellow>。</red>", player.getUniqueId());
            ChannelSelectGUI.open(player);
            return;
        }

        if (slot == 53 && isOwner) {
            String delName = customChan.getDisplayName();
            PlayerChannelManager.deleteChannel(customChan.getId());
            ChannelManager.setPlayerChannel(player, "global");
            ChatUtils.sendMessage(player, "<red>已成功解散群組頻道 <yellow>" + delName + "</yellow>。已切換回預設頻道。");
            ChannelSelectGUI.open(player);
            return;
        }

        if (clickedItem.getType() == Material.PLAYER_HEAD && isOwner) {
            SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
            if (meta != null && meta.getOwningPlayer() != null) {
                OfflinePlayer targetP = meta.getOwningPlayer();
                UUID targetUuid = targetP.getUniqueId();

                if (targetUuid.equals(player.getUniqueId())) return;

                if (clickType.isLeftClick()) {
                    customChan.getMembers().remove(targetUuid);
                    PlayerChannelManager.save();
                    String pName = targetP.getName() != null ? targetP.getName() : targetUuid.toString();
                    ChatUtils.sendMessage(player, "<green>已將 <yellow>" + pName + "</yellow> 踢出群組頻道。");
                    PlayerChannelManager.broadcastToMembers(customChan, "<red>🚪 玩家 <yellow>" + pName + "</yellow> 已被踢出群組頻道 <yellow>" + customChan.getDisplayName() + "</yellow>。</red>", player.getUniqueId());

                    if (targetP.isOnline() && targetP.getPlayer() != null) {
                        ChatUtils.sendMessage(targetP.getPlayer(), "<red>你已被踢出頻道 <yellow>" + customChan.getDisplayName() + "</yellow>。");
                        if (ChannelManager.getPlayerSelectedKey(targetP.getPlayer()).equalsIgnoreCase(customChan.getId())) {
                            ChannelManager.setPlayerChannel(targetP.getPlayer(), "global");
                        }
                    }
                    PlayerChannelManageGUI.openForChannel(player, customChan);
                } else if (clickType.isRightClick()) {
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

    private void handleSettingsClick(Player player, String channelId, int slot, ItemStack clickedItem) {
        PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(channelId);
        if (customChan == null) {
            ChannelSelectGUI.open(player);
            return;
        }

        if (!customChan.getOwner().equals(player.getUniqueId())) {
            ChatUtils.sendMessage(player, "<red>僅有頻道隊長可以修改頻道設定！");
            PlayerChannelManageGUI.openForChannel(player, customChan);
            return;
        }

        // Slot 10: 存取權限模式切換
        if (slot == 10) {
            PlayerChannelManager.Mode newMode = customChan.getMode() == PlayerChannelManager.Mode.PUBLIC ? PlayerChannelManager.Mode.PRIVATE : PlayerChannelManager.Mode.PUBLIC;
            customChan.setMode(newMode);
            PlayerChannelManager.save();
            ChatUtils.sendMessage(player, "<green>頻道模式已切換為：<yellow>" + (newMode == PlayerChannelManager.Mode.PUBLIC ? "公共 (PUBLIC)" : "私人 (PRIVATE)") + "</yellow>");
            ChannelSettingsGUI.open(player, customChan);
            return;
        }

        // Slot 11: 修改頻道顯示名稱
        if (slot == 11) {
            PlayerInputManager.expectInput(player, PlayerInputManager.InputType.RENAME_CHANNEL, customChan.getId());
            return;
        }

        // Slot 12: 修改頻道簡介說明
        if (slot == 12) {
            PlayerInputManager.expectInput(player, PlayerInputManager.InputType.SET_DESCRIPTION, customChan.getId());
            return;
        }

        // Slot 13: 修改頻道規則規範
        if (slot == 13) {
            PlayerInputManager.expectInput(player, PlayerInputManager.InputType.SET_RULES, customChan.getId());
            return;
        }

        // Slot 14: 頻道色彩主題樣式切換
        if (slot == 14) {
            String currentTheme = customChan.getColorTheme();
            int curIdx = COLOR_PRESETS.indexOf(currentTheme);
            int nextIdx = (curIdx + 1) % COLOR_PRESETS.size();
            String nextTheme = COLOR_PRESETS.get(nextIdx);

            customChan.setColorTheme(nextTheme);
            PlayerChannelManager.save();
            ChatUtils.sendMessage(player, "<green>已為頻道套用新的色彩主題樣式！");
            ChannelSettingsGUI.open(player, customChan);
            return;
        }

        // Slot 16: 設定專屬 Discord Webhook 網址
        if (slot == 16) {
            PlayerInputManager.expectInput(player, PlayerInputManager.InputType.SET_WEBHOOK, customChan.getId());
            return;
        }

        // Slot 17: 測試 Webhook 連線
        if (slot == 17) {
            String webhookUrl = customChan.getWebhookUrl();
            if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
                String msg = Main.getInstance().getLanguageConfig().getString("channel.webhook-not-bound", "<red>該頻道尚未綁定 Discord Webhook 網址！");
                ChatUtils.sendMessage(player, msg);
                return;
            }

            String testingMsg = Main.getInstance().getLanguageConfig().getString("channel.webhook-testing", "<yellow>正在連線測試 Discord Webhook URL...");
            ChatUtils.sendMessage(player, testingMsg);

            me.xydesu.chatconduit.integration.WebhookManager.testWebhook(webhookUrl, customChan.getDisplayName(), player, result -> {
                if (!player.isOnline()) return;
                if (result.success()) {
                    String successMsg = Main.getInstance().getLanguageConfig().getString("channel.webhook-test-success", "<green>✅ Webhook 連線測試成功！測試訊息已送達 Discord 頻道。");
                    ChatUtils.sendMessage(player, successMsg);
                    try {
                        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                    } catch (Exception ignored) {}
                } else {
                    String failFmt = Main.getInstance().getLanguageConfig().getString("channel.webhook-test-failed", "<red>❌ Webhook 連線測試失敗！原因: <yellow><reason>");
                    String failMsg = failFmt.replace("<reason>", result.errorMessage() != null ? result.errorMessage() : "未知錯誤");
                    ChatUtils.sendMessage(player, failMsg);
                    try {
                        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                    } catch (Exception ignored) {}
                }
            });
            return;
        }

        // Slot 22: 返回頻道管理頁面
        if (slot == 22) {
            PlayerChannelManageGUI.openForChannel(player, customChan);
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
                        custChan.getPendingInvites().remove(player.getUniqueId());
                        custChan.getMembers().add(player.getUniqueId());
                        PlayerChannelManager.save();
                        ChannelManager.setPlayerChannel(player, custChan.getId());

                        ChatUtils.sendMessage(player, "<green>成功加入群組頻道 <yellow>" + custChan.getDisplayName() + "</yellow>！");
                        PlayerChannelManager.broadcastToMembers(custChan, "<green>▶ 玩家 <yellow>" + player.getName() + "</yellow> 已接受邀請加入群組頻道 <yellow>" + custChan.getDisplayName() + "</yellow>！", player.getUniqueId());
                        ChannelSelectGUI.open(player);
                    } else if (clickType.isRightClick()) {
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

    private void handleOnlinePlayersSelectClick(Player player, String channelId, int slot, ItemStack clickedItem) {
        PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(channelId);
        if (customChan == null) {
            ChannelSelectGUI.open(player);
            return;
        }

        if (slot == 45) {
            PlayerChannelManageGUI.openForChannel(player, customChan);
            return;
        }

        if (slot == 53) {
            PlayerInputManager.expectInput(player, PlayerInputManager.InputType.INVITE_PLAYER, customChan.getId());
            return;
        }

        if (clickedItem.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
            if (meta != null && meta.getOwningPlayer() != null) {
                OfflinePlayer targetP = meta.getOwningPlayer();
                if (targetP.isOnline() && targetP.getPlayer() != null) {
                    Player targetPlayer = targetP.getPlayer();
                    customChan.getPendingInvites().add(targetPlayer.getUniqueId());
                    PlayerChannelManager.save();

                    ChatUtils.sendMessage(player, "<green>已成功邀請 <yellow>" + targetPlayer.getName() + "</yellow> 加入群組頻道。");
                    ChatUtils.sendInviteNotification(player, targetPlayer, customChan);

                    OnlinePlayersGUI.open(player, customChan);
                }
            }
        }
    }

    private void handleMessageSettingsClick(Player player, int slot, ItemStack clickedItem) {
        if (slot == 11) {
            boolean current = ChannelManager.isDeathMessagesEnabled(player);
            ChannelManager.setDeathMessagesEnabled(player, !current);
            String stateStr = !current ? "<green>顯示 (ON)</green>" : "<red>隱藏 (OFF)</red>";
            ChatUtils.sendMessage(player, "<gray>已將死亡訊息通知切換為： " + stateStr);
            MessageSettingsGUI.open(player);
            return;
        }

        if (slot == 15) {
            boolean current = ChannelManager.isJoinMessagesEnabled(player);
            ChannelManager.setJoinMessagesEnabled(player, !current);
            String stateStr = !current ? "<green>顯示 (ON)</green>" : "<red>隱藏 (OFF)</red>";
            ChatUtils.sendMessage(player, "<gray>已將玩家進出通知切換為： " + stateStr);
            MessageSettingsGUI.open(player);
            return;
        }

        if (slot == 22) {
            ChannelSelectGUI.open(player);
        }
    }
}
