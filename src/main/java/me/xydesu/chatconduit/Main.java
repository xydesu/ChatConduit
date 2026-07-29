package me.xydesu.chatconduit;

import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.command.ChannelCommand;
import me.xydesu.chatconduit.command.ChatConduitCommand;
import me.xydesu.chatconduit.command.PlayerChannelCommand;
import me.xydesu.chatconduit.gui.GUIListener;
import me.xydesu.chatconduit.gui.PlayerInputManager;
import me.xydesu.chatconduit.listener.ChatListener;
import me.xydesu.chatconduit.listener.PlayerListener;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class Main extends JavaPlugin {

    private static Main instance;
    private FileConfiguration languageConfig;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        saveDefaultLanguageFiles();
        loadLanguageConfig();
        me.xydesu.chatconduit.gui.GUIManager.load();

        // 初始化資料庫連線池與自動遷移
        me.xydesu.chatconduit.database.DatabaseManager.init();
        me.xydesu.chatconduit.database.DataMigrationManager.runMigrationCheck();
        me.xydesu.chatconduit.mute.MuteManager.init();

        // 載入頻道與玩家資料
        ChannelManager.loadChannels();
        ChannelManager.loadPlayerData(); // 載入玩家當前發言頻道紀錄
        PlayerChannelManager.load();     // 載入玩家自建群組頻道

        // 註冊監聽器 (包含選單 GUIListener 與對話輸入 PlayerInputManager)
        getServer().getPluginManager().registerEvents(new ChatListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);
        getServer().getPluginManager().registerEvents(new GUIListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerInputManager(), this);

        // 註冊指令
        ChatConduitCommand chatConduitCmd = new ChatConduitCommand();
        if (getCommand("chatconduit") != null) {
            getCommand("chatconduit").setExecutor(chatConduitCmd);
            getCommand("chatconduit").setTabCompleter(chatConduitCmd);
        }

        ChannelCommand channelCmd = new ChannelCommand();
        if (getCommand("channel") != null) {
            getCommand("channel").setExecutor(channelCmd);
            getCommand("channel").setTabCompleter(channelCmd);
        }

        PlayerChannelCommand pcCmd = new PlayerChannelCommand();
        if (getCommand("playerchannel") != null) {
            getCommand("playerchannel").setExecutor(pcCmd);
            getCommand("playerchannel").setTabCompleter(pcCmd);
        }

        me.xydesu.chatconduit.command.MuteCommand muteCmd = new me.xydesu.chatconduit.command.MuteCommand();
        if (getCommand("mute") != null) {
            getCommand("mute").setExecutor(muteCmd);
            getCommand("mute").setTabCompleter(muteCmd);
        }
        if (getCommand("unmute") != null) {
            getCommand("unmute").setExecutor(muteCmd);
            getCommand("unmute").setTabCompleter(muteCmd);
        }
        if (getCommand("mutelist") != null) {
            getCommand("mutelist").setExecutor(muteCmd);
            getCommand("mutelist").setTabCompleter(muteCmd);
        }

        // 初始化 DiscordSRV 溝通模組
        me.xydesu.chatconduit.integration.DiscordSRVHook.init();

        // 初始化 Redis 跨服通訊模組
        me.xydesu.chatconduit.redis.RedisManager.init();

        getLogger().info("ChatConduit (含全 Chest GUI 零指令系統、HikariCP 資料庫與 Redis 多伺服器支持) 已成功啟動！");
    }

    @Override
    public void onDisable() {
        // 關閉 Redis 通訊連線池
        me.xydesu.chatconduit.redis.RedisManager.close();

        me.xydesu.chatconduit.integration.DiscordSRVHook.shutdown();
        // 關服時自動儲存所有資料 (同步寫入)
        ChannelManager.saveAllPlayerData();
        PlayerChannelManager.saveImmediately();
        // 關閉資料庫連線池
        me.xydesu.chatconduit.database.DatabaseManager.close();
    }

    public static Main getInstance() {
        return instance;
    }

    public FileConfiguration getLanguageConfig() {
        return languageConfig;
    }

    private void saveDefaultLanguageFiles() {
        String[] defaultLanguages = {"zh-TW.yml", "en-US.yml"};
        for (String langFile : defaultLanguages) {
            File file = new File(getDataFolder(), "lang/" + langFile);
            if (!file.exists()) {
                saveResource("lang/" + langFile, false);
            }
        }
    }

    public void loadLanguageConfig() {
        String langName = getConfig().getString("language", "zh-TW");
        File langFolder = new File(getDataFolder(), "lang");
        File languageFile = new File(langFolder, langName + ".yml");

        if (!languageFile.exists()) {
            getLogger().warning("找不到語言檔案 " + langName + ".yml，已回退使用預設的 zh-TW.yml！");
            languageFile = new File(langFolder, "zh-TW.yml");
        }

        languageConfig = YamlConfiguration.loadConfiguration(languageFile);
    }

    public void reloadPluginConfigs() {
        me.xydesu.chatconduit.redis.RedisManager.close();
        reloadConfig();
        saveDefaultLanguageFiles();
        loadLanguageConfig();
        me.xydesu.chatconduit.gui.GUIManager.reload();
        ChannelManager.loadChannels();
        ChannelManager.loadPlayerData();
        PlayerChannelManager.load();
        me.xydesu.chatconduit.mute.MuteManager.init();
        me.xydesu.chatconduit.redis.RedisManager.init();
    }
}
