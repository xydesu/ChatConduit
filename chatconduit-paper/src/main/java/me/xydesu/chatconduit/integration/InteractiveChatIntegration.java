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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.regex.Matcher;
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

    private static final Pattern GENERIC_TAG_PATTERN = Pattern.compile("(?i)\\[[^\\]]+\\]|§f\\[[^\\]]+\\]§r|<(?:chat|ic|interactivechat)=[^:]+:?(\\[[^\\]]+\\]|[^\\]:]+)?:?>");

    private static class ICPlaceholderReflect {
        Object rawObj;
        Pattern keywordPattern;
        String replaceText;
        List<String> hoverLines;
        String clickActionStr;
        String clickValueStr;

        @SuppressWarnings("unchecked")
        public static ICPlaceholderReflect fromObject(Object obj) {
            if (obj == null) return null;
            ICPlaceholderReflect ref = new ICPlaceholderReflect();
            ref.rawObj = obj;
            try {
                Class<?> c = obj.getClass();
                for (Method m : c.getMethods()) {
                    if (m.getParameterCount() != 0) continue;
                    String mName = m.getName().toLowerCase();
                    m.setAccessible(true);
                    Object val = m.invoke(obj);
                    if (val == null) continue;

                    if (mName.equals("getkeyword") || mName.equals("keyword") || mName.equals("getpattern")) {
                        if (val instanceof Pattern p) ref.keywordPattern = p;
                        else if (val instanceof String s) {
                            try { ref.keywordPattern = Pattern.compile(s); } catch (Throwable ignored) {}
                        }
                    } else if (mName.equals("getreplacetext") || mName.equals("replacetext") || mName.equals("getreplace")) {
                        ref.replaceText = extractStringFromObject(val);
                    } else if (mName.contains("hover")) {
                        extractHover(val, ref);
                    } else if (mName.contains("click")) {
                        extractClick(val, ref);
                    }
                }
            } catch (Throwable t) {
                Main.getInstance().getLogger().log(Level.FINE, "[InteractiveChat-Debug] 讀取 ICPlaceholder 物件失敗:", t);
            }

            return (ref.keywordPattern != null || (ref.replaceText != null && !ref.replaceText.isEmpty())) ? ref : null;
        }

        private static String extractStringFromObject(Object obj) {
            if (obj == null) return null;
            if (obj instanceof String s) return s;

            try {
                Class<?> c = obj.getClass();
                for (Method m : c.getMethods()) {
                    if (m.getParameterCount() == 0 && m.getReturnType() == String.class) {
                        String name = m.getName().toLowerCase();
                        if (name.contains("text") || name.contains("string") || name.contains("value") || name.contains("raw") || name.contains("get")) {
                            m.setAccessible(true);
                            String res = (String) m.invoke(obj);
                            if (res != null && !res.isEmpty() && !res.contains("@")) {
                                return res;
                            }
                        }
                    }
                }

                for (java.lang.reflect.Field f : c.getDeclaredFields()) {
                    if (f.getType() == String.class) {
                        f.setAccessible(true);
                        String res = (String) f.get(obj);
                        if (res != null && !res.isEmpty() && !res.contains("@")) {
                            return res;
                        }
                    }
                }
            } catch (Throwable ignored) {}

            String str = obj.toString();
            if (str.contains("@")) {
                return null;
            }
            return str;
        }

        private static void extractHover(Object hoverObj, ICPlaceholderReflect ref) {
            if (hoverObj instanceof List<?> list) {
                ref.hoverLines = new ArrayList<>();
                for (Object item : list) {
                    String extracted = extractStringFromObject(item);
                    if (extracted != null) ref.hoverLines.add(extracted);
                }
            } else if (hoverObj instanceof String s) {
                ref.hoverLines = List.of(s);
            } else {
                try {
                    Method getEnableM = hoverObj.getClass().getMethod("getEnable");
                    if (getEnableM != null && Boolean.FALSE.equals(getEnableM.invoke(hoverObj))) return;
                } catch (Throwable ignored) {}
                try {
                    Method getTextM = hoverObj.getClass().getMethod("getText");
                    Object tVal = getTextM.invoke(hoverObj);
                    if (tVal instanceof List<?> list) {
                        ref.hoverLines = new ArrayList<>();
                        for (Object item : list) {
                            String extracted = extractStringFromObject(item);
                            if (extracted != null) ref.hoverLines.add(extracted);
                        }
                    } else if (tVal != null) {
                        String extracted = extractStringFromObject(tVal);
                        if (extracted != null) ref.hoverLines = List.of(extracted);
                    }
                } catch (Throwable ignored) {}
            }
        }

        private static void extractClick(Object clickObj, ICPlaceholderReflect ref) {
            try {
                Method getEnableM = clickObj.getClass().getMethod("getEnable");
                if (getEnableM != null && Boolean.FALSE.equals(getEnableM.invoke(clickObj))) return;
            } catch (Throwable ignored) {}
            try {
                Method getActionM = clickObj.getClass().getMethod("getAction");
                Object aVal = getActionM.invoke(clickObj);
                if (aVal != null) ref.clickActionStr = String.valueOf(aVal);

                Method getValueM = clickObj.getClass().getMethod("getValue");
                Object vVal = getValueM.invoke(clickObj);
                if (vVal != null) ref.clickValueStr = String.valueOf(vVal);
            } catch (Throwable ignored) {}
        }

        public Component buildComponent(Player player, Matcher matcher) {
            String rep = replaceText;
            if (rep == null || rep.isEmpty()) {
                rep = matcher.group(0);
            }
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    rep = rep.replace("$" + i, matcher.group(i));
                }
            }

            Component comp = ChatUtils.parse(player, rep);

            if (hoverLines != null && !hoverLines.isEmpty()) {
                List<Component> hoverComps = new ArrayList<>();
                for (String line : hoverLines) {
                    for (int i = 1; i <= matcher.groupCount(); i++) {
                        if (matcher.group(i) != null) {
                            line = line.replace("$" + i, matcher.group(i));
                        }
                    }
                    hoverComps.add(ChatUtils.parse(player, line));
                }
                Component hoverContent = Component.empty();
                for (int i = 0; i < hoverComps.size(); i++) {
                    if (i > 0) hoverContent = hoverContent.append(Component.newline());
                    hoverContent = hoverContent.append(hoverComps.get(i));
                }
                comp = comp.hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(hoverContent));
            }

            if (clickActionStr != null && clickValueStr != null && !clickValueStr.isEmpty()) {
                String val = clickValueStr;
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    if (matcher.group(i) != null) {
                        val = val.replace("$" + i, matcher.group(i));
                    }
                }
                val = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, val);

                String actUpper = clickActionStr.toUpperCase();
                if (actUpper.contains("SUGGEST")) {
                    comp = comp.clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand(val));
                } else if (actUpper.contains("RUN")) {
                    comp = comp.clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand(val));
                } else if (actUpper.contains("URL")) {
                    comp = comp.clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl(val));
                } else if (actUpper.contains("CLIPBOARD") || actUpper.contains("COPY")) {
                    comp = comp.clickEvent(net.kyori.adventure.text.event.ClickEvent.copyToClipboard(val));
                }
            }

            return comp;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<ICPlaceholderReflect> fetchICPlaceholders() {
        List<ICPlaceholderReflect> result = new ArrayList<>();
        try {
            Class<?> apiClass = Class.forName("com.loohp.interactivechat.api.InteractiveChatAPI");
            List<Object> rawList = null;
            try {
                Method m = apiClass.getMethod("getICPlaceholderList");
                rawList = (List<Object>) m.invoke(null);
            } catch (Throwable t) {
                try {
                    Method m2 = apiClass.getMethod("getPlaceholderList");
                    rawList = (List<Object>) m2.invoke(null);
                } catch (Throwable ignored) {}
            }

            if (rawList != null) {
                for (Object o : rawList) {
                    ICPlaceholderReflect ref = ICPlaceholderReflect.fromObject(o);
                    if (ref != null && ref.keywordPattern != null) {
                        result.add(ref);
                    }
                }
            }
        } catch (Throwable t) {
            Main.getInstance().getLogger().log(Level.FINE, "[InteractiveChat-Debug] 獲取 ICPlaceholder 列表失敗:", t);
        }
        return result;
    }

    private static Component processItemPlaceholders(Player player, String message) {
        try {
            ItemStack mainHand = player.getInventory().getItemInMainHand();

            Component mainItemComp = null;
            if (mainHand != null && mainHand.getType() != Material.AIR && createItemDisplayComponentM != null) {
                try {
                    Object rawRes = createItemDisplayComponentM.invoke(null, player, mainHand);
                    mainItemComp = convertRelocatedAdventureComponent(rawRes);
                } catch (Throwable t) {
                    Main.getInstance().getLogger().log(Level.FINE, "[InteractiveChat-Debug] 呼叫 createItemDisplayComponent 失敗:", t);
                }
            }

            // 1. 動態嘗試從 InteractiveChat API 獲取服主在 CustomPlaceholders.yml 中設定的所有原生 ICPlaceholder 規則
            List<ICPlaceholderReflect> activeICPlaceholders = fetchICPlaceholders();

            Matcher matcher = GENERIC_TAG_PATTERN.matcher(message);
            Component builder = Component.empty();
            int lastEnd = 0;
            boolean foundAny = false;

            while (matcher.find()) {
                foundAny = true;
                String lead = message.substring(lastEnd, matcher.start());
                if (!lead.isEmpty()) {
                    String cleanedPart = ChatUtils.cleanInteractiveChatPlaceholders(lead);
                    builder = builder.append(ChatUtils.parse(player, cleanedPart));
                }

                String matchedText = matcher.group(0);
                String tagContent = matchedText.replaceAll("[\\[\\]]", "").trim().toLowerCase();

                if (tagContent.equals("item") || tagContent.equals("i") || tagContent.equals("hand") || tagContent.contains("item")) {
                    if (mainItemComp != null) {
                        builder = builder.append(mainItemComp);
                    } else {
                        builder = builder.append(Component.text("[\u200Bitem]"));
                    }
                } else {
                    // 優先比對 InteractiveChat 原生 CustomPlaceholders.yml 載入的 Pattern
                    ICPlaceholderReflect matchedIC = null;
                    Matcher icMatcher = null;
                    for (ICPlaceholderReflect ic : activeICPlaceholders) {
                        if (ic.keywordPattern != null) {
                            Matcher m = ic.keywordPattern.matcher(matchedText);
                            if (m.find()) {
                                matchedIC = ic;
                                icMatcher = m;
                                break;
                            }
                        }
                    }

                    if (matchedIC != null && icMatcher != null) {
                        // 100% 原汁原味還原 InteractiveChat 服主在 CustomPlaceholders.yml 設定的 Replace, Hover 與 Click！
                        builder = builder.append(matchedIC.buildComponent(player, icMatcher));
                    } else {
                        // 次要嘗試從 config.yml 或預設清單中讀取
                        String customFormat = Main.getInstance().getConfig().getString("interactivechat.placeholders." + tagContent);
                        Component customComp = null;

                        if (customFormat != null && !customFormat.isEmpty()) {
                            customComp = ChatUtils.parse(player, customFormat);
                        } else {
                            switch (tagContent) {
                                case "ping" -> customComp = ChatUtils.parse(player, "&f%player_colored_ping% &bms");
                                case "inv", "inventory" -> customComp = ChatUtils.parse(player, "&b[&f%player_name%'s Inventory&b]");
                                case "ender", "ec" -> customComp = ChatUtils.parse(player, "&d[&f%player_name%'s Ender Chest&d]");
                                case "money", "m", "balance" -> {
                                    Component base = ChatUtils.parse(player, "&e[&f%player_name%'s Balance&e]");
                                    base = base.hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(ChatUtils.parse(player, "&6%player_name%'s Balance: $%vault_eco_balance_commas%")))
                                               .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand("/pay " + player.getName() + " "));
                                    customComp = base;
                                }
                                case "loohpjames" -> {
                                    Component base = ChatUtils.parse(player, "&3&lLoohp&6&lJames");
                                    base = base.hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(ChatUtils.parse(player, "&eVisit the author's website!\n&bClick me!")))
                                               .clickEvent(net.kyori.adventure.text.event.ClickEvent.openUrl("https://loohpjames.com"));
                                    customComp = base;
                                }
                                case "gametime" -> customComp = ChatUtils.parse(player, "%player_world_time_24%");
                                case "time" -> customComp = ChatUtils.parse(player, "%server_time_dd/MM/yyyy HH:mm:ss zzz%");
                                case "pos" -> customComp = ChatUtils.parse(player, "&bWorld: &f%player_world% &eX:&f%player_x% &eY:&f%player_y% &eZ:&f%player_z%");
                                default -> customComp = ChatUtils.parse(player, ChatUtils.cleanInteractiveChatPlaceholders(matchedText));
                            }
                        }

                        if (customComp != null) {
                            builder = builder.append(customComp);
                        } else {
                            builder = builder.append(ChatUtils.parse(player, ChatUtils.cleanInteractiveChatPlaceholders(matchedText)));
                        }
                    }
                }

                lastEnd = matcher.end();
            }

            if (foundAny) {
                String tail = message.substring(lastEnd);
                if (!tail.isEmpty()) {
                    String cleanedPart = ChatUtils.cleanInteractiveChatPlaceholders(tail);
                    builder = builder.append(ChatUtils.parse(player, cleanedPart));
                }
                return builder;
            }

            String cleanedAll = ChatUtils.cleanInteractiveChatPlaceholders(message);
            return ChatUtils.parse(player, cleanedAll);
        } catch (Throwable t) {
            Main.getInstance().getLogger().log(Level.WARNING, "[InteractiveChat-Debug] 解析標籤 Component 時例外:", t);
            return ChatUtils.parse(player, ChatUtils.cleanInteractiveChatPlaceholders(message));
        }
    }

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
                if (m.getReturnType() == void.class || m.getReturnType() == Void.TYPE) {
                    continue; // 排除 void 回傳型態之發送/動作方法
                }
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

            // 1. 優先嘗試 InteractiveChat API 處理 [item] / [ping] / [inv] / [ender] / [money] 等全套標籤
            if (GENERIC_TAG_PATTERN.matcher(message).find()) {
                Main.getInstance().getLogger().info("[InteractiveChat-Debug] 偵測到動態標籤，準備進行全套佔位符格式化與 Component 生成...");
                Component resultComp = processItemPlaceholders(player, message);
                if (resultComp != null) {
                    String json = net.kyori.adventure.text.serializer.gson.GsonComponentSerializer.gson().serialize(resultComp);
                    Main.getInstance().getLogger().info("[InteractiveChat-Debug] 成功建構包含動態標籤之 Component! GsonJSON=" + json);
                    return resultComp;
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
                    gsonM.setAccessible(true);
                    Object gsonInst = gsonM.invoke(null);
                    Method serializeM = null;
                    for (Method m : gsonInst.getClass().getMethods()) {
                        if (m.getName().equals("serialize") && m.getParameterCount() == 1) {
                            serializeM = m;
                            break;
                        }
                    }
                    if (serializeM != null) {
                        serializeM.setAccessible(true);
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
