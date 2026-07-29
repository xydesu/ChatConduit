package me.xydesu.chatconduit.redis;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.JedisPubSub;

import java.util.UUID;
import java.util.logging.Level;

/**
 * 處理從 Redis 訂閱收到的跨伺服器聊天廣播
 *
 * @author xydesu
 */
public class RedisMessageListener extends JedisPubSub {

    @Override
    public void onMessage(String channel, String message) {
        if (message == null || message.trim().isEmpty()) {
            return;
        }

        try {
            // 嘗試解析為 MutePacket
            if (message.contains("\"mutedBy\"") || (message.contains("\"expireAt\"") && message.contains("\"action\"") && !message.contains("\"targetPlayerName\""))) {
                MutePacket mutePacket = MutePacket.fromJson(message);
                if (mutePacket != null && mutePacket.action() != null) {
                    me.xydesu.chatconduit.mute.MuteManager.handleRemoteMutePacket(mutePacket);
                    return;
                }
            }

            // 嘗試解析為 PlayerChannelSyncPacket
            if (message.contains("\"channelId\"") && message.contains("\"action\"") && (message.contains("\"ownerUuid\"") || message.contains("\"colorTheme\"") || message.contains("\"members\"") || message.contains("\"originServerId\""))) {
                PlayerChannelSyncPacket syncPacket = PlayerChannelSyncPacket.fromJson(message);
                if (syncPacket != null && syncPacket.getAction() != null) {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> PlayerChannelManager.syncFromRemote(syncPacket));
                    return;
                }
            }

            // 嘗試解析為 ChannelInvitePacket
            if (message.contains("\"action\"") && message.contains("\"targetPlayerName\"")) {
                ChannelInvitePacket invitePacket = ChannelInvitePacket.fromJson(message);
                if (invitePacket != null) {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> processInvitePacket(invitePacket));
                    return;
                }
            }

            // 嘗試解析為 PrivateMessagePacket
            if (message.contains("\"targetName\"") && message.contains("\"rawMessage\"") && !message.contains("\"channelName\"")) {
                PrivateMessagePacket pmPacket = PrivateMessagePacket.fromJson(message);
                if (pmPacket != null && pmPacket.getSenderServerId() != null) {
                    // 忽略來自本伺服器的發送封包 (本服發送者已直接本地處理)
                    if (!pmPacket.getSenderServerId().equalsIgnoreCase(RedisManager.getServerId())) {
                        Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                                me.xydesu.chatconduit.message.PrivateMessageManager.handleIncomingPrivateMessage(pmPacket)
                        );
                    }
                    return;
                }
            }

            ChatMessagePacket packet = ChatMessagePacket.fromJson(message);
            if (packet == null) return;

            // 忽略來自本伺服器的訊息 (本服訊息已在 local 即時廣播過)
            if (packet.getServerId() != null && packet.getServerId().equalsIgnoreCase(RedisManager.getServerId())) {
                return;
            }

            // 切換至 Paper/Spigot 主執行緒進行廣播與 Component 渲染
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> processRemoteMessage(packet));
        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.WARNING, "解析來自 Redis 的跨服訊息封包時失敗:", e);
        }
    }

    /**
     * 處理並渲染來自遠端伺服器的訊息
     */
    private void processRemoteMessage(ChatMessagePacket packet) {
        String channelKeyOrName = packet.getChannelName();
        String rawMessage = packet.getRawMessage();
        String senderName = packet.getSenderName();
        String remoteServerId = packet.getServerId() != null ? packet.getServerId() : "Remote";

        PlayerChannelManager.CustomChannel customChannel = PlayerChannelManager.getChannel(channelKeyOrName);
        ChannelManager.Channel sysChannel = ChannelManager.getChannel(channelKeyOrName);

        // 如果既不是 key 匹配的系統頻道也不是自訂頻道，嘗試比對系統頻道名稱
        if (sysChannel == null && customChannel == null) {
            for (ChannelManager.Channel ch : ChannelManager.getChannels().values()) {
                if (ch.name().equalsIgnoreCase(channelKeyOrName)) {
                    sysChannel = ch;
                    break;
                }
            }
        }

        Component channelPrefixComponent;

        if (customChannel != null) {
            String channelColor = customChannel.getColorTheme();
            String rawPrefixText = channelColor + "[" + customChannel.getDisplayName() + "]</gradient>";
            if (!channelColor.startsWith("<gradient:")) {
                rawPrefixText = channelColor + "[" + customChannel.getDisplayName() + "]";
            }

            String hoverStr = customChannel.getColorTheme() + "<bold>=== 跨服群組頻道: <channel_name> ===</bold></gradient>\n" +
                    "<gray>來源伺服器: <aqua><server_id></aqua>\n" +
                    "<gray>發言玩家: <yellow><sender></yellow>\n" +
                    "<gray>頻道簡介: <white><description></white>\n\n" +
                    "<yellow>▶ 點擊切換發言頻道</yellow>";

            Component hoverComponent = ChatUtils.parseNoItalic(null, hoverStr,
                    Placeholder.unparsed("channel_name", customChannel.getDisplayName()),
                    Placeholder.unparsed("server_id", remoteServerId),
                    Placeholder.unparsed("sender", senderName),
                    Placeholder.unparsed("description", customChannel.getDescription())
            );

            channelPrefixComponent = ChatUtils.parseNoItalic(null, rawPrefixText)
                    .hoverEvent(HoverEvent.showText(hoverComponent))
            .clickEvent(ClickEvent.runCommand("/playerchannel switch " + customChannel.getId()));
        } else if (sysChannel != null) {
            String rawPrefixText = sysChannel.color() + "[" + sysChannel.name() + "]</gradient>";
            if (!sysChannel.color().startsWith("<gradient:")) {
                rawPrefixText = sysChannel.color() + "[" + sysChannel.name() + "]";
            }

            String hoverStr = sysChannel.color() + "<bold>=== 跨服官方頻道: <sys_name> ===</bold></gradient>\n" +
                    "<gray>來源伺服器: <aqua><server_id></aqua>\n" +
                    "<gray>發言玩家: <yellow><sender></yellow>\n" +
                    "<gray>頻道簡介: <white><description></white>\n\n" +
                    "<yellow>▶ 點擊切換發言至此頻道</yellow>";

            Component hoverComponent = ChatUtils.parseNoItalic(null, hoverStr,
                    Placeholder.unparsed("sys_name", sysChannel.name()),
                    Placeholder.unparsed("server_id", remoteServerId),
                    Placeholder.unparsed("sender", senderName),
                    Placeholder.unparsed("description", sysChannel.description())
            );

            channelPrefixComponent = ChatUtils.parseNoItalic(null, rawPrefixText)
                    .hoverEvent(HoverEvent.showText(hoverComponent))
                    .clickEvent(ClickEvent.runCommand("/channel " + sysChannel.key()));
        } else {
            // 備用無樣式頻道標籤
            channelPrefixComponent = ChatUtils.parseNoItalic(null, "<gray>[" + channelKeyOrName + "]");
        }

        // 訊息文字元件 (若有傳送 messageJson，先清理 InteractiveChat 內部標籤避免遠端無快取時出錯，再反序列化復原 Component)
        Component playerMessageComponent = null;
        if (packet.getMessageJson() != null && !packet.getMessageJson().isEmpty()) {
            try {
                String cleanedJson = ChatUtils.cleanInteractiveChatPlaceholders(packet.getMessageJson());
                playerMessageComponent = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(cleanedJson);
            } catch (Exception ignored) {}
        }
        if (playerMessageComponent == null) {
            String cleanedMessage = ChatUtils.cleanInteractiveChatPlaceholders(rawMessage);
            playerMessageComponent = ChatUtils.parseLegacy(cleanedMessage);
        }

        String rawChatFormat = packet.getChatFormat();
        if (rawChatFormat == null || rawChatFormat.isEmpty()) {
            rawChatFormat = Main.getInstance().getConfig().getString(
                    "chat-format",
                    "<white><channel_prefix> <dark_gray>[<gray>{server}<dark_gray>] <gray>[%luckperms_prefix%<gray>] <white><player>> <white><message>"
            );
        }

        // 如果模板含有 {server}，將其替換為 remoteServerId；否則若無伺服器則消除
        String formattedTemplate = rawChatFormat.replace("{server}", remoteServerId);

        org.bukkit.OfflinePlayer senderOfflinePlayer = null;
        if (packet.getSenderUuid() != null && !packet.getSenderUuid().isEmpty()) {
            try {
                senderOfflinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(packet.getSenderUuid()));
            } catch (Exception ignored) {}
        }

        Component fullChatMessage = ChatUtils.parse(
                senderOfflinePlayer,
                formattedTemplate,
                Placeholder.component("channel_prefix", channelPrefixComponent),
                Placeholder.unparsed("player", senderName),
                Placeholder.component("message", playerMessageComponent)
        );

        // 發送給本服對應成員或全服玩家
        if (customChannel != null) {
            for (UUID memberUuid : customChannel.getMembers()) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline()) {
                    member.sendMessage(fullChatMessage);
                }
            }
            Bukkit.getConsoleSender().sendMessage(fullChatMessage);
        } else {
            String sysPerm = sysChannel != null ? sysChannel.permission() : "";
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (sysPerm.isEmpty() || onlinePlayer.hasPermission(sysPerm)) {
                    onlinePlayer.sendMessage(fullChatMessage);
                }
            }
            Bukkit.getConsoleSender().sendMessage(fullChatMessage);
        }
    }

    /**
     * 處理來自 Redis 的跨服頻道邀請與成員狀態變動封包
     */
    private void processInvitePacket(ChannelInvitePacket packet) {
        if (packet == null || packet.getAction() == null) return;

        switch (packet.getAction()) {
            case INVITE -> {
                // 檢查目標玩家是否在本伺服器在線
                Player targetPlayer = Bukkit.getPlayerExact(packet.getTargetPlayerName());
                if (targetPlayer == null || !targetPlayer.isOnline()) {
                    targetPlayer = Bukkit.getPlayer(packet.getTargetPlayerName());
                }

                if (targetPlayer != null && targetPlayer.isOnline()) {
                    PlayerChannelManager.CustomChannel channel = PlayerChannelManager.getOrLoadChannel(packet.getChannelId());
                    if (channel == null) {
                        UUID senderUuid = null;
                        try {
                            if (packet.getSenderUuid() != null) {
                                senderUuid = UUID.fromString(packet.getSenderUuid());
                            }
                        } catch (Exception ignored) {}

                        if (senderUuid == null) {
                            senderUuid = UUID.randomUUID();
                        }

                        channel = new PlayerChannelManager.CustomChannel(
                                packet.getChannelId(),
                                packet.getChannelDisplayName() != null ? packet.getChannelDisplayName() : packet.getChannelId(),
                                senderUuid,
                                PlayerChannelManager.Mode.PRIVATE,
                                "<gradient:#a8c0ff:#3f2b96>"
                        );
                        PlayerChannelManager.registerChannel(channel);
                    }

                    channel.getPendingInvites().add(targetPlayer.getUniqueId());
                    PlayerChannelManager.saveChannel(channel);

                    // 發送跨服互動邀請推播給目標玩家
                    ChatUtils.sendRemoteInviteNotification(
                            packet.getSenderName(),
                            packet.getOriginServerId(),
                            targetPlayer,
                            packet.getChannelId(),
                            packet.getChannelDisplayName()
                    );
                }
            }
            case ACCEPT -> {
                // 有玩家在遠端接受了頻道邀請
                PlayerChannelManager.CustomChannel channel = PlayerChannelManager.getOrLoadChannel(packet.getChannelId());
                if (channel != null) {
                    // 同步成員清單
                    try {
                        UUID targetUuid = UUID.fromString(packet.getSenderUuid());
                        channel.getPendingInvites().remove(targetUuid);
                        if (!channel.getMembers().contains(targetUuid)) {
                            channel.getMembers().add(targetUuid);
                            PlayerChannelManager.saveChannel(channel);
                        }
                    } catch (Exception ignored) {}

                    // 向本服的頻道成員發送加入通知廣播
                    PlayerChannelManager.broadcastToMembers(
                            channel,
                            "<green>▶ 玩家 <yellow>" + packet.getSenderName() + "</yellow> <gray>(來自 " + packet.getOriginServerId() + ")</gray> 已加入群組頻道 <yellow>" + channel.getDisplayName() + "</yellow>！",
                            null
                    );
                }
            }
            case REJECT -> {
                // 目標玩家拒絕了邀請，通知隊長
                Player senderPlayer = Bukkit.getPlayerExact(packet.getSenderName());
                if (senderPlayer == null || !senderPlayer.isOnline()) {
                    senderPlayer = Bukkit.getPlayer(packet.getSenderName());
                }
                if (senderPlayer != null && senderPlayer.isOnline()) {
                    ChatUtils.sendMessage(senderPlayer, "<gray>玩家 <yellow>" + packet.getTargetPlayerName() + "</yellow> 拒絕了加入頻道 <yellow>" + packet.getChannelDisplayName() + "</yellow> 的邀請。");
                }
            }
            default -> {}
        }
    }
}
