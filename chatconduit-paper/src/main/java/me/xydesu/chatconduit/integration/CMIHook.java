package me.xydesu.chatconduit.integration;

import me.xydesu.chatconduit.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * CMI 插件 AFK 自動掛機狀態監測 Hook
 *
 * @author xydesu
 */
public class CMIHook {

    private static final Map<UUID, Boolean> afkPlayers = new ConcurrentHashMap<>();
    private static boolean enabled = false;

    /**
     * 初始化 CMI Hook (檢測 CMI 是否存在並註冊動態事件監聽器)
     */
    public static void init() {
        if (!Bukkit.getPluginManager().isPluginEnabled("CMI")) {
            Main.getInstance().getLogger().info("[CMIHook] 未檢測到 CMI 插件，AFK 監測功能將保持停用。");
            return;
        }

        try {
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) Class.forName("com.Zrips.CMI.events.CMIPlayerAfkStatusChangeEvent");

            EventExecutor executor = (listener, event) -> {
                if (eventClass.isInstance(event)) {
                    handleAfkEvent(event);
                }
            };

            Bukkit.getPluginManager().registerEvent(
                    eventClass,
                    new Listener() {},
                    EventPriority.MONITOR,
                    executor,
                    Main.getInstance()
            );

            enabled = true;
            Main.getInstance().getLogger().info("[CMIHook] 成功載入 CMI 插件整合與 CMIPlayerAfkStatusChangeEvent 事件監聽！");
        } catch (ClassNotFoundException e) {
            Main.getInstance().getLogger().warning("[CMIHook] 找到 CMI 插件，但無法載入 CMIPlayerAfkStatusChangeEvent 類別。");
        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.WARNING, "[CMIHook] 初始化 CMI 事件監聽時發生未知錯誤:", e);
        }
    }

    private static void handleAfkEvent(Object event) {
        try {
            UUID playerUuid = null;
            Method getPlayerMethod = null;
            try {
                getPlayerMethod = event.getClass().getMethod("getPlayer");
            } catch (NoSuchMethodException e) {
                try {
                    getPlayerMethod = event.getClass().getMethod("getUser");
                } catch (NoSuchMethodException ignored) {}
            }

            if (getPlayerMethod != null) {
                Object playerObj = getPlayerMethod.invoke(event);
                if (playerObj instanceof Player p) {
                    playerUuid = p.getUniqueId();
                } else if (playerObj != null) {
                    try {
                        Method getUuidMethod = playerObj.getClass().getMethod("getUniqueId");
                        playerUuid = (UUID) getUuidMethod.invoke(playerObj);
                    } catch (Exception ignored) {}
                }
            }

            Boolean isAfk = null;
            for (String methodName : new String[]{"getAfkState", "isAfk", "isAfkState"}) {
                try {
                    Method m = event.getClass().getMethod(methodName);
                    Object val = m.invoke(event);
                    if (val instanceof Boolean b) {
                        isAfk = b;
                        break;
                    }
                } catch (NoSuchMethodException ignored) {}
            }

            if (playerUuid != null && isAfk != null) {
                afkPlayers.put(playerUuid, isAfk);
                Main.getInstance().getLogger().info("[CMIHook] 玩家 AFK 狀態更新: " + playerUuid + " -> " + isAfk);
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.WARNING, "[CMIHook] 處理 CMI AFK 事件時發生錯誤:", e);
        }
    }

    /**
     * 查詢玩家是否處於 AFK (離開/掛機) 狀態
     */
    public static boolean isAfk(Player player) {
        if (player == null) return false;
        return isAfk(player.getUniqueId());
    }

    /**
     * 根據 UUID 查詢玩家 AFK 狀態
     */
    public static boolean isAfk(UUID uuid) {
        if (uuid == null || !enabled) return false;
        return afkPlayers.getOrDefault(uuid, false);
    }

    /**
     * 離線時清理快取
     */
    public static void removePlayer(UUID uuid) {
        if (uuid != null) {
            afkPlayers.remove(uuid);
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }
}
