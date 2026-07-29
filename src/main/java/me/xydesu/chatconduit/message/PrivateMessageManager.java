package me.xydesu.chatconduit.message;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.mute.MuteManager;
import me.xydesu.chatconduit.redis.PrivateMessagePacket;
import me.xydesu.chatconduit.redis.RedisManager;
import me.xydesu.chatconduit.redis.RedisPlayerRegistry;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 跨服與本地私訊 (/msg, /tell, /reply) 核心管理器
 *
 * @author xydesu
 */
public class PrivateMessageManager {

    // 儲存玩家個人上次私訊對象 (發送者 UUID -> 目標玩家名稱 TargetName)
    private static final Map<UUID, String> replyTargets = new ConcurrentHashMap<>();

    /**
     * 獲取玩家的快速回覆對象名稱
     */
    public static String getReplyTarget(UUID playerUuid) {
        return replyTargets.get(playerUuid);
    }

    /**
     * 設定玩家的快速回覆對象名稱
     */
    public static void setReplyTarget(UUID playerUuid, String targetName) {
        if (playerUuid != null && targetName != null && !targetName.isEmpty()) {
            replyTargets.put(playerUuid, targetName);
        }
    }

    /**
     * 移除玩家的回覆對象對應關係
     */
    public static void removeReplyTarget(UUID playerUuid) {
        if (playerUuid != null) {
            replyTargets.remove(playerUuid);
        }
    }

    /**
     * 發送私訊
     *
     * @param sender 發送者玩家
     * @param targetName 目標玩家名稱
     * @param rawMessage 訊息文字
     */
    public static void sendPrivateMessage(Player sender, String targetName, String rawMessage) {
        FileConfiguration config = Main.getInstance().getConfig();
        boolean enabled = config.getBoolean("private-message.enabled", true);
        if (!enabled) {
            ChatUtils.sendMessage(sender, ChatUtils.getMessage("msg.disabled"));
            return;
        }

        // 檢查發送者是否被禁言
        if (MuteManager.isMuted(sender.getUniqueId())) {
            MuteManager.MuteEntry entry = MuteManager.getMute(sender.getUniqueId());
            if (entry != null) {
                String reason = entry.reason() != null && !entry.reason().isEmpty()
                        ? entry.reason()
                        : ChatUtils.getMessage("mute.default-reason");
                if (entry.isPermanent()) {
                    ChatUtils.sendMessage(sender, ChatUtils.getMessage("mute.chat-blocked-perm")
                            .replace("<reason>", reason));
                } else {
                    String durationStr = me.xydesu.chatconduit.command.MuteCommand.formatDuration(entry.getRemainingMillis());
                    ChatUtils.sendMessage(sender, ChatUtils.getMessage("mute.chat-blocked")
                            .replace("<time>", durationStr)
                            .replace("<reason>", reason));
                }
            }
            return;
        }

        // 不可傳送私訊給自己
        if (sender.getName().equalsIgnoreCase(targetName)) {
            ChatUtils.sendMessage(sender, ChatUtils.getMessage("msg.cannot-msg-self"));
            return;
        }

        String localServerId = RedisManager.getServerId();
        Player localTarget = Bukkit.getPlayerExact(targetName);
        if (localTarget == null) {
            localTarget = Bukkit.getPlayer(targetName);
        }

        // 1. 本地伺服器找到線上目標玩家
        if (localTarget != null && localTarget.isOnline()) {
            sendLocalPrivateMessage(sender, localTarget, localServerId, rawMessage);
            return;
        }

        // 2. 本地未找到，查詢 Redis 跨服線上快取
        if (RedisManager.isEnabled()) {
            RedisPlayerRegistry.PlayerData remoteData = RedisPlayerRegistry.getPlayerData(targetName);
            if (remoteData != null && remoteData.getServerId() != null) {
                String targetServerId = remoteData.getServerId();

                String messageJson = null;
                if (sender.hasPermission("chatconduit.chat.color")) {
                    try {
                        messageJson = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(ChatUtils.parseLegacy(rawMessage));
                    } catch (Exception ignored) {}
                }

                // 構建跨服私訊封包
                PrivateMessagePacket packet = new PrivateMessagePacket(
                        sender.getUniqueId().toString(),
                        sender.getName(),
                        localServerId,
                        remoteData.getUuid(),
                        remoteData.getName(),
                        targetServerId,
                        rawMessage,
                        System.currentTimeMillis(),
                        messageJson
                );

                // 發送至 Redis PubSub
                RedisManager.publishPrivateMessage(packet);

                // 為發送者渲染並顯示寄出訊息
                renderAndSendSenderMessage(sender, remoteData.getName(), targetServerId, rawMessage);

                // 更新發送者的回覆對象
                setReplyTarget(sender.getUniqueId(), remoteData.getName());
                return;
            }
        }

        // 3. 全服皆找不到目標線上玩家
        ChatUtils.sendMessage(sender, ChatUtils.getMessage("msg.player-not-found")
                .replace("<target>", targetName));
    }

    /**
     * 處理本地玩家間的私訊
     */
    private static void sendLocalPrivateMessage(Player sender, Player target, String serverId, String rawMessage) {
        renderAndSendSenderMessage(sender, target.getName(), serverId, rawMessage);
        renderAndSendReceiverMessage(target, sender.getName(), serverId, rawMessage, null);

        setReplyTarget(sender.getUniqueId(), target.getName());
        setReplyTarget(target.getUniqueId(), sender.getName());
    }

    /**
     * 處理遠端收到的私訊封包
     */
    public static void handleIncomingPrivateMessage(PrivateMessagePacket packet) {
        if (packet == null || packet.getTargetName() == null) return;

        Player localTarget = Bukkit.getPlayerExact(packet.getTargetName());
        if (localTarget == null || !localTarget.isOnline()) {
            localTarget = Bukkit.getPlayer(packet.getTargetName());
        }

        if (localTarget != null && localTarget.isOnline()) {
            String senderName = packet.getSenderName();
            String senderServerId = packet.getSenderServerId() != null ? packet.getSenderServerId() : "Remote";

            Component customComp = null;
            if (packet.getMessageJson() != null && !packet.getMessageJson().isEmpty()) {
                try {
                    customComp = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(packet.getMessageJson());
                } catch (Exception ignored) {}
            }

            renderAndSendReceiverMessage(localTarget, senderName, senderServerId, packet.getRawMessage(), customComp);

            // 更新接收者的回覆對象為遠端發送者
            setReplyTarget(localTarget.getUniqueId(), senderName);
        }
    }

    /**
     * 為寄件者渲染並發送私訊
     */
    private static void renderAndSendSenderMessage(Player sender, String targetName, String targetServerId, String rawMessage) {
        String template = Main.getInstance().getConfig().getString(
                "private-message.sender-format",
                "<gray>[<green>我</green> -> <yellow>{target}</yellow><dark_gray>(<aqua>{target_server}</aqua>)</dark_gray>] <white>{message}"
        );

        String formatted = template
                .replace("{target}", targetName)
                .replace("{target_server}", targetServerId != null ? targetServerId : "");

        Component messageComponent = ChatUtils.parseLegacy(rawMessage);
        Component fullComponent = ChatUtils.parse(sender, formatted)
                .replaceText(builder -> builder.matchLiteral("{message}").replacement(messageComponent));

        sender.sendMessage(fullComponent);
    }

    /**
     * 為收件者渲染並發送私訊
     */
    private static void renderAndSendReceiverMessage(Player receiver, String senderName, String senderServerId, String rawMessage, Component customMessageComponent) {
        String template = Main.getInstance().getConfig().getString(
                "private-message.receiver-format",
                "<gray>[<yellow>{sender}</yellow><dark_gray>(<aqua>{sender_server}</aqua>)</dark_gray> -> <green>我</green>] <white>{message}"
        );

        String formatted = template
                .replace("{sender}", senderName)
                .replace("{sender_server}", senderServerId != null ? senderServerId : "");

        Component messageComponent = customMessageComponent;
        if (messageComponent == null) {
            String cleanedMessage = ChatUtils.cleanInteractiveChatPlaceholders(rawMessage);
            messageComponent = ChatUtils.parseLegacy(cleanedMessage);
        }
        final Component finalMsgComp = messageComponent;

        Component fullComponent = ChatUtils.parse(receiver, formatted)
                .replaceText(builder -> builder.matchLiteral("{message}").replacement(finalMsgComp));

        receiver.sendMessage(fullComponent);

        // 同時輸出至 Console 以供監控紀錄
        Bukkit.getConsoleSender().sendMessage(
                ChatUtils.parse(null, "<gray>[PrivateMsg] <yellow>" + senderName + "</yellow> -> <yellow>" + receiver.getName() + "</yellow>: " + rawMessage)
        );
    }
}
