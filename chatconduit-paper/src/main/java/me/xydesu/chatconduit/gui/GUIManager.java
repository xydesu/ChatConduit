package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * GUI 配置檔案管理器，負責載入、解析與提供 GUI 介面設定檔。
 *
 * @author xydesu
 */
public class GUIManager {

    private static final Map<String, FileConfiguration> guiConfigs = new HashMap<>();
    private static final String[] GUI_FILES = {
            "channel_select.yml",
            "channel_settings.yml",
            "message_settings.yml",
            "online_players.yml",
            "pending_invites.yml",
            "player_channel_manage.yml"
    };

    /**
     * 載入並初始化所有 GUI 配置檔案
     */
    public static void load() {
        guiConfigs.clear();
        Main plugin = Main.getInstance();
        File guiFolder = new File(plugin.getDataFolder(), "gui");
        if (!guiFolder.exists()) {
            guiFolder.mkdirs();
        }

        for (String fileName : GUI_FILES) {
            File targetFile = new File(guiFolder, fileName);
            if (!targetFile.exists()) {
                plugin.saveResource("gui/" + fileName, false);
            }
            String key = fileName.replace(".yml", "");
            FileConfiguration config = YamlConfiguration.loadConfiguration(targetFile);
            guiConfigs.put(key, config);
        }
    }

    /**
     * 重載所有 GUI 配置檔案
     */
    public static void reload() {
        load();
    }

    /**
     * 取得指定 GUI 的配置檔案
     *
     * @param guiName GUI 名稱 (例如 "channel_select")
     * @return FileConfiguration，若未找到則傳回 null
     */
    public static FileConfiguration getConfig(String guiName) {
        return guiConfigs.get(guiName);
    }

    /**
     * 取得指定 GUI 設定檔中的標題名稱
     *
     * @param guiName GUI 名稱
     * @param defaultTitle 預設標題
     * @return 標題字串
     */
    public static String getTitle(String guiName, String defaultTitle) {
        FileConfiguration config = getConfig(guiName);
        if (config == null) return defaultTitle;
        return config.getString("title", defaultTitle);
    }

    /**
     * 取得指定 GUI 設定檔中的尺寸容量
     *
     * @param guiName GUI 名稱
     * @param defaultSize 預設尺寸
     * @return 尺寸數值
     */
    public static int getSize(String guiName, int defaultSize) {
        FileConfiguration config = getConfig(guiName);
        if (config == null) return defaultSize;
        return config.getInt("size", defaultSize);
    }

    /**
     * 取得指定項目的 Slot 號碼
     *
     * @param config GUI 設定檔
     * @param itemKey 項目 Key
     * @param defaultSlot 預設 Slot
     * @return Slot 號碼
     */
    public static int getSlot(FileConfiguration config, String itemKey, int defaultSlot) {
        if (config == null) return defaultSlot;
        return config.getInt("items." + itemKey + ".slot", defaultSlot);
    }

    /**
     * 取得指定項目的 Slot 陣列
     *
     * @param config GUI 設定檔
     * @param path 設定路徑 (例如 "slots.system-channels" 或 "items.filler-glass.slots")
     * @param defaultSlots 預設 Slot 陣列
     * @return Slot 陣列
     */
    public static int[] getSlots(FileConfiguration config, String path, int[] defaultSlots) {
        if (config == null || !config.contains(path)) return defaultSlots;
        List<Integer> list = config.getIntegerList(path);
        if (list.isEmpty()) return defaultSlots;
        return list.stream().mapToInt(i -> i).toArray();
    }

    /**
     * 根據配置檔與 Key 建構 ItemStack 物品
     *
     * @param config GUI 設定檔
     * @param itemKey 項目 Key
     * @param defaultMaterial 預設材質
     * @param replacements 佔位符替換 Map
     * @return 建立之 ItemStack
     */
    public static ItemStack createItem(FileConfiguration config, String itemKey, Material defaultMaterial, Map<String, String> replacements) {
        if (config == null || !config.contains("items." + itemKey)) {
            return new ItemStack(defaultMaterial != null ? defaultMaterial : Material.PAPER);
        }

        String basePath = "items." + itemKey;
        String matStr = config.getString(basePath + ".material");
        Material mat = defaultMaterial;
        if (matStr != null && !matStr.isEmpty()) {
            Material found = Material.matchMaterial(matStr);
            if (found != null) {
                mat = found;
            }
        }
        if (mat == null) mat = Material.PAPER;

        String name = config.getString(basePath + ".name", "");
        List<String> lore = config.getStringList(basePath + ".lore");
        boolean glow = config.getBoolean(basePath + ".glow", false);
        int customModelData = config.getInt(basePath + ".custom-model-data", 0);
        String skullTexture = config.getString(basePath + ".skull-texture", null);

        // 如果設定了自訂頭顱 Texture
        if (skullTexture != null && !skullTexture.isEmpty()) {
            List<String> formattedLore = formatList(lore, replacements);
            String formattedName = formatString(name, replacements);
            return ChatUtils.createCustomHead(skullTexture, formattedName, formattedLore);
        }

        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (!name.isEmpty()) {
                meta.displayName(ChatUtils.parseNoItalic(formatString(name, replacements)));
            }

            if (!lore.isEmpty()) {
                List<Component> parsedLore = new ArrayList<>();
                for (String line : lore) {
                    parsedLore.add(ChatUtils.parseNoItalic(formatString(line, replacements)));
                }
                meta.lore(parsedLore);
            }

            if (glow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            if (customModelData > 0) {
                meta.setCustomModelData(customModelData);
            }

            item.setItemMeta(meta);
        }
        return item;
    }

    private static String formatString(String input, Map<String, String> replacements) {
        if (input == null || input.isEmpty() || replacements == null || replacements.isEmpty()) {
            return input;
        }
        String result = input;
        for (Map.Entry<String, String> entry : replacements.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    private static List<String> formatList(List<String> input, Map<String, String> replacements) {
        if (input == null || input.isEmpty()) return new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (String line : input) {
            result.add(formatString(line, replacements));
        }
        return result;
    }
}
