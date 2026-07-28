package me.xydesu.chatConduit.Channel;

import me.xydesu.chatConduit.Main;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PlayerChannelManager {

    private static File file;
    private static FileConfiguration config;
    private static final Map<String, CustomChannel> customChannels = new HashMap<>();

    public enum Mode { PUBLIC, PRIVATE }

    public static class CustomChannel {
        private final String id;
        private String displayName;
        private UUID owner;
        private Mode mode;
        private final Set<UUID> members = new HashSet<>();
        private final Set<UUID> pendingInvites = new HashSet<>();

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
            try { file.createNewFile(); } catch (IOException ignored) {}
        }
        config = YamlConfiguration.loadConfiguration(file);

        if (config.contains("channels")) {
            for (String id : config.getConfigurationSection("channels").getKeys(false)) {
                String name = config.getString("channels." + id + ".name", id);
                UUID owner = UUID.fromString(config.getString("channels." + id + ".owner"));
                Mode mode = Mode.valueOf(config.getString("channels." + id + ".mode", "PRIVATE"));

                CustomChannel channel = new CustomChannel(id, name, owner, mode);
                List<String> memberList = config.getStringList("channels." + id + ".members");
                for (String m : memberList) {
                    channel.getMembers().add(UUID.fromString(m));
                }
                customChannels.put(id.toLowerCase(), channel);
            }
        }
    }

    public static void save() {
        config.set("channels", null);
        for (CustomChannel ch : customChannels.values()) {
            String path = "channels." + ch.getId() + ".";
            config.set(path + "name", ch.getDisplayName());
            config.set(path + "owner", ch.getOwner().toString());
            config.set(path + "mode", ch.getMode().name());

            List<String> memberStrings = ch.getMembers().stream().map(UUID::toString).toList();
            config.set(path + "members", memberStrings);
        }
        try { config.save(file); } catch (IOException e) { e.printStackTrace(); }
    }

    public static CustomChannel getChannel(String id) {
        return customChannels.get(id.toLowerCase());
    }

    public static Map<String, CustomChannel> getCustomChannels() {
        return customChannels;
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
     * 解散/刪除頻道
     */
    public static boolean deleteChannel(String id) {
        if (customChannels.containsKey(id.toLowerCase())) {
            customChannels.remove(id.toLowerCase());
            save();
            return true;
        }
        return false;
    }
}