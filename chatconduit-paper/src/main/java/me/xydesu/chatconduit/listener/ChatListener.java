package me.xydesu.chatconduit.listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.gui.PlayerInputManager;
import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class ChatListener implements Listener {

    /**
     * 攔截舊版 Spigot AsyncPlayerChatEvent (CMI、Essentials 等舊插件使用的事件)
     * 避免 CMI 搶先印出一次訊息
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        event.getRecipients().clear();
    }

    /**
     * 處理 Paper 新版 AsyncChatEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        // 若該發言屬於 GUI 對話框輸入 (例如建立或重命名頻道)，跳過廣播
        if (PlayerInputManager.isInputPending(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        // 檢查玩家是否在 ChatConduit 禁言清單中
        me.xydesu.chatconduit.mute.MuteManager.MuteEntry muteEntry = me.xydesu.chatconduit.mute.MuteManager.getMute(player.getUniqueId());
        if (muteEntry != null) {
            event.setCancelled(true);
            String reason = muteEntry.reason() != null && !muteEntry.reason().isEmpty() ? muteEntry.reason() : "No reason provided";
            String noticeMsg;
            if (muteEntry.isPermanent()) {
                noticeMsg = Main.getInstance().getLanguageConfig().getString(
                        "mute.chat-blocked-perm",
                        "<red>You are permanently muted. Reason: <reason>"
                ).replace("<reason>", reason);
            } else {
                String remainingStr = me.xydesu.chatconduit.command.MuteCommand.formatDuration(muteEntry.getRemainingMillis());
                noticeMsg = Main.getInstance().getLanguageConfig().getString(
                        "mute.chat-blocked",
                        "<red>You are muted for <time> Reason: <reason>"
                ).replace("<time>", remainingStr).replace("<reason>", reason);
            }
            ChatUtils.sendMessage(player, noticeMsg);
            return;
        }



        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (rawMessage.isEmpty()) return;

        String selectedKey = ChannelManager.getPlayerSelectedKey(player);
        PlayerChannelManager.CustomChannel customChannel = PlayerChannelManager.getChannel(selectedKey);

        String channelName = "";
        String channelColor = "";
        String finalMessage = rawMessage;

        // 1. 使用 ChannelManager 的 Prefix 快取清單進行匹配 (單獨輸入與 prefix-key 相同之符號時直接當作普通發言處理)
        List<ChannelManager.Channel> prefixChannels = ChannelManager.getPrefixChannelsCache();

        ChannelManager.Channel matchedSysChan = null;
        boolean matchedPrefix = false;
        for (ChannelManager.Channel ch : prefixChannels) {
            if (rawMessage.startsWith(ch.prefixKey()) && !rawMessage.equals(ch.prefixKey())) {
                if (ch.permission().isEmpty() || player.hasPermission(ch.permission())) {
                    channelName = ch.name();
                    channelColor = ch.color();
                    finalMessage = rawMessage.substring(ch.prefixKey().length()).trim();
                    matchedPrefix = true;
                    matchedSysChan = ch;
                    customChannel = null;
                    break;
                }
            }
        }

        // 2. 若未匹配 Prefix-Key，依當前選擇頻道決定名稱與顏色
        if (!matchedPrefix) {
            if (customChannel != null) {
                if (!customChannel.getMembers().contains(player.getUniqueId())) {
                    String notInGroupMsg = Main.getInstance().getLanguageConfig().getString(
                            "channel.not-in-group",
                            "<red>You are no longer in this group channel! Reverted to default channel."
                    );
                    ChatUtils.sendMessage(player, notInGroupMsg);
                    // 安全將重置狀態調度至主執行緒操作
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> ChannelManager.setPlayerChannel(player, "global"));
                    return;
                }
                channelName = customChannel.getDisplayName();
                channelColor = customChannel.getColorTheme();
            } else {
                matchedSysChan = ChannelManager.getPlayerChannel(player);
                channelName = matchedSysChan.name();
                channelColor = matchedSysChan.color();
            }
        }

        if (finalMessage.isEmpty()) return;

        // 3. 構造具備 HoverEvent (懸停視窗: 簡介與規則) 與 ClickEvent (點擊切換頻道) 的頻道 Prefix Component
        Component channelPrefixComponent;
        if (customChannel != null) {
            String rawPrefixText = formatMiniMessageTag(channelColor, "[<channel_name>]");

            Player onlineOwner = Bukkit.getPlayer(customChannel.getOwner());
            String ownerName = onlineOwner != null ? onlineOwner.getName() : null;
            if (ownerName == null) {
                org.bukkit.OfflinePlayer offP = Bukkit.getOfflinePlayer(customChannel.getOwner());
                ownerName = offP.getName() != null ? offP.getName() : customChannel.getOwner().toString().substring(0, 8);
            }
            String modeStr = customChannel.getMode() == PlayerChannelManager.Mode.PUBLIC ? "<green>PUBLIC (公共)</green>" : "<red>PRIVATE (私人)</red>";

            String hoverHeader = formatMiniMessageTag(customChannel.getColorTheme(), "<bold>=== 群組頻道: <channel_name> ===</bold>");
            String hoverStr = hoverHeader + "\n" +
                    "<gray>頻道隊長: <yellow><owner_name></yellow>\n" +
                    "<gray>頻道權限: " + modeStr + "\n" +
                    "<gray>成員數量: <yellow><member_count> 人</yellow>\n" +
                    "<gray>頻道簡介: <white><description></white>\n" +
                    "<gray>頻道規則: <red><rules></red>\n\n" +
                    "<yellow>▶ 點擊快速切換發言至此頻道 / 開啟選單</yellow>";

            Component hoverComponent = ChatUtils.parseNoItalic(player, hoverStr,
                    Placeholder.unparsed("channel_name", customChannel.getDisplayName()),
                    Placeholder.unparsed("owner_name", ownerName),
                    Placeholder.unparsed("member_count", String.valueOf(customChannel.getMembers().size())),
                    Placeholder.unparsed("description", customChannel.getDescription()),
                    Placeholder.unparsed("rules", customChannel.getRules())
            );

            channelPrefixComponent = ChatUtils.parseNoItalic(player, rawPrefixText, Placeholder.unparsed("channel_name", customChannel.getDisplayName()))
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(hoverComponent))
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/playerchannel switch " + customChannel.getId()));
        } else {
            ChannelManager.Channel sysChannel = matchedSysChan;
            if (sysChannel == null) sysChannel = ChannelManager.getChannel(selectedKey);
            if (sysChannel == null) sysChannel = ChannelManager.getPlayerChannel(player);

            // 檢查發言玩家是否擁有發言系統頻道的權限
            if (!sysChannel.permission().isEmpty() && !player.hasPermission(sysChannel.permission())) {
                String noPermMsg = Main.getInstance().getLanguageConfig().getString(
                        "channel.no-permission",
                        "<red>你沒有權限進入此頻道！已為你切換回預設頻道。"
                );
                ChatUtils.sendMessage(player, noPermMsg);
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> ChannelManager.setPlayerChannel(player, "global"));
                return;
            }

            String rawPrefixText = formatMiniMessageTag(sysChannel.color(), "[" + sysChannel.name() + "]");

            String prefixKeyStr = sysChannel.prefixKey().isEmpty() ? "無 (選單切換)" : sysChannel.prefixKey();
            String hoverHeader = formatMiniMessageTag(sysChannel.color(), "<bold>=== 官方頻道: <sys_name> ===</bold>");
            String hoverStr = hoverHeader + "\n" +
                    "<gray>頻道類型: <green>● 官方系統頻道</green>\n" +
                    "<gray>快捷鍵前綴: <yellow><prefix_key></yellow>\n" +
                    "<gray>頻道簡介: <white><description></white>\n" +
                    "<gray>頻道規則: <red><rules></red>\n\n" +
                    "<yellow>▶ 點擊快速切換發言至此頻道</yellow>";

            Component hoverComponent = ChatUtils.parseNoItalic(player, hoverStr,
                    Placeholder.unparsed("sys_name", sysChannel.name()),
                    Placeholder.unparsed("prefix_key", prefixKeyStr),
                    Placeholder.unparsed("description", sysChannel.description()),
                    Placeholder.unparsed("rules", sysChannel.rules())
            );

            channelPrefixComponent = ChatUtils.parseNoItalic(player, rawPrefixText)
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(hoverComponent))
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/channel " + sysChannel.key()));
        }

        // 優先透過 InteractiveChat API 處理玩家發言包含的 [item] / [inv] 動態組件
        Component playerMessage = null;
        if (me.xydesu.chatconduit.integration.InteractiveChatIntegration.isAvailable()) {
            playerMessage = me.xydesu.chatconduit.integration.InteractiveChatIntegration.processMessageToComponent(player, finalMessage);
        }

        if (playerMessage == null) {
            playerMessage = event.message();
        }

        if (playerMessage == null || (playerMessage.children().isEmpty() && playerMessage.hoverEvent() == null && playerMessage.clickEvent() == null)) {
            if (player.hasPermission("chatconduit.chat.color")) {
                playerMessage = ChatUtils.parseLegacy(finalMessage);
            } else {
                playerMessage = Component.text(finalMessage);
            }
        }

        if (playerMessage != null) {
            playerMessage = ChatUtils.cleanComponentInteractiveChatTags(playerMessage);
        }

        String rawChatFormat = Main.getInstance().getConfig().getString(
                "chat-format",
                "<white><channel_prefix> <dark_gray>[<gray>{server}<dark_gray>] <gray>[%luckperms_prefix%<gray>] <white><player>> <white><message>"
        );

        String formatWithPapi = rawChatFormat;
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            try {
                formatWithPapi = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, rawChatFormat);
            } catch (Exception ignored) {}
        }

        String currentServerId = me.xydesu.chatconduit.redis.RedisManager.getServerId();
        String formattedTemplate = formatWithPapi.replace("{server}", currentServerId != null ? currentServerId : "");

        Component fullChatMessage = ChatUtils.parse(
                player,
                formattedTemplate,
                Placeholder.component("channel_prefix", channelPrefixComponent),
                Placeholder.component("player", player.displayName()),
                Placeholder.component("message", playerMessage)
        );

        event.viewers().clear();
        event.setCancelled(true);

        // 4. 發送對象判斷與訊息發送
        String channelIdentifier;
        if (customChannel != null) {
            channelIdentifier = customChannel.getId();
            for (UUID memberUuid : customChannel.getMembers()) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline()) {
                    member.sendMessage(fullChatMessage);
                }
            }
            Bukkit.getConsoleSender().sendMessage(fullChatMessage);

            // 派發非同步外接 Webhook 訊息 (若有設定)
            me.xydesu.chatconduit.integration.WebhookManager.sendWebhook(customChannel, player, finalMessage);
        } else {
            ChannelManager.Channel targetSysChan = matchedSysChan != null ? matchedSysChan : ChannelManager.getPlayerChannel(player);
            channelIdentifier = targetSysChan.key();
            String sysPerm = targetSysChan.permission();

            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (sysPerm.isEmpty() || onlinePlayer.hasPermission(sysPerm)) {
                    onlinePlayer.sendMessage(fullChatMessage);
                }
            }
            Bukkit.getConsoleSender().sendMessage(fullChatMessage);
        }

        // 5. 跨伺服器 Redis 訊息廣播 (異步)
        if (me.xydesu.chatconduit.redis.RedisManager.isEnabled()) {
            String messageJson = null;
            try {
                messageJson = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(playerMessage);
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.WARNING, "[InteractiveChat-Debug] 序列化 playerMessage 為 JSON 時失敗:", e);
            }

            Main.getInstance().getLogger().info("[InteractiveChat-Debug] 發送 Redis ChatMessagePacket - sender=" + player.getName() + " (UUID: " + player.getUniqueId() + "), server=" + currentServerId + ", channel=" + channelIdentifier + ", rawMessage=\"" + finalMessage + "\", messageJson=" + messageJson);

            me.xydesu.chatconduit.redis.ChatMessagePacket packet = new me.xydesu.chatconduit.redis.ChatMessagePacket(
                    player.getUniqueId().toString(),
                    player.getName(),
                    channelIdentifier,
                    finalMessage,
                    currentServerId,
                    System.currentTimeMillis(),
                    formatWithPapi,
                    messageJson
            );
            me.xydesu.chatconduit.redis.RedisManager.publishChatMessage(packet);
        }
    }

    private static String formatMiniMessageTag(String colorTheme, String content) {
        if (colorTheme == null || colorTheme.isEmpty()) {
            return content;
        }
        if (colorTheme.startsWith("<gradient:") && !colorTheme.endsWith("</gradient>")) {
            return colorTheme + content + "</gradient>";
        }
        return colorTheme + content;
    }
}
