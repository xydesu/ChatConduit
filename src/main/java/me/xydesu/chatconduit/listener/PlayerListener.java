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
        // 非同步加載玩家頻道設定與安全性檢查，避免主執行緒阻塞
        ChannelManager.loadPlayerDataAsync(player);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        me.xydesu.chatconduit.gui.PlayerInputManager.clearPendingInput(event.getPlayer().getUniqueId());
        // 玩家離線時非同步寫入保存，並從記憶體 Map 卸載資料防止洩漏
        ChannelManager.savePlayerData(event.getPlayer().getUniqueId());
        ChannelManager.unloadPlayerData(event.getPlayer().getUniqueId());
    }
}

