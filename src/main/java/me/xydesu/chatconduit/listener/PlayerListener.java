package me.xydesu.chatconduit.listener;

import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.channel.PlayerChannelManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String currentKey = ChannelManager.getPlayerSelectedKey(player);

        // 安全檢查：若玩家先前選擇的頻道已被刪除或失去權限，自動回退到預設頻道
        PlayerChannelManager.CustomChannel customChan = PlayerChannelManager.getChannel(currentKey);
        ChannelManager.Channel sysChan = ChannelManager.getChannel(currentKey);

        if (customChan != null) {
            if (!customChan.getMembers().contains(player.getUniqueId())) {
                ChannelManager.setPlayerChannel(player, "global");
                String resetMsg = me.xydesu.chatconduit.Main.getInstance().getLanguageConfig().getString(
                        "channel.reverted-on-join",
                        "<gray>提示：您先前所在的群組頻道已離開或解散，已自動為您切換回預設頻道。"
                );
                me.xydesu.chatconduit.util.ChatUtils.sendMessage(player, resetMsg);
            }
        } else if (sysChan != null) {
            if (!sysChan.permission().isEmpty() && !player.hasPermission(sysChan.permission())) {
                ChannelManager.setPlayerChannel(player, "global");
                String resetMsg = me.xydesu.chatconduit.Main.getInstance().getLanguageConfig().getString(
                        "channel.reverted-on-join-no-perm",
                        "<gray>提示：您先前選擇的系統頻道已無存取權限，已自動為您切換回預設頻道。"
                );
                me.xydesu.chatconduit.util.ChatUtils.sendMessage(player, resetMsg);
            }
        } else {
            ChannelManager.setPlayerChannel(player, "global");
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        me.xydesu.chatconduit.gui.PlayerInputManager.clearPendingInput(event.getPlayer().getUniqueId());
        // 玩家離線時非同步寫入保存
        ChannelManager.savePlayerData(event.getPlayer().getUniqueId());
    }
}
