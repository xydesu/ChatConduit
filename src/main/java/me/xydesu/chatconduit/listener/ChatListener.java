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

        // 3. 組合聊天 Component
        Component playerMessage = ChatUtils.parseLegacy(finalMessage);

        String rawChatFormat = Main.getInstance().getConfig().getString(
                "chat-format",
                "<white>[<channel_color><channel_name><white>] <gray>[%luckperms_prefix%<gray>] <white><player>> <white><message>"
        );

        String formattedWithChannel = rawChatFormat
                .replace("<channel_color>", channelColor)
                .replace("<channel_name>", channelName);

        Component fullChatMessage = ChatUtils.parse(
                player,
                formattedWithChannel,
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
        } else {
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                onlinePlayer.sendMessage(fullChatMessage);
            }
            Bukkit.getConsoleSender().sendMessage(fullChatMessage);
        }
    }
}
