package me.xydesu.chatConduit.Channel;

import me.xydesu.chatConduit.Main;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ChannelManager {

    private static final Map<String, Channel> systemChannels = new HashMap<>();
    private static final Map<UUID, String> playerSelectedChannel = new HashMap<>();
    private static String defaultChannelKey = "global";

    private static File dataFile;
    private static FileConfiguration dataConfig;

    public record Channel(String key, String name, String color, String prefixKey, String permission) {}

    /**
     * 從 config.yml 載入系統頻道設定
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

                systemChannels.put(key.toLowerCase(), new Channel(key, name, color, prefixKey, permission));
            }
        }
    }

    /**
     * 載入玩家頻道選擇資料 (player-data.yml)
     */
    public static void loadPlayerData() {
        playerSelectedChannel.clear();
        dataFile = new File(Main.getInstance().getDataFolder(), "player-data.yml");
        if (!dataFile.exists()) {
            try { dataFile.createNewFile(); } catch (IOException ignored) {}
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);

        if (dataConfig.contains("players")) {
            for (String uuidStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    String channelKey = dataConfig.getString("players." + uuidStr);
                    playerSelectedChannel.put(uuid, channelKey.toLowerCase());
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    /**
     * 儲存單一玩家資料
     */
    public static void savePlayerData(UUID uuid) {
        if (dataConfig == null) return;
        String key = playerSelectedChannel.getOrDefault(uuid, defaultChannelKey);
        dataConfig.set("players." + uuid.toString(), key);
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 儲存所有線上與紀錄中的玩家資料 (關服時調用)
     */
    public static void saveAllPlayerData() {
        if (dataConfig == null) return;
        for (Map.Entry<UUID, String> entry : playerSelectedChannel.entrySet()) {
            dataConfig.set("players." + entry.getKey().toString(), entry.getValue());
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Channel> getChannels() {
        return systemChannels;
    }

    public static Channel getChannel(String key) {
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
        savePlayerData(player.getUniqueId()); // 變更頻道時立即非同步/寫入檔案保存
    }
}