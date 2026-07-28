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

        event.viewers().clear();
        event.setCancelled(true);

        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        if (rawMessage.isEmpty()) return;

        String selectedKey = ChannelManager.getPlayerSelectedKey(player);
        PlayerChannelManager.CustomChannel customChannel = PlayerChannelManager.getChannel(selectedKey);

        String channelName = "";
        String channelColor = "";
        String finalMessage = rawMessage;

        // 1. 使用 ChannelManager 的 Prefix 快取清單進行匹配
        List<ChannelManager.Channel> prefixChannels = ChannelManager.getPrefixChannelsCache();

        boolean matchedPrefix = false;
        for (ChannelManager.Channel ch : prefixChannels) {
            if (rawMessage.startsWith(ch.prefixKey())) {
                if (ch.permission().isEmpty() || player.hasPermission(ch.permission())) {
                    channelName = ch.name();
                    channelColor = ch.color();
                    finalMessage = rawMessage.substring(ch.prefixKey().length()).trim();
                    matchedPrefix = true;
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
                ChannelManager.Channel sysChannel = ChannelManager.getPlayerChannel(player);
                channelName = sysChannel.name();
                channelColor = sysChannel.color();
            }
        }

        if (finalMessage.isEmpty()) return;

        // 3. 構造具備 HoverEvent (懸停視窗: 簡介與規則) 與 ClickEvent (點擊切換頻道) 的頻道 Prefix Component
        Component channelPrefixComponent;
        if (customChannel != null) {
            String rawPrefixText = channelColor + "[" + channelName + "]</gradient>";
            if (!channelColor.startsWith("<gradient:")) {
                rawPrefixText = channelColor + "[" + channelName + "]";
            }

            org.bukkit.OfflinePlayer ownerP = Bukkit.getOfflinePlayer(customChannel.getOwner());
            String ownerName = ownerP.getName() != null ? ownerP.getName() : customChannel.getOwner().toString();
            String modeStr = customChannel.getMode() == PlayerChannelManager.Mode.PUBLIC ? "<green>PUBLIC (公共)</green>" : "<red>PRIVATE (私人)</red>";

            String hoverStr = customChannel.getColorTheme() + "<bold>=== 群組頻道: " + customChannel.getDisplayName() + " ===</bold></gradient>\n" +
                    "<gray>頻道隊長: <yellow>" + ownerName + "</yellow>\n" +
                    "<gray>頻道權限: " + modeStr + "\n" +
                    "<gray>成員數量: <yellow>" + customChannel.getMembers().size() + " 人</yellow>\n" +
                    "<gray>頻道簡介: <white>" + customChannel.getDescription() + "</white>\n" +
                    "<gray>頻道規則: <red>" + customChannel.getRules() + "</red>\n\n" +
                    "<yellow>▶ 點擊快速切換發言至此頻道 / 開啟選單</yellow>";

            Component hoverComponent = ChatUtils.parseNoItalic(player, hoverStr);

            channelPrefixComponent = ChatUtils.parseNoItalic(player, rawPrefixText)
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(hoverComponent))
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/playerchannel switch " + customChannel.getId()));
        } else {
            ChannelManager.Channel sysChannel = ChannelManager.getChannel(selectedKey);
            if (sysChannel == null) sysChannel = ChannelManager.getPlayerChannel(player);

            String prefixSymbol = sysChannel.prefixKey() != null ? sysChannel.prefixKey() : "";
            String sysDisplayName = prefixSymbol + sysChannel.name();
            String rawPrefixText = sysChannel.color() + "[" + sysDisplayName + "]</gradient>";
            if (!sysChannel.color().startsWith("<gradient:")) {
                rawPrefixText = sysChannel.color() + "[" + sysDisplayName + "]";
            }

            String prefixKeyStr = sysChannel.prefixKey().isEmpty() ? "無 (選單切換)" : sysChannel.prefixKey();
            String hoverStr = sysChannel.color() + "<bold>=== 官方頻道: " + sysChannel.name() + " ===</bold></gradient>\n" +
                    "<gray>頻道類型: <green>● 官方系統頻道</green>\n" +
                    "<gray>快捷鍵前綴: <yellow>" + prefixKeyStr + "</yellow>\n" +
                    "<gray>頻道簡介: <white>" + sysChannel.description() + "</white>\n" +
                    "<gray>頻道規則: <red>" + sysChannel.rules() + "</red>\n\n" +
                    "<yellow>▶ 點擊快速切換發言至此頻道</yellow>";

            Component hoverComponent = ChatUtils.parseNoItalic(player, hoverStr);

            channelPrefixComponent = ChatUtils.parseNoItalic(player, rawPrefixText)
                    .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(hoverComponent))
                    .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/playerchannel switch " + sysChannel.key()));
        }

        Component playerMessage = ChatUtils.parseLegacy(finalMessage);

        String rawChatFormat = Main.getInstance().getConfig().getString(
                "chat-format",
                "<white><channel_prefix> <gray>[%luckperms_prefix%<gray>] <white><player>> <white><message>"
        );

        Component fullChatMessage = ChatUtils.parse(
                player,
                rawChatFormat,
                Placeholder.component("channel_prefix", channelPrefixComponent),
                Placeholder.component("player", player.displayName()),
                Placeholder.component("message", playerMessage)
        );

        // 4. 發送對象判斷
        if (customChannel != null) {
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
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendMessage(fullChatMessage);
            }
            Bukkit.getConsoleSender().sendMessage(fullChatMessage);
        }
    }
}
