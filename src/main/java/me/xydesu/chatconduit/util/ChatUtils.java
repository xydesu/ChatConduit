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
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUtils {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();
    private static final Pattern PAPI_PATTERN = Pattern.compile("%([^%]+)%");

    /**
     * 解析 MiniMessage 格式字串，自動處理 PlaceholderAPI 佔位符與 Legacy 顏色碼
     *
     * @param player    對應的玩家 (支援 Player 或 OfflinePlayer，若為 null 則跳過 PAPI 解析)
     * @param format    帶有 MiniMessage 標籤或 %papi% 佔位符的格式字串
     * @param extraTags 額外傳入的 MiniMessage 標籤 (例如 <player>, <message>)
     * @return 解析完成的 Adventure Component
     */
    public static Component parse(OfflinePlayer player, String format, TagResolver... extraTags) {
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
        if (player != null && formattedText.indexOf('%') != -1 && Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            Matcher matcher = PAPI_PATTERN.matcher(formattedText);
            StringBuilder sb = null;
            int index = 0;

            while (matcher.find()) {
                if (sb == null) {
                    sb = new StringBuilder(formattedText.length() + 32);
                }
                String fullPlaceholder = matcher.group(0);

                try {
                    String papiResult = PlaceholderAPI.setPlaceholders(player, fullPlaceholder);
                    if (papiResult != null && !papiResult.equals(fullPlaceholder)) {
                        Component papiComponent = LEGACY_SERIALIZER.deserialize(papiResult);
                        String papiTagName = "papi_" + index++;
                        resolverBuilder.resolver(Placeholder.component(papiTagName, papiComponent));
                        matcher.appendReplacement(sb, Matcher.quoteReplacement("<" + papiTagName + ">"));
                        continue;
                    }
                } catch (Exception ignored) {}
                matcher.appendReplacement(sb, Matcher.quoteReplacement(fullPlaceholder));
            }
            if (sb != null) {
                matcher.appendTail(sb);
                formattedText = sb.toString();
            }
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

    public static Component parseNoItalic(OfflinePlayer player, String format, TagResolver... extraTags) {
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

    /**
     * 發送帶有互動式 ClickEvent (點擊執行指令) 與 HoverEvent (懸停提示) 的頻道邀請對話訊息 (分兩行呈現)
     */
    public static void sendInviteNotification(Player inviter, Player targetPlayer, me.xydesu.chatconduit.channel.PlayerChannelManager.CustomChannel customChan) {
        String prefix = Main.getInstance().getLanguageConfig().getString("prefix", "");

        // 第一行: 邀請抬頭訊息
        String line1Text = prefix + "<yellow>" + inviter.getName() + " 邀請你加入頻道 [<green>" + customChan.getDisplayName() + "<yellow>]！";
        Component line1Component = parseNoItalic(targetPlayer, line1Text);

        // 第二行: 互動按鈕 [✔ 點擊接受] 與 [✖ 點擊拒絕] 放置在下一行
        Component acceptBtn = parseNoItalic("<green><bold>[✔ 點擊接受]</bold></green>")
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/pc accept " + customChan.getId()))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(parseNoItalic("<green>點擊立即接受並加入頻道 <yellow>" + customChan.getDisplayName())));

        Component space = Component.text("   ");

        Component denyBtn = parseNoItalic("<red><bold>[✖ 點擊拒絕]</bold></red>")
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/pc deny " + customChan.getId()))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(parseNoItalic("<red>點擊拒絕加入頻道 <yellow>" + customChan.getDisplayName())));

        Component line2Component = parseNoItalic("  ").append(acceptBtn).append(space).append(denyBtn);

        targetPlayer.sendMessage(line1Component);
        targetPlayer.sendMessage(line2Component);
    }

    /**
     * 發送來自跨服 Redis 的頻道邀請訊息 (分兩行呈現)
     */
    public static void sendRemoteInviteNotification(String inviterName, String originServerId, Player targetPlayer, String channelId, String channelDisplayName) {
        String prefix = Main.getInstance().getLanguageConfig().getString("prefix", "");

        String line1Text = prefix + "<yellow>" + inviterName + " <gray>(來自 " + originServerId + ")</gray> 邀請你加入頻道 [<green>" + channelDisplayName + "<yellow>]！";
        Component line1Component = parseNoItalic(targetPlayer, line1Text);

        Component acceptBtn = parseNoItalic("<green><bold>[✔ 點擊接受]</bold></green>")
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/pc accept " + channelId))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(parseNoItalic("<green>點擊立即接受並加入頻道 <yellow>" + channelDisplayName)));

        Component space = Component.text("   ");

        Component denyBtn = parseNoItalic("<red><bold>[✖ 點擊拒絕]</bold></red>")
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/pc deny " + channelId))
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(parseNoItalic("<red>點擊拒絕加入頻道 <yellow>" + channelDisplayName)));

        Component line2Component = parseNoItalic("  ").append(acceptBtn).append(space).append(denyBtn);

        targetPlayer.sendMessage(line1Component);
        targetPlayer.sendMessage(line2Component);
    }

    /**
     * 建立具有自訂 Base64 紋理的玩家頭顱物品
     *
     * @param base64Texture Base64 格式的頭顱紋理數據
     * @param name          物品顯示名稱
     * @param lore          物品 Lore 說明列表
     * @return 包含自訂紋理的 ItemStack
     */
    public static ItemStack createCustomHead(String base64Texture, String name, List<String> lore) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            if (base64Texture != null && !base64Texture.isEmpty()) {
                PlayerProfile profile = Bukkit.createProfile(UUID.nameUUIDFromBytes(base64Texture.getBytes()), null);
                profile.setProperty(new ProfileProperty("textures", base64Texture));
                meta.setPlayerProfile(profile);
            }
            meta.displayName(parseNoItalic(name));
            if (lore != null && !lore.isEmpty()) {
                List<Component> parsedLore = new ArrayList<>();
                for (String line : lore) {
                    parsedLore.add(parseNoItalic(line));
                }
                meta.lore(parsedLore);
            }
            head.setItemMeta(meta);
        }
        return head;
    }
}
