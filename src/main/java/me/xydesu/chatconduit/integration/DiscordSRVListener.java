package me.xydesu.chatconduit.integration;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import github.scarsz.discordsrv.api.events.GameChatMessagePreProcessEvent;
import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DiscordSRVListener {

    /**
     * 監聽並攔截/修改 Minecraft 傳送至 Discord 的發言事件 (避免雙重發送 duplicates)
     */
    @Subscribe
    public void onGameChatMessagePreProcess(GameChatMessagePreProcessEvent event) {
        Player player = event.getPlayer();
        if (player == null) return;

        String selectedKey = ChannelManager.getPlayerSelectedKey(player);
        boolean isCustom = PlayerChannelManager.getChannel(selectedKey) != null;

        boolean allowCustom = Main.getInstance().getConfig().getBoolean("discordsrv.forward-custom-group-channels", false);
        if (isCustom && !allowCustom) {
            event.setCancelled(true);
            return;
        }

        // 將遊戲內當前選擇的頻道 Key 設定給 DiscordSRV，利於 DiscordSRV 進行頻道映射 (如 global, trade 等)
        event.setChannel(selectedKey);
    }

    /**
     * 監聽並轉發 Discord 至 Minecraft 遊戲內頻道的發言
     */
    @Subscribe
    public void onDiscordMessage(DiscordGuildMessagePostProcessEvent event) {
        if (event.getAuthor() == null || event.getAuthor().isBot()) return;

        String channelName = event.getChannel().getName();
        String authorName = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
        String messageText = event.getMessage().getContentDisplay();

        if (messageText.isEmpty()) return;

        String targetChannelKey = event.getChannel().getName();

        // 格式化來自 Discord 的訊息
        String format = "<blue>[Discord]</blue> <gray>[<gold>" + channelName + "<gray>]</gray> <white>" + authorName + "</white>: <white>" + messageText + "</white>";
        Component discordMsgComponent = ChatUtils.parseLegacy(format);

        PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(targetChannelKey);
        if (customChan != null) {
            for (UUID memberUuid : customChan.getMembers()) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline()) {
                    member.sendMessage(discordMsgComponent);
                }
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String curKey = ChannelManager.getPlayerSelectedKey(player);
                if (curKey.equalsIgnoreCase(targetChannelKey) || targetChannelKey.equalsIgnoreCase("global") || targetChannelKey.equalsIgnoreCase("main")) {
                    player.sendMessage(discordMsgComponent);
                }
            }
        }
    }
}
