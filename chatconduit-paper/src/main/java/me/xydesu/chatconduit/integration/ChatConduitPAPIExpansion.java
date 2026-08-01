package me.xydesu.chatconduit.integration;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.xydesu.chatconduit.channel.ChannelManager;
import me.xydesu.chatconduit.friend.FriendManager;
import me.xydesu.chatconduit.mute.MuteManager;
import me.xydesu.chatconduit.redis.RedisManager;
import me.xydesu.chatconduit.redis.RedisPlayerRegistry;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

/**
 * PlaceholderAPI 變數擴充插件整合
 *
 * @author xydesu
 */
public class ChatConduitPAPIExpansion extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "chatconduit";
    }

    @Override
    public @NotNull String getAuthor() {
        return "xydesu";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        String param = params.toLowerCase();
        FriendManager friendManager = FriendManager.getInstance();

        switch (param) {
            case "friend_count" -> {
                if (friendManager == null) return "0";
                return String.valueOf(friendManager.getFriends(player.getUniqueId()).size());
            }
            case "online_friends" -> {
                if (friendManager == null) return "0";
                Set<UUID> friends = friendManager.getFriends(player.getUniqueId());
                int count = 0;
                for (UUID fUuid : friends) {
                    OfflinePlayer offP = Bukkit.getOfflinePlayer(fUuid);
                    if (offP.isOnline()) {
                        count++;
                    } else if (RedisManager.isEnabled()) {
                        String name = offP.getName();
                        if (name != null && RedisPlayerRegistry.getPlayerData(name) != null) {
                            count++;
                        }
                    }
                }
                return String.valueOf(count);
            }
            case "pending_requests" -> {
                if (friendManager == null) return "0";
                try {
                    return String.valueOf(friendManager.getIncomingRequestsAsync(player.getUniqueId()).join().size());
                } catch (Exception e) {
                    return "0";
                }
            }
            case "channel" -> {
                if (!player.isOnline()) return "";
                return ChannelManager.getPlayerSelectedKey(player.getPlayer());
            }
            case "muted" -> {
                return String.valueOf(MuteManager.isMuted(player.getUniqueId()));
            }
            default -> {
                return null;
            }
        }
    }
}
