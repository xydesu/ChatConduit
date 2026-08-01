package me.xydesu.chatconduit.database.dao;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 黑名單 DAO (Data Access Object)
 *
 * @author xydesu
 */
public class FriendBlockDAO {

    /**
     * 新增玩家至黑名單
     *
     * @param playerUuid 執行屏蔽的玩家 UUID
     * @param blockedUuid 被屏蔽的玩家 UUID
     * @return boolean 是否成功新增
     */
    public static boolean blockPlayer(UUID playerUuid, UUID blockedUuid) {
        boolean isMySQL = "mysql".equals(DatabaseManager.getDbType());
        String sql;
        if (isMySQL) {
            sql = "INSERT IGNORE INTO chatconduit_friend_blocks (player_uuid, blocked_uuid) VALUES (?, ?);";
        } else {
            sql = "INSERT OR IGNORE INTO chatconduit_friend_blocks (player_uuid, blocked_uuid) VALUES (?, ?);";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, blockedUuid.toString());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "新增至黑名單時失敗 (" + playerUuid + " -> " + blockedUuid + "):", e);
            return false;
        }
    }

    /**
     * 從黑名單中移除玩家
     *
     * @param playerUuid 執行解鎖的玩家 UUID
     * @param blockedUuid 被解鎖的玩家 UUID
     * @return boolean 是否成功解除
     */
    public static boolean unblockPlayer(UUID playerUuid, UUID blockedUuid) {
        String sql = "DELETE FROM chatconduit_friend_blocks WHERE player_uuid = ? AND blocked_uuid = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, blockedUuid.toString());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "從黑名單移除玩家時失敗 (" + playerUuid + " -> " + blockedUuid + "):", e);
            return false;
        }
    }

    /**
     * 查詢玩家的黑名單清單
     *
     * @param playerUuid 玩家 UUID
     * @return Set<UUID> 被屏蔽的玩家 UUID 集合
     */
    public static Set<UUID> getBlockedPlayers(UUID playerUuid) {
        Set<UUID> blocked = new HashSet<>();
        String sql = "SELECT blocked_uuid FROM chatconduit_friend_blocks WHERE player_uuid = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        blocked.add(UUID.fromString(rs.getString("blocked_uuid")));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "查詢黑名單時失敗 (" + playerUuid + "):", e);
        }
        return blocked;
    }

    /**
     * 檢查 playerUuid 是否已屏蔽 blockedUuid
     *
     * @param playerUuid 玩家 UUID
     * @param blockedUuid 被檢查玩家 UUID
     * @return boolean 是否已被屏蔽
     */
    public static boolean isBlocked(UUID playerUuid, UUID blockedUuid) {
        String sql = "SELECT 1 FROM chatconduit_friend_blocks WHERE player_uuid = ? AND blocked_uuid = ? LIMIT 1;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, blockedUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "檢查黑名單狀態時失敗 (" + playerUuid + " -> " + blockedUuid + "):", e);
        }
        return false;
    }
}
