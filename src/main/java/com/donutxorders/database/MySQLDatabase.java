package com.donutxorders.database;

import com.donutxorders.core.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * MySQL implementation of DatabaseManager for DonutxOrders.
 */
public class MySQLDatabase extends DatabaseManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private HikariDataSource dataSource;

    public MySQLDatabase(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public void connect() {
        try {
            HikariConfig hikariConfig = new HikariConfig();
            String host = configManager.getString("database.mysql.host", "localhost");
            int port = configManager.getInt("database.mysql.port", 3306);
            String database = configManager.getString("database.mysql.database", "donutxorders");
            String username = configManager.getString("database.mysql.username", "root");
            String password = configManager.getString("database.mysql.password", "password");
            boolean useSSL = configManager.getBoolean("database.mysql.ssl", false);

            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + useSSL + "&autoReconnect=true&characterEncoding=utf8");
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);

            hikariConfig.setMaximumPoolSize(configManager.getInt("database.connection-pool.maximum-pool-size", 10));
            hikariConfig.setMinimumIdle(configManager.getInt("database.connection-pool.minimum-idle", 2));
            hikariConfig.setConnectionTimeout(configManager.getInt("database.connection-pool.connection-timeout", 30000));
            hikariConfig.setIdleTimeout(configManager.getInt("database.connection-pool.idle-timeout", 600000));
            hikariConfig.setMaxLifetime(configManager.getInt("database.connection-pool.max-lifetime", 1800000));

            dataSource = new HikariDataSource(hikariConfig);
            plugin.getLogger().info("Connected to MySQL database.");
            createTables();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to MySQL database.", e);
        }
    }

    @Override
    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Disconnected from MySQL database.");
        }
    }

    @Override
    public boolean isConnected() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error checking MySQL connection state.", e);
            return false;
        }
    }

    @Override
    public void createTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Orders table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS orders (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "player_uuid VARCHAR(36) NOT NULL," +
                "status VARCHAR(32) NOT NULL," +
                "created_at BIGINT NOT NULL," +
                "expires_at BIGINT," +
                "world VARCHAR(64)," +
                "x DOUBLE," +
                "y DOUBLE," +
                "z DOUBLE," +
                "fee DOUBLE DEFAULT 0.0," +
                "quantity INT NOT NULL," +
                "price_per_item DOUBLE NOT NULL," +
                "delivered_amount INT DEFAULT 0" +
                ") ENGINE=InnoDB;"
            );
            // Order items table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS order_items (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "order_id INT NOT NULL," +
                "item_type VARCHAR(64) NOT NULL," +
                "amount INT NOT NULL," +
                "meta TEXT," +
                "deliverer_uuid VARCHAR(36)," +
                "delivered_at BIGINT," +
                "payment_amount DOUBLE DEFAULT 0.0," +
                "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB;"
            );
            // Player data table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_data (" +
                "player_uuid VARCHAR(36) PRIMARY KEY," +
                "orders_created INT DEFAULT 0," +
                "orders_completed INT DEFAULT 0," +
                "orders_delivered INT DEFAULT 0," +
                "money_spent DOUBLE DEFAULT 0.0," +
                "money_earned DOUBLE DEFAULT 0.0" +
                ") ENGINE=InnoDB;"
            );
            // Deliveries table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS deliveries (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "order_id INT NOT NULL," +
                "deliverer_uuid VARCHAR(36) NOT NULL," +
                "delivered_at BIGINT," +
                "reward DOUBLE DEFAULT 0.0," +
                "FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB;"
            );
            plugin.getLogger().info("MySQL tables created or verified.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create MySQL tables.", e);
        }
    }

    @Override
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            connect();
        }
        return dataSource.getConnection();
    }

    // Example: Insert order (implement other CRUD as needed)
    @Override
    public int insertOrder(UUID playerUuid, String status, long createdAt, Long expiresAt, String world, double x, double y, double z, double fee) {
        String sql = "INSERT INTO orders (player_uuid, status, created_at, expires_at, world, x, y, z, fee) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, status);
            ps.setLong(3, createdAt);
            if (expiresAt != null) {
                ps.setLong(4, expiresAt);
            } else {
                ps.setNull(4, Types.BIGINT);
            }
            ps.setString(5, world);
            ps.setDouble(6, x);
            ps.setDouble(7, y);
            ps.setDouble(8, z);
            ps.setDouble(9, fee);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to insert order into MySQL.", e);
        }
        return -1;
    }

    // Example: Get all orders (implement other queries as needed)
    @Override
    public List<Integer> getAllOrderIds() {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM orders";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to fetch order IDs from MySQL.", e);
        }
        return ids;
    }

    // CRUD for OrderItem
    public void insertOrderItem(int orderId, String itemType, int amount, String meta, UUID delivererUuid, long deliveredAt, double paymentAmount) {
        String sql = "INSERT INTO order_items (order_id, item_type, amount, meta, deliverer_uuid, delivered_at, payment_amount) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, itemType);
            ps.setInt(3, amount);
            ps.setString(4, meta);
            ps.setString(5, delivererUuid != null ? delivererUuid.toString() : null);
            ps.setLong(6, deliveredAt);
            ps.setDouble(7, paymentAmount);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to insert order item into MySQL.", e);
        }
    }

    public void updateOrderItem(int id, int orderId, String itemType, int amount, String meta, UUID delivererUuid, long deliveredAt, double paymentAmount) {
        String sql = "UPDATE order_items SET order_id = ?, item_type = ?, amount = ?, meta = ?, deliverer_uuid = ?, delivered_at = ?, payment_amount = ? WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setString(2, itemType);
            ps.setInt(3, amount);
            ps.setString(4, meta);
            ps.setString(5, delivererUuid != null ? delivererUuid.toString() : null);
            ps.setLong(6, deliveredAt);
            ps.setDouble(7, paymentAmount);
            ps.setInt(8, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to update order item in MySQL.", e);
        }
    }

    public void deleteOrderItem(int id) {
        String sql = "DELETE FROM order_items WHERE id = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete order item from MySQL.", e);
        }
    }

    public ResultSet getOrderItem(int id) {
        String sql = "SELECT * FROM order_items WHERE id = ?";
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, id);
            return ps.executeQuery();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to fetch order item from MySQL.", e);
        }
        return null;
    }

    // CRUD for PlayerData
    public void insertOrUpdatePlayerData(UUID playerUuid, int ordersCreated, int ordersCompleted, int ordersDelivered, double moneySpent, double moneyEarned) {
        String sql = "REPLACE INTO player_data (player_uuid, orders_created, orders_completed, orders_delivered, money_spent, money_earned) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, ordersCreated);
            ps.setInt(3, ordersCompleted);
            ps.setInt(4, ordersDelivered);
            ps.setDouble(5, moneySpent);
            ps.setDouble(6, moneyEarned);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to insert/update player data in MySQL.", e);
        }
    }

    public void deletePlayerData(UUID playerUuid) {
        String sql = "DELETE FROM player_data WHERE player_uuid = ?";
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete player data in MySQL.", e);
        }
    }

    public ResultSet getPlayerData(UUID playerUuid) {
        String sql = "SELECT * FROM player_data WHERE player_uuid = ?";
        try {
            Connection conn = getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, playerUuid.toString());
            return ps.executeQuery();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to fetch player data from MySQL.", e);
        }
        return null;
    }

    // Implement other abstract methods from DatabaseManager as needed, following the same pattern.
    // Each method should use MySQL syntax, handle exceptions, and
}