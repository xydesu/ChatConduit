package me.xydesu.chatconduit.chatcolor;

import me.xydesu.chatconduit.Main;
import me.xydesu.chatconduit.database.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * 玩家預設聊天顏色對話格式管理類別
 * 支援全套 Legacy 色碼 (&a, &c, &6...) 與 MiniMessage/Hex 色碼 (<#FF5555>)
 *
 * @author xydesu
 */
public class ChatColorManager {

    private static final Map<UUID, String> PLAYER_COLOR_CACHE = new ConcurrentHashMap<>();

    /**
     * 初始化資料庫資料表並預載所有玩家聊天顏色紀錄
     */
    public static void init() {
        PLAYER_COLOR_CACHE.clear();
        String sql = "SELECT uuid, color FROM chatconduit_player_colors";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    UUID uuid = UUID.fromString(rs.getString("uuid"));
                    String color = rs.getString("color");
                    if (color != null && !color.trim().isEmpty()) {
                        PLAYER_COLOR_CACHE.put(uuid, color.trim());
                    }
                } catch (Exception ignored) {}
            }
            Main.getInstance().getLogger().info("[ChatColor] 已成功讀取 " + PLAYER_COLOR_CACHE.size() + " 筆玩家聊天顏色設定！");
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "[ChatColor] 載入玩家聊天顏色設定失敗:", e);
        }
    }

    /**
     * 獲取玩家目前的聊天顏色設定
     *
     * @param uuid 玩家 UUID
     * @return 顏色代碼 (如 "&a", "<#FF5555>")，若未設定則傳回 null
     */
    public static String getChatColor(UUID uuid) {
        if (uuid == null) return null;
        return PLAYER_COLOR_CACHE.get(uuid);
    }

    /**
     * 設定並儲存玩家的聊天顏色
     *
     * @param uuid  玩家 UUID
     * @param color 顏色代碼，若為 null 或空值則代表清除重置
     */
    public static void setChatColor(UUID uuid, String color) {
        if (uuid == null) return;

        if (color == null || color.trim().isEmpty()) {
            removeChatColor(uuid);
            return;
        }

        String formattedColor = color.trim();
        PLAYER_COLOR_CACHE.put(uuid, formattedColor);

        boolean isMySQL = "mysql".equals(DatabaseManager.getDbType());
        String sql;
        if (isMySQL) {
            sql = "INSERT INTO chatconduit_player_colors (uuid, color) VALUES (?, ?) "
                + "ON DUPLICATE KEY UPDATE color = VALUES(color);";
        } else {
            sql = "INSERT OR REPLACE INTO chatconduit_player_colors (uuid, color) VALUES (?, ?);";
        }

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.setString(2, formattedColor);
            ps.executeUpdate();
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "[ChatColor] 儲存玩家聊天顏色至資料庫失敗: " + uuid, e);
        }
    }

    /**
     * 重置並移除玩家的聊天顏色設定
     *
     * @param uuid 玩家 UUID
     */
    public static void removeChatColor(UUID uuid) {
        if (uuid == null) return;
        PLAYER_COLOR_CACHE.remove(uuid);

        String sql = "DELETE FROM chatconduit_player_colors WHERE uuid = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "[ChatColor] 從資料庫移除玩家聊天顏色失敗: " + uuid, e);
        }
    }
}
