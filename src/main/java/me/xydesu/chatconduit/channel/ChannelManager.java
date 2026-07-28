package me.xydesu.chatconduit.channel;

import me.xydesu.chatconduit.Main;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChannelManager {

    private static final Map<String, Channel> systemChannels = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerSelectedChannel = new ConcurrentHashMap<>();
    private static volatile List<Channel> prefixChannelsCache = List.of();
    private static String defaultChannelKey = "global";

    private static File dataFile;
    private static FileConfiguration dataConfig;
    private static final Object FILE_LOCK = new Object();

    public record Channel(String key, String name, String color, String prefixKey, String permission, String description, String rules) {}

    /**
     * 從 config.yml 載入系統頻道設定並更新快取
     */
    public static void loadChannels() {
        systemChannels.clear();
        ConfigurationSection section = Main.getInstance().getConfig().getConfigurationSection("channels");
        defaultChannelKey = Main.getInstance().getConfig().getString("default-channel", "global");

        if (section != null) {
            for (String key : section.getKeys(false)) {
                String name = section.getString(key + ".name", key);
                String color = section.getString(key + ".color", "<white>");
                String prefixKey = section.getString(key + ".prefix-key", "");
                String permission = section.getString(key + ".permission", "");
                String description = section.getString(key + ".description", "官方系統頻道");
                String rules = section.getString(key + ".rules", "遵守伺服器通用社群規範");

                systemChannels.put(key.toLowerCase(), new Channel(key, name, color, prefixKey, permission, description, rules));
            }
        }

        // 預先過濾並排序 Prefix 頻道快取清單
        prefixChannelsCache = systemChannels.values().stream()
                .filter(c -> c.prefixKey() != null && !c.prefixKey().isEmpty())
                .sorted(Comparator.comparingInt((Channel c) -> c.prefixKey().length()).reversed())
                .toList();
    }

    /**
     * 載入玩家頻道選擇資料 (player-data.yml)
     */
    public static void loadPlayerData() {
        playerSelectedChannel.clear();
        dataFile = new File(Main.getInstance().getDataFolder(), "player-data.yml");
        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                Main.getInstance().getLogger().warning("無法建立 player-data.yml: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("players")) {
            ConfigurationSection playersSection = dataConfig.getConfigurationSection("players");
            if (playersSection != null) {
                for (String uuidStr : playersSection.getKeys(false)) {
                    try {
                        UUID uuid = UUID.fromString(uuidStr);
                        String channelKey = dataConfig.getString("players." + uuidStr);
                        if (channelKey != null) {
                            playerSelectedChannel.put(uuid, channelKey.toLowerCase());
                        }
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }
    }

    /**
     * 儲存單一玩家資料（非同步進行寫入）
     */
    public static void savePlayerData(UUID uuid) {
        if (dataConfig == null) return;
        String key = playerSelectedChannel.getOrDefault(uuid, defaultChannelKey);

        Runnable saveTask = () -> {
            synchronized (FILE_LOCK) {
                dataConfig.set("players." + uuid.toString(), key);
                try {
                    dataConfig.save(dataFile);
                } catch (IOException e) {
                    Main.getInstance().getLogger().severe("無法儲存玩家數據 " + uuid + ": " + e.getMessage());
                }
            }
        };

        if (Main.getInstance().isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), saveTask);
        } else {
            saveTask.run();
        }
    }

    /**
     * 儲存所有線上與紀錄中的玩家資料 (關服或 Reload 時調用，可同步執行)
     */
    public static void saveAllPlayerData() {
        if (dataConfig == null) return;
        synchronized (FILE_LOCK) {
            for (Map.Entry<UUID, String> entry : playerSelectedChannel.entrySet()) {
                dataConfig.set("players." + entry.getKey().toString(), entry.getValue());
            }
            try {
                dataConfig.save(dataFile);
            } catch (IOException e) {
                Main.getInstance().getLogger().severe("無法儲存所有玩家數據: " + e.getMessage());
            }
        }
    }

    public static Map<String, Channel> getChannels() {
        return Collections.unmodifiableMap(systemChannels);
    }

    public static List<Channel> getPrefixChannelsCache() {
        return prefixChannelsCache;
    }

    public static Channel getChannel(String key) {
        if (key == null) return null;
        return systemChannels.get(key.toLowerCase());
    }

    public static String getPlayerSelectedKey(Player player) {
        return playerSelectedChannel.getOrDefault(player.getUniqueId(), defaultChannelKey);
    }

    public static Channel getPlayerChannel(Player player) {
        String key = getPlayerSelectedKey(player);
        Channel channel = getChannel(key);
        return (channel != null) ? channel : getChannel(defaultChannelKey);
    }

    public static void setPlayerChannel(Player player, String channelKey) {
        playerSelectedChannel.put(player.getUniqueId(), channelKey.toLowerCase());
        savePlayerData(player.getUniqueId());
    }
}
