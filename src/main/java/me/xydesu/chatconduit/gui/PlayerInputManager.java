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
                    if (cleanInput.isEmpty() || cleanInput.length() > 20) {
                        String msg = Main.getInstance().getLanguageConfig().getString("channel.name-invalid", "<red>頻道名稱格式無效！");
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
                            String msg = Main.getInstance().getLanguageConfig().getString("channel.name-invalid", "<red>頻道名稱格式無效！");
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

                    if (cleanInput.isEmpty() || cleanInput.length() > 20) {
                        ChatUtils.sendMessage(player, "<red>頻道顯示名稱長度必須介於 1 至 20 個字元！");
                        ChannelSettingsGUI.open(player, customChan);
                        return;
                    }

                    customChan.setDisplayName(cleanInput);
                    PlayerChannelManager.save();
                    ChatUtils.sendMessage(player, "<green>已成功將頻道顯示名稱修改為：<yellow>" + cleanInput + "</yellow>！");
                    ChannelSettingsGUI.open(player, customChan);
                } else if (session.type() == InputType.INVITE_PLAYER) {
                    PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(session.extraData());
                    if (customChan == null) {
                        ChannelSelectGUI.open(player);
                        return;
                    }

                    String targetName = input.trim();
                    Player targetPlayer = Bukkit.getPlayerExact(targetName);
                    if (targetPlayer == null) {
                        targetPlayer = Bukkit.getPlayer(targetName);
                    }

                    if (targetPlayer == null || !targetPlayer.isOnline()) {
                        ChatUtils.sendMessage(player, "<red>找不到玩家 <yellow>" + input + "</yellow> 或該玩家未在線！");
                        OnlinePlayersGUI.open(player, customChan);
                        return;
                    }

                    if (customChan.getMembers().contains(targetPlayer.getUniqueId())) {
                        ChatUtils.sendMessage(player, "<red>玩家 <yellow>" + targetPlayer.getName() + "</yellow> 已經是此頻道的成員！");
                        OnlinePlayersGUI.open(player, customChan);
                        return;
                    }

                    customChan.getPendingInvites().add(targetPlayer.getUniqueId());
                    PlayerChannelManager.save();

                    ChatUtils.sendMessage(player, "<green>已成功邀請 <yellow>" + targetPlayer.getName() + "</yellow> 加入群組頻道。");
                    ChatUtils.sendInviteNotification(player, targetPlayer, customChan);

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
                    ChatUtils.sendMessage(player, "<green>已成功為頻道綁定外接 Discord Webhook 網址！");
                    ChannelSettingsGUI.open(player, customChan);
                } else if (session.type() == InputType.SET_DESCRIPTION) {
                    PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(session.extraData());
                    if (customChan == null) {
                        ChannelSelectGUI.open(player);
                        return;
                    }

                    if (cleanInput.isEmpty() || cleanInput.length() > 60) {
                        ChatUtils.sendMessage(player, "<red>頻道簡介長度必須介於 1 至 60 個字元！");
                        ChannelSettingsGUI.open(player, customChan);
                        return;
                    }

                    customChan.setDescription(cleanInput);
                    PlayerChannelManager.save();
                    ChatUtils.sendMessage(player, "<green>已成功修改頻道簡介說明！");
                    ChannelSettingsGUI.open(player, customChan);
                } else if (session.type() == InputType.SET_RULES) {
                    PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(session.extraData());
                    if (customChan == null) {
                        ChannelSelectGUI.open(player);
                        return;
                    }

                    if (cleanInput.isEmpty() || cleanInput.length() > 60) {
                        ChatUtils.sendMessage(player, "<red>頻道規則長度必須介於 1 至 60 個字元！");
                        ChannelSettingsGUI.open(player, customChan);
                        return;
                    }

                    customChan.setRules(cleanInput);
                    PlayerChannelManager.save();
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
