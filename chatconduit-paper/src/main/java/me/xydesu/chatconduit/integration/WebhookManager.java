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

        String cleanMsg = message;

        // 轉義 JSON 特殊字元
        String jsonPayload = String.format(
                "{\"username\": \"%s\", \"avatar_url\": \"%s\", \"content\": \"%s\"}",
                escapeJson(username),
                escapeJson(avatarUrl),
                escapeJson(cleanMsg)
        );

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
    }

    public record TestResult(boolean success, int statusCode, String errorMessage) {}

    /**
     * 非同步測試傳送 Discord Webhook 連線訊息
     */
    public static void testWebhook(String webhookUrl, String channelName, Player player, java.util.function.Consumer<TestResult> callback) {
        if (webhookUrl == null || webhookUrl.trim().isEmpty() || (!webhookUrl.startsWith("http://") && !webhookUrl.startsWith("https://"))) {
            if (callback != null) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> callback.accept(new TestResult(false, -1, "無效的 Webhook 網址 (必須以 http:// 或 https:// 開頭)")));
            }
            return;
        }

        FileConfiguration config = Main.getInstance().getConfig();
        String usernameFmt = config.getString("discordsrv.webhook.username-format", "%player% [%channel%]");
        String avatarUrlFmt = config.getString("discordsrv.webhook.avatar-url", "https://mc-heads.net/avatar/%player%/64");

        String pName = player != null ? player.getName() : "System";
        String pUuid = player != null ? player.getUniqueId().toString() : "";
        String cName = channelName != null ? channelName : "TestChannel";

        String username = usernameFmt.replace("%player%", pName).replace("%channel%", cName);
        String avatarUrl = avatarUrlFmt.replace("%player%", pName).replace("%uuid%", pUuid);
        String testMessage = "✅ **[ChatConduit] Webhook 連線測試成功！**\\n> 頻道: **" + cName + "**\\n> 測試觸發者: `" + pName + "`";

        String jsonPayload = String.format(
                "{\"username\": \"%s\", \"avatar_url\": \"%s\", \"content\": \"%s\"}",
                escapeJson(username),
                escapeJson(avatarUrl),
                escapeJson(testMessage)
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl.trim()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        int code = response.statusCode();
                        boolean isSuccess = code >= 200 && code < 300;
                        String errReason = isSuccess ? null : "HTTP " + code + (response.body() != null && !response.body().isEmpty() ? " - " + response.body() : "");
                        TestResult result = new TestResult(isSuccess, code, errReason);
                        if (callback != null) {
                            Bukkit.getScheduler().runTask(Main.getInstance(), () -> callback.accept(result));
                        }
                    })
                    .exceptionally(throwable -> {
                        String causeMsg = throwable.getCause() != null ? throwable.getCause().getMessage() : throwable.getMessage();
                        TestResult result = new TestResult(false, -1, causeMsg != null ? causeMsg : "連線逾時或網路錯誤");
                        if (callback != null) {
                            Bukkit.getScheduler().runTask(Main.getInstance(), () -> callback.accept(result));
                        }
                        return null;
                    });
        } catch (Exception e) {
            if (callback != null) {
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> callback.accept(new TestResult(false, -1, e.getMessage())));
            }
        }
    }

    /**
     * 當頻道解散/刪除時，清理冷卻時間快取，防止記憶體殘留
     */
    public static void removeCooldown(String channelId) {
        if (channelId != null) {
            LAST_SENT_MAP.remove(channelId.toLowerCase());
        }
    }

    public static String cleanInteractiveChatPlaceholders(String text) {
        return text != null ? text : "";
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
