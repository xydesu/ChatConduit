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
 * 好友關係 DAO (Data Access Object)
 *
 * @author xydesu
 */
public class FriendDAO {

    /**
     * 建立雙向好友關係
     *
     * @param playerUuid 玩家 UUID
     * @param friendUuid 好友 UUID
     * @return boolean 是否成功新增
     */
    public static boolean addFriend(UUID playerUuid, UUID friendUuid) {
        boolean isMySQL = "mysql".equals(DatabaseManager.getDbType());
        String sql;
        if (isMySQL) {
            sql = "INSERT IGNORE INTO chatconduit_friends (player_uuid, friend_uuid) VALUES (?, ?), (?, ?);";
        } else {
            sql = "INSERT OR IGNORE INTO chatconduit_friends (player_uuid, friend_uuid) VALUES (?, ?), (?, ?);";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, friendUuid.toString());
            ps.setString(3, friendUuid.toString());
            ps.setString(4, playerUuid.toString());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "建立好友關係時失敗 (" + playerUuid + " <-> " + friendUuid + "):", e);
            return false;
        }
    }

    /**
     * 解除雙向好友關係
     *
     * @param playerUuid 玩家 UUID
     * @param friendUuid 好友 UUID
     * @return boolean 是否成功解除
     */
    public static boolean removeFriend(UUID playerUuid, UUID friendUuid) {
        String sql = "DELETE FROM chatconduit_friends WHERE (player_uuid = ? AND friend_uuid = ?) OR (player_uuid = ? AND friend_uuid = ?);";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, friendUuid.toString());
            ps.setString(3, friendUuid.toString());
            ps.setString(4, playerUuid.toString());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "解除好友關係時失敗 (" + playerUuid + " <-> " + friendUuid + "):", e);
            return false;
        }
    }

    /**
     * 查詢玩家的所有好友 UUID 清單
     *
     * @param playerUuid 玩家 UUID
     * @return Set<UUID> 好友 UUID 集合
     */
    public static Set<UUID> getFriends(UUID playerUuid) {
        Set<UUID> friends = new HashSet<>();
        String sql = "SELECT friend_uuid FROM chatconduit_friends WHERE player_uuid = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        friends.add(UUID.fromString(rs.getString("friend_uuid")));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "查詢玩家好友列表時失敗 (" + playerUuid + "):", e);
        }
        return friends;
    }

    /**
     * 判斷兩名玩家是否為好友
     *
     * @param playerUuid 玩家 UUID
     * @param friendUuid 目標 UUID
     * @return boolean 是否為好友
     */
    public static boolean isFriend(UUID playerUuid, UUID friendUuid) {
        String sql = "SELECT 1 FROM chatconduit_friends WHERE player_uuid = ? AND friend_uuid = ? LIMIT 1;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, friendUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "檢查好友狀態時失敗 (" + playerUuid + " <-> " + friendUuid + "):", e);
        }
        return false;
    }
}
