package me.xydesu.chatconduit.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInputManager implements Listener {

    private static final Map<UUID, InputSession> pendingInputs = new ConcurrentHashMap<>();
    private static final Set<UUID> currentlyProcessing = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, org.bukkit.scheduler.BukkitTask> timeoutTasks = new ConcurrentHashMap<>();

    public enum InputType {
        CREATE_CHANNEL,
        RENAME_CHANNEL,
        INVITE_PLAYER,
        SET_WEBHOOK,
        SET_DESCRIPTION,
        SET_RULES
    }

    public record InputSession(InputType type, String extraData) {}

    public static void clearPendingInput(UUID uuid) {
        pendingInputs.remove(uuid);
        currentlyProcessing.remove(uuid);
        org.bukkit.scheduler.BukkitTask task = timeoutTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    public static boolean isInputPending(UUID uuid) {
        return pendingInputs.containsKey(uuid) || currentlyProcessing.contains(uuid);
    }

    public static void expectInput(Player player, InputType type) {
        expectInput(player, type, null);
    }

    public static void expectInput(Player player, InputType type, String extraData) {
        clearPendingInput(player.getUniqueId());

        pendingInputs.put(player.getUniqueId(), new InputSession(type, extraData));
        player.closeInventory();

        int timeoutSeconds = Main.getInstance().getConfig().getInt("player-channels.session-timeout-seconds", 45);
        if (timeoutSeconds > 0) {
            org.bukkit.scheduler.BukkitTask task = Bukkit.getScheduler().runTaskLater(Main.getInstance(), () -> {
                if (pendingInputs.remove(player.getUniqueId()) != null) {
                    currentlyProcessing.remove(player.getUniqueId());
                    timeoutTasks.remove(player.getUniqueId());
                    if (player.isOnline()) {
                        String timeoutMsg = Main.getInstance().getLanguageConfig().getString("channel.input-timeout", "<red>對話框輸入已逾時，自動取消操作。");
                        ChatUtils.sendMessage(player, timeoutMsg);
                    }
                }
            }, timeoutSeconds * 20L);
            timeoutTasks.put(player.getUniqueId(), task);
        }

        ChatUtils.sendMessage(player, "");
        if (type == InputType.CREATE_CHANNEL) {
            ChatUtils.sendMessage(player, "<gradient:#00d2ff:#3a7bd5><bold>=== 建立頻道對話框提示 ===</bold></gradient>");
            ChatUtils.sendMessage(player, "<yellow>請在對話框直接輸入新頻道名稱 <gray>(輸入 cancel 可取消)：");
        } else if (type == InputType.RENAME_CHANNEL) {
            ChatUtils.sendMessage(player, "<gradient:#00d2ff:#3a7bd5><bold>=== 重命名頻道對話框提示 ===</bold></gradient>");
            ChatUtils.sendMessage(player, "<yellow>請在對話框輸入新的頻道顯示名稱 <gray>(輸入 cancel 可取消)：");
        } else if (type == InputType.INVITE_PLAYER) {
            ChatUtils.sendMessage(player, "<gradient:#00d2ff:#3a7bd5><bold>=== 頻道邀請對話框提示 ===</bold></gradient>");
            ChatUtils.sendMessage(player, "<yellow>請在對話框輸入要邀請的線上玩家名稱 <gray>(輸入 cancel 可取消)：");
        } else if (type == InputType.SET_WEBHOOK) {
            ChatUtils.sendMessage(player, "<gradient:#00d2ff:#3a7bd5><bold>=== Webhook 設定對話框提示 ===</bold></gradient>");
            ChatUtils.sendMessage(player, "<yellow>請在對話框輸入 Discord Webhook 網址 <gray>(輸入 clear 可解除綁定，輸入 cancel 可取消)：");
        } else if (type == InputType.SET_DESCRIPTION) {
            ChatUtils.sendMessage(player, "<gradient:#00d2ff:#3a7bd5><bold>=== 頻道簡介對話框提示 ===</bold></gradient>");
            ChatUtils.sendMessage(player, "<yellow>請在對話框輸入頻道的介紹說明 <gray>(輸入 cancel 可取消)：");
        } else if (type == InputType.SET_RULES) {
            ChatUtils.sendMessage(player, "<gradient:#00d2ff:#3a7bd5><bold>=== 頻道規則對話框提示 ===</bold></gradient>");
            ChatUtils.sendMessage(player, "<yellow>請在對話框輸入頻道的規章守則 <gray>(輸入 cancel 可取消)：");
        }
        ChatUtils.sendMessage(player, "");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChatInput(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        InputSession session = pendingInputs.get(uuid);
        if (session == null) return;

        currentlyProcessing.add(uuid);
        pendingInputs.remove(uuid);
        org.bukkit.scheduler.BukkitTask task = timeoutTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }

        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            try {
                if (input.equalsIgnoreCase("cancel")) {
                    ChatUtils.sendMessage(player, "<gray>已取消對話框輸入。");
                    if (session.extraData() != null) {
                        PlayerChannelManager.CustomChannel c = PlayerChannelManager.getChannel(session.extraData());
                        if (c != null) {
                            if (session.type() == InputType.SET_WEBHOOK || session.type() == InputType.RENAME_CHANNEL || session.type() == InputType.SET_DESCRIPTION || session.type() == InputType.SET_RULES) {
                                ChannelSettingsGUI.open(player, c);
                                return;
                            }
                            OnlinePlayersGUI.open(player, c);
                            return;
                        }
                    }
                    ChannelSelectGUI.open(player);
                    return;
                }

                String cleanInput = sanitizeInput(input);

                if (session.type() == InputType.CREATE_CHANNEL) {
                    int nameMin = Main.getInstance().getConfig().getInt("player-channels.name-min-length", 1);
                    int nameMax = Main.getInstance().getConfig().getInt("player-channels.name-max-length", 20);

                    if (cleanInput.length() < nameMin || cleanInput.length() > nameMax) {
                        String msg = Main.getInstance().getLanguageConfig().getString("channel.name-invalid", "<red>頻道名稱格式無效！")
                                .replace("<min>", String.valueOf(nameMin))
                                .replace("<max>", String.valueOf(nameMax));
                        ChatUtils.sendMessage(player, msg);
                        ChannelSelectGUI.open(player);
                        return;
                    }

                    PlayerChannelManager.CreateResult res = PlayerChannelManager.tryCreateChannel(cleanInput, player);
                    switch (res) {
                        case SUCCESS -> {
                            String msg = Main.getInstance().getLanguageConfig().getString("channel.create-success", "<green>成功建立群組頻道 <yellow><name>！").replace("<name>", cleanInput);
                            ChatUtils.sendMessage(player, msg);
                            ChannelManager.setPlayerChannel(player, cleanInput.toLowerCase());

                            PlayerChannelManager.CustomChannel newChan = PlayerChannelManager.getChannel(cleanInput.toLowerCase());
                            if (newChan != null) {
                                PlayerChannelManageGUI.openForChannel(player, newChan);
                            } else {
                                ChannelSelectGUI.open(player);
                            }
                        }
                        case RESERVED_KEYWORD -> {
                            String msg = Main.getInstance().getLanguageConfig().getString("channel.name-blacklisted", "<red>該名稱包含系統保留字，無法作為頻道名稱！");
                            ChatUtils.sendMessage(player, msg);
                            ChannelSelectGUI.open(player);
                        }
                        case LIMIT_REACHED -> {
                            int max = Main.getInstance().getConfig().getInt("player-channels.max-per-player", 3);
                            String msg = Main.getInstance().getLanguageConfig().getString("channel.create-limit-reached", "<red>您創建的群組頻道數量已達上限（最多 <limit> 個）！").replace("<limit>", String.valueOf(max));
                            ChatUtils.sendMessage(player, msg);
                            ChannelSelectGUI.open(player);
                        }
                        case ALREADY_EXISTS -> {
                            String msg = Main.getInstance().getLanguageConfig().getString("channel.create-exists", "<red>該頻道名稱已存在！");
                            ChatUtils.sendMessage(player, msg);
                            ChannelSelectGUI.open(player);
                        }
                        default -> {
                            String msg = Main.getInstance().getLanguageConfig().getString("channel.name-invalid", "<red>頻道名稱格式無效！")
                                    .replace("<min>", String.valueOf(nameMin))
                                    .replace("<max>", String.valueOf(nameMax));
                            ChatUtils.sendMessage(player, msg);
                            ChannelSelectGUI.open(player);
                        }
                    }

                } else if (session.type() == InputType.RENAME_CHANNEL) {
                    PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(session.extraData());
                    if (customChan == null) {
                        ChannelSelectGUI.open(player);
                        return;
                    }

                    int nameMin = Main.getInstance().getConfig().getInt("player-channels.name-min-length", 1);
                    int nameMax = Main.getInstance().getConfig().getInt("player-channels.name-max-length", 20);
                    if (cleanInput.length() < nameMin || cleanInput.length() > nameMax) {
                        String msg = Main.getInstance().getLanguageConfig().getString("channel.name-invalid", "<red>頻道名稱格式無效！")
                                .replace("<min>", String.valueOf(nameMin))
                                .replace("<max>", String.valueOf(nameMax));
                        ChatUtils.sendMessage(player, msg);
                        ChannelSettingsGUI.open(player, customChan);
                        return;
                    }

                    customChan.setDisplayName(cleanInput);
                    PlayerChannelManager.save();
                    PlayerChannelManager.publishSync(me.xydesu.chatconduit.redis.PlayerChannelSyncPacket.Action.UPDATE, customChan, null, null);
                    ChatUtils.sendMessage(player, "<green>已成功將頻道顯示名稱修改為：<yellow>" + cleanInput + "</yellow>！");
                    ChannelSettingsGUI.open(player, customChan);
                } else if (session.type() == InputType.INVITE_PLAYER) {
                    PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getOrLoadChannel(session.extraData());
                    if (customChan == null) {
                        ChannelSelectGUI.open(player);
                        return;
                    }

                    String targetName = input.trim();
                    Player targetPlayer = Bukkit.getPlayerExact(targetName);
                    if (targetPlayer == null) {
                        targetPlayer = Bukkit.getPlayer(targetName);
                    }

                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        if (customChan.getMembers().contains(targetPlayer.getUniqueId())) {
                            ChatUtils.sendMessage(player, "<red>玩家 <yellow>" + targetPlayer.getName() + "</yellow> 已經是此頻道的成員！");
                            OnlinePlayersGUI.open(player, customChan);
                            return;
                        }

                        customChan.getPendingInvites().add(targetPlayer.getUniqueId());
                        PlayerChannelManager.saveChannel(customChan);

                        ChatUtils.sendMessage(player, "<green>已成功邀請 <yellow>" + targetPlayer.getName() + "</yellow> 加入群組頻道。");
                        ChatUtils.sendInviteNotification(player, targetPlayer, customChan);
                    } else if (me.xydesu.chatconduit.redis.RedisManager.isEnabled()) {
                        org.bukkit.OfflinePlayer offPlayer = Bukkit.getOfflinePlayer(targetName);
                        if (offPlayer != null && offPlayer.getUniqueId() != null) {
                            if (customChan.getMembers().contains(offPlayer.getUniqueId())) {
                                ChatUtils.sendMessage(player, "<red>玩家 <yellow>" + targetName + "</yellow> 已經是此頻道的成員！");
                                OnlinePlayersGUI.open(player, customChan);
                                return;
                            }
                            customChan.getPendingInvites().add(offPlayer.getUniqueId());
                            PlayerChannelManager.saveChannel(customChan);
                        }

                        me.xydesu.chatconduit.redis.ChannelInvitePacket invitePacket = new me.xydesu.chatconduit.redis.ChannelInvitePacket(
                                me.xydesu.chatconduit.redis.ChannelInvitePacket.Action.INVITE,
                                player.getUniqueId().toString(),
                                player.getName(),
                                targetName,
                                customChan.getId(),
                                customChan.getDisplayName(),
                                me.xydesu.chatconduit.redis.RedisManager.getServerId(),
                                System.currentTimeMillis()
                        );
                        me.xydesu.chatconduit.redis.RedisManager.publishInvitePacket(invitePacket);

                        ChatUtils.sendMessage(player, "<green>已嘗試透過 Redis 跨服廣播發送頻道邀請給玩家 <yellow>" + targetName + "</yellow>！");
                    } else {
                        ChatUtils.sendMessage(player, "<red>找不到玩家 <yellow>" + targetName + "</yellow> 或該玩家未在線！");
                    }

                    OnlinePlayersGUI.open(player, customChan);
                } else if (session.type() == InputType.SET_WEBHOOK) {
                    PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(session.extraData());
                    if (customChan == null) {
                        ChannelSelectGUI.open(player);
                        return;
                    }

                    if (input.equalsIgnoreCase("clear")) {
                        customChan.setWebhookUrl(null);
                        PlayerChannelManager.save();
                        PlayerChannelManager.publishSync(me.xydesu.chatconduit.redis.PlayerChannelSyncPacket.Action.UPDATE, customChan, null, null);
                        ChatUtils.sendMessage(player, "<green>已成功解除該頻道的 Discord Webhook 綁定！");
                        ChannelSettingsGUI.open(player, customChan);
                        return;
                    }

                    if (!input.startsWith("http://") && !input.startsWith("https://")) {
                        ChatUtils.sendMessage(player, "<red>無效的 Webhook 網址！請確保以 http:// 或 https:// 開頭。");
                        ChannelSettingsGUI.open(player, customChan);
                        return;
                    }

                    customChan.setWebhookUrl(input.trim());
                    PlayerChannelManager.save();
                    PlayerChannelManager.publishSync(me.xydesu.chatconduit.redis.PlayerChannelSyncPacket.Action.UPDATE, customChan, null, null);
                    ChatUtils.sendMessage(player, "<green>已成功為頻道綁定外接 Discord Webhook 網址！");
                    ChannelSettingsGUI.open(player, customChan);

                    String testingMsg = Main.getInstance().getLanguageConfig().getString("channel.webhook-testing", "<yellow>正在連線測試 Discord Webhook URL...");
                    ChatUtils.sendMessage(player, testingMsg);

                    me.xydesu.chatconduit.integration.WebhookManager.testWebhook(customChan.getWebhookUrl(), customChan.getDisplayName(), player, result -> {
                        if (!player.isOnline()) return;
                        if (result.success()) {
                            String successMsg = Main.getInstance().getLanguageConfig().getString("channel.webhook-test-success", "<green>✅ Webhook 連線測試成功！測試訊息已送達 Discord 頻道。");
                            ChatUtils.sendMessage(player, successMsg);
                            try {
                                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
                            } catch (Exception ignored) {}
                        } else {
                            String failFmt = Main.getInstance().getLanguageConfig().getString("channel.webhook-test-failed", "<red>❌ Webhook 連線測試失敗！原因: <yellow><reason>");
                            String failMsg = failFmt.replace("<reason>", result.errorMessage() != null ? result.errorMessage() : "未知錯誤");
                            ChatUtils.sendMessage(player, failMsg);
                            try {
                                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 0.8f, 1.0f);
                            } catch (Exception ignored) {}
                        }
                    });
                } else if (session.type() == InputType.SET_DESCRIPTION) {
                    PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(session.extraData());
                    if (customChan == null) {
                        ChannelSelectGUI.open(player);
                        return;
                    }

                    int descMax = Main.getInstance().getConfig().getInt("player-channels.description-max-length", 60);
                    if (cleanInput.isEmpty() || cleanInput.length() > descMax) {
                        String msg = Main.getInstance().getLanguageConfig().getString("channel.description-invalid", "<red>頻道簡介長度必須介於 1 至 <max> 個字元！")
                                .replace("<max>", String.valueOf(descMax));
                        ChatUtils.sendMessage(player, msg);
                        ChannelSettingsGUI.open(player, customChan);
                        return;
                    }

                    customChan.setDescription(cleanInput);
                    PlayerChannelManager.save();
                    PlayerChannelManager.publishSync(me.xydesu.chatconduit.redis.PlayerChannelSyncPacket.Action.UPDATE, customChan, null, null);
                    ChatUtils.sendMessage(player, "<green>已成功修改頻道簡介說明！");
                    ChannelSettingsGUI.open(player, customChan);
                } else if (session.type() == InputType.SET_RULES) {
                    PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(session.extraData());
                    if (customChan == null) {
                        ChannelSelectGUI.open(player);
                        return;
                    }

                    int rulesMax = Main.getInstance().getConfig().getInt("player-channels.rules-max-length", 60);
                    if (cleanInput.isEmpty() || cleanInput.length() > rulesMax) {
                        String msg = Main.getInstance().getLanguageConfig().getString("channel.rules-invalid", "<red>頻道守則長度必須介於 1 至 <max> 個字元！")
                                .replace("<max>", String.valueOf(rulesMax));
                        ChatUtils.sendMessage(player, msg);
                        ChannelSettingsGUI.open(player, customChan);
                        return;
                    }

                    customChan.setRules(cleanInput);
                    PlayerChannelManager.save();
                    PlayerChannelManager.publishSync(me.xydesu.chatconduit.redis.PlayerChannelSyncPacket.Action.UPDATE, customChan, null, null);
                    ChatUtils.sendMessage(player, "<green>已成功修改頻道守則！");
                    ChannelSettingsGUI.open(player, customChan);
                }
            } finally {
                currentlyProcessing.remove(uuid);
            }
        });
    }

    /**
     * 淨化玩家對話框輸入，剝離角括號標籤以防止 MiniMessage 格式標籤注入
     */
    private static String sanitizeInput(String text) {
        if (text == null) return "";
        return text.replace("<", "").replace(">", "").trim();
    }
}
