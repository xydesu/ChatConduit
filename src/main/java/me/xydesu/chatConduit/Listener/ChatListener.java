package me.xydesu.chatConduit.Listener;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.xydesu.chatConduit.Channel.ChannelManager;
import me.xydesu.chatConduit.Channel.PlayerChannelManager;
import me.xydesu.chatConduit.Main;
import me.xydesu.chatConduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ChatListener implements Listener {

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        event.setCancelled(true);

        Player player = event.getPlayer();
        String rawMessage = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        String selectedKey = ChannelManager.getPlayerSelectedKey(player);
        PlayerChannelManager.CustomChannel customChannel = PlayerChannelManager.getChannel(selectedKey);

        String channelName = "";
        String channelColor = "";
        String finalMessage = rawMessage;

        // 1. 檢查 Prefix-Key 快速觸發系統頻道
        List<ChannelManager.Channel> prefixChannels = ChannelManager.getChannels().values().stream()
                .filter(c -> c.prefixKey() != null && !c.prefixKey().isEmpty())
                .sorted(Comparator.comparingInt((ChannelManager.Channel c) -> c.prefixKey().length()).reversed())
                .toList();

        boolean matchedPrefix = false;
        for (ChannelManager.Channel ch : prefixChannels) {
            if (rawMessage.startsWith(ch.prefixKey())) {
                if (ch.permission().isEmpty() || player.hasPermission(ch.permission())) {
                    channelName = ch.name();
                    channelColor = ch.color();
                    finalMessage = rawMessage.substring(ch.prefixKey().length()).trim();
                    matchedPrefix = true;
                    // 當使用 PrefixKey 時，會覆蓋當前選取的自訂頻道，視為系統頻道廣播
                    customChannel = null;
                    break;
                }
            }
        }

        // 2. 若未匹配 Prefix-Key，依當前選擇頻道決定名稱與顏色
        if (!matchedPrefix) {
            if (customChannel != null) {
                // 檢查發言者是否還在該群組頻道內
                if (!customChannel.getMembers().contains(player.getUniqueId())) {
                    String notInGroupMsg = Main.getInstance().getLanguageConfig().getString(
                            "channel.not-in-group",
                            "<red>You are no longer in this group channel! Reverted to default channel."
                    );
                    ChatUtils.sendMessage(player, notInGroupMsg);
                    ChannelManager.setPlayerChannel(player, "global");
                    return;
                }
                channelName = customChannel.getDisplayName();
                channelColor = "<gradient:#a8c0ff:#3f2b96>";
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

        // 4. 發送對象判斷（關鍵修訂點）
        if (customChannel != null) {
            // 【玩家自訂/私人群組頻道】：只發送給已加入該頻道成員（Members）且在線的玩家
            for (UUID memberUuid : customChannel.getMembers()) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline()) {
                    member.sendMessage(fullChatMessage);
                }
            }
            // 控制台紀錄一份訊息
            Bukkit.getConsoleSender().sendMessage(fullChatMessage);
        } else {
            // 【系統公用頻道】：廣播給全伺服器所有人（event.viewers()）
            for (var viewer : event.viewers()) {
                viewer.sendMessage(fullChatMessage);
            }
        }
    }
}