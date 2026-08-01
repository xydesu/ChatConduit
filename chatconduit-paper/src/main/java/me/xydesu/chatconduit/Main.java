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
        me.xydesu.chatconduit.gui.GUIManager.load();

        // 初始化資料庫連線池與自動遷移
        me.xydesu.chatconduit.database.DatabaseManager.init();
        me.xydesu.chatconduit.database.DataMigrationManager.runMigrationCheck();
        me.xydesu.chatconduit.mute.MuteManager.init();
        me.xydesu.chatconduit.chatcolor.ChatColorManager.init();

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

        me.xydesu.chatconduit.command.MsgCommand msgCmd = new me.xydesu.chatconduit.command.MsgCommand();
        if (getCommand("msg") != null) {
            getCommand("msg").setExecutor(msgCmd);
            getCommand("msg").setTabCompleter(msgCmd);
        }
        if (getCommand("reply") != null) {
            getCommand("reply").setExecutor(msgCmd);
            getCommand("reply").setTabCompleter(msgCmd);
        }

        me.xydesu.chatconduit.command.ChatColorCommand chatColorCmd = new me.xydesu.chatconduit.command.ChatColorCommand();
        if (getCommand("chatcolor") != null) {
            getCommand("chatcolor").setExecutor(chatColorCmd);
            getCommand("chatcolor").setTabCompleter(chatColorCmd);
        }

        // 初始化 DiscordSRV 溝通模組
        me.xydesu.chatconduit.integration.DiscordSRVHook.init();

        // 初始化 CMI AFK 狀態監測模組
        me.xydesu.chatconduit.integration.CMIHook.init();

        // 初始化 Redis 跨服通訊模組
        me.xydesu.chatconduit.redis.RedisManager.init();

        sendStartupBanner();
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

        sendShutdownBanner();
    }

    private void sendStartupBanner() {
        String version = getDescription().getVersion();
        String author = String.join(", ", getDescription().getAuthors());
        if (author.isEmpty()) {
            author = "xydesu";
        }

        String dbType = me.xydesu.chatconduit.database.DatabaseManager.getDbType();
        String dbStatus = dbType != null ? dbType.toUpperCase() : "UNKNOWN";

        String redisStatus = me.xydesu.chatconduit.redis.RedisManager.isEnabled()
                ? "<green>Enabled</green> <gray>(Server: " + me.xydesu.chatconduit.redis.RedisManager.getServerId() + ")</gray>"
                : "<red>Disabled</red>";

        String discordSrvStatus = getServer().getPluginManager().isPluginEnabled("DiscordSRV")
                ? "<green>Hooked</green>"
                : "<yellow>Not Found</yellow>";

        String cmiStatus = me.xydesu.chatconduit.integration.CMIHook.isEnabled()
                ? "<green>Hooked (AFK Track)</green>"
                : "<yellow>Not Found</yellow>";

        String icStatus = getServer().getPluginManager().isPluginEnabled("InteractiveChat")
                ? "<green>Hooked</green>"
                : "<yellow>Not Found</yellow>";

        String papiStatus = getServer().getPluginManager().isPluginEnabled("PlaceholderAPI")
                ? "<green>Hooked</green>"
                : "<yellow>Not Found</yellow>";

        int sysChanCount = ChannelManager.getChannels().size();
        int playerChanCount = PlayerChannelManager.getCustomChannels().size();

        String[] banner = {
            "",
            "<gradient:#00FFA3:#00B8FF><bold>  ____ _           _   ____                _   _ </bold></gradient>",
            "<gradient:#00FFA3:#00B8FF><bold> / ___| |__   __ _| |_/ ___|___  _ __   __| |_| |</bold></gradient>",
            "<gradient:#00FFA3:#00B8FF><bold>| |   | '_ \\ / _` | __| |   / _ \\| '_ \\ / _` | | |</bold></gradient>",
            "<gradient:#00FFA3:#00B8FF><bold>| |___| | | | (_| | |_| |__| (_) | | | | (_| |_|_|</bold></gradient>",
            "<gradient:#00FFA3:#00B8FF><bold> \\____|_| |_|\\__,_|\\__|\\____\\___/|_| |_|\\__,_(_|_)</bold></gradient>",
            "",
            "<dark_gray>┌────────────────────────────────────────────────────────┐</dark_gray>",
            "<dark_gray>│</dark_gray>  <gradient:#00FFA3:#00B8FF><bold>ChatConduit</bold></gradient> <gray>v" + version + "</gray> <dark_gray>| Created by</dark_gray> <gold>" + author + "</gold>",
            "<dark_gray>├────────────────────────────────────────────────────────┤</dark_gray>",
            "<dark_gray>│</dark_gray>  <gray>• Database Driver :</gray> <green>HikariCP (" + dbStatus + ")</green>",
            "<dark_gray>│</dark_gray>  <gray>• Redis Sync      :</gray> " + redisStatus,
            "<dark_gray>│</dark_gray>  <gray>• DiscordSRV      :</gray> " + discordSrvStatus,
            "<dark_gray>│</dark_gray>  <gray>• CMI Hook        :</gray> " + cmiStatus,
            "<dark_gray>│</dark_gray>  <gray>• InteractiveChat :</gray> " + icStatus,
            "<dark_gray>│</dark_gray>  <gray>• PlaceholderAPI  :</gray> " + papiStatus,
            "<dark_gray>│</dark_gray>  <gray>• Channels Loaded :</gray> <aqua>" + sysChanCount + " System</aqua> <dark_gray>/</dark_gray> <aqua>" + playerChanCount + " Player</aqua>",
            "<dark_gray>└────────────────────────────────────────────────────────┘</dark_gray>",
            "<green>✔ ChatConduit 零指令聊天頻道系統已成功啟動運作！</green>",
            ""
        };

        for (String line : banner) {
            getServer().getConsoleSender().sendMessage(me.xydesu.chatconduit.util.ChatUtils.parse(null, line));
        }
    }

    private void sendShutdownBanner() {
        String version = getDescription().getVersion();
        String[] banner = {
            "",
            "<dark_gray>┌────────────────────────────────────────────────────────┐</dark_gray>",
            "<dark_gray>│</dark_gray>  <gradient:#FF416C:#FF4B2B><bold>ChatConduit</bold></gradient> <gray>v" + version + "</gray> <dark_gray>| Shutting down...</dark_gray>",
            "<dark_gray>├────────────────────────────────────────────────────────┤</dark_gray>",
            "<dark_gray>│</dark_gray>  <gray>• Saving Player Data       :</gray> <green>DONE</green>",
            "<dark_gray>│</dark_gray>  <gray>• Closing Database Pool    :</gray> <green>DONE</green>",
            "<dark_gray>│</dark_gray>  <gray>• Closing Redis Connections:</gray> <green>DONE</green>",
            "<dark_gray>└────────────────────────────────────────────────────────┘</dark_gray>",
            "<yellow>✖ ChatConduit 插件已成功安全關閉與卸載。</yellow>",
            ""
        };

        for (String line : banner) {
            getServer().getConsoleSender().sendMessage(me.xydesu.chatconduit.util.ChatUtils.parse(null, line));
        }
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

