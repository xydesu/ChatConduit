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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebhookManager {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    // 匹配 InteractiveChat 內部佔位符 (例如 <chat=UUID:[item]:> 或 <chat=UUID:[ping]:>)
    private static final Pattern INTERACTIVE_CHAT_PATTERN = Pattern.compile("<chat=[^:>]+:(\\[[^\\]]+\\]|[^:>]+):?>");

    private static final java.util.Map<String, Long> LAST_SENT_MAP = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 1000; // 每頻道 1 秒速率限制

    /**
     * 非同步發送玩家群組對話訊息至隊長設定的 Discord Webhook URL
     */
    public static void sendWebhook(PlayerChannelManager.CustomChannel customChan, Player player, String message) {
        if (customChan == null || player == null || message == null || message.isEmpty()) return;

        String webhookUrl = customChan.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.trim().isEmpty() || !webhookUrl.startsWith("http")) return;

        // 速率限制檢查
        long now = System.currentTimeMillis();
        Long lastSent = LAST_SENT_MAP.get(customChan.getId());
        if (lastSent != null && (now - lastSent) < COOLDOWN_MS) {
            return;
        }
        LAST_SENT_MAP.put(customChan.getId(), now);

        FileConfiguration config = Main.getInstance().getConfig();

        boolean enabled = config.getBoolean("discordsrv.webhook.allow-custom-webhooks", true);
        if (!enabled) return;

        String usernameFmt = config.getString("discordsrv.webhook.username-format", "%player% [%channel%]");
        String avatarUrlFmt = config.getString("discordsrv.webhook.avatar-url", "https://mc-heads.net/avatar/%player%/64");

        String username = usernameFmt.replace("%player%", player.getName())
                .replace("%channel%", customChan.getDisplayName());

        String avatarUrl = avatarUrlFmt.replace("%player%", player.getName())
                .replace("%uuid%", player.getUniqueId().toString());

        // 清理 InteractiveChat 插件產生的內部未解析標籤 <chat=UUID:[item]:> 轉為乾淨的 [item]
        String cleanMsg = cleanInteractiveChatPlaceholders(message);

        // 轉義 JSON 特殊字元
        String jsonPayload = String.format(
                "{\"username\": \"%s\", \"avatar_url\": \"%s\", \"content\": \"%s\"}",
                escapeJson(username),
                escapeJson(avatarUrl),
                escapeJson(cleanMsg)
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

    /**
     * 清理 InteractiveChat 內部佔位符標籤 (將 <chat=UUID:[item]:> 轉為乾淨的 [item])
     */
    public static String cleanInteractiveChatPlaceholders(String text) {
        if (text == null || text.isEmpty()) return "";
        Matcher matcher = INTERACTIVE_CHAT_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String tag = matcher.group(1);
            if (!tag.startsWith("[")) {
                tag = "[" + tag + "]";
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(tag));
        }
        matcher.appendTail(sb);
        return sb.toString();
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
