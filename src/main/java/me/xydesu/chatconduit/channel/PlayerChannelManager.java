package me.xydesu.chatconduit.channel;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.database.dao.PlayerChannelDAO;
import me.xydesu.chatconduit.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerChannelManager {

    private static final Map<String, CustomChannel> customChannels = new ConcurrentHashMap<>();

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

    /**
     * 從資料庫載入自訂群組頻道資料
     */
    public static void load() {
        customChannels.clear();
        Map<String, CustomChannel> dbChannels = PlayerChannelDAO.loadAllCustomChannels();
        customChannels.putAll(dbChannels);
    }

    /**
     * 儲存全部或特定頻道（非同步寫入資料庫）
     */
    public static void save() {
        saveImmediately();
    }

    public static void saveImmediately() {
        for (CustomChannel channel : customChannels.values()) {
            PlayerChannelDAO.saveCustomChannel(channel);
        }
    }

    public static void saveChannel(CustomChannel channel) {
        if (channel == null) return;
        Bukkit.getAsyncScheduler().runNow(Main.getInstance(), task -> {
            PlayerChannelDAO.saveCustomChannel(channel);
        });
    }

    public static CustomChannel getChannel(String id) {
        if (id == null) return null;
        return customChannels.get(id.toLowerCase());
    }

    public static Map<String, CustomChannel> getCustomChannels() {
        return Collections.unmodifiableMap(customChannels);
    }

    private static final Set<String> RESERVED_KEYWORDS = Set.of(
            "cancel", "create", "invite", "accept", "deny", "reject", "leave",
            "members", "manage", "kick", "transfer", "delete", "gui", "help",
            "reload", "global", "clear", "main", "admin"
    );

    public static boolean isReservedKeyword(String name) {
        if (name == null) return true;
        return RESERVED_KEYWORDS.contains(name.toLowerCase().trim());
    }

    public static int getOwnedChannelCount(UUID owner) {
        if (owner == null) return 0;
        int count = 0;
        for (CustomChannel ch : customChannels.values()) {
            if (owner.equals(ch.getOwner())) {
                count++;
            }
        }
        return count;
    }

    public enum CreateResult {
        SUCCESS,
        ALREADY_EXISTS,
        RESERVED_KEYWORD,
        LIMIT_REACHED,
        INVALID_NAME
    }

    public static CreateResult tryCreateChannel(String name, Player owner) {
        if (name == null) return CreateResult.INVALID_NAME;
        String cleanName = name.trim();
        if (cleanName.isEmpty() || cleanName.length() > 20) return CreateResult.INVALID_NAME;
        String id = cleanName.toLowerCase();

        if (isReservedKeyword(id)) return CreateResult.RESERVED_KEYWORD;
        if (customChannels.containsKey(id) || ChannelManager.getChannel(id) != null) return CreateResult.ALREADY_EXISTS;

        int maxAllowed = Main.getInstance().getConfig().getInt("player-channels.max-per-player", 3);
        if (!owner.hasPermission("chatconduit.admin.bypasslimit") && getOwnedChannelCount(owner.getUniqueId()) >= maxAllowed) {
            return CreateResult.LIMIT_REACHED;
        }

        CustomChannel channel = new CustomChannel(id, cleanName, owner.getUniqueId(), Mode.PRIVATE, "<gradient:#a8c0ff:#3f2b96>");
        customChannels.put(id, channel);
        saveChannel(channel);
        return CreateResult.SUCCESS;
    }

    public static boolean createChannel(String name, Player owner) {
        return tryCreateChannel(name, owner) == CreateResult.SUCCESS;
    }

    /**
     * 解散/刪除頻道
     */
    public static boolean deleteChannel(String id) {
        String targetId = id.toLowerCase();
        CustomChannel removed = customChannels.remove(targetId);
        if (removed != null) {
            me.xydesu.chatconduit.integration.WebhookManager.removeCooldown(targetId);

            // 非同步從 DB 刪除
            Bukkit.getAsyncScheduler().runNow(Main.getInstance(), task -> {
                PlayerChannelDAO.deleteCustomChannel(targetId);
            });

            String resetMsg = Main.getInstance().getLanguageConfig().getString(
                    "channel.disbanded-reverted",
                    "<red>群組頻道 <yellow>" + removed.getDisplayName() + "</yellow> 已被解散/刪除！自動為你切換回預設頻道。"
            );

            for (UUID memberUuid : removed.getMembers()) {
                Player member = Bukkit.getPlayer(memberUuid);
                if (member != null && member.isOnline()) {
                    if (ChannelManager.getPlayerSelectedKey(member).equalsIgnoreCase(targetId)) {
                        ChannelManager.setPlayerChannel(member, "global");
                    }
                    ChatUtils.sendMessage(member, resetMsg);
                    member.closeInventory();
                }
            }
            return true;
        }
        return false;
    }

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
