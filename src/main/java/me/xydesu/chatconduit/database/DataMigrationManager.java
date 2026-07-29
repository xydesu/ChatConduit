package me.xydesu.chatconduit.database;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.channel.PlayerChannelManager.CustomChannel;
import me.xydesu.chatconduit.channel.PlayerChannelManager.Mode;
import me.xydesu.chatconduit.database.dao.PlayerChannelDAO;
import me.xydesu.chatconduit.database.dao.PlayerDAO;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 資料自動遷移管理器
 * 檢測舊版 YAML / JSON 檔案並一次性自動匯入至資料庫中
 */
public class DataMigrationManager {

    /**
     * 執行自動資料遷移作業
     */
    public static void runMigrationCheck() {
        File dataFolder = Main.getInstance().getDataFolder();

        // 1. 遷移 player-data.yml
        File playerDataFile = new File(dataFolder, "player-data.yml");
        if (playerDataFile.exists() && playerDataFile.length() > 0) {
            Main.getInstance().getLogger().info("檢測到舊版 player-data.yml，開始自動遷移至資料庫...");
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(playerDataFile);
                if (config.contains("players")) {
                    ConfigurationSection playersSec = config.getConfigurationSection("players");
                    if (playersSec != null) {
                        int count = 0;
                        for (String uuidStr : playersSec.getKeys(false)) {
                            try {
                                UUID uuid = UUID.fromString(uuidStr);
                                String channelKey = config.getString("players." + uuidStr);
                                if (channelKey != null) {
                                    PlayerDAO.savePlayerData(uuid, "MigratedPlayer", channelKey.toLowerCase(), Collections.emptySet(), true, true);
                                    count++;
                                }
                            } catch (IllegalArgumentException ignored) {}
                        }
                        Main.getInstance().getLogger().info("成功遷移 " + count + " 筆玩家頻道資料至資料庫！");
                    }
                }

                // 備份舊檔案
                File backupFile = new File(dataFolder, "player-data.yml.bak");
                if (playerDataFile.renameTo(backupFile)) {
                    Main.getInstance().getLogger().info("舊版 player-data.yml 已更名為 player-data.yml.bak 留存備份。");
                }
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.SEVERE, "遷移 player-data.yml 時出錯:", e);
            }
        }

        // 2. 遷移 player-channels.yml
        File playerChannelsFile = new File(dataFolder, "player-channels.yml");
        if (playerChannelsFile.exists() && playerChannelsFile.length() > 0) {
            Main.getInstance().getLogger().info("檢測到舊版 player-channels.yml，開始自動遷移至資料庫...");
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(playerChannelsFile);
                if (config.contains("channels")) {
                    ConfigurationSection channelsSec = config.getConfigurationSection("channels");
                    if (channelsSec != null) {
                        int count = 0;
                        for (String id : channelsSec.getKeys(false)) {
                            String name = config.getString("channels." + id + ".name", id);
                            String ownerStr = config.getString("channels." + id + ".owner");
                            if (ownerStr == null) continue;

                            try {
                                UUID owner = UUID.fromString(ownerStr);
                                Mode mode = Mode.valueOf(config.getString("channels." + id + ".mode", "PRIVATE"));
                                String theme = config.getString("channels." + id + ".color-theme", "<gradient:#a8c0ff:#3f2b96>");
                                String webhook = config.getString("channels." + id + ".webhook-url", null);
                                String desc = config.getString("channels." + id + ".description", "尚無頻道簡介說明");
                                String rules = config.getString("channels." + id + ".rules", "遵守社群規範，友善交流。");

                                CustomChannel channel = new CustomChannel(id, name, owner, mode, theme, webhook, desc, rules);

                                if (config.contains("channels." + id + ".members")) {
                                    for (String memStr : config.getStringList("channels." + id + ".members")) {
                                        try {
                                            channel.getMembers().add(UUID.fromString(memStr));
                                        } catch (IllegalArgumentException ignored) {}
                                    }
                                }

                                PlayerChannelDAO.saveCustomChannel(channel);
                                count++;
                            } catch (IllegalArgumentException ignored) {}
                        }
                        Main.getInstance().getLogger().info("成功遷移 " + count + " 個自訂群組頻道至資料庫！");
                    }
                }

                // 備份舊檔案
                File backupFile = new File(dataFolder, "player-channels.yml.bak");
                if (playerChannelsFile.renameTo(backupFile)) {
                    Main.getInstance().getLogger().info("舊版 player-channels.yml 已更名為 player-channels.yml.bak 留存備份。");
                }
            } catch (Exception e) {
                Main.getInstance().getLogger().log(Level.SEVERE, "遷移 player-channels.yml 時出錯:", e);
            }
        }
    }
}
