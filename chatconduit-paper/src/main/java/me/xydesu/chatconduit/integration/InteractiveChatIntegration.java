package me.xydesu.chatconduit.integration;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.util.logging.Level;

/**
 * InteractiveChat 插件軟性相容 (SoftDepend Wrapper) 整合類別
 * 使用反射機制安全判定與呼叫 API，避免未安裝插件時發生 ClassNotFoundException
 *
 * @author xydesu
 */
public class InteractiveChatIntegration {

    private static Boolean available = null;
    private static Method transformMethod = null;

    /**
     * 檢查伺服器是否已安裝並啟用 InteractiveChat 插件
     */
    public static boolean isAvailable() {
        if (available == null) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("InteractiveChat");
            available = plugin != null && plugin.isEnabled();
            if (available) {
                Main.getInstance().getLogger().info("[InteractiveChat] 已偵測到 InteractiveChat 插件並成功建立整合！");
            } else {
                Main.getInstance().getLogger().info("[InteractiveChat] 未偵測到 InteractiveChat 插件 (軟性相容模式關閉)。");
            }
        }
        return available;
    }

    /**
     * 重置快取狀態 (可用於 Reload 時)
     */
    public static void resetCache() {
        available = null;
        transformMethod = null;
    }

    /**
     * 嘗試呼叫 InteractiveChat API 解析玩家文字訊息並返回完整 Component (包含 Hover/Click Event)
     * 若插件未開啟或 API 不可用，則返回 null
     *
     * @param player  發言玩家
     * @param message 原始訊息
     * @return 包含 HoverEvent 的 Adventure Component，失敗則返回 null
     */
    public static Component processMessageToComponent(Player player, String message) {
        if (!isAvailable() || message == null || message.isEmpty()) {
            return null;
        }

        try {
            Class<?> apiClass = Class.forName("com.loohp.interactivechat.api.InteractiveChatAPI");
            if (transformMethod == null) {
                for (Method m : apiClass.getDeclaredMethods()) {
                    if (m.getName().contains("transform") || m.getName().contains("parse")) {
                        m.setAccessible(true);
                        transformMethod = m;
                        break;
                    }
                }
            }

            if (transformMethod != null) {
                Main.getInstance().getLogger().info("[InteractiveChat-Debug] 正在透過 API 處理玩家 " + player.getName() + " 的訊息: " + message);
                Object result;
                if (transformMethod.getParameterCount() == 2) {
                    result = transformMethod.invoke(null, player, message);
                } else if (transformMethod.getParameterCount() == 1) {
                    result = transformMethod.invoke(null, message);
                } else {
                    result = null;
                }

                if (result instanceof Component componentResult) {
                    Main.getInstance().getLogger().info("[InteractiveChat-Debug] API 解析成功 (Adventure Component)。");
                    return componentResult;
                } else if (result instanceof net.md_5.bungee.api.chat.BaseComponent[] bungeeComponents) {
                    String json = net.md_5.bungee.chat.ComponentSerializer.toString(bungeeComponents);
                    Component adventureComp = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(json);
                    Main.getInstance().getLogger().info("[InteractiveChat-Debug] API 解析成功 (Bungee BaseComponent[] -> Adventure Component)。");
                    return adventureComp;
                } else if (result instanceof String stringResult) {
                    Main.getInstance().getLogger().info("[InteractiveChat-Debug] API 解析成功 (String): " + stringResult);
                    return ChatUtils.parseLegacy(stringResult);
                }
            }
        } catch (Throwable t) {
            Main.getInstance().getLogger().log(Level.WARNING, "[InteractiveChat-Debug] 呼叫 InteractiveChat API 時拋出異常:", t);
        }

        return null;
    }

    /**
     * 嘗試呼叫 InteractiveChat API 解析玩家文字訊息
     * 若插件未開啟或 API 不可用，則返回原始訊息
     *
     * @param player  發言玩家
     * @param message 原始訊息
     * @return 處理完畢的文字
     */
    public static String processMessage(Player player, String message) {
        Component comp = processMessageToComponent(player, message);
        if (comp != null) {
            return net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(comp);
        }
        return message;
    }

    /**
     * 安全還原或淨化訊息中的 InteractiveChat 標籤
     * 遵照跨服設定，若無法解析則將標籤還原為乾淨原樣文字 (如 [item] / [inv])
     *
     * @param text 輸入文字
     * @return 淨化後的文字
     */
    public static String cleanOrFormatPlaceholders(String text) {
        return ChatUtils.cleanInteractiveChatPlaceholders(text);
    }
}
