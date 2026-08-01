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
 * 使用動態反射機制精準判定參數型態並呼叫 API，避免未安裝插件時發生 ClassNotFoundException 或 IllegalArgumentException
 *
 * @author xydesu
 */
public class InteractiveChatIntegration {

    private static Boolean available = null;
    private static Method componentMethod = null;
    private static Method stringMethod = null;
    private static Method bungeeMethod = null;

    /**
     * 檢查伺服器是否已安裝並啟用 InteractiveChat 插件
     */
    public static boolean isAvailable() {
        if (available == null) {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("InteractiveChat");
            available = plugin != null && plugin.isEnabled();
            if (available) {
                Main.getInstance().getLogger().info("[InteractiveChat] 已偵測到 InteractiveChat 插件並成功建立整合！");
                findApiMethods();
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
        componentMethod = null;
        stringMethod = null;
        bungeeMethod = null;
    }

    private static void findApiMethods() {
        try {
            Class<?> apiClass = Class.forName("com.loohp.interactivechat.api.InteractiveChatAPI");
            Main.getInstance().getLogger().info("[InteractiveChat-Debug] 正在掃描 InteractiveChatAPI 方法列表...");
            for (Method m : apiClass.getDeclaredMethods()) {
                String name = m.getName().toLowerCase();
                if (name.contains("transform") || name.contains("parse") || name.contains("process") || name.contains("replace")) {
                    Class<?>[] pTypes = m.getParameterTypes();
                    if (pTypes.length == 2 && Player.class.isAssignableFrom(pTypes[0])) {
                        m.setAccessible(true);
                        if (Component.class.isAssignableFrom(pTypes[1]) && componentMethod == null) {
                            componentMethod = m;
                            Main.getInstance().getLogger().info("[InteractiveChat-Debug] 已匹配 Adventure Component 方法: " + m);
                        } else if (pTypes[1] == String.class && stringMethod == null) {
                            stringMethod = m;
                            Main.getInstance().getLogger().info("[InteractiveChat-Debug] 已匹配 String 方法: " + m);
                        } else if (pTypes[1].getName().contains("BaseComponent") && bungeeMethod == null) {
                            bungeeMethod = m;
                            Main.getInstance().getLogger().info("[InteractiveChat-Debug] 已匹配 Bungee BaseComponent[] 方法: " + m);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Main.getInstance().getLogger().log(Level.WARNING, "[InteractiveChat-Debug] 掃描 API 方法時例外:", t);
        }
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

        Main.getInstance().getLogger().info("[InteractiveChat-Debug] 準備處理 API 請求 - 玩家: " + player.getName() + " (UUID: " + player.getUniqueId() + "), 原始訊息: \"" + message + "\"");

        try {
            if (componentMethod == null && stringMethod == null && bungeeMethod == null) {
                findApiMethods();
            }

            // 1. 優先嘗試 (Player, Component) 方法
            if (componentMethod != null) {
                Main.getInstance().getLogger().info("[InteractiveChat-Debug] 呼叫方法: " + componentMethod.getName() + "(Player, Component)");
                Component inputComp = ChatUtils.parseLegacy(message);
                Object result = componentMethod.invoke(null, player, inputComp);
                Component res = handleResult(result);
                if (res != null) return res;
            }

            // 2. 備用嘗試 (Player, String) 方法
            if (stringMethod != null) {
                Main.getInstance().getLogger().info("[InteractiveChat-Debug] 呼叫方法: " + stringMethod.getName() + "(Player, String)");
                Object result = stringMethod.invoke(null, player, message);
                Component res = handleResult(result);
                if (res != null) return res;
            }

            // 3. 備用嘗試 (Player, BaseComponent[]) 方法
            if (bungeeMethod != null) {
                Main.getInstance().getLogger().info("[InteractiveChat-Debug] 呼叫方法: " + bungeeMethod.getName() + "(Player, BaseComponent[])");
                Object bungeeInput = net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message);
                Object result = bungeeMethod.invoke(null, player, bungeeInput);
                Component res = handleResult(result);
                if (res != null) return res;
            }
        } catch (Throwable t) {
            Main.getInstance().getLogger().log(Level.WARNING, "[InteractiveChat-Debug] 呼叫 InteractiveChat API 時拋出異常:", t);
        }

        return null;
    }

    private static Component handleResult(Object result) {
        if (result == null) {
            Main.getInstance().getLogger().info("[InteractiveChat-Debug] API 方法回傳 null");
            return null;
        }
        if (result instanceof Component componentResult) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(componentResult);
            String json = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(componentResult);
            Main.getInstance().getLogger().info("[InteractiveChat-Debug] API 解析成功 (Adventure Component) PlainText=[" + plain + "] GsonJSON=[" + json + "]");
            return componentResult;
        } else if (result instanceof net.md_5.bungee.api.chat.BaseComponent[] bungeeComponents) {
            String json = net.md_5.bungee.chat.ComponentSerializer.toString(bungeeComponents);
            Component adventureComp = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(json);
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(adventureComp);
            Main.getInstance().getLogger().info("[InteractiveChat-Debug] API 解析成功 (Bungee BaseComponent[]) PlainText=[" + plain + "] GsonJSON=[" + json + "]");
            return adventureComp;
        } else if (result instanceof String stringResult) {
            Main.getInstance().getLogger().info("[InteractiveChat-Debug] API 解析成功 (String): [" + stringResult + "]");
            return ChatUtils.parseLegacy(stringResult);
        } else {
            Main.getInstance().getLogger().info("[InteractiveChat-Debug] API 回傳未知型態: " + result.getClass().getName());
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
