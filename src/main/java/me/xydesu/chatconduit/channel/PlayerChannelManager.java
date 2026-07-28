package me.xydesu.chatconduit.channel;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerChannelManager {

    private static File file;
    private static FileConfiguration config;
    private static final Map<String, CustomChannel> customChannels = new ConcurrentHashMap<>();
    private static final Object FILE_LOCK = new Object();

    public enum Mode { PUBLIC, PRIVATE }

    public static class CustomChannel {
        private final String id;
        private String displayName;
        private UUID owner;
        private Mode mode;
        private final Set<UUID> members = ConcurrentHashMap.newKeySet();
        private final Set<UUID> pendingInvites = ConcurrentHashMap.newKeySet();

        public CustomChannel(String id, String displayName, UUID owner, Mode mode) {
            this.id = id.toLowerCase();
            this.displayName = displayName;
            this.owner = owner;
            this.mode = mode;
            this.members.add(owner);
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public UUID getOwner() { return owner; }
        public void setOwner(UUID owner) { this.owner = owner; }
        public Mode getMode() { return mode; }
        public Set<UUID> getMembers() { return members; }
        public Set<UUID> getPendingInvites() { return pendingInvites; }
    }

    public static void load() {
        customChannels.clear();
        file = new File(Main.getInstance().getDataFolder(), "player-channels.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                Main.getInstance().getLogger().warning("無法建立 player-channels.yml: " + e.getMessage());
            }
        }
        config = YamlConfiguration.loadConfiguration(file);

        if (config.contains("channels")) {
            ConfigurationSection channelsSec = config.getConfigurationSection("channels");
            if (channelsSec != null) {
                for (String id : channelsSec.getKeys(false)) {
                    String name = config.getString("channels." + id + ".name", id);
                    String ownerStr = config.getString("channels." + id + ".owner");
                    if (ownerStr == null) continue;

                    UUID owner = UUID.fromString(ownerStr);
                    Mode mode = Mode.valueOf(config.getString("channels." + id + ".mode", "PRIVATE"));

                    CustomChannel channel = new CustomChannel(id, name, owner, mode);

                    List<String> memberList = config.getStringList("channels." + id + ".members");
                    for (String m : memberList) {
                        try {
                            channel.getMembers().add(UUID.fromString(m));
                        } catch (IllegalArgumentException ignored) {}
                    }

                    List<String> inviteList = config.getStringList("channels." + id + ".pending-invites");
                    for (String inv : inviteList) {
                        try {
                            channel.getPendingInvites().add(UUID.fromString(inv));
                        } catch (IllegalArgumentException ignored) {}
                    }

                    customChannels.put(id.toLowerCase(), channel);
                }
            }
        }
    }

    public static void save() {
        Runnable saveTask = () -> {
            synchronized (FILE_LOCK) {
                config.set("channels", null);
                for (CustomChannel ch : customChannels.values()) {
                    String path = "channels." + ch.getId() + ".";
                    config.set(path + "name", ch.getDisplayName());
                    config.set(path + "owner", ch.getOwner().toString());
                    config.set(path + "mode", ch.getMode().name());

                    List<String> memberStrings = ch.getMembers().stream().map(UUID::toString).toList();
                    config.set(path + "members", memberStrings);

                    List<String> inviteStrings = ch.getPendingInvites().stream().map(UUID::toString).toList();
                    config.set(path + "pending-invites", inviteStrings);
                }
                try {
                    config.save(file);
                } catch (IOException e) {
                    Main.getInstance().getLogger().severe("無法儲存自訂頻道數據: " + e.getMessage());
                }
            }
        };

        if (Main.getInstance().isEnabled()) {
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), saveTask);
        } else {
            saveTask.run();
        }
    }

    public static CustomChannel getChannel(String id) {
        if (id == null) return null;
        return customChannels.get(id.toLowerCase());
    }

    public static Map<String, CustomChannel> getCustomChannels() {
        return Collections.unmodifiableMap(customChannels);
    }

    public static boolean createChannel(String name, Player owner) {
        String id = name.toLowerCase();
        if (customChannels.containsKey(id) || ChannelManager.getChannel(id) != null) {
            return false;
        }
        CustomChannel channel = new CustomChannel(id, name, owner.getUniqueId(), Mode.PRIVATE);
        customChannels.put(id, channel);
        save();
        return true;
    }

    /**
     * 解散/刪除頻道，並將線上停留在該頻道的玩家狀態同步重置為 global
     */
    public static boolean deleteChannel(String id) {
        String targetId = id.toLowerCase();
        CustomChannel removed = customChannels.remove(targetId);
        if (removed != null) {
            save();

            // 線上玩家狀態同步即時修正
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (ChannelManager.getPlayerSelectedKey(player).equalsIgnoreCase(targetId)) {
                    ChannelManager.setPlayerChannel(player, "global");
                    String resetMsg = Main.getInstance().getLanguageConfig().getString(
                            "channel.disbanded-reverted",
                            "<red>Group channel <yellow>" + removed.getDisplayName() + "</yellow> was deleted. Switched back to default channel."
                    );
                    ChatUtils.sendMessage(player, resetMsg);
                }
            }
            return true;
        }
        return false;
    }
}
