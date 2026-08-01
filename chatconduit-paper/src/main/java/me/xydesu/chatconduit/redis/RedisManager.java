package me.xydesu.chatconduit.redis;

import me.xydesu.chatconduit.Main;
import org.bukkit.configuration.file.FileConfiguration;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.util.logging.Level;

/**
 * Redis 管理器，負責 JedisPool 管理、PubSub 訊息發遞與監聽線程
 *
 * @author xydesu
 */
public class RedisManager {

    private static JedisPool jedisPool;
    private static JedisPubSub pubSubListener;
    private static Thread pubSubThread;

    private static boolean enabled = false;
    private static String serverId = "";
    private static String redisChannel = "chatconduit:global_chat";

    /**
     * 初始化 Redis 連線與 Pub/Sub 監聽器
     */
    public static void init() {
        FileConfiguration config = Main.getInstance().getConfig();

        enabled = config.getBoolean("redis.enabled", false);
        serverId = config.getString("server-id", "survival-1");

        if (!enabled) {
            Main.getInstance().getLogger().info("Redis 跨服同步功能未啟用。");
            return;
        }

        String host = config.getString("redis.host", "localhost");
        int port = config.getInt("redis.port", 6379);
        String password = config.getString("redis.password", "");
        boolean ssl = config.getBoolean("redis.ssl", false);
        redisChannel = config.getString("redis.channel", "chatconduit:global_chat");
        int maxConnections = config.getInt("redis.max-connections", 8);
        int timeout = config.getInt("redis.timeout", 2000);

        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(maxConnections);
        poolConfig.setMaxIdle(maxConnections);
        poolConfig.setMinIdle(1);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);

        try {
            if (password == null || password.trim().isEmpty()) {
                jedisPool = new JedisPool(poolConfig, host, port, timeout, ssl);
            } else {
                jedisPool = new JedisPool(poolConfig, host, port, timeout, password, ssl);
            }

            // 測試連線
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.ping();
            }

            Main.getInstance().getLogger().info("Redis 連線池成功初始化！伺服器識別名稱: [" + serverId + "]");

            // 啟動 Pub/Sub 監聽線程與快取刷新
            startSubscriberThread();
            RedisPlayerRegistry.refreshCacheAsync();
        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "無法連接至 Redis 伺服器，跨服聊天功能將暫時停用:", e);
            enabled = false;
            closePool();
        }
    }

    /**
     * 啟動 Redis 訂閱非同步執行緒
     */
    private static void startSubscriberThread() {
        pubSubThread = new Thread(() -> {
            while (enabled && !Thread.currentThread().isInterrupted()) {
                pubSubListener = new RedisMessageListener();
                try (Jedis jedis = jedisPool.getResource()) {
                    Main.getInstance().getLogger().info("Redis 訂閱線程已成功啟動，正在監聽頻道: " + redisChannel);
                    jedis.subscribe(pubSubListener, redisChannel);
                } catch (Exception e) {
                    if (enabled) {
                        Main.getInstance().getLogger().warning("Redis 訂閱中斷，將於 5 秒後重新連線... (原因: " + e.getMessage() + ")");
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException interruptedException) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }, "ChatConduit-RedisSubscriberThread");

        pubSubThread.setDaemon(true);
        pubSubThread.start();
    }

    private static void executePublish(Runnable task) {
        if (org.bukkit.Bukkit.isPrimaryThread()) {
            Main.getInstance().getServer().getScheduler().runTaskAsynchronously(Main.getInstance(), task);
        } else {
            task.run();
        }
    }

    /**
     * 發送聊天訊息封包至 Redis
     *
     * @param packet 訊息封包
     */
    public static void publishChatMessage(ChatMessagePacket packet) {
        if (!enabled || jedisPool == null || jedisPool.isClosed()) {
            return;
        }

        executePublish(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(redisChannel, packet.toJson());
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.WARNING, "發送 Redis 廣播訊息失敗:", e);
            }
        });
    }

    /**
     * 發送頻道邀請/同步封包至 Redis
     *
     * @param packet 邀請封包
     */
    public static void publishInvitePacket(ChannelInvitePacket packet) {
        if (!enabled || jedisPool == null || jedisPool.isClosed()) {
            return;
        }

        executePublish(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(redisChannel, packet.toJson());
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.WARNING, "發送 Redis 頻道邀請廣播失敗:", e);
            }
        });
    }

    /**
     * 發送禁言/解禁狀態封包至 Redis
     *
     * @param packet 禁言封包
     */
    public static void publishMutePacket(MutePacket packet) {
        if (!enabled || jedisPool == null || jedisPool.isClosed()) {
            return;
        }

        executePublish(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(redisChannel, packet.toJson());
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.WARNING, "發送 Redis 禁言狀態廣播失敗:", e);
            }
        });
    }

    /**
     * 發送玩家頻道同步與變動封包至 Redis
     *
     * @param packet 同步封包
     */
    public static void publishPlayerChannelSyncPacket(PlayerChannelSyncPacket packet) {
        if (!enabled || jedisPool == null || jedisPool.isClosed()) {
            return;
        }

        executePublish(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(redisChannel, packet.toJson());
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.WARNING, "發送 Redis 玩家頻道同步廣播失敗:", e);
            }
        });
    }

    /**
     * 發送私訊封包至 Redis
     *
     * @param packet 私訊封包
     */
    public static void publishPrivateMessage(PrivateMessagePacket packet) {
        if (!enabled || jedisPool == null || jedisPool.isClosed()) {
            return;
        }

        executePublish(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(redisChannel, packet.toJson());
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.WARNING, "發送 Redis 私訊廣播失敗:", e);
            }
        });
    }

    /**
     * 發送好友線上狀態變更封包至 Redis
     *
     * @param packet 狀態變更封包
     */
    public static void publishFriendStatus(FriendStatusPacket packet) {
        if (!enabled || jedisPool == null || jedisPool.isClosed()) {
            return;
        }

        executePublish(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(redisChannel, packet.toJson());
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.WARNING, "發送 Redis 好友狀態變更廣播失敗:", e);
            }
        });
    }

    /**
     * 發送好友申請與社交動作通知封包至 Redis
     *
     * @param packet 好友申請通知封包
     */
    public static void publishFriendRequestNotify(FriendRequestNotifyPacket packet) {
        if (!enabled || jedisPool == null || jedisPool.isClosed()) {
            return;
        }

        executePublish(() -> {
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.publish(redisChannel, packet.toJson());
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.WARNING, "發送 Redis 好友申請通知廣播失敗:", e);
            }
        });
    }

    /**
     * 獲取 Jedis 連線資源，供組件內部進行同步或非同步 Redis 操作
     * 調用者有責任調用 jedis.close() 來歸還連線池資源
     */
    public static Jedis getJedis() {
        if (!enabled || jedisPool == null || jedisPool.isClosed()) {
            return null;
        }
        try {
            return jedisPool.getResource();
        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.FINE, "獲取 Jedis 資源失敗:", e);
            return null;
        }
    }


    /**
     * 關閉 Redis 資源與連線池
     */
    public static void close() {
        enabled = false;
        RedisPlayerRegistry.clearCache();

        if (pubSubListener != null && pubSubListener.isSubscribed()) {
            try {
                pubSubListener.unsubscribe();
            } catch (Exception ignored) {
            }
        }

        if (pubSubThread != null && pubSubThread.isAlive()) {
            pubSubThread.interrupt();
            try {
                pubSubThread.join(1000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        closePool();
    }

    private static void closePool() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            try {
                jedisPool.close();
                Main.getInstance().getLogger().info("Redis 連線池已成功關閉。");
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.WARNING, "關閉 Redis 連線池時發生異常:", e);
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static String getServerId() {
        return serverId;
    }
}
