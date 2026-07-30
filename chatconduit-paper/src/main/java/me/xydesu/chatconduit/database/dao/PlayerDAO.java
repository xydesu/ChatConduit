package me.xydesu.chatconduit.database.dao;

import me.xydesu.chatconduit.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * 玩家個人頻道設定 DAO (Data Access Object)
 */
public class PlayerDAO {

    public record PlayerData(UUID uuid, String playerName, String currentChannel, Set<String> listeningChannels, boolean deathMessagesEnabled, boolean joinMessagesEnabled) {}

    /**
     * 根據玩家 UUID 獲取資料
     *
     * @param uuid 玩家 UUID
     * @return PlayerData 玩家資料，若無紀錄則回傳 null
     */
    public static PlayerData getPlayerData(UUID uuid) {
        String sql = "SELECT player_name, current_channel, listening_channels, death_messages_enabled, join_messages_enabled FROM chatconduit_player_data WHERE uuid = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String name = rs.getString("player_name");
                    String currentChannel = rs.getString("current_channel");
                    String listeningRaw = rs.getString("listening_channels");
                    boolean deathMsg = rs.getInt("death_messages_enabled") != 0;
                    boolean joinMsg = rs.getInt("join_messages_enabled") != 0;

                    Set<String> listeningSet = new HashSet<>();
                    if (listeningRaw != null && !listeningRaw.trim().isEmpty()) {
                        listeningSet.addAll(Arrays.asList(listeningRaw.split(",")));
                    }

                    return new PlayerData(uuid, name, currentChannel, listeningSet, deathMsg, joinMsg);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 儲存或更新玩家資料 (跨 SQLite 與 MySQL 相容之兩段式與語法)
     */
    public static void savePlayerData(UUID uuid, String playerName, String currentChannel, Set<String> listeningChannels, boolean deathMessagesEnabled, boolean joinMessagesEnabled) {
        boolean isMySQL = "mysql".equals(DatabaseManager.getDbType());
        String listeningStr = listeningChannels != null ? String.join(",", listeningChannels) : "";

        String sql;
        if (isMySQL) {
            sql = "INSERT INTO chatconduit_player_data (uuid, player_name, current_channel, listening_channels, death_messages_enabled, join_messages_enabled) VALUES (?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), current_channel = VALUES(current_channel), listening_channels = VALUES(listening_channels), death_messages_enabled = VALUES(death_messages_enabled), join_messages_enabled = VALUES(join_messages_enabled);";
        } else {
            sql = "INSERT OR REPLACE INTO chatconduit_player_data (uuid, player_name, current_channel, listening_channels, death_messages_enabled, join_messages_enabled) VALUES (?, ?, ?, ?, ?, ?);";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, playerName != null ? playerName : "Unknown");
            ps.setString(3, currentChannel);
            ps.setString(4, listeningStr);
            ps.setInt(5, deathMessagesEnabled ? 1 : 0);
            ps.setInt(6, joinMessagesEnabled ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 獲取所有已記錄的玩家資料清單
     *
     * @return PlayerData 的列表
     */
    public static List<PlayerData> getAllKnownPlayers() {
        List<PlayerData> list = new ArrayList<>();
        String sql = "SELECT uuid, player_name, current_channel, listening_channels, death_messages_enabled, join_messages_enabled FROM chatconduit_player_data LIMIT 100";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String name = rs.getString("player_name");
                    String currentChannel = rs.getString("current_channel");
                    String listeningRaw = rs.getString("listening_channels");
                    boolean deathMsg = rs.getInt("death_messages_enabled") != 0;
                    boolean joinMsg = rs.getInt("join_messages_enabled") != 0;

                    Set<String> listeningSet = new HashSet<>();
                    if (listeningRaw != null && !listeningRaw.trim().isEmpty()) {
                        listeningSet.addAll(Arrays.asList(listeningRaw.split(",")));
                    }
                    list.add(new PlayerData(uuid, name, currentChannel, listeningSet, deathMsg, joinMsg));
                } catch (Exception ignored) {}
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}
