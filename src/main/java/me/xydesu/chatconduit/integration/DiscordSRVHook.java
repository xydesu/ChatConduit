package me.xydesu.chatconduit.integration;

import github.scarsz.discordsrv.DiscordSRV;
import me.xydesu.chatconduit.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class DiscordSRVHook {

    private static boolean enabled = false;
    private static DiscordSRVListener listener;

    public static void init() {
        if (Bukkit.getPluginManager().isPluginEnabled("DiscordSRV")) {
            boolean configEnabled = Main.getInstance().getConfig().getBoolean("discordsrv.enabled", true);
            if (!configEnabled) {
                Main.getInstance().getLogger().info("[DiscordSRV] 整合已被 config.yml 停用。");
                return;
            }

            try {
                listener = new DiscordSRVListener();
                DiscordSRV.api.subscribe(listener);
                enabled = true;
                Main.getInstance().getLogger().info("[DiscordSRV] 成功掛載並註冊 DiscordSRV 雙向頻道溝通模組！");
            } catch (Exception e) {
                Main.getInstance().getLogger().warning("[DiscordSRV] 初始化溝通模組失敗: " + e.getMessage());
            }
        } else {
            Main.getInstance().getLogger().info("[DiscordSRV] 伺服器未安裝 DiscordSRV，跳過整合。");
        }
    }

    public static void shutdown() {
        if (enabled && listener != null) {
            try {
                DiscordSRV.api.unsubscribe(listener);
            } catch (Exception ignored) {}
            enabled = false;
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void sendToDiscord(Player player, String channelKey, String rawMessage) {
        if (!enabled) return;

        try {
            boolean allowCustom = Main.getInstance().getConfig().getBoolean("discordsrv.forward-custom-group-channels", false);
            if (!allowCustom && me.xydesu.chatconduit.channel.PlayerChannelManager.getChannel(channelKey) != null) {
                return;
            }

            DiscordSRV.getPlugin().processChatMessage(player, rawMessage, channelKey, false);
        } catch (Exception e) {
            Main.getInstance().getLogger().warning("[DiscordSRV] 訊息轉發至 Discord 失敗: " + e.getMessage());
        }
    }
}
