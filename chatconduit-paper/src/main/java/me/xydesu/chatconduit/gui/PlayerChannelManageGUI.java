package me.xydesu.chatconduit.gui;

import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import me.xydesu.chatconduit.util.ChatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerChannelManageGUI {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy/MM/dd HH:mm");

    public static void open(Player player) {
        String currentKey = ChannelManager.getPlayerSelectedKey(player);
        PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(currentKey);

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
        FileConfiguration config = GUIManager.getConfig("player_channel_manage");

        String titleStr = GUIManager.getTitle("player_channel_manage", "<gradient:#a8c0ff:#3f2b96><bold>群組管理 - <channel_name></bold></gradient>")
                .replace("<channel_name>", customChan.getDisplayName());
        Component titleComponent = ChatUtils.parse(player, titleStr);

        int size = GUIManager.getSize("player_channel_manage", 54);
        GUIHolder holder = new GUIHolder(GUIHolder.GUIType.PLAYER_CHANNEL_MANAGE, customChan.getId());
        Inventory inv = Bukkit.createInventory(holder, size, titleComponent);

        // 裝飾邊框
        ItemStack glassFiller = GUIManager.createItem(config, "filler-glass", Material.GRAY_STAINED_GLASS_PANE, null);
        int[] fillerSlots = GUIManager.getSlots(config, "items.filler-glass.slots", new int[]{0,1,2,3,5,6,7,8,46,47,48,50,52});
        for (int s : fillerSlots) {
            if (s < size) inv.setItem(s, glassFiller);
        }

        boolean isOwner = customChan.getOwner().equals(player.getUniqueId());

        // Slot 4: 頻道詳細設定按鈕
        int infoSlot = GUIManager.getSlot(config, "channel-info", 4);
        if (infoSlot < size) {
            Material modeMat = customChan.getMode() == PlayerChannelManager.Mode.PUBLIC ? Material.OAK_DOOR : Material.IRON_DOOR;
            OfflinePlayer ownerP = Bukkit.getOfflinePlayer(customChan.getOwner());
            String ownerName = ownerP.getName() != null ? ownerP.getName() : customChan.getOwner().toString();
            String accessMode = customChan.getMode() == PlayerChannelManager.Mode.PUBLIC ? "<green>公共 (PUBLIC)</green>" : "<red>私人 (PRIVATE)</red>";
            String ownerTip = isOwner ? "<yellow>▶ 點擊開啟詳細設定選單 (更名/模式/色彩)</yellow>" : "<gray>僅隊長可開啟與修改頻道詳細設定";

            Map<String, String> infoReplacements = Map.of(
                    "<channel_name>", customChan.getDisplayName(),
                    "<owner_name>", ownerName,
                    "<access_mode>", accessMode,
                    "<member_count>", String.valueOf(customChan.getMembers().size()),
                    "<owner_tip>", ownerTip
            );
            inv.setItem(infoSlot, GUIManager.createItem(config, "channel-info", modeMat, infoReplacements));
        }

        // 成員頭顱清單
        int[] memberSlots = GUIManager.getSlots(config, "slots.member-heads", new int[]{
                9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26,
                27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
        });
        int memberIdx = 0;

        for (UUID memberUuid : customChan.getMembers()) {
            if (memberIdx >= memberSlots.length) break;

            OfflinePlayer offP = Bukkit.getOfflinePlayer(memberUuid);
            boolean memberIsOwner = memberUuid.equals(customChan.getOwner());
            boolean isOnline = offP.isOnline() && offP.getPlayer() != null;

            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            if (meta != null) {
                if (isOnline && offP.getPlayer() != null) {
                    meta.setPlayerProfile(offP.getPlayer().getPlayerProfile());
                } else {
                    meta.setOwningPlayer(offP);
                }
                String pName = offP.getName() != null ? offP.getName() : memberUuid.toString();
                meta.displayName(ChatUtils.parseNoItalic((memberIsOwner ? "<gold><bold>[隊長] " : "<gray>[成員] ") + "<white><bold>" + pName + "</bold>"));

                List<Component> lore = new ArrayList<>();
                lore.add(ChatUtils.parseNoItalic("<gray>職位身份: " + (memberIsOwner ? "<gold><bold>頻道創始隊長</bold></gold>" : "<white>普通成員</white>")));
                lore.add(ChatUtils.parseNoItalic("<gray>連線狀態: " + (isOnline ? "<green>● 線上中</green>" : "<red>○ 離線</red>")));

                if (isOnline && offP.getPlayer() != null) {
                    Player memberOnline = offP.getPlayer();
                    lore.add(ChatUtils.parseNoItalic("<gray>所在世界: <yellow>" + memberOnline.getWorld().getName() + "</yellow>"));
                    String curChan = ChannelManager.getPlayerSelectedKey(memberOnline);
                    boolean isSpeakingHere = curChan.equalsIgnoreCase(customChan.getId());
                    lore.add(ChatUtils.parseNoItalic("<gray>當前頻道: " + (isSpeakingHere ? "<green>✓ 正在本頻道發言</green>" : "<gray>其他頻道 (" + curChan + ")</gray>")));
                    lore.add(ChatUtils.parseNoItalic("<gray>連線延遲: <yellow>" + memberOnline.getPing() + " ms</yellow>"));
                } else {
                    long lastPlayed = offP.getLastPlayed();
                    if (lastPlayed > 0) {
                        lore.add(ChatUtils.parseNoItalic("<gray>最後上線: <yellow>" + DATE_FORMAT.format(new Date(lastPlayed)) + "</yellow>"));
                    }
                }

                lore.add(ChatUtils.parseNoItalic("<gray>UUID: <dark_gray>" + memberUuid.toString().substring(0, Math.min(18, memberUuid.toString().length())) + "...</dark_gray>"));

                if (isOwner && !memberIsOwner) {
                    lore.add(ChatUtils.parseNoItalic(""));
                    lore.add(ChatUtils.parseNoItalic("<yellow>▶ 左鍵點擊: 踢出該成員</yellow>"));
                    lore.add(ChatUtils.parseNoItalic("<gold>▶ 右鍵點擊: 轉讓隊長給該成員</gold>"));
                }
                meta.lore(lore);
                head.setItemMeta(meta);
            }
            int slot = memberSlots[memberIdx++];
            if (slot < size) inv.setItem(slot, head);
        }

        // 底部功能按鈕
        int backSlot = GUIManager.getSlot(config, "back-button", 45);
        if (backSlot < size) {
            inv.setItem(backSlot, GUIManager.createItem(config, "back-button", Material.ARROW, null));
        }

        if (isOwner) {
            int inviteSlot = GUIManager.getSlot(config, "invite-players", 49);
            if (inviteSlot < size) {
                inv.setItem(inviteSlot, GUIManager.createItem(config, "invite-players", Material.WRITABLE_BOOK, null));
            }
        }

        if (!isOwner) {
            int leaveSlot = GUIManager.getSlot(config, "leave-channel", 51);
            if (leaveSlot < size) {
                inv.setItem(leaveSlot, GUIManager.createItem(config, "leave-channel", Material.OAK_DOOR, null));
            }
        }

        if (isOwner) {
            int disbandSlot = GUIManager.getSlot(config, "disband-channel", 53);
            if (disbandSlot < size) {
                inv.setItem(disbandSlot, GUIManager.createItem(config, "disband-channel", Material.TNT, null));
            }
        }

        player.openInventory(inv);
    }
}
