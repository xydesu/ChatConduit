package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerChannelManageGUI {

    public static void open(Player player) {
        String currentKey = ChannelManager.getPlayerSelectedKey(player);
        PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(currentKey);

        // 若玩家當前沒有在任何自訂頻道中，搜尋玩家所擁有的或加入的第一個自訂頻道
        if (customChan == null) {
            for (PlayerChannelManager.CustomChannel c : PlayerChannelManager.getCustomChannels().values()) {
                if (c.getMembers().contains(player.getUniqueId())) {
                    customChan = c;
                    break;
                }
            }
        }

        if (customChan == null) {
            ChatUtils.sendMessage(player, "<red>你目前不處於任何自訂群組頻道中！請先建立或加入頻道。");
            return;
        }

        openForChannel(player, customChan);
    }

    public static void openForChannel(Player player, PlayerChannelManager.CustomChannel customChan) {
        String titleStr = "<gradient:#a8c0ff:#3f2b96><bold>群組管理 - " + customChan.getDisplayName() + "</bold></gradient>";
        Component titleComponent = ChatUtils.parse(player, titleStr);

        GUIHolder holder = new GUIHolder(GUIHolder.GUIType.PLAYER_CHANNEL_MANAGE, customChan.getId());
        Inventory inv = Bukkit.createInventory(holder, 54, titleComponent);

        // 裝飾
        ItemStack glassFiller = createItem(Material.GRAY_STAINED_GLASS_PANE, "<gray> ");
        for (int i = 0; i < 9; i++) inv.setItem(i, glassFiller);
        for (int i = 45; i < 54; i++) inv.setItem(i, glassFiller);

        boolean isOwner = customChan.getOwner().equals(player.getUniqueId());

        // Slot 4: 頻道模式與狀態按鈕
        Material modeMat = customChan.getMode() == PlayerChannelManager.Mode.PUBLIC ? Material.OAK_DOOR : Material.IRON_DOOR;
        String modeName = "<gold><bold>頻道設定: " + customChan.getDisplayName() + "</bold>";
        List<String> modeLore = new ArrayList<>();
        modeLore.add("<gray>隊長: <yellow>" + (Bukkit.getOfflinePlayer(customChan.getOwner()).getName() != null ? Bukkit.getOfflinePlayer(customChan.getOwner()).getName() : customChan.getOwner().toString()));
        modeLore.add("<gray>存取模式: " + (customChan.getMode() == PlayerChannelManager.Mode.PUBLIC ? "<green>公共 (PUBLIC)" : "<red>私人 (PRIVATE)"));
        modeLore.add("<gray>成員總數: <yellow>" + customChan.getMembers().size() + " 人");
        modeLore.add("");
        if (isOwner) {
            modeLore.add("<yellow>▶ 點擊切換模式 (PUBLIC / PRIVATE)</yellow>");
        } else {
            modeLore.add("<gray>僅隊長可修改頻道設定");
        }
        inv.setItem(4, createItem(modeMat, modeName, modeLore));

        // Slot 10 ~ 34: 成員頭顱清單
        int slot = 10;
        for (UUID memberUuid : customChan.getMembers()) {
            if (slot >= 44) break;

            OfflinePlayer offP = Bukkit.getOfflinePlayer(memberUuid);
            boolean memberIsOwner = memberUuid.equals(customChan.getOwner());
            boolean isOnline = offP.isOnline() && offP.getPlayer() != null;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                meta.setOwningPlayer(offP);
                String pName = offP.getName() != null ? offP.getName() : memberUuid.toString();
                meta.displayName(ChatUtils.parse(null, (memberIsOwner ? "<gold>[隊長] " : "<gray>[成員] ") + "<white>" + pName));

                List<Component> lore = new ArrayList<>();
                lore.add(ChatUtils.parse(null, "<gray>狀態: " + (isOnline ? "<green>● 在線" : "<red>○ 離線")));
                if (isOwner && !memberIsOwner) {
                    lore.add(ChatUtils.parse(null, ""));
                    lore.add(ChatUtils.parse(null, "<yellow>▶ 左鍵點擊: 踢出該成員</yellow>"));
                    lore.add(ChatUtils.parse(null, "<gold>▶ 右鍵點擊: 轉讓隊長給該成員</gold>"));
                }
                meta.lore(lore);
                head.setItemMeta(meta);
            }
            inv.setItem(slot++, head);

            if (slot % 9 == 8) slot += 2;
        }

        // 底部功能按鈕
        // Slot 45: 返回頻道主頁
        inv.setItem(45, createItem(Material.ARROW, "<yellow><bold>← 返回頻道大廳</bold>", List.of("<gray>回到頻道選擇選單")));

        // Slot 49: 邀請提示
        inv.setItem(49, createItem(Material.WRITABLE_BOOK, "<green><bold>✉ 邀請新玩家加入</bold>", List.of(
                "<gray>若要邀請其他玩家加入，請使用指令：",
                "<gold>/pc invite <玩家名稱>",
                "",
                "<gray>受邀玩家將會在選單中收到通知！"
        )));

        // Slot 53: 解散頻道 (僅 Owner 可用)
        if (isOwner) {
            inv.setItem(53, createItem(Material.TNT, "<red><bold>✖ 解散並刪除頻道</bold>", List.of(
                    "<gray>解散此頻道並踢出所有人",
                    "",
                    "<red>▶ 點擊確認解散頻道</red>"
            )));
        }

        player.openInventory(inv);
    }

    private static ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(ChatUtils.parse(null, name));
            if (lore != null && !lore.isEmpty()) {
                List<Component> parsedLore = new ArrayList<>();
                for (String line : lore) {
                    parsedLore.add(ChatUtils.parse(null, line));
                }
                meta.lore(parsedLore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createItem(Material material, String name) {
        return createItem(material, name, null);
    }
}
