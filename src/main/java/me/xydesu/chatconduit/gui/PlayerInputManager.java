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

    public enum InputType {
        CREATE_CHANNEL,
        RENAME_CHANNEL,
        INVITE_PLAYER,
        SET_WEBHOOK
    }

    public record InputSession(InputType type, String extraData) {}

    public static boolean isInputPending(UUID uuid) {
        return pendingInputs.containsKey(uuid) || currentlyProcessing.contains(uuid);
    }

    public static void expectInput(Player player, InputType type) {
        expectInput(player, type, null);
    }

    public static void expectInput(Player player, InputType type, String extraData) {
        pendingInputs.put(player.getUniqueId(), new InputSession(type, extraData));
        player.closeInventory();

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

        event.setCancelled(true);
        String input = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
            try {
                if (input.equalsIgnoreCase("cancel")) {
                    ChatUtils.sendMessage(player, "<gray>已取消對話框輸入。");
                    if (session.extraData() != null) {
                        PlayerChannelManager.CustomChannel c = PlayerChannelManager.getChannel(session.extraData());
                        if (c != null) {
                            if (session.type() == InputType.SET_WEBHOOK || session.type() == InputType.RENAME_CHANNEL) {
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

                if (session.type() == InputType.CREATE_CHANNEL) {
                    if (input.isEmpty() || input.length() > 20) {
                        ChatUtils.sendMessage(player, "<red>頻道名稱長度必須介於 1 至 20 個字元！");
                        ChannelSelectGUI.open(player);
                        return;
                    }

                    if (PlayerChannelManager.createChannel(input, player)) {
                        String msg = Main.getInstance().getLanguageConfig().getString("channel.create-success", "<green>成功建立群組頻道 <yellow><name>！").replace("<name>", input);
                        ChatUtils.sendMessage(player, msg);
                        ChannelManager.setPlayerChannel(player, input.toLowerCase());

                        PlayerChannelManager.CustomChannel newChan = PlayerChannelManager.getChannel(input.toLowerCase());
                        if (newChan != null) {
                            PlayerChannelManageGUI.openForChannel(player, newChan);
                        } else {
                            ChannelSelectGUI.open(player);
                        }
                    } else {
                        String msg = Main.getInstance().getLanguageConfig().getString("channel.create-exists", "<red>該頻道名稱已存在！");
                        ChatUtils.sendMessage(player, msg);
                        ChannelSelectGUI.open(player);
                    }
                } else if (session.type() == InputType.RENAME_CHANNEL) {
                    PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(session.extraData());
                    if (customChan == null) {
                        ChannelSelectGUI.open(player);
                        return;
                    }

                    if (input.isEmpty() || input.length() > 20) {
                        ChatUtils.sendMessage(player, "<red>頻道顯示名稱長度必須介於 1 至 20 個字元！");
                        ChannelSettingsGUI.open(player, customChan);
                        return;
                    }

                    customChan.setDisplayName(input);
                    PlayerChannelManager.save();
                    ChatUtils.sendMessage(player, "<green>已成功將頻道顯示名稱修改為：<yellow>" + input + "</yellow>！");
                    ChannelSettingsGUI.open(player, customChan);
                } else if (session.type() == InputType.INVITE_PLAYER) {
                    PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(session.extraData());
                    if (customChan == null) {
                        ChannelSelectGUI.open(player);
                        return;
                    }

                    Player targetPlayer = Bukkit.getPlayerExact(input);
                    if (targetPlayer == null) {
                        targetPlayer = Bukkit.getPlayer(input);
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
                }
            } finally {
                currentlyProcessing.remove(uuid);
            }
        });
    }
}
