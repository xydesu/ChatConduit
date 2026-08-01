package me.xydesu.chatconduit.database.dao;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.database.DatabaseManager;
import me.xydesu.chatconduit.friend.model.PlayerSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

/**
 * 玩家偏好設定 DAO (Data Access Object)
 *
 * @author xydesu
 */
public class PlayerSettingsDAO {

    /**
     * 讀取玩家偏好設定（若不存在則自動回傳預設設定）
     *
     * @param uuid 玩家 UUID
     * @return PlayerSettings 玩家偏好設定模型
     */
    public static PlayerSettings getSettings(UUID uuid) {
        String sql = "SELECT allow_friend_requests, allow_teleport, allow_private_messages FROM chatconduit_player_settings WHERE uuid = ?;";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    boolean allowRequests = rs.getInt("allow_friend_requests") != 0;
                    boolean allowTeleport = rs.getInt("allow_teleport") != 0;
                    boolean allowPm = rs.getInt("allow_private_messages") != 0;
                    return new PlayerSettings(uuid, allowRequests, allowTeleport, allowPm);
                }
            }
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "讀取玩家偏好設定時失敗 (" + uuid + "):", e);
        }
        return PlayerSettings.createDefault(uuid);
    }

    /**
     * 儲存玩家偏好設定至資料庫
     *
     * @param settings 玩家偏好設定模型
     * @return boolean 是否成功儲存
     */
    public static boolean saveSettings(PlayerSettings settings) {
        boolean isMySQL = "mysql".equals(DatabaseManager.getDbType());
        String sql;
        if (isMySQL) {
            sql = "INSERT INTO chatconduit_player_settings (uuid, allow_friend_requests, allow_teleport, allow_private_messages) VALUES (?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE allow_friend_requests = VALUES(allow_friend_requests), allow_teleport = VALUES(allow_teleport), allow_private_messages = VALUES(allow_private_messages);";
        } else {
            sql = "INSERT OR REPLACE INTO chatconduit_player_settings (uuid, allow_friend_requests, allow_teleport, allow_private_messages) VALUES (?, ?, ?, ?);";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, settings.getUuid().toString());
            ps.setInt(2, settings.isAllowFriendRequests() ? 1 : 0);
            ps.setInt(3, settings.isAllowTeleport() ? 1 : 0);
            ps.setInt(4, settings.isAllowPrivateMessages() ? 1 : 0);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "儲存玩家偏好設定時失敗 (" + settings.getUuid() + "):", e);
            return false;
        }
    }
}
