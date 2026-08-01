package me.xydesu.chatconduit.listener;

import me.xydesu.chatconduit.channel.ChannelManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.BroadcastMessageEvent;

public class PlayerListener implements Listener {

    private static volatile long lastDeathTime = 0;
    private static volatile long lastJoinQuitTime = 0;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        lastJoinQuitTime = System.currentTimeMillis();
        // 最優先非同步加載玩家頻道與訊息偏好設定，避免主執行緒阻塞
        ChannelManager.loadPlayerDataAsync(player);
        me.xydesu.chatconduit.friend.FriendManager.getInstance().loadPlayerDataAsync(player.getUniqueId());
        // 註冊玩家線上狀態至 Redis 快取
        me.xydesu.chatconduit.redis.RedisPlayerRegistry.registerPlayer(player, me.xydesu.chatconduit.redis.RedisManager.getServerId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        lastJoinQuitTime = System.currentTimeMillis();
        me.xydesu.chatconduit.gui.PlayerInputManager.clearPendingInput(event.getPlayer().getUniqueId());
        me.xydesu.chatconduit.message.PrivateMessageManager.removeReplyTarget(event.getPlayer().getUniqueId());
        // 從 Redis 快取中移除玩家
        me.xydesu.chatconduit.redis.RedisPlayerRegistry.unregisterPlayer(event.getPlayer());
        // 玩家離線時非同步寫入保存，並從記憶體 Map 卸載資料防止洩漏
        ChannelManager.savePlayerData(event.getPlayer().getUniqueId());
        ChannelManager.unloadPlayerData(event.getPlayer().getUniqueId());
        me.xydesu.chatconduit.friend.FriendManager.getInstance().unloadPlayerData(event.getPlayer().getUniqueId());
        me.xydesu.chatconduit.integration.CMIHook.removePlayer(event.getPlayer().getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        lastDeathTime = System.currentTimeMillis();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBroadcastMessage(BroadcastMessageEvent event) {
        if (event.getRecipients().isEmpty()) return;

        String plainText = PlainTextComponentSerializer.plainText().serialize(event.message()).toLowerCase();
        long now = System.currentTimeMillis();

        boolean isDeath = (now - lastDeathTime < 1000) || plainText.contains("☠") || plainText.contains("fell from") || plainText.contains("slain") || plainText.contains("died") || plainText.contains("killed");
        boolean isJoinQuit = (now - lastJoinQuitTime < 1000) || plainText.contains("joined") || plainText.contains("left") || plainText.contains("加入了遊戲") || plainText.contains("離開了遊戲");

        if (isDeath) {
            event.getRecipients().removeIf(sender -> sender instanceof Player p && !ChannelManager.isDeathMessagesEnabled(p));
        } else if (isJoinQuit) {
            event.getRecipients().removeIf(sender -> sender instanceof Player p && !ChannelManager.isJoinMessagesEnabled(p));
        }
    }
}

