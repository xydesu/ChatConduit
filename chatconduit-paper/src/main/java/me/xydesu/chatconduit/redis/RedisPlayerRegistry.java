package me.xydesu.chatconduit.redis;

import com.google.gson.Gson;
import me.xydesu.chatconduit.Main;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import redis.clients.jedis.Jedis;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * Redis 全服線上玩家登記冊
 * 提供跨服玩家在線狀態記錄、伺服器查找與 Tab 補全支援
 *
 * @author xydesu
 */
public class RedisPlayerRegistry {

    private static final String REDIS_HASH_KEY = "chatconduit:online_players";
    private static final Gson GSON = new Gson();

    private static final Map<String, PlayerData> cachedPlayerDataMap = new java.util.concurrent.ConcurrentHashMap<>();

    public static class PlayerData {
        private String uuid;
        private String name;
        private String serverId;
        private long lastSeen;

        public PlayerData() {}

        public PlayerData(String uuid, String name, String serverId, long lastSeen) {
            this.uuid = uuid;
            this.name = name;
            this.serverId = serverId;
            this.lastSeen = lastSeen;
        }

        public String getUuid() { return uuid; }
        public String getName() { return name; }
        public String getServerId() { return serverId; }
        public long getLastSeen() { return lastSeen; }
    }

    /**
     * 註冊玩家上線狀態至 Redis 並同步更新本地快取
     */
    public static void registerPlayer(Player player, String serverId) {
        if (!RedisManager.isEnabled()) return;

        PlayerData data = new PlayerData(
                player.getUniqueId().toString(),
                player.getName(),
                serverId,
                System.currentTimeMillis()
        );
        cachedPlayerDataMap.put(player.getName().toLowerCase(), data);

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                Jedis jedis = RedisManager.getJedis();
                if (jedis == null) return;
                try {
                    jedis.hset(REDIS_HASH_KEY, player.getName().toLowerCase(), GSON.toJson(data));
                } finally {
                    jedis.close();
                }
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.WARNING, "註冊玩家上線狀態至 Redis 時發生例外:", e);
            }
        });
    }

    /**
     * 從 Redis 與本地快取移除玩家下線狀態
     */
    public static void unregisterPlayer(Player player) {
        if (!RedisManager.isEnabled()) return;

        cachedPlayerDataMap.remove(player.getName().toLowerCase());

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                Jedis jedis = RedisManager.getJedis();
                if (jedis == null) return;
                try {
                    String existingJson = jedis.hget(REDIS_HASH_KEY, player.getName().toLowerCase());
                    if (existingJson != null) {
                        PlayerData data = GSON.fromJson(existingJson, PlayerData.class);
                        // 僅當現存資料屬於本伺服器時才移除，防止跨服登入競態條件
                        if (data != null && RedisManager.getServerId().equalsIgnoreCase(data.getServerId())) {
                            jedis.hdel(REDIS_HASH_KEY, player.getName().toLowerCase());
                        }
                    }
                } finally {
                    jedis.close();
                }
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.WARNING, "從 Redis 移除玩家下線狀態時發生例外:", e);
            }
        });
    }

    /**
     * 獲取指定玩家的 Redis 註冊資料 (非阻塞，優先自記憶體快取讀取)
     */
    public static PlayerData getPlayerData(String playerName) {
        if (!RedisManager.isEnabled() || playerName == null) return null;

        PlayerData cached = cachedPlayerDataMap.get(playerName.toLowerCase());
        if (cached != null) {
            return cached;
        }

        // 若非主執行緒呼叫且快取未命中，可嘗試直接向 Redis 查詢並寫回快取
        if (!Bukkit.isPrimaryThread()) {
            try {
                Jedis jedis = RedisManager.getJedis();
                if (jedis == null) return null;
                try {
                    String json = jedis.hget(REDIS_HASH_KEY, playerName.toLowerCase());
                    if (json != null && !json.isEmpty()) {
                        PlayerData data = GSON.fromJson(json, PlayerData.class);
                        if (data != null) {
                            cachedPlayerDataMap.put(playerName.toLowerCase(), data);
                            return data;
                        }
                    }
                } finally {
                    jedis.close();
                }
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.FINE, "從 Redis 查詢玩家資料失敗: " + playerName, e);
            }
        }

        return null;
    }

    /**
     * 依 UUID 從本地 Redis 快取查找線上玩家資料。
     */
    public static PlayerData getPlayerDataByUuid(String playerUuid) {
        if (!RedisManager.isEnabled() || playerUuid == null) return null;

        for (PlayerData data : cachedPlayerDataMap.values()) {
            if (data != null && playerUuid.equalsIgnoreCase(data.getUuid())) {
                return data;
            }
        }

        return null;
    }

    /**
     * 獲取所有線上玩家名稱 Set (含全服線上玩家，用於 Tab 補全與私訊補全，完全讀取快取不阻塞主執行緒)
     */
    public static Set<String> getOnlinePlayerNames() {
        Set<String> playerNames = new HashSet<>();

        // 首先填入本服玩家名稱
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerNames.add(player.getName());
        }

        if (!RedisManager.isEnabled()) {
            return playerNames;
        }

        // 填入全服非同步快取中的線上玩家名稱
        for (PlayerData data : cachedPlayerDataMap.values()) {
            if (data != null && data.getName() != null) {
                playerNames.add(data.getName());
            }
        }

        return playerNames;
    }

    /**
     * 非同步從 Redis 刷新全服線上玩家記憶體快取 (由排程器或初始化調用)
     */
    public static void refreshCacheAsync() {
        if (!RedisManager.isEnabled()) {
            cachedPlayerDataMap.clear();
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                Jedis jedis = RedisManager.getJedis();
                if (jedis == null) return;
                try {
                    Map<String, String> entries = jedis.hgetAll(REDIS_HASH_KEY);
                    if (entries != null) {
                        Map<String, PlayerData> newCache = new java.util.HashMap<>();
                        for (Map.Entry<String, String> entry : entries.entrySet()) {
                            PlayerData data = GSON.fromJson(entry.getValue(), PlayerData.class);
                            if (data != null && data.getName() != null) {
                                newCache.put(entry.getKey().toLowerCase(), data);
                            }
                        }
                        cachedPlayerDataMap.clear();
                        cachedPlayerDataMap.putAll(newCache);
                    }
                } finally {
                    jedis.close();
                }
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.FINE, "非同步刷新全服線上玩家快取時發生例外:", e);
            }
        });
    }

    /**
     * 清理快取記憶體
     */
    public static void clearCache() {
        cachedPlayerDataMap.clear();
    }

    /**
     * 清理本伺服器在 Redis 的舊殘留玩家資料
     */
    public static void clearServerPlayers(String serverId) {
        if (!RedisManager.isEnabled()) return;

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                Jedis jedis = RedisManager.getJedis();
                if (jedis == null) return;
                try {
                    Map<String, String> entries = jedis.hgetAll(REDIS_HASH_KEY);
                    if (entries != null) {
                        for (Map.Entry<String, String> entry : entries.entrySet()) {
                            PlayerData data = GSON.fromJson(entry.getValue(), PlayerData.class);
                            if (data != null && serverId.equalsIgnoreCase(data.getServerId())) {
                                jedis.hdel(REDIS_HASH_KEY, entry.getKey());
                                cachedPlayerDataMap.remove(entry.getKey().toLowerCase());
                            }
                        }
                    }
                } finally {
                    jedis.close();
                }
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.WARNING, "清理本伺服器舊殘留 Redis 玩家資料時發生例外:", e);
            }
        });
    }
}
