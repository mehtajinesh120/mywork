package com.donutxorders.database;

import com.donutxorders.core.DonutxOrders;
import com.donutxorders.models.Order;

import org.bukkit.configuration.file.FileConfiguration;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public abstract class DatabaseManager {
    
    public abstract java.util.List<com.donutxorders.models.Order> getAllOrders();
    public abstract boolean updateOrderSync(com.donutxorders.models.Order order);
    public abstract boolean deleteOrderSync(String orderId);

    protected final DonutxOrders plugin;
    protected final FileConfiguration config;
    
    protected final ConcurrentHashMap<String, PreparedStatement> preparedStatements;
    
    // SQLite database file path
    protected String databaseFilePath;
    
    // Table names
    protected static final String ORDERS_TABLE = "donutx_orders";
    protected static final String ORDER_ITEMS_TABLE = "donutx_order_items";
    protected static final String PLAYER_DATA_TABLE = "donutx_player_data";
    
    /**
     * Factory method to create appropriate database manager
     */
    public static DatabaseManager createDatabaseManager(DonutxOrders plugin) {
        return new SQLiteDatabase(plugin);
    }
    
    protected DatabaseManager(DonutxOrders plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager().getConfig();
        this.preparedStatements = new ConcurrentHashMap<>();
        loadDatabaseSettings();
        // Explicitly load SQLite JDBC driver for both Paper and Spigot
        try {
            Class.forName("org.sqlite.JDBC");
            plugin.getLogger().info("SQLite JDBC driver loaded successfully.");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().severe("SQLite JDBC driver not found! Please ensure it is available on your server. For Spigot, place sqlite-jdbc-3.44.1.0.jar in the /libs or /plugins folder. For Paper, the library should be auto-downloaded.");
        }
    }
    
    /**
     * Load database settings from configuration
     */
    private void loadDatabaseSettings() {
        // Only load SQLite file path
        databaseFilePath = config.getString("database.file", "plugins/DonutxOrders/orders.db");
    }
    
    /**
     * Initialize database connection and setup
     */
    public CompletableFuture<Boolean> initializeAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Connect to database
                if (!connect()) {
                    plugin.getLogger().severe("Failed to connect to database");
                    return false;
                }
                
                // Create tables
                if (!createTables()) {
                    plugin.getLogger().severe("Failed to create database tables");
                    return false;
                }
                
                // Prepare statements
                if (!prepareStatements()) {
                    plugin.getLogger().severe("Failed to prepare database statements");
                    return false;
                }
                
                plugin.getLogger().info("Database initialized successfully");
                return true;
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Error during database initialization", e);
                return false;
            }
        });
    }
    
    /**
     * Connect to the database
     */
    public boolean connect() {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFilePath)) {
            if (connection.isValid(5)) {
                plugin.getLogger().info("Database connection established successfully");
                return true;
            } else {
                plugin.getLogger().severe("Database connection test failed");
                return false;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to database", e);
            return false;
        }
    }
    
    /**
     * Disconnect from the database
     */
    public void disconnect() {
        try {
            // Close prepared statements
            for (PreparedStatement statement : preparedStatements.values()) {
                if (statement != null && !statement.isClosed()) {
                    statement.close();
                }
            }
            preparedStatements.clear();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Error while disconnecting from database", e);
        }
    }
    
    /**
     * Create database tables
     */
    public boolean createTables() {
        try (Connection connection = getConnection()) {
            // Create orders table
            String createOrdersTable = getCreateOrdersTableSQL();
            try (Statement statement = connection.createStatement()) {
                statement.execute(createOrdersTable);
            }
            
            // Create order items table
            String createOrderItemsTable = getCreateOrderItemsTableSQL();
            try (Statement statement = connection.createStatement()) {
                statement.execute(createOrderItemsTable);
            }
            
            // Create player data table
            String createPlayerDataTable = getCreatePlayerDataTableSQL();
            try (Statement statement = connection.createStatement()) {
                statement.execute(createPlayerDataTable);
            }
            
            plugin.getLogger().info("Database tables created successfully");
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create database tables", e);
            return false;
        }
    }
    
    /**
     * Get SQL for creating orders table
     */
    protected abstract String getCreateOrdersTableSQL();
    
    /**
     * Get SQL for creating order items table
     */
    protected abstract String getCreateOrderItemsTableSQL();
    
    /**
     * Get SQL for creating player data table
     */
    protected abstract String getCreatePlayerDataTableSQL();
    
    /**
     * Prepare frequently used SQL statements
     */
    protected boolean prepareStatements() {
        try (Connection connection = getConnection()) {
            // Order management statements
            prepareStatement("INSERT_ORDER", getInsertOrderSQL());
            prepareStatement("UPDATE_ORDER", getUpdateOrderSQL());
            prepareStatement("DELETE_ORDER", getDeleteOrderSQL());
            prepareStatement("SELECT_ORDER", getSelectOrderSQL());
            prepareStatement("SELECT_PLAYER_ORDERS", getSelectPlayerOrdersSQL());
            prepareStatement("SELECT_ALL_ORDERS", getSelectAllOrdersSQL());
            prepareStatement("SELECT_EXPIRED_ORDERS", getSelectExpiredOrdersSQL());
            
            // Order items statements
            prepareStatement("INSERT_ORDER_ITEM", getInsertOrderItemSQL());
            prepareStatement("DELETE_ORDER_ITEMS", getDeleteOrderItemsSQL());
            prepareStatement("SELECT_ORDER_ITEMS", getSelectOrderItemsSQL());
            
            // Player data statements
            prepareStatement("INSERT_PLAYER_DATA", getInsertPlayerDataSQL());
            prepareStatement("UPDATE_PLAYER_DATA", getUpdatePlayerDataSQL());
            prepareStatement("SELECT_PLAYER_DATA", getSelectPlayerDataSQL());
            
            // Cleanup statements
            prepareStatement("DELETE_EXPIRED_ORDERS", getDeleteExpiredOrdersSQL());
            prepareStatement("DELETE_ORPHANED_ITEMS", getDeleteOrphanedItemsSQL());
            
            plugin.getLogger().info("Prepared statements created successfully");
            return true;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to prepare statements", e);
            return false;
        }
    }
    
    /**
     * Prepare a statement and store it in the cache
     */
    private void prepareStatement(String name, String sql) throws SQLException {
        PreparedStatement statement = getConnection().prepareStatement(sql);
        preparedStatements.put(name, statement);
    }
    
    /**
     * Get SQL statements for different operations
     */
    protected abstract String getInsertOrderSQL();
    protected abstract String getUpdateOrderSQL();
    protected abstract String getDeleteOrderSQL();
    protected abstract String getSelectOrderSQL();
    protected abstract String getSelectPlayerOrdersSQL();
    protected abstract String getSelectAllOrdersSQL();
    protected abstract String getSelectExpiredOrdersSQL();
    protected abstract String getInsertOrderItemSQL();
    protected abstract String getDeleteOrderItemsSQL();
    protected abstract String getSelectOrderItemsSQL();
    protected abstract String getInsertPlayerDataSQL();
    protected abstract String getUpdatePlayerDataSQL();
    protected abstract String getSelectPlayerDataSQL();
    protected abstract String getDeleteExpiredOrdersSQL();
    protected abstract String getDeleteOrphanedItemsSQL();
    
    /**
     * Get a direct connection to SQLite database
     */
    public Connection getConnection() throws SQLException {
        // Use the plugin's data folder for the SQLite DB file
        String dbPath = plugin.getDataFolder().getAbsolutePath() + "/orders.db";
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }
    
    /**
     * Get a prepared statement
     */
    protected PreparedStatement getPreparedStatement(String name) throws SQLException {
        PreparedStatement statement = preparedStatements.get(name);
        if (statement == null || statement.isClosed()) {
            throw new SQLException("Prepared statement not available: " + name);
        }
        return statement;
    }
    
    /**
     * Save an order to the database
     */
    public CompletableFuture<Boolean> saveOrder(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                connection.setAutoCommit(false);
                
                try {
                    // Insert order
                    insertOrder(connection, order);
                    
                    // Insert order items
                    insertOrderItems(connection, order);
                    
                    connection.commit();
                    
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("Order saved successfully: " + order.getId());
                    }
                    
                    return true;
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save order: " + order.getId(), e);
                return false;
            }
        });
    }
    
    /**
     * Insert order into database
     */
    protected abstract void insertOrder(Connection connection, Order order) throws SQLException;
    
    /**
     * Insert order items into database
     */
    protected abstract void insertOrderItems(Connection connection, Order order) throws SQLException;
    
    /**
     * Load orders from the database
     */
    public CompletableFuture<List<Order>> loadOrders() {
        return CompletableFuture.supplyAsync(() -> {
            List<Order> orders = new ArrayList<>();
            
            try (Connection connection = getConnection()) {
                orders = loadOrdersFromDatabase(connection);
                
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Loaded " + orders.size() + " orders from database");
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load orders from database", e);
            }
            
            return orders;
        });
    }
    
    /**
     * Load orders from database connection
     */
    protected abstract List<Order> loadOrdersFromDatabase(Connection connection) throws SQLException;
    
    /**
     * Delete an order from the database
     */
    public CompletableFuture<Boolean> deleteOrder(String orderId) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                connection.setAutoCommit(false);
                
                try {
                    // Delete order items first
                    deleteOrderItemsFromDatabase(connection, orderId);
                    
                    // Delete order
                    deleteOrderFromDatabase(connection, orderId);
                    
                    connection.commit();
                    
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("Order deleted successfully: " + orderId);
                    }
                    
                    return true;
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete order: " + orderId, e);
                return false;
            }
        });
    }
    
    /**
     * Delete order from database
     */
    protected abstract void deleteOrderFromDatabase(Connection connection, String orderId) throws SQLException;
    
    /**
     * Delete order items from database
     */
    protected abstract void deleteOrderItemsFromDatabase(Connection connection, String orderId) throws SQLException;
    
    /**
     * Update an order in the database
     */
    public CompletableFuture<Boolean> updateOrder(Order order) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                connection.setAutoCommit(false);
                
                try {
                    // Update order
                    updateOrderInDatabase(connection, order);
                    
                    // Delete existing items
                    deleteOrderItemsFromDatabase(connection, String.valueOf(order.getId()));
                    
                    // Insert updated items
                    insertOrderItems(connection, order);
                    
                    connection.commit();
                    
                    if (plugin.getConfigManager().isDebugMode()) {
                        plugin.getLogger().info("Order updated successfully: " + order.getId());
                    }
                    
                    return true;
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to update order: " + order.getId(), e);
                return false;
            }
        });
    }
    
    /**
     * Update order in database
     */
    protected abstract void updateOrderInDatabase(Connection connection, Order order) throws SQLException;
    
    /**
     * Get orders for a specific player
     */
    public CompletableFuture<List<Order>> getPlayerOrders(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            List<Order> orders = new ArrayList<>();
            
            try (Connection connection = getConnection()) {
                orders = getPlayerOrdersFromDatabase(connection, playerId);
                
                if (plugin.getConfigManager().isDebugMode()) {
                    plugin.getLogger().info("Loaded " + orders.size() + " orders for player: " + playerId);
                }
                
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load orders for player: " + playerId, e);
            }
            
            return orders;
        });
    }
    
    /**
     * Get player orders from database
     */
    protected abstract List<Order> getPlayerOrdersFromDatabase(Connection connection, UUID playerId) throws SQLException;
    
    /**
     * Clean up expired orders
     */
    public CompletableFuture<Integer> cleanupExpiredOrders() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                connection.setAutoCommit(false);
                
                try {
                    // Get expired orders first
                    List<String> expiredOrderIds = getExpiredOrderIds(connection);
                    
                    // Delete expired order items
                    int deletedItems = deleteExpiredOrderItems(connection, expiredOrderIds);
                    
                    // Delete expired orders
                    int deletedOrders = deleteExpiredOrdersFromDatabase(connection);
                    
                    // Delete orphaned items
                    int orphanedItems = deleteOrphanedItems(connection);
                    
                    connection.commit();
                    
                    plugin.getLogger().info("Cleanup completed: " + deletedOrders + " orders, " + 
                                          deletedItems + " items, " + orphanedItems + " orphaned items");
                    
                    return deletedOrders;
                } catch (SQLException e) {
                    connection.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to cleanup expired orders", e);
                return 0;
            }
        });
    }
    
    /**
     * Get expired order IDs
     */
    protected abstract List<String> getExpiredOrderIds(Connection connection) throws SQLException;
    
    /**
     * Delete expired order items
     */
    protected abstract int deleteExpiredOrderItems(Connection connection, List<String> expiredOrderIds) throws SQLException;
    
    /**
     * Delete expired orders from database
     */
    protected abstract int deleteExpiredOrdersFromDatabase(Connection connection) throws SQLException;
    
    /**
     * Delete orphaned items
     */
    protected abstract int deleteOrphanedItems(Connection connection) throws SQLException;
    
    /**
     * Execute a batch operation
     */
    protected int executeBatch(PreparedStatement statement) throws SQLException {
        int[] results = statement.executeBatch();
        int total = 0;
        for (int result : results) {
            if (result > 0) {
                total += result;
            }
        }
        return total;
    }
    
    /**
     * Check if the database is connected
     */
    public boolean isConnected() {
        try (Connection conn = getConnection()) {
            return conn != null && conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Get database statistics
     */
    public CompletableFuture<DatabaseStats> getDatabaseStats() {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection connection = getConnection()) {
                DatabaseStats stats = new DatabaseStats();
                
                // Get table counts
                stats.totalOrders = getTableCount(connection, ORDERS_TABLE);
                stats.totalOrderItems = getTableCount(connection, ORDER_ITEMS_TABLE);
                stats.totalPlayers = getTableCount(connection, PLAYER_DATA_TABLE);
                
                // Get active orders count
                stats.activeOrders = getActiveOrdersCount(connection);
                
                // Get expired orders count
                stats.expiredOrders = getExpiredOrdersCount(connection);
                
                return stats;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to get database statistics", e);
                return new DatabaseStats();
            }
        });
    }
    
    /**
     * Get table count
     */
    protected abstract int getTableCount(Connection connection, String tableName) throws SQLException;
    
    /**
     * Get active orders count
     */
    protected abstract int getActiveOrdersCount(Connection connection) throws SQLException;
    
    /**
     * Get expired orders count
     */
    protected abstract int getExpiredOrdersCount(Connection connection) throws SQLException;
    
    /**
     * Close resources
     */
    public void close() {
        disconnect();
    }
    
    /**
     * Database statistics class
     */
    public static class DatabaseStats {
        public int totalOrders = 0;
        public int totalOrderItems = 0;
        public int totalPlayers = 0;
        public int activeOrders = 0;
        public int expiredOrders = 0;
        
        @Override
        public String toString() {
            return String.format("DatabaseStats{totalOrders=%d, totalOrderItems=%d, totalPlayers=%d, activeOrders=%d, expiredOrders=%d}",
                    totalOrders, totalOrderItems, totalPlayers, activeOrders, expiredOrders);
        }
    }
}