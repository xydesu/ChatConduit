package me.xydesu.chatconduit.channel;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.database.dao.PlayerDAO;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 頻道總管理器（結合記憶體快取與非同步 DAO 儲存）
 */
public class ChannelManager {

    private static final Map<String, Channel> systemChannels = new ConcurrentHashMap<>();
    private static final Map<UUID, String> playerSelectedChannel = new ConcurrentHashMap<>();
    private static volatile List<Channel> prefixChannelsCache = List.of();
    private static String defaultChannelKey = "global";

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

        prefixChannelsCache = systemChannels.values().stream()
                .filter(c -> c.prefixKey() != null && !c.prefixKey().isEmpty())
                .sorted(Comparator.comparingInt((Channel c) -> c.prefixKey().length()).reversed())
                .toList();
    }

    /**
     * 載入玩家頻道選擇資料（由資料庫轉寫入記憶體快取）
     */
    public static void loadPlayerData() {
        playerSelectedChannel.clear();
    }

    /**
     * 儲存單一玩家頻道選擇資料（非同步寫入資料庫）
     */
    public static void savePlayerData(Player player) {
        if (player == null) return;
        savePlayerData(player.getUniqueId(), player.getName());
    }

    /**
     * 根據 UUID 與名稱非同步儲存玩家頻道資料
     */
    public static void savePlayerData(UUID uuid) {
        if (uuid == null) return;
        Player player = Bukkit.getPlayer(uuid);
        String name = player != null ? player.getName() : "Unknown";
        savePlayerData(uuid, name);
    }

    public static void savePlayerData(UUID uuid, String playerName) {
        if (uuid == null) return;
        String key = playerSelectedChannel.getOrDefault(uuid, defaultChannelKey);
        Bukkit.getAsyncScheduler().runNow(Main.getInstance(), task -> {
            PlayerDAO.savePlayerData(uuid, playerName, key, Collections.emptySet());
        });
    }

    /**
     * 儲存所有線上玩家資料 (關服或 Reload 時調用)
     */
    public static void saveAllPlayerData() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            String key = playerSelectedChannel.getOrDefault(uuid, defaultChannelKey);
            PlayerDAO.savePlayerData(uuid, player.getName(), key, Collections.emptySet());
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
        if (player == null) return defaultChannelKey;
        return playerSelectedChannel.getOrDefault(player.getUniqueId(), defaultChannelKey);
    }

    /**
     * 非同步載入玩家頻道設定，避免阻塞主執行緒
     */
    public static void loadPlayerDataAsync(Player player) {
        if (player == null) return;
        UUID uuid = player.getUniqueId();
        Bukkit.getAsyncScheduler().runNow(Main.getInstance(), task -> {
            PlayerDAO.PlayerData data = PlayerDAO.getPlayerData(uuid);
            String channelKey = defaultChannelKey;
            if (data != null && data.currentChannel() != null && !data.currentChannel().isEmpty()) {
                channelKey = data.currentChannel().toLowerCase();
            }
            playerSelectedChannel.put(uuid, channelKey);

            String finalKey = channelKey;
            Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                if (!player.isOnline()) return;
                PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(finalKey);
                ChannelManager.Channel sysChan = ChannelManager.getChannel(finalKey);

                if (customChan != null) {
                    if (!customChan.getMembers().contains(uuid)) {
                        setPlayerChannel(player, defaultChannelKey);
                        String resetMsg = Main.getInstance().getLanguageConfig().getString(
                                "channel.reverted-on-join",
                                "<gray>提示：您先前所在的群組頻道已離開或解散，已自動為您切換回預設頻道。"
                        );
                        me.xydesu.chatconduit.util.ChatUtils.sendMessage(player, resetMsg);
                    }
                } else if (sysChan != null) {
                    if (!sysChan.permission().isEmpty() && !player.hasPermission(sysChan.permission())) {
                        setPlayerChannel(player, defaultChannelKey);
                        String resetMsg = Main.getInstance().getLanguageConfig().getString(
                                "channel.reverted-on-join-no-perm",
                                "<gray>提示：您先前選擇的系統頻道已無存取權限，已自動為您切換回預設頻道。"
                        );
                        me.xydesu.chatconduit.util.ChatUtils.sendMessage(player, resetMsg);
                    }
                } else if (!finalKey.equals(defaultChannelKey)) {
                    setPlayerChannel(player, defaultChannelKey);
                }
            });
        });
    }

    public static Channel getPlayerChannel(Player player) {
        String key = getPlayerSelectedKey(player);
        Channel channel = getChannel(key);
        return (channel != null) ? channel : getChannel(defaultChannelKey);
    }

    public static void setPlayerChannel(Player player, String channelKey) {
        if (player == null) return;
        playerSelectedChannel.put(player.getUniqueId(), channelKey.toLowerCase());
        savePlayerData(player);
    }

    /**
     * 玩家離線時清理記憶體 Map 資源
     */
    public static void unloadPlayerData(UUID uuid) {
        if (uuid != null) {
            playerSelectedChannel.remove(uuid);
        }
    }
}
