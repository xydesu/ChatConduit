package me.xydesu.chatconduit;

import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.command.ChannelCommand;
import me.xydesu.chatconduit.command.ChatConduitCommand;
import me.xydesu.chatconduit.command.PlayerChannelCommand;
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

        // 載入頻道與玩家資料
        ChannelManager.loadChannels();
        ChannelManager.loadPlayerData(); // 載入玩家當前發言頻道紀錄
        PlayerChannelManager.load();     // 載入玩家自建群組頻道

        // 註冊監聽器
        getServer().getPluginManager().registerEvents(new ChatListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(), this);

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

        getLogger().info("ChatConduit 已成功啟動！");
    }

    @Override
    public void onDisable() {
        // 關服時自動儲存所有資料 (同步寫入)
        ChannelManager.saveAllPlayerData();
        PlayerChannelManager.save();
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
        reloadConfig();
        saveDefaultLanguageFiles();
        loadLanguageConfig();
        ChannelManager.loadChannels();
        ChannelManager.loadPlayerData();
        PlayerChannelManager.load();
    }
}
