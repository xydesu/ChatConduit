package me.xydesu.chatconduit.integration;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class WebhookManager {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /**
     * 非同步發送玩家群組對話訊息至隊長設定的 Discord Webhook URL
     */
    public static void sendWebhook(PlayerChannelManager.CustomChannel customChan, Player player, String message) {
        if (customChan == null || player == null || message == null || message.isEmpty()) return;

        String webhookUrl = customChan.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.trim().isEmpty() || !webhookUrl.startsWith("http")) return;

        FileConfiguration config = Main.getInstance().getConfig();

        boolean enabled = config.getBoolean("discordsrv.webhook.allow-custom-webhooks", true);
        if (!enabled) return;

        String usernameFmt = config.getString("discordsrv.webhook.username-format", "%player% [%channel%]");
        String avatarUrlFmt = config.getString("discordsrv.webhook.avatar-url", "https://mc-heads.net/avatar/%player%/64");

        String username = usernameFmt.replace("%player%", player.getName())
                .replace("%channel%", customChan.getDisplayName());

        String avatarUrl = avatarUrlFmt.replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString());

        // 轉義 JSON 特殊字元
        String jsonPayload = String.format(
                "{\"username\": \"%s\", \"avatar_url\": \"%s\", \"content\": \"%s\"}",
                escapeJson(username),
                escapeJson(avatarUrl),
                escapeJson(message)
        );

        // 非同步在背景排程器發送 HTTP POST 請求 (避免主執行緒卡頓)
        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(webhookUrl.trim()))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .timeout(Duration.ofSeconds(5))
                        .build();

                HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                        .exceptionally(throwable -> {
                            Main.getInstance().getLogger().warning("發送 Webhook 訊息失敗 [" + customChan.getId() + "]: " + throwable.getMessage());
                            return null;
                        });
            } catch (Exception e) {
                Main.getInstance().getLogger().warning("無法發送 Webhook 請求: " + e.getMessage());
            }
        });
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
