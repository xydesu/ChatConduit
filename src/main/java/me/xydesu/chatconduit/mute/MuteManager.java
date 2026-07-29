package me.xydesu.chatconduit.mute;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.database.DatabaseManager;
import me.xydesu.chatconduit.redis.MutePacket;
import me.xydesu.chatconduit.redis.RedisManager;
import org.bukkit.Bukkit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 禁言管理器，負責本機快取、資料庫持久化與 Redis 跨服同步
 * 
 * @author xydesu
 */
public class MuteManager {

    private static final Map<UUID, MuteEntry> activeMutes = new ConcurrentHashMap<>();

    /**
     * 禁言條目資料結構
     */
    public record MuteEntry(UUID uuid, String playerName, String reason, long mutedAt, long expireAt, String mutedBy) {
        public boolean isPermanent() {
            return expireAt <= 0;
        }

        public boolean isExpired() {
            return !isPermanent() && System.currentTimeMillis() >= expireAt;
        }

        public long getRemainingMillis() {
            if (isPermanent()) return -1;
            return Math.max(0, expireAt - System.currentTimeMillis());
        }
    }

    /**
     * 初始化並非同步從資料庫載入現有禁言清單
     */
    public static void init() {
        activeMutes.clear();
        Bukkit.getAsyncScheduler().runNow(Main.getInstance(), task -> loadMutesFromDatabase());
    }

    /**
     * 從資料庫讀取尚未過期的禁言記錄
     */
    private static void loadMutesFromDatabase() {
        String sql = "SELECT uuid, player_name, reason, muted_at, expire_at, muted_by FROM chatconduit_mutes;";
        long now = System.currentTimeMillis();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String playerName = rs.getString("player_name");
                String reason = rs.getString("reason");
                long mutedAt = rs.getLong("muted_at");
                long expireAt = rs.getLong("expire_at");
                String mutedBy = rs.getString("muted_by");

                MuteEntry entry = new MuteEntry(uuid, playerName, reason, mutedAt, expireAt, mutedBy);
                if (!entry.isExpired()) {
                    activeMutes.put(uuid, entry);
                } else {
                    // 已過期，非同步自資料庫清理
                    deleteMuteFromDb(uuid);
                }
            }
            Main.getInstance().getLogger().info("成功從資料庫載入 " + activeMutes.size() + " 筆有效禁言記錄！");
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "載入禁言資料庫失敗:", e);
        }
    }

    /**
     * 查詢玩家禁言狀態，若已過期自動移除
     */
    public static MuteEntry getMute(UUID uuid) {
        if (uuid == null) return null;
        MuteEntry entry = activeMutes.get(uuid);
        if (entry == null) return null;

        if (entry.isExpired()) {
            activeMutes.remove(uuid);
            Bukkit.getAsyncScheduler().runNow(Main.getInstance(), task -> deleteMuteFromDb(uuid));
            return null;
        }

        return entry;
    }

    /**
     * 檢查玩家是否正處於禁言狀態
     */
    public static boolean isMuted(UUID uuid) {
        return getMute(uuid) != null;
    }

    /**
     * 執行禁言操作
     *
     * @param uuid 玩家 UUID
     * @param playerName 玩家名稱
     * @param reason 禁言原因
     * @param expireAt 到期時間戳 (-1 代表永久)
     * @param mutedBy 執行者名稱
     */
    public static void mutePlayer(UUID uuid, String playerName, String reason, long expireAt, String mutedBy) {
        long mutedAt = System.currentTimeMillis();
        MuteEntry entry = new MuteEntry(uuid, playerName, reason, mutedAt, expireAt, mutedBy);

        activeMutes.put(uuid, entry);

        // 本地全服廣播禁言訊息
        broadcastMuteAnnouncement(entry, false);

        // 寫入資料庫 (非同步)
        Bukkit.getAsyncScheduler().runNow(Main.getInstance(), task -> saveMuteToDb(entry));

        // 發送 Redis 跨服廣播
        if (RedisManager.isEnabled()) {
            MutePacket packet = new MutePacket(
                    MutePacket.Action.MUTE,
                    uuid.toString(),
                    playerName,
                    reason,
                    mutedAt,
                    expireAt,
                    mutedBy,
                    RedisManager.getServerId()
            );
            RedisManager.publishMutePacket(packet);
        }
    }

    /**
     * 執行解禁操作
     */
    public static boolean unmutePlayer(UUID uuid, String unmutedBy) {
        MuteEntry entry = activeMutes.remove(uuid);

        String playerName = entry != null ? entry.playerName() : "Unknown";
        if (entry != null) {
            broadcastUnmuteAnnouncement(playerName, unmutedBy, false);
        }

        // 自資料庫刪除 (非同步)
        Bukkit.getAsyncScheduler().runNow(Main.getInstance(), task -> deleteMuteFromDb(uuid));

        // 發送 Redis 跨服廣播
        if (RedisManager.isEnabled()) {
            MutePacket packet = new MutePacket(
                    MutePacket.Action.UNMUTE,
                    uuid.toString(),
                    playerName,
                    "",
                    System.currentTimeMillis(),
                    0,
                    unmutedBy,
                    RedisManager.getServerId()
            );
            RedisManager.publishMutePacket(packet);
        }

        return entry != null;
    }

    /**
     * 處理來自 Redis 的遠端禁言/解禁封包
     */
    public static void handleRemoteMutePacket(MutePacket packet) {
        if (packet == null || packet.uuid() == null) return;

        try {
            UUID uuid = UUID.fromString(packet.uuid());
            if (packet.action() == MutePacket.Action.MUTE) {
                MuteEntry entry = new MuteEntry(
                        uuid,
                        packet.playerName(),
                        packet.reason(),
                        packet.mutedAt(),
                        packet.expireAt(),
                        packet.mutedBy()
                );
                if (!entry.isExpired()) {
                    activeMutes.put(uuid, entry);
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> broadcastMuteAnnouncement(entry, true));
                }
            } else if (packet.action() == MutePacket.Action.UNMUTE) {
                activeMutes.remove(uuid);
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> broadcastUnmuteAnnouncement(packet.playerName(), packet.mutedBy(), true));
            }
        } catch (IllegalArgumentException e) {
            Main.getInstance().getLogger().warning("無效的 MutePacket UUID: " + packet.uuid());
        }
    }

    /**
     * 全服廣播禁言公告訊息
     */
    public static void broadcastMuteAnnouncement(MuteEntry entry, boolean isRemote) {
        if (entry == null) return;

        String reason = entry.reason() != null && !entry.reason().isEmpty() ? entry.reason() : Main.getInstance().getLanguageConfig().getString("mute.default-reason", "No reason provided");
        String durationStr = me.xydesu.chatconduit.command.MuteCommand.formatDuration(entry.getRemainingMillis());
        String mutedBy = entry.mutedBy() != null ? entry.mutedBy() : "Console";

        String template;
        if (entry.isPermanent()) {
            template = Main.getInstance().getLanguageConfig().getString(
                    "mute.broadcast-perm",
                    "<gradient:#ff416c:#ff4b2b>[系統公告]</gradient> <red>玩家 <yellow><player></yellow> 已被 <yellow><by></yellow> 永久禁言！原因: <gray><reason></gray>"
            );
        } else {
            template = Main.getInstance().getLanguageConfig().getString(
                    "mute.broadcast",
                    "<gradient:#ff416c:#ff4b2b>[系統公告]</gradient> <red>玩家 <yellow><player></yellow> 已被 <yellow><by></yellow> 禁言 <yellow><time></yellow>！原因: <gray><reason></gray>"
            );
        }

        String broadcastMsg = template.replace("<player>", entry.playerName())
                .replace("<time>", durationStr)
                .replace("<reason>", reason)
                .replace("<by>", mutedBy);

        for (org.bukkit.entity.Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            me.xydesu.chatconduit.util.ChatUtils.sendMessage(onlinePlayer, broadcastMsg);
        }
        Bukkit.getConsoleSender().sendMessage(me.xydesu.chatconduit.util.ChatUtils.parseLegacy(broadcastMsg));
    }

    /**
     * 全服廣播解禁公告訊息
     */
    public static void broadcastUnmuteAnnouncement(String playerName, String unmutedBy, boolean isRemote) {
        String template = Main.getInstance().getLanguageConfig().getString(
                "mute.unmute-broadcast",
                "<gradient:#55ffff:#00aa00>[系統公告]</gradient> <green>玩家 <yellow><player></yellow> 的禁言已被 <yellow><by></yellow> 解除。"
        );
        String broadcastMsg = template.replace("<player>", playerName).replace("<by>", unmutedBy != null ? unmutedBy : "Console");

        for (org.bukkit.entity.Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            me.xydesu.chatconduit.util.ChatUtils.sendMessage(onlinePlayer, broadcastMsg);
        }
        Bukkit.getConsoleSender().sendMessage(me.xydesu.chatconduit.util.ChatUtils.parseLegacy(broadcastMsg));
    }

    /**
     * 取得所有當前有效的禁言清單
     */
    public static List<MuteEntry> getAllActiveMutes() {
        List<MuteEntry> list = new ArrayList<>();
        Iterator<Map.Entry<UUID, MuteEntry>> iterator = activeMutes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, MuteEntry> mapEntry = iterator.next();
            MuteEntry entry = mapEntry.getValue();
            if (entry.isExpired()) {
                iterator.remove();
                Bukkit.getAsyncScheduler().runNow(Main.getInstance(), task -> deleteMuteFromDb(entry.uuid()));
            } else {
                list.add(entry);
            }
        }
        return list;
    }

    /**
     * 寫入或更新禁言資料至資料庫
     */
    private static void saveMuteToDb(MuteEntry entry) {
        boolean isMySQL = "mysql".equals(DatabaseManager.getDbType());
        String sql;

        if (isMySQL) {
            sql = "REPLACE INTO chatconduit_mutes (uuid, player_name, reason, muted_at, expire_at, muted_by) VALUES (?, ?, ?, ?, ?, ?);";
        } else {
            sql = "INSERT OR REPLACE INTO chatconduit_mutes (uuid, player_name, reason, muted_at, expire_at, muted_by) VALUES (?, ?, ?, ?, ?, ?);";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, entry.uuid().toString());
            ps.setString(2, entry.playerName());
            ps.setString(3, entry.reason() != null ? entry.reason() : "");
            ps.setLong(4, entry.mutedAt());
            ps.setLong(5, entry.expireAt());
            ps.setString(6, entry.mutedBy() != null ? entry.mutedBy() : "Console");

            ps.executeUpdate();
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "儲存禁言資料至資料庫失敗:", e);
        }
    }

    /**
     * 自資料庫刪除指定 UUID 的禁言記錄
     */
    private static void deleteMuteFromDb(UUID uuid) {
        String sql = "DELETE FROM chatconduit_mutes WHERE uuid = ?;";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "自資料庫刪除禁言資料失敗:", e);
        }
    }
}
