package me.xydesu.chatconduit.database.dao;

import me.xydesu.chatconduit.channel.PlayerChannelManager.CustomChannel;
import me.xydesu.chatconduit.channel.PlayerChannelManager.Mode;
import me.xydesu.chatconduit.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * 玩家自建群組頻道 DAO
 */
public class PlayerChannelDAO {

    /**
     * 從資料庫載入所有玩家自建頻道與成員權限清單
     *
     * @return 頻道 ID 對應 CustomChannel 的 Map
     */
    public static Map<String, CustomChannel> loadAllCustomChannels() {
        Map<String, CustomChannel> channels = new HashMap<>();

        String sqlChannels = "SELECT channel_name, display_name, owner_uuid, is_private, webhook_url FROM chatconduit_player_channels";
        String sqlMembers = "SELECT player_uuid FROM chatconduit_channel_members WHERE channel_name = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement psChan = conn.prepareStatement(sqlChannels);
             ResultSet rsChan = psChan.executeQuery()) {

            while (rsChan.next()) {
                String id = rsChan.getString("channel_name");
                String displayName = rsChan.getString("display_name");
                UUID owner = UUID.fromString(rsChan.getString("owner_uuid"));
                boolean isPrivate = rsChan.getInt("is_private") == 1;
                String webhookUrl = rsChan.getString("webhook_url");

                Mode mode = isPrivate ? Mode.PRIVATE : Mode.PUBLIC;
                CustomChannel channel = new CustomChannel(id, displayName, owner, mode, "<gradient:#a8c0ff:#3f2b96>", webhookUrl, "自訂對話頻道", "遵守社群規範");

                // 載入該頻道的成員清單
                try (PreparedStatement psMem = conn.prepareStatement(sqlMembers)) {
                    psMem.setString(1, id);
                    try (ResultSet rsMem = psMem.executeQuery()) {
                        while (rsMem.next()) {
                            try {
                                UUID memberUuid = UUID.fromString(rsMem.getString("player_uuid"));
                                channel.getMembers().add(memberUuid);
                            } catch (IllegalArgumentException ignored) {}
                        }
                    }
                }

                channels.put(id.toLowerCase(), channel);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return channels;
    }

    /**
     * 儲存或更新自訂頻道
     */
    public static void saveCustomChannel(CustomChannel channel) {
        boolean isMySQL = "mysql".equals(DatabaseManager.getDbType());

        String sqlChannel;
        if (isMySQL) {
            sqlChannel = "INSERT INTO chatconduit_player_channels (channel_name, display_name, owner_uuid, is_private, webhook_url) VALUES (?, ?, ?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), owner_uuid = VALUES(owner_uuid), is_private = VALUES(is_private), webhook_url = VALUES(webhook_url);";
        } else {
            sqlChannel = "INSERT OR REPLACE INTO chatconduit_player_channels (channel_name, display_name, owner_uuid, is_private, webhook_url) VALUES (?, ?, ?, ?, ?);";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sqlChannel)) {
            ps.setString(1, channel.getId().toLowerCase());
            ps.setString(2, channel.getDisplayName());
            ps.setString(3, channel.getOwner().toString());
            ps.setInt(4, channel.getMode() == Mode.PRIVATE ? 1 : 0);
            ps.setString(5, channel.getWebhookUrl());
            ps.executeUpdate();

            // 更新成員表
            saveChannelMembers(conn, channel);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 刪除自訂頻道及其所有成員資料
     */
    public static void deleteCustomChannel(String channelId) {
        String sqlMembers = "DELETE FROM chatconduit_channel_members WHERE channel_name = ?";
        String sqlChannel = "DELETE FROM chatconduit_player_channels WHERE channel_name = ?";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement psMem = conn.prepareStatement(sqlMembers);
             PreparedStatement psChan = conn.prepareStatement(sqlChannel)) {

            psMem.setString(1, channelId.toLowerCase());
            psMem.executeUpdate();

            psChan.setString(1, channelId.toLowerCase());
            psChan.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 寫入頻道成員
     */
    private static void saveChannelMembers(Connection conn, CustomChannel channel) throws SQLException {
        boolean isMySQL = "mysql".equals(DatabaseManager.getDbType());

        String sqlMember;
        if (isMySQL) {
            sqlMember = "INSERT INTO chatconduit_channel_members (channel_name, player_uuid, role) VALUES (?, ?, ?) "
                    + "ON DUPLICATE KEY UPDATE role = VALUES(role);";
        } else {
            sqlMember = "INSERT OR REPLACE INTO chatconduit_channel_members (channel_name, player_uuid, role) VALUES (?, ?, ?);";
        }

        try (PreparedStatement ps = conn.prepareStatement(sqlMember)) {
            for (UUID memberUuid : channel.getMembers()) {
                String role = memberUuid.equals(channel.getOwner()) ? "OWNER" : "MEMBER";
                ps.setString(1, channel.getId().toLowerCase());
                ps.setString(2, memberUuid.toString());
                ps.setString(3, role);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
