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
        private String colorTheme;
        private String webhookUrl;
        private String description;
        private String rules;
        private final Set<UUID> members = ConcurrentHashMap.newKeySet();
        private final Set<UUID> pendingInvites = ConcurrentHashMap.newKeySet();

        public CustomChannel(String id, String displayName, UUID owner, Mode mode, String colorTheme, String webhookUrl, String description, String rules) {
            this.id = id.toLowerCase();
            this.displayName = displayName;
            this.owner = owner;
            this.mode = mode;
            this.colorTheme = colorTheme != null ? colorTheme : "<gradient:#a8c0ff:#3f2b96>";
            this.webhookUrl = webhookUrl;
            this.description = description != null ? description : "尚無頻道簡介說明";
            this.rules = rules != null ? rules : "遵守社群規範，友善交流。";
            this.members.add(owner);
        }

        public CustomChannel(String id, String displayName, UUID owner, Mode mode, String colorTheme) {
            this(id, displayName, owner, mode, colorTheme, null, null, null);
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public UUID getOwner() { return owner; }
        public void setOwner(UUID owner) { this.owner = owner; }
        public Mode getMode() { return mode; }
        public void setMode(Mode mode) { this.mode = mode; }
        public String getColorTheme() { return colorTheme != null ? colorTheme : "<gradient:#a8c0ff:#3f2b96>"; }
        public void setColorTheme(String colorTheme) { this.colorTheme = colorTheme; }
        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
        public String getDescription() { return description != null ? description : "尚無頻道簡介說明"; }
        public void setDescription(String description) { this.description = description; }
        public String getRules() { return rules != null ? rules : "遵守社群規範，友善交流。"; }
        public void setRules(String rules) { this.rules = rules; }
        public Set<UUID> getMembers() { return members; }
        public Set<UUID> getPendingInvites() { return pendingInvites; }
    }

    public static void load() {
        synchronized (FILE_LOCK) {
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
                        String theme = config.getString("channels." + id + ".color-theme", "<gradient:#a8c0ff:#3f2b96>");
                        String webhook = config.getString("channels." + id + ".webhook-url", null);
                        String desc = config.getString("channels." + id + ".description", "尚無頻道簡介說明");
                        String rules = config.getString("channels." + id + ".rules", "遵守社群規範，友善交流。");

                        CustomChannel channel = new CustomChannel(id, name, owner, mode, theme, webhook, desc, rules);

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
    }

    private static volatile boolean isDirty = false;
    private static org.bukkit.scheduler.BukkitTask pendingSaveTask = null;

    public static void save() {
        isDirty = true;
        scheduleDebouncedSave();
    }

    public static void saveImmediately() {
        if (config == null || !isDirty) return;
        synchronized (FILE_LOCK) {
            if (!isDirty) return;
            config.set("channels", null);
            for (CustomChannel ch : customChannels.values()) {
                String path = "channels." + ch.getId() + ".";
                config.set(path + "name", ch.getDisplayName());
                config.set(path + "owner", ch.getOwner().toString());
                config.set(path + "mode", ch.getMode().name());
                config.set(path + "color-theme", ch.getColorTheme());
                config.set(path + "webhook-url", ch.getWebhookUrl());
                config.set(path + "description", ch.getDescription());
                config.set(path + "rules", ch.getRules());

                List<String> memberStrings = ch.getMembers().stream().map(UUID::toString).toList();
                config.set(path + "members", memberStrings);

                List<String> inviteStrings = ch.getPendingInvites().stream().map(UUID::toString).toList();
                config.set(path + "pending-invites", inviteStrings);
            }
            try {
                config.save(file);
                isDirty = false;
            } catch (IOException e) {
                Main.getInstance().getLogger().severe("無法儲存自訂頻道數據: " + e.getMessage());
            }
        }
    }

    private static synchronized void scheduleDebouncedSave() {
        if (!Main.getInstance().isEnabled()) {
            saveImmediately();
            return;
        }
        if (pendingSaveTask == null || pendingSaveTask.isCancelled()) {
            pendingSaveTask = Bukkit.getScheduler().runTaskLaterAsynchronously(Main.getInstance(), () -> {
                saveImmediately();
                pendingSaveTask = null;
            }, 60L); // 3 秒 (60 ticks) 防抖延遲
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
        CustomChannel channel = new CustomChannel(id, name, owner.getUniqueId(), Mode.PRIVATE, "<gradient:#a8c0ff:#3f2b96>");
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

            String resetMsg = Main.getInstance().getLanguageConfig().getString(
                    "channel.disbanded-reverted",
                    "<red>群組頻道 <yellow>" + removed.getDisplayName() + "</yellow> 已被解散/刪除！自動為你切換回預設頻道。"
            );

            // 廣播給所有線上的群組成員並將發言頻道重置為 global
            for (UUID memberUuid : removed.getMembers()) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline()) {
                    if (ChannelManager.getPlayerSelectedKey(member).equalsIgnoreCase(targetId)) {
                        ChannelManager.setPlayerChannel(member, "global");
                    }
                    ChatUtils.sendMessage(member, resetMsg);
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 廣播訊息給群組頻道的線上記錄成員 (可排除指定玩家 UUID)
     */
    public static void broadcastToMembers(CustomChannel customChan, String message, UUID excludeUuid) {
        if (customChan == null || message == null || message.isEmpty()) return;
        for (UUID memberUuid : customChan.getMembers()) {
            if (excludeUuid != null && memberUuid.equals(excludeUuid)) continue;
            Player member = Bukkit.getPlayer(memberUuid);
            if (member != null && member.isOnline()) {
                ChatUtils.sendMessage(member, message);
            }
        }
    }
}
