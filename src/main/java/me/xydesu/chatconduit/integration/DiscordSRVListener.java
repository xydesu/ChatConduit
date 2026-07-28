package me.xydesu.chatconduit.integration;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePostProcessEvent;
import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DiscordSRVListener {

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
