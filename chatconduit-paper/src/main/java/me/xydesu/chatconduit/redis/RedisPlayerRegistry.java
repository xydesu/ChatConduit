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
     * 註冊玩家上線狀態至 Redis
     */
    public static void registerPlayer(Player player, String serverId) {
        if (!RedisManager.isEnabled()) return;

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try {
                Jedis jedis = RedisManager.getJedis();
                if (jedis == null) return;
                try {
                    PlayerData data = new PlayerData(
                            player.getUniqueId().toString(),
                            player.getName(),
                            serverId,
                            System.currentTimeMillis()
                    );
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
     * 從 Redis 移除玩家下線狀態
     */
    public static void unregisterPlayer(Player player) {
        if (!RedisManager.isEnabled()) return;

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
     * 獲取指定玩家的 Redis 註冊資料 (同步)
     */
    public static PlayerData getPlayerData(String playerName) {
        if (!RedisManager.isEnabled() || playerName == null) return null;

        try {
            Jedis jedis = RedisManager.getJedis();
            if (jedis == null) return null;
            try {
                String json = jedis.hget(REDIS_HASH_KEY, playerName.toLowerCase());
                if (json != null && !json.isEmpty()) {
                    return GSON.fromJson(json, PlayerData.class);
                }
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.FINE, "從 Redis 查詢玩家資料失敗: " + playerName, e);
        }
        return null;
    }

    /**
     * 獲取所有線上玩家名稱 Set (含全服線上玩家，用於 Tab 補全)
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

        try {
            Jedis jedis = RedisManager.getJedis();
            if (jedis == null) return playerNames;
            try {
                Map<String, String> entries = jedis.hgetAll(REDIS_HASH_KEY);
                if (entries != null) {
                    for (String json : entries.values()) {
                        PlayerData data = GSON.fromJson(json, PlayerData.class);
                        if (data != null && data.getName() != null) {
                            playerNames.add(data.getName());
                        }
                    }
                }
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.FINE, "從 Redis 獲取全服玩家名稱清單時發生例外:", e);
        }

        return playerNames;
    }

    /**
     * 清理本伺服器在 Redis 的舊殘留玩家資料
     */
    public static void clearServerPlayers(String serverId) {
        if (!RedisManager.isEnabled()) return;

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
                        }
                    }
                }
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.WARNING, "清理本伺服器舊殘留 Redis 玩家資料時發生例外:", e);
        }
    }
}
