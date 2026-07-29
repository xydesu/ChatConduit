package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MessageSettingsGUI {

    public static void open(Player player) {
        String titleStr = Main.getInstance().getLanguageConfig().getString(
                "gui.title-message-settings",
                "<gradient:#ff9a9e:#fecfef><bold>個人訊息顯示設定</bold></gradient>"
        );
        Component titleComponent = ChatUtils.parse(player, titleStr);

        GUIHolder holder = new GUIHolder(GUIHolder.GUIType.MESSAGE_SETTINGS);
        Inventory inv = Bukkit.createInventory(holder, 27, titleComponent);
        holder.setInventory(inv);

        // 背景裝飾
        ItemStack glassFiller = createItem(Material.GRAY_STAINED_GLASS_PANE, "<gray> ");
        for (int i = 0; i < 27; i++) {
            inv.setItem(i, glassFiller);
        }

        boolean deathEnabled = ChannelManager.isDeathMessagesEnabled(player);
        boolean joinEnabled = ChannelManager.isJoinMessagesEnabled(player);

        // Slot 11: 死亡訊息切換按鈕
        Material deathMaterial = deathEnabled ? Material.SKELETON_SKULL : Material.WITHER_SKELETON_SKULL;
        String deathStatusStr = deathEnabled ? "<green><bold>✓ 顯示中 (ON)</bold></green>" : "<red><bold>✗ 已隱藏 (OFF)</bold></red>";
        List<String> deathLore = List.of(
                "<gray>開關其他玩家或個人死亡時之系統通知",
                "<gray>當前狀態: " + deathStatusStr,
                "",
                "<yellow>▶ 點擊切換顯示/隱藏</yellow>"
        );
        inv.setItem(11, createItem(deathMaterial, "<red><bold>☠ 死亡訊息通知</bold></red>", deathLore, deathEnabled));

        // Slot 15: 進離場訊息切換按鈕
        Material joinMaterial = joinEnabled ? Material.OAK_DOOR : Material.IRON_DOOR;
        String joinStatusStr = joinEnabled ? "<green><bold>✓ 顯示中 (ON)</bold></green>" : "<red><bold>✗ 已隱藏 (OFF)</bold></red>";
        List<String> joinLore = List.of(
                "<gray>開關玩家加入與離開伺服器時之系統通知",
                "<gray>當前狀態: " + joinStatusStr,
                "",
                "<yellow>▶ 點擊切換顯示/隱藏</yellow>"
        );
        inv.setItem(15, createItem(joinMaterial, "<gold><bold>🚪 玩家進出通知</bold></gold>", joinLore, joinEnabled));

        // Slot 22: 返回主頻道控制台按鈕
        ItemStack backItem = createItem(Material.ARROW, "<yellow><bold>◀ 返回頻道控制台</bold></yellow>", List.of("<gray>點擊返回主選單"), false);
        inv.setItem(22, backItem);

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material material, String name, List<String> lore, boolean glow) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ChatUtils.parseNoItalic(name));
            if (lore != null && !lore.isEmpty()) {
                List<Component> parsedLore = new ArrayList<>();
                for (String line : lore) {
                    parsedLore.add(ChatUtils.parseNoItalic(line));
                }
                meta.lore(parsedLore);
            }
            if (glow) {
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createItem(Material material, String name) {
        return createItem(material, name, null, false);
    }
}
