package me.xydesu.chatconduit.listener;

import me.xydesu.chatconduit.channel.ChannelManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinDataLoad(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // 最優先非同步加載玩家頻道與訊息偏好設定，避免主執行緒阻塞
        ChannelManager.loadPlayerDataAsync(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoinMessage(PlayerJoinEvent event) {
        Component joinMsg = event.joinMessage();
        if (joinMsg != null) {
            event.joinMessage(null);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (ChannelManager.isJoinMessagesEnabled(p)) {
                    p.sendMessage(joinMsg);
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        me.xydesu.chatconduit.gui.PlayerInputManager.clearPendingInput(event.getPlayer().getUniqueId());

        Component quitMsg = event.quitMessage();
        if (quitMsg != null) {
            event.quitMessage(null);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (ChannelManager.isJoinMessagesEnabled(p)) {
                    p.sendMessage(quitMsg);
                }
            }
        }

        // 玩家離線時非同步寫入保存，並從記憶體 Map 卸載資料防止洩漏
        ChannelManager.savePlayerData(event.getPlayer().getUniqueId());
        ChannelManager.unloadPlayerData(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Component deathMsg = event.deathMessage();
        if (deathMsg != null) {
            event.deathMessage(null);
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (ChannelManager.isDeathMessagesEnabled(p)) {
                    p.sendMessage(deathMsg);
                }
            }
        }
    }
}

