package me.xydesu.chatconduit.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.xydesu.chatconduit.Main;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;

/**
 * 資料庫管理器，負責 HikariCP 連線池管理與資料表初始化
 */
public class DatabaseManager {

    private static HikariDataSource dataSource;
    private static String dbType;

    /**
     * 初始化資料庫連線池與資料表
     */
    public static void init() {
        FileConfiguration config = Main.getInstance().getConfig();
        dbType = config.getString("database.type", "sqlite").toLowerCase();

        HikariConfig hikariConfig = new HikariConfig();

        if ("mysql".equals(dbType)) {
            String host = config.getString("database.mysql.host", "localhost");
            int port = config.getInt("database.mysql.port", 3306);
            String database = config.getString("database.mysql.database", "chatconduit");
            String username = config.getString("database.mysql.username", "root");
            String password = config.getString("database.mysql.password", "");
            int poolSize = config.getInt("database.mysql.pool-size", 10);
            long maxLifetime = config.getLong("database.mysql.max-lifetime", 1800000);
            long connectionTimeout = config.getLong("database.mysql.connection-timeout", 10000);

            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setMaximumPoolSize(poolSize);
            hikariConfig.setMaxLifetime(maxLifetime);
            hikariConfig.setConnectionTimeout(connectionTimeout);
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        } else {
            // 預設為 sqlite
            dbType = "sqlite";
            String fileName = config.getString("database.sqlite-file", "storage.db");
            File dbFile = new File(Main.getInstance().getDataFolder(), fileName);

            hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
            hikariConfig.setMaximumPoolSize(5);
            hikariConfig.setConnectionTestQuery("SELECT 1");
        }

        hikariConfig.setPoolName("ChatConduit-HikariPool");

        try {
            dataSource = new HikariDataSource(hikariConfig);
            Main.getInstance().getLogger().info("資料庫連線池成功啟動！模式: " + dbType.toUpperCase());
            createTables();
        } catch (Exception e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "初始化資料庫時發生嚴重錯誤:", e);
        }
    }

    /**
     * 獲取資料庫連線
     *
     * @return Connection 連線物件
     * @throws SQLException 當連線失敗時拋出例外
     */
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("資料庫連線池尚未初始化或已關閉！");
        }
        return dataSource.getConnection();
    }

    /**
     * 取得當前資料庫類型 ("sqlite" 或 "mysql")
     */
    public static String getDbType() {
        return dbType;
    }

    /**
     * 自動建立必要的資料表
     */
    private static void createTables() {
        boolean isMySQL = "mysql".equals(dbType);

        String createPlayerDataSql;
        String createPlayerChannelsSql;
        String createChannelMembersSql;

        if (isMySQL) {
            createPlayerDataSql = "CREATE TABLE IF NOT EXISTS chatconduit_player_data ("
                    + "uuid VARCHAR(36) PRIMARY KEY, "
                    + "player_name VARCHAR(32) NOT NULL, "
                    + "current_channel VARCHAR(64) NOT NULL, "
                    + "listening_channels TEXT, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

            createPlayerChannelsSql = "CREATE TABLE IF NOT EXISTS chatconduit_player_channels ("
                    + "channel_name VARCHAR(64) PRIMARY KEY, "
                    + "display_name VARCHAR(64) NOT NULL, "
                    + "owner_uuid VARCHAR(36) NOT NULL, "
                    + "password VARCHAR(128) DEFAULT NULL, "
                    + "is_private TINYINT(1) NOT NULL DEFAULT 0, "
                    + "webhook_url VARCHAR(256) DEFAULT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";

            createChannelMembersSql = "CREATE TABLE IF NOT EXISTS chatconduit_channel_members ("
                    + "channel_name VARCHAR(64) NOT NULL, "
                    + "player_uuid VARCHAR(36) NOT NULL, "
                    + "role VARCHAR(16) NOT NULL, "
                    + "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "PRIMARY KEY (channel_name, player_uuid)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;";
        } else {
            createPlayerDataSql = "CREATE TABLE IF NOT EXISTS chatconduit_player_data ("
                    + "uuid TEXT PRIMARY KEY, "
                    + "player_name TEXT NOT NULL, "
                    + "current_channel TEXT NOT NULL, "
                    + "listening_channels TEXT, "
                    + "updated_at DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ");";

            createPlayerChannelsSql = "CREATE TABLE IF NOT EXISTS chatconduit_player_channels ("
                    + "channel_name TEXT PRIMARY KEY, "
                    + "display_name TEXT NOT NULL, "
                    + "owner_uuid TEXT NOT NULL, "
                    + "password TEXT DEFAULT NULL, "
                    + "is_private INTEGER NOT NULL DEFAULT 0, "
                    + "webhook_url TEXT DEFAULT NULL, "
                    + "created_at DATETIME DEFAULT CURRENT_TIMESTAMP"
                    + ");";

            createChannelMembersSql = "CREATE TABLE IF NOT EXISTS chatconduit_channel_members ("
                    + "channel_name TEXT NOT NULL, "
                    + "player_uuid TEXT NOT NULL, "
                    + "role TEXT NOT NULL, "
                    + "joined_at DATETIME DEFAULT CURRENT_TIMESTAMP, "
                    + "PRIMARY KEY (channel_name, player_uuid)"
                    + ");";
        }

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createPlayerDataSql);
            stmt.execute(createPlayerChannelsSql);
            stmt.execute(createChannelMembersSql);
            Main.getInstance().getLogger().info("資料庫資料表結構初始化驗證完成！");
        } catch (SQLException e) {
            Main.getInstance().getLogger().log(Level.SEVERE, "建立資料表結構時失敗:", e);
        }
    }

    /**
     * 關閉資料庫連線池
     */
    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            Main.getInstance().getLogger().info("資料庫連線池已成功關閉。");
        }
    }
}
