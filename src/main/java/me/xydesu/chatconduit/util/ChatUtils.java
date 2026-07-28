package me.xydesu.chatconduit.util;

import me.clip.placeholderapi.PlaceholderAPI;
import me.xydesu.chatconduit.Main;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern PAPI_PATTERN = Pattern.compile("%([^%]+)%");

    /**
     * 解析 MiniMessage 格式字串，自動處理 PlaceholderAPI 佔位符與 Legacy 顏色碼
     *
     * @param player    對應的玩家 (若為 null 則跳過 PAPI 解析)
     * @param format    帶有 MiniMessage 標籤或 %papi% 佔位符的格式字串
     * @param extraTags 額外傳入的 MiniMessage 標籤 (例如 <player>, <message>)
     * @return 解析完成的 Adventure Component
     */
    public static Component parse(Player player, String format, TagResolver... extraTags) {
        if (format == null || format.isEmpty()) {
            return Component.empty();
        }

        String formattedText = format;
        TagResolver.Builder resolverBuilder = TagResolver.builder();

        // 1. 載入外部傳入的標籤 (例如 <player>, <message>)
        for (TagResolver tag : extraTags) {
            resolverBuilder.resolver(tag);
        }

        // 2. 動態解析 PlaceholderAPI 佔位符，並將 Legacy 顏色轉為 Component 安全注入
        if (player != null && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            Matcher matcher = PAPI_PATTERN.matcher(formattedText);
            StringBuilder sb = new StringBuilder();
            int index = 0;

            while (matcher.find()) {
                String fullPlaceholder = matcher.group(0);

                try {
                    String papiResult = PlaceholderAPI.setPlaceholders(player, fullPlaceholder);
                    Component papiComponent = LEGACY_SERIALIZER.deserialize(papiResult != null ? papiResult : "");

                    String papiTagName = "papi_" + index++;
                    resolverBuilder.resolver(Placeholder.component(papiTagName, papiComponent));
                    matcher.appendReplacement(sb, Matcher.quoteReplacement("<" + papiTagName + ">"));
                } catch (Exception e) {
                    matcher.appendReplacement(sb, Matcher.quoteReplacement(fullPlaceholder));
                }
            }
            matcher.appendTail(sb);
            formattedText = sb.toString();
        }

        return MINI_MESSAGE.deserialize(formattedText, resolverBuilder.build());
    }

    /**
     * 解析 MiniMessage 格式字串，並強制移除 Minecraft 物品預設的斜體 (italic) 效果
     */
    public static Component parseNoItalic(String format, TagResolver... extraTags) {
        if (format == null || format.isEmpty()) {
            return Component.empty().decoration(TextDecoration.ITALIC, false);
        }
        String text = format.startsWith("<!italic>") ? format : "<!italic>" + format;
        return parse(null, text, extraTags).decoration(TextDecoration.ITALIC, false);
    }

    public static Component parseNoItalic(Player player, String format, TagResolver... extraTags) {
        if (format == null || format.isEmpty()) {
            return Component.empty().decoration(TextDecoration.ITALIC, false);
        }
        String text = format.startsWith("<!italic>") ? format : "<!italic>" + format;
        return parse(player, text, extraTags).decoration(TextDecoration.ITALIC, false);
    }

    /**
     * 將輸入的純文字轉成支援 Legacy 顏色碼 (&a, &c, &#RRGGBB) 的 Component
     *
     * @param text 包含 Legacy 顏色碼的字串
     * @return Adventure Component
     */
    public static Component parseLegacy(String text) {
        if (text == null) return Component.empty();
        return LEGACY_SERIALIZER.deserialize(text);
    }

    /**
     * 發送帶有插件 Prefix 的系統訊息給玩家或控制台
     *
     * @param sender                接收訊息的對象 (Player 或 Console)
     * @param messagePathOrContent 語言檔內的訊息路徑或純文字
     */
    public static void sendMessage(CommandSender sender, String messagePathOrContent) {
        String prefix = Main.getInstance().getLanguageConfig().getString("prefix", "");
        Player player = (sender instanceof Player p) ? p : null;

        Component component = parse(player, prefix + messagePathOrContent);
        sender.sendMessage(component);
    }
}
