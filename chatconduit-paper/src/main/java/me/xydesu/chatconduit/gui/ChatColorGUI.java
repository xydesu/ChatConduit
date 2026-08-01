package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.chatcolor.ChatColorManager;
import me.xydesu.chatconduit.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * 玩家預設聊天顏色選擇 GUI 選單 (包含 VIP 漸變與彩虹限定顏色)
 *
 * @author xydesu
 */
public class ChatColorGUI {

    public record ColorOption(int slot, String code, String name, Material material, String requiredPermission) {}

    public static final List<ColorOption> COLOR_OPTIONS = List.of(
            // 基礎顏色 (Slot 10 ~ 16)
            new ColorOption(10, "&f", "&f純白色 (White)", Material.WHITE_WOOL, null),
            new ColorOption(11, "&7", "&7亮灰色 (Light Gray)", Material.LIGHT_GRAY_WOOL, null),
            new ColorOption(12, "&8", "&8暗灰色 (Dark Gray)", Material.GRAY_WOOL, null),
            new ColorOption(13, "&c", "&c鮮紅色 (Red)", Material.RED_WOOL, null),
            new ColorOption(14, "&4", "&4暗紅色 (Dark Red)", Material.NETHER_WART_BLOCK, null),
            new ColorOption(15, "&6", "&6橙黃色 (Gold/Orange)", Material.ORANGE_WOOL, null),
            new ColorOption(16, "&e", "&e明黃色 (Yellow)", Material.YELLOW_WOOL, null),

            // 基礎顏色 (Slot 19 ~ 25)
            new ColorOption(19, "&a", "&a亮綠色 (Lime)", Material.LIME_WOOL, null),
            new ColorOption(20, "&2", "&2暗綠色 (Dark Green)", Material.GREEN_WOOL, null),
            new ColorOption(21, "&b", "&b亮青色 (Aqua)", Material.LIGHT_BLUE_WOOL, null),
            new ColorOption(22, "&3", "&3暗青色 (Dark Aqua)", Material.CYAN_WOOL, null),
            new ColorOption(23, "&9", "&9亮藍色 (Blue)", Material.BLUE_WOOL, null),
            new ColorOption(24, "&d", "&d粉紅色 (Pink)", Material.PINK_WOOL, null),
            new ColorOption(25, "&5", "&5暗紫色 (Purple)", Material.PURPLE_WOOL, null),

            // VIP / 特殊權限漸變與彩虹限定顏色 (Slot 28 ~ 34)
            new ColorOption(28, "<gradient:#ff7e5f:#feb47b>", "<gradient:#ff7e5f:#feb47b>🌅 夕陽漸變 (Sunset)</gradient>", Material.FIRE_CHARGE, "chatconduit.chatcolor.gradient"),
            new ColorOption(29, "<gradient:#8a2387:#e94057:#f27121>", "<gradient:#8a2387:#e94057:#f27121>🌌 賽博霓虹 (Cyberpunk)</gradient>", Material.AMETHYST_SHARD, "chatconduit.chatcolor.gradient"),
            new ColorOption(30, "<gradient:#2b5876:#4e4376>", "<gradient:#2b5876:#4e4376>🌊 深海藍浪 (Ocean)</gradient>", Material.PRISMARINE_CRYSTALS, "chatconduit.chatcolor.gradient"),
            new ColorOption(31, "<gradient:#11998e:#38ef7d>", "<gradient:#11998e:#38ef7d>🌿 翡翠綠霓 (Emerald)</gradient>", Material.EMERALD, "chatconduit.chatcolor.gradient"),
            new ColorOption(32, "<gradient:#ff9a9e:#fecfef>", "<gradient:#ff9a9e:#fecfef>🌸 夢幻櫻花 (Sakura)</gradient>", Material.PINK_PETALS, "chatconduit.chatcolor.vip"),
            new ColorOption(33, "<gradient:#ffe000:#799f0c>", "<gradient:#ffe000:#799f0c>👑 尊爵皇金 (Royal Gold)</gradient>", Material.GOLD_INGOT, "chatconduit.chatcolor.vip"),
            new ColorOption(34, "<rainbow>", "<rainbow>🌈 幻彩彩虹 (Rainbow Magic)</rainbow>", Material.NETHER_STAR, "chatconduit.chatcolor.rainbow")
    );

    public static void openGUI(Player player) {
        GUIHolder holder = new GUIHolder(GUIHolder.GUIType.CHAT_COLOR);
        Inventory inv = Bukkit.createInventory(holder, 45, ChatUtils.parseNoItalic("&8聊天文字顏色選擇面板"));
        holder.setInventory(inv);

        // 填充背景灰色玻璃板
        ItemStack glass = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta glassMeta = glass.getItemMeta();
        if (glassMeta != null) {
            glassMeta.displayName(ChatUtils.parseNoItalic(" "));
            glass.setItemMeta(glassMeta);
        }
        for (int i = 0; i < 45; i++) {
            inv.setItem(i, glass);
        }

        String currentColor = ChatColorManager.getChatColor(player.getUniqueId());

        // 擺放所有顏色項目
        for (ColorOption opt : COLOR_OPTIONS) {
            boolean isCurrent = opt.code().equalsIgnoreCase(currentColor);
            boolean hasPerm = opt.requiredPermission() == null || player.hasPermission(opt.requiredPermission());

            ItemStack item = new ItemStack(opt.material());
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.displayName(ChatUtils.parseNoItalic(opt.name()));

                List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
                lore.add(ChatUtils.parseNoItalic("&7預覽效果: " + opt.code() + "這是一條測試發言對話"));
                lore.add(ChatUtils.parseNoItalic(""));

                if (!hasPerm) {
                    lore.add(ChatUtils.parseNoItalic("&c🔒 尚未解鎖此限定聊天顏色"));
                    lore.add(ChatUtils.parseNoItalic("&7需要權限: &e" + opt.requiredPermission()));
                } else if (isCurrent) {
                    lore.add(ChatUtils.parseNoItalic("&a✔ 當前已選擇此顏色"));
                    meta.setEnchantmentGlintOverride(true);
                } else {
                    lore.add(ChatUtils.parseNoItalic("&e▶ 點擊選擇套用此顏色"));
                }

                meta.lore(lore);
                item.setItemMeta(meta);
            }
            inv.setItem(opt.slot(), item);
        }

        // Slot 40: ❌ 重置顏色項目
        ItemStack resetItem = new ItemStack(Material.BARRIER);
        ItemMeta resetMeta = resetItem.getItemMeta();
        if (resetMeta != null) {
            resetMeta.displayName(ChatUtils.parseNoItalic("&c❌ 重置預設顏色"));
            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            lore.add(ChatUtils.parseNoItalic("&7恢復使用預設發言文字顏色"));
            lore.add(ChatUtils.parseNoItalic(""));
            if (currentColor == null || currentColor.isEmpty()) {
                lore.add(ChatUtils.parseNoItalic("&a✔ 當前已為預設狀態"));
            } else {
                lore.add(ChatUtils.parseNoItalic("&e▶ 點擊清除自訂聊天顏色"));
            }
            resetMeta.lore(lore);
            resetItem.setItemMeta(resetMeta);
        }
        inv.setItem(40, resetItem);

        player.openInventory(inv);
    }
}
