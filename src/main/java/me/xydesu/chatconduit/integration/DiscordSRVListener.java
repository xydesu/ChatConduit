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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.UUID;

public class DiscordSRVListener {

    /**
     * 監聽並攔截/修改 Minecraft 傳送至 Discord 的發言事件 (依據 channel-mapping 精準映射頻道)
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

        // 讀取 config.yml 的 channel-mapping 映射設定
        String mappedChannel = Main.getInstance().getConfig().getString("discordsrv.channel-mapping." + selectedKey.toLowerCase());
        if (mappedChannel != null && !mappedChannel.isEmpty()) {
            event.setChannel(mappedChannel);
        } else {
            event.setChannel(selectedKey);
        }
    }

    /**
     * 監聽並轉發 Discord 至 Minecraft 遊戲內頻道的發言 (依據 Discord 頻道 ID/名稱反向精準發送至指定頻道)
     */
    @Subscribe
    public void onDiscordMessage(DiscordGuildMessagePostProcessEvent event) {
        if (event.getAuthor() == null || event.getAuthor().isBot()) return;

        String discordChannelId = event.getChannel().getId();
        String discordChannelName = event.getChannel().getName();
        String authorName = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
        String messageText = event.getMessage().getContentDisplay();

        if (messageText.isEmpty()) return;

        // 反向尋找這則 Discord 訊息屬於哪個 ChatConduit 頻道 Key
        String targetChannelKey = null;
        ConfigurationSection mapSec = Main.getInstance().getConfig().getConfigurationSection("discordsrv.channel-mapping");

        if (mapSec != null) {
            for (String key : mapSec.getKeys(false)) {
                String mappedValue = mapSec.getString(key);
                if (mappedValue != null && (mappedValue.equalsIgnoreCase(discordChannelId) || mappedValue.equalsIgnoreCase(discordChannelName))) {
                    targetChannelKey = key.toLowerCase();
                    break;
                }
            }
        }

        // 若無明確 mapping 匹配，則嘗試以 Discord 頻道名稱作為對應 Key
        if (targetChannelKey == null) {
            targetChannelKey = discordChannelName.toLowerCase();
        }

        PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(targetChannelKey);

        // 格式化來自 Discord 的訊息
        String displayChannelTitle = customChan != null ? customChan.getDisplayName() : targetChannelKey;
        ChannelManager.Channel sysChan = ChannelManager.getChannel(targetChannelKey);
        if (sysChan != null) {
            displayChannelTitle = sysChan.name();
        }

        String format = "<blue>[Discord]</blue> <gray>[<gold>" + displayChannelTitle + "<gray>]</gray> <white>" + authorName + "</white>: <white>" + messageText + "</white>";
        Component discordMsgComponent = ChatUtils.parseLegacy(format);

        if (customChan != null) {
            boolean allowCustom = Main.getInstance().getConfig().getBoolean("discordsrv.forward-custom-group-channels", false);
            if (!allowCustom) return;

            for (UUID memberUuid : customChan.getMembers()) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline()) {
                    member.sendMessage(discordMsgComponent);
                }
            }
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                String curKey = ChannelManager.getPlayerSelectedKey(player);
                if (curKey.equalsIgnoreCase(targetChannelKey) || (targetChannelKey.equalsIgnoreCase("main") && curKey.equalsIgnoreCase("global"))) {
                    player.sendMessage(discordMsgComponent);
                }
            }
        }
    }
}
