package me.xydesu.chatconduit.database.dao;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.database.DatabaseManager;
import me.xydesu.chatconduit.friend.model.FriendRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 好友申請 DAO (Data Access Object)
 *
 * @author xydesu
 */
public class FriendRequestDAO {

    /**
     * 新增或更新好友申請
     *
     * @param senderUuid 發送者 UUID
     * @param receiverUuid 接收者 UUID
     * @return boolean 是否發送成功
     */
    public static boolean sendRequest(UUID senderUuid, UUID receiverUuid) {
        boolean isMySQL = "mysql".equals(DatabaseManager.getDbType());
        long now = System.currentTimeMillis();

        String sql;
        if (isMySQL) {
            sql = "INSERT INTO chatconduit_friend_requests (sender_uuid, receiver_uuid, created_at) VALUES (?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);";
        } else {
            sql = "INSERT OR REPLACE INTO chatconduit_friend_requests (sender_uuid, receiver_uuid, created_at) VALUES (?, ?, ?);";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, senderUuid.toString());
            ps.setString(2, receiverUuid.toString());
            ps.setLong(3, now);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "寫入好友申請至資料庫時失敗 (" + senderUuid + " -> " + receiverUuid + "):", e);
            return false;
        }
    }

    /**
     * 刪除/撤回好友申請
     *
     * @param senderUuid 發送者 UUID
     * @param receiverUuid 接收者 UUID
     * @return boolean 是否成功刪除
     */
    public static boolean removeRequest(UUID senderUuid, UUID receiverUuid) {
        String sql = "DELETE FROM chatconduit_friend_requests WHERE sender_uuid = ? AND receiver_uuid = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, senderUuid.toString());
            ps.setString(2, receiverUuid.toString());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "從資料庫移除好友申請時失敗 (" + senderUuid + " -> " + receiverUuid + "):", e);
            return false;
        }
    }

    /**
     * 獲取玩家收到的未處理好友申請
     *
     * @param receiverUuid 接收者 UUID
     * @return List<FriendRequest> 好友申請列表
     */
    public static List<FriendRequest> getIncomingRequests(UUID receiverUuid) {
        List<FriendRequest> requests = new ArrayList<>();
        String sql = "SELECT sender_uuid, created_at FROM chatconduit_friend_requests WHERE receiver_uuid = ? ORDER BY created_at DESC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, receiverUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        UUID sender = UUID.fromString(rs.getString("sender_uuid"));
                        long timestamp = rs.getLong("created_at");
                        requests.add(new FriendRequest(sender, receiverUuid, timestamp));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "查詢接收好友申請列表時失敗 (" + receiverUuid + "):", e);
        }
        return requests;
    }

    /**
     * 獲取玩家發出的未處理好友申請
     *
     * @param senderUuid 發送者 UUID
     * @return List<FriendRequest> 好友申請列表
     */
    public static List<FriendRequest> getOutgoingRequests(UUID senderUuid) {
        List<FriendRequest> requests = new ArrayList<>();
        String sql = "SELECT receiver_uuid, created_at FROM chatconduit_friend_requests WHERE sender_uuid = ? ORDER BY created_at DESC;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, senderUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        UUID receiver = UUID.fromString(rs.getString("receiver_uuid"));
                        long timestamp = rs.getLong("created_at");
                        requests.add(new FriendRequest(senderUuid, receiver, timestamp));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "查詢發出好友申請列表時失敗 (" + senderUuid + "):", e);
        }
        return requests;
    }

    /**
     * 檢查是否存在未處理的好友申請
     *
     * @param senderUuid 發送者 UUID
     * @param receiverUuid 接收者 UUID
     * @return boolean 是否存在申請
     */
    public static boolean hasPendingRequest(UUID senderUuid, UUID receiverUuid) {
        String sql = "SELECT 1 FROM chatconduit_friend_requests WHERE sender_uuid = ? AND receiver_uuid = ? LIMIT 1;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, senderUuid.toString());
            ps.setString(2, receiverUuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "檢查好友申請狀態時失敗 (" + senderUuid + " -> " + receiverUuid + "):", e);
        }
        return false;
    }
}
