package me.xydesu.chatconduit.integration;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.logging.Level;
import java.util.regex.Pattern;

/**
 * InteractiveChat 插件軟性相容 (SoftDepend Wrapper) 整合類別
 * 使用全方位動態反射機制呼叫 InteractiveChatAPI (包含 createItemDisplayComponent 等核心 API)
 *
 * @author xydesu
 */
public class InteractiveChatIntegration {

    private static Boolean available = null;
    private static Method createItemDisplayComponentM = null;
    private static Method componentMethod = null;
    private static Method stringMethod = null;
    private static Method bungeeMethod = null;
    private static Object apiInstance = null;

    private static final Pattern ITEM_PATTERN = Pattern.compile("(?i)\\[(item|i|hand)\\]");
    private static final Pattern OFFHAND_PATTERN = Pattern.compile("(?i)\\[(offhand|off)\\]");

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
        createItemDisplayComponentM = null;
        componentMethod = null;
        stringMethod = null;
        bungeeMethod = null;
        apiInstance = null;
    }

    private static void findApiMethods() {
        try {
            Class<?> apiClass = Class.forName("com.loohp.interactivechat.api.InteractiveChatAPI");
            Main.getInstance().getLogger().info("[InteractiveChat-Debug] 正在全面掃描 " + apiClass.getName() + " 的方法清單...");

            // 檢查是否為 Instance 單例 API (例如 InteractiveChatAPI.getInstance())
            try {
                Method getInstanceM = apiClass.getMethod("getInstance");
                if (Modifier.isStatic(getInstanceM.getModifiers())) {
                    apiInstance = getInstanceM.invoke(null);
                    Main.getInstance().getLogger().info("[InteractiveChat-Debug] 成功獲取 API 單例物件: " + apiInstance);
                }
            } catch (Throwable ignored) {}

            // 嘗試獲取 createItemDisplayComponent(Player, ItemStack)
            try {
                createItemDisplayComponentM = apiClass.getMethod("createItemDisplayComponent", Player.class, ItemStack.class);
                createItemDisplayComponentM.setAccessible(true);
                Main.getInstance().getLogger().info("[InteractiveChat-Debug] >> 已成功綁定 createItemDisplayComponent(Player, ItemStack) 方法！");
            } catch (Throwable ignored) {}

            Method[] methods = apiClass.getMethods();
            for (Method m : methods) {
                StringBuilder paramsStr = new StringBuilder();
                for (Class<?> p : m.getParameterTypes()) {
                    if (paramsStr.length() > 0) paramsStr.append(", ");
                    paramsStr.append(p.getSimpleName());
                }
                Main.getInstance().getLogger().info("[InteractiveChat-Debug] 發現方法: " + m.getName() + "(" + paramsStr + ") -> " + m.getReturnType().getSimpleName() + " (Static: " + Modifier.isStatic(m.getModifiers()) + ")");

                Class<?>[] pTypes = m.getParameterTypes();
                if (pTypes.length == 2) {
                    int senderIdx = -1;
                    if (CommandSender.class.isAssignableFrom(pTypes[0])) senderIdx = 0;
                    else if (CommandSender.class.isAssignableFrom(pTypes[1])) senderIdx = 1;

                    if (senderIdx != -1) {
                        m.setAccessible(true);
                        int targetIdx = senderIdx == 0 ? 1 : 0;
                        Class<?> targetType = pTypes[targetIdx];

                        if (Component.class.isAssignableFrom(targetType) && componentMethod == null) {
                            componentMethod = m;
                            Main.getInstance().getLogger().info("[InteractiveChat-Debug] >> 綁定 Component API 方法: " + m.getName());
                        } else if (targetType == String.class && stringMethod == null) {
                            stringMethod = m;
                            Main.getInstance().getLogger().info("[InteractiveChat-Debug] >> 綁定 String API 方法: " + m.getName());
                        } else if (targetType.getName().contains("BaseComponent") && bungeeMethod == null) {
                            bungeeMethod = m;
                            Main.getInstance().getLogger().info("[InteractiveChat-Debug] >> 綁定 Bungee API 方法: " + m.getName());
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
            if (createItemDisplayComponentM == null && componentMethod == null && stringMethod == null && bungeeMethod == null) {
                findApiMethods();
            }

            // 1. 優先嘗試 InteractiveChatAPI.createItemDisplayComponent 處理 [item] / [offhand] 標籤
            if (createItemDisplayComponentM != null) {
                boolean hasItemTag = ITEM_PATTERN.matcher(message).find();
                boolean hasOffhandTag = OFFHAND_PATTERN.matcher(message).find();

                if (hasItemTag || hasOffhandTag) {
                    Main.getInstance().getLogger().info("[InteractiveChat-Debug] 偵測到物品標籤，準備呼叫 InteractiveChatAPI.createItemDisplayComponent 生成展示 Component...");
                    Component resultComp = processItemPlaceholders(player, message);
                    if (resultComp != null) {
                        String json = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(resultComp);
                        Main.getInstance().getLogger().info("[InteractiveChat-Debug] 成功建構包含 [item] 展示之 Component! GsonJSON=" + json);
                        return resultComp;
                    }
                }
            }

            // 2. 備用嘗試 Component 方法
            if (componentMethod != null) {
                Main.getInstance().getLogger().info("[InteractiveChat-Debug] 正在呼叫 Component API 方法: " + componentMethod.getName());
                Component inputComp = ChatUtils.parseLegacy(message);
                Object target = Modifier.isStatic(componentMethod.getModifiers()) ? null : apiInstance;
                Object[] args = componentMethod.getParameterTypes()[0].isAssignableFrom(Player.class)
                        ? new Object[]{player, inputComp}
                        : new Object[]{inputComp, player};
                Object result = componentMethod.invoke(target, args);
                Component res = handleResult(result);
                if (res != null) return res;
            }

            // 3. 備用嘗試 String 方法
            if (stringMethod != null) {
                Main.getInstance().getLogger().info("[InteractiveChat-Debug] 正在呼叫 String API 方法: " + stringMethod.getName());
                Object target = Modifier.isStatic(stringMethod.getModifiers()) ? null : apiInstance;
                Object[] args = stringMethod.getParameterTypes()[0].isAssignableFrom(Player.class)
                        ? new Object[]{player, message}
                        : new Object[]{message, player};
                Object result = stringMethod.invoke(target, args);
                Component res = handleResult(result);
                if (res != null) return res;
            }

            // 4. 備用嘗試 Bungee 方法
            if (bungeeMethod != null) {
                Main.getInstance().getLogger().info("[InteractiveChat-Debug] 正在呼叫 Bungee API 方法: " + bungeeMethod.getName());
                Object bungeeInput = net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message);
                Object target = Modifier.isStatic(bungeeMethod.getModifiers()) ? null : apiInstance;
                Object[] args = bungeeMethod.getParameterTypes()[0].isAssignableFrom(Player.class)
                        ? new Object[]{player, bungeeInput}
                        : new Object[]{bungeeInput, player};
                Object result = bungeeMethod.invoke(target, args);
                Component res = handleResult(result);
                if (res != null) return res;
            }
        } catch (Throwable t) {
            Main.getInstance().getLogger().log(Level.WARNING, "[InteractiveChat-Debug] 呼叫 InteractiveChat API 時拋出異常:", t);
        }

        return null;
    }

    private static Component processItemPlaceholders(Player player, String message) {
        try {
            ItemStack mainHand = player.getInventory().getItemInMainHand();
            ItemStack offHand = player.getInventory().getItemInOffHand();

            Component mainItemComp = null;
            if (mainHand != null && mainHand.getType() != Material.AIR) {
                Object rawRes = createItemDisplayComponentM.invoke(null, player, mainHand);
                mainItemComp = convertRelocatedAdventureComponent(rawRes);
            }

            Component offItemComp = null;
            if (offHand != null && offHand.getType() != Material.AIR) {
                Object rawRes = createItemDisplayComponentM.invoke(null, player, offHand);
                offItemComp = convertRelocatedAdventureComponent(rawRes);
            }

            String current = message;
            Component builder = Component.empty();

            String[] parts = ITEM_PATTERN.split(current, -1);
            for (int i = 0; i < parts.length; i++) {
                if (!parts[i].isEmpty()) {
                    builder = builder.append(ChatUtils.parseLegacy(parts[i]));
                }
                if (i < parts.length - 1) {
                    if (mainItemComp != null) {
                        builder = builder.append(mainItemComp);
                    } else {
                        builder = builder.append(Component.text("[item]"));
                    }
                }
            }

            return builder;
        } catch (Throwable t) {
            Main.getInstance().getLogger().log(Level.WARNING, "[InteractiveChat-Debug] 解析 [item] 標籤 Component 時失敗:", t);
            return null;
        }
    }

    private static Component convertRelocatedAdventureComponent(Object rawObj) {
        if (rawObj == null) return null;
        if (rawObj instanceof Component paperComp) {
            return paperComp;
        }

        String className = rawObj.getClass().getName();
        // 處理被 InteractiveChat 內部 Shaded/Relocated 的 Adventure Component
        if (className.contains("adventure") || className.contains("Component")) {
            try {
                Class<?> icGsonClass = null;
                try {
                    icGsonClass = Class.forName("com.loohp.interactivechat.libs.net.kyori.adventure.text.serializer.gson.GsonComponentSerializer");
                } catch (ClassNotFoundException e) {
                    // 若找不到預設包名，動態尋找物件介面包名組合
                    for (Class<?> c : rawObj.getClass().getInterfaces()) {
                        if (c.getName().contains("Component")) {
                            String pkg = c.getPackageName();
                            icGsonClass = Class.forName(pkg + ".serializer.gson.GsonComponentSerializer");
                            break;
                        }
                    }
                }

                if (icGsonClass != null) {
                    Method gsonM = icGsonClass.getMethod("gson");
                    Object gsonInst = gsonM.invoke(null);
                    Method serializeM = null;
                    for (Method m : gsonInst.getClass().getMethods()) {
                        if (m.getName().equals("serialize") && m.getParameterCount() == 1) {
                            serializeM = m;
                            break;
                        }
                    }
                    if (serializeM != null) {
                        String json = (String) serializeM.invoke(gsonInst, rawObj);
                        Main.getInstance().getLogger().info("[InteractiveChat-Debug] 成功跨 ClassLoader 轉檔 Relocated Component JSON: " + json);
                        return net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(json);
                    }
                }
            } catch (Throwable t) {
                Main.getInstance().getLogger().log(Level.WARNING, "[InteractiveChat-Debug] 跨 ClassLoader 轉檔 Relocated Component 失敗:", t);
            }
        }

        if (rawObj instanceof net.md_5.bungee.api.chat.BaseComponent[] bungeeComponents) {
            String json = net.md_5.bungee.chat.ComponentSerializer.toString(bungeeComponents);
            return net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().deserialize(json);
        }

        if (rawObj instanceof String stringResult) {
            return ChatUtils.parseLegacy(stringResult);
        }

        return null;
    }

    private static Component handleResult(Object result) {
        if (result == null) {
            Main.getInstance().getLogger().info("[InteractiveChat-Debug] API 方法回傳 null");
            return null;
        }

        Component comp = convertRelocatedAdventureComponent(result);
        if (comp != null) {
            String plain = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(comp);
            String json = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(comp);
            Main.getInstance().getLogger().info("[InteractiveChat-Debug] API 解析與轉檔成功 PlainText=[" + plain + "] GsonJSON=[" + json + "]");
            return comp;
        }

        Main.getInstance().getLogger().info("[InteractiveChat-Debug] API 回傳未知型態: " + result.getClass().getName());
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
