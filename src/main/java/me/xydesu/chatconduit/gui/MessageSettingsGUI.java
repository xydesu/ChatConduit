package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class MessageSettingsGUI {

    public static void open(Player player) {
        FileConfiguration config = GUIManager.getConfig("message_settings");

        String titleStr = GUIManager.getTitle("message_settings", Main.getInstance().getLanguageConfig().getString(
                "gui.title-message-settings",
                "<gradient:#ff9a9e:#fecfef><bold>個人訊息顯示設定</bold></gradient>"
        ));
        Component titleComponent = ChatUtils.parse(player, titleStr);

        int size = GUIManager.getSize("message_settings", 27);
        GUIHolder holder = new GUIHolder(GUIHolder.GUIType.MESSAGE_SETTINGS);
        Inventory inv = Bukkit.createInventory(holder, size, titleComponent);
        holder.setInventory(inv);

        // 背景裝飾
        ItemStack glassFiller = GUIManager.createItem(config, "filler-glass", Material.GRAY_STAINED_GLASS_PANE, null);
        int[] fillerSlots = GUIManager.getSlots(config, "items.filler-glass.slots", new int[]{0,1,2,3,4,5,6,7,8,9,10,12,13,14,16,17,18,19,20,21,23,24,25,26});
        for (int s : fillerSlots) {
            if (s < size) inv.setItem(s, glassFiller);
        }

        boolean deathEnabled = ChannelManager.isDeathMessagesEnabled(player);
        boolean joinEnabled = ChannelManager.isJoinMessagesEnabled(player);

        // Slot 11: 死亡訊息切換按鈕
        int deathSlot = GUIManager.getSlot(config, "death-messages", 11);
        if (deathSlot < size) {
            Material deathMaterial = deathEnabled ? Material.SKELETON_SKULL : Material.WITHER_SKELETON_SKULL;
            String deathStatusStr = deathEnabled ? "<green><bold>✓ 顯示中 (ON)</bold></green>" : "<red><bold>✗ 已隱藏 (OFF)</bold></red>";
            ItemStack item = GUIManager.createItem(config, "death-messages", deathMaterial, Map.of("<death_status>", deathStatusStr));
            inv.setItem(deathSlot, item);
        }

        // Slot 15: 進離場訊息切換按鈕
        int joinSlot = GUIManager.getSlot(config, "join-messages", 15);
        if (joinSlot < size) {
            Material joinMaterial = joinEnabled ? Material.OAK_DOOR : Material.IRON_DOOR;
            String joinStatusStr = joinEnabled ? "<green><bold>✓ 顯示中 (ON)</bold></green>" : "<red><bold>✗ 已隱藏 (OFF)</bold></red>";
            ItemStack item = GUIManager.createItem(config, "join-messages", joinMaterial, Map.of("<join_status>", joinStatusStr));
            inv.setItem(joinSlot, item);
        }

        // Slot 22: 返回主頻道控制台按鈕
        int backSlot = GUIManager.getSlot(config, "back-button", 22);
        if (backSlot < size) {
            inv.setItem(backSlot, GUIManager.createItem(config, "back-button", Material.ARROW, null));
        }

        player.openInventory(inv);
    }
}
