package com.donutxorders.database;

import com.donutxorders.core.ConfigManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

/**
 * SQLite implementation of DatabaseManager for DonutxOrders.
 */
public class SQLiteDatabase extends DatabaseManager {

    private final JavaPlugin plugin;
    private final ConfigManager configManager;
    private Connection connection;
    private File dbFile;

    public SQLiteDatabase(JavaPlugin plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public void connect() {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }
            dbFile = new File(plugin.getDataFolder(), "orders.db");
            String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();
            connection = DriverManager.getConnection(url);
            plugin.getLogger().info("Connected to SQLite database.");
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to SQLite database.", e);
        }
    }

    @Override
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                plugin.getLogger().info("Disconnected from SQLite database.");
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error while disconnecting from SQLite database.", e);
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error checking SQLite connection state.", e);
            return false;
        }
    }

    @Override
    public void createTables() {
        try (Statement stmt = connection.createStatement()) {
            // Orders table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS orders (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "player_uuid TEXT NOT NULL," +
                "status TEXT NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "expires_at INTEGER," +
                "world TEXT," +
                "x DOUBLE," +
                "y DOUBLE," +
                "z DOUBLE," +
                "fee DOUBLE DEFAULT 0.0," +
                "quantity INTEGER NOT NULL," +
                "price_per_item DOUBLE NOT NULL," +
                "delivered_amount INTEGER DEFAULT 0" +
                ");"
            );
            // Order items table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS order_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "order_id INTEGER NOT NULL," +
                "item_type TEXT NOT NULL," +
                "amount INTEGER NOT NULL," +
                "meta TEXT," +
                "deliverer_uuid TEXT," +
                "delivered_at INTEGER," +
                "payment_amount DOUBLE DEFAULT 0.0," +
                "FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE" +
                ");"
            );
            // Player data table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS player_data (" +
                "player_uuid TEXT PRIMARY KEY," +
                "orders_created INTEGER DEFAULT 0," +
                "orders_completed INTEGER DEFAULT 0," +
                "orders_delivered INTEGER DEFAULT 0," +
                "money_spent DOUBLE DEFAULT 0.0," +
                "money_earned DOUBLE DEFAULT 0.0" +
                ");"
            );
            // Deliveries table
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS deliveries (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "order_id INTEGER NOT NULL," +
                "deliverer_uuid TEXT NOT NULL," +
                "delivered_at INTEGER," +
                "reward DOUBLE DEFAULT 0.0," +
                "FOREIGN KEY(order_id) REFERENCES orders(id) ON DELETE CASCADE" +
                ");"
            );
            plugin.getLogger().info("SQLite tables created or verified.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create SQLite tables.", e);
        }
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    // Example: Insert order (implement other CRUD as needed)
    @Override
    public int insertOrder(UUID playerUuid, String status, long createdAt, Long expiresAt, String world, double x, double y, double z, double fee) {
        String sql = "INSERT INTO orders (player_uuid, status, created_at, expires_at, world, x, y, z, fee) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, playerUuid.toString());
            ps.setString(2, status);
            ps.setLong(3, createdAt);
            if (expiresAt != null) {
                ps.setLong(4, expiresAt);
            } else {
                ps.setNull(4, Types.INTEGER);
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
            plugin.getLogger().log(Level.SEVERE, "Failed to insert order into SQLite.", e);
        }
        return -1;
    }

    // Example: Get all orders (implement other queries as needed)
    @Override
    public List<Integer> getAllOrderIds() {
        List<Integer> ids = new ArrayList<>();
        String sql = "SELECT id FROM orders";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                ids.add(rs.getInt("id"));
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to fetch order IDs from SQLite.", e);
        }
        return ids;
    }

    // CRUD for PlayerData
    public void insertOrUpdatePlayerData(UUID playerUuid, int ordersCreated, int ordersCompleted, int ordersDelivered, double moneySpent, double moneyEarned) {
        String sql = "REPLACE INTO player_data (player_uuid, orders_created, orders_completed, orders_delivered, money_spent, money_earned) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.setInt(2, ordersCreated);
            ps.setInt(3, ordersCompleted);
            ps.setInt(4, ordersDelivered);
            ps.setDouble(5, moneySpent);
            ps.setDouble(6, moneyEarned);
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to insert/update player data in SQLite.", e);
        }
    }

    public void deletePlayerData(UUID playerUuid) {
        String sql = "DELETE FROM player_data WHERE player_uuid = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, playerUuid.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to delete player data in SQLite.", e);
        }
    }

    public ResultSet getPlayerData(UUID playerUuid) {
        String sql = "SELECT * FROM player_data WHERE player_uuid = ?";
        try {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, playerUuid.toString());
            return ps.executeQuery();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to fetch player data from SQLite.", e);
        }
        return null;
    }

    // Implement other abstract methods from DatabaseManager as needed, following the same pattern.
    // Each method should use SQLite syntax, handle exceptions, and
}