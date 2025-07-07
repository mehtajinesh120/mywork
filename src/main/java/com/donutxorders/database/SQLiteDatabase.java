package com.donutxorders.database;

import com.donutxorders.models.Order;
import com.donutxorders.models.OrderItem;
import com.donutxorders.models.OrderStatus;

import com.donutxorders.core.DonutxOrders;
import com.donutxorders.database.DatabaseManager;
import com.donutxorders.models.Order;
import com.donutxorders.models.OrderItem;
import com.donutxorders.models.OrderStatus;
import com.zaxxer.hikari.HikariConfig;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;

public class SQLiteDatabase extends DatabaseManager {

    @Override
    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(getSelectAllOrdersSQL());
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Order order = Order.fromResultSet(rs, null);
                orders.add(order);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return orders;
    }

    


    @Override
    public boolean updateOrderSync(Order order) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(getUpdateOrderSQL())) {
            statement.setString(1, order.getStatus().name());
            if (order.getExpiresAt() > 0) {
                statement.setLong(2, order.getExpiresAt());
            } else {
                statement.setNull(2, java.sql.Types.BIGINT);
            }
            statement.setDouble(3, order.getFee());
            statement.setDouble(4, order.getTotalPrice());
            statement.setString(5, order.getDescription());
            statement.setString(6, String.valueOf(order.getId()));
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean deleteOrderSync(String orderId) {
        try (Connection connection = getConnection();
             PreparedStatement statement = connection.prepareStatement(getDeleteOrderSQL())) {
            statement.setString(1, orderId);
            int rows = statement.executeUpdate();
            return rows > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private File dbFile;

    public SQLiteDatabase(DonutxOrders plugin) {
        super(plugin);
    }

    @Override
    protected void configureConnection(HikariConfig config) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        dbFile = new File(plugin.getDataFolder(), "orders.db");
        String jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        config.setJdbcUrl(jdbcUrl);
        config.setDriverClassName("org.sqlite.JDBC");
    }

    @Override
    protected String getCreateOrdersTableSQL() {
        return "CREATE TABLE IF NOT EXISTS " + ORDERS_TABLE + " (" +
                "id TEXT PRIMARY KEY," +
                "player_uuid TEXT NOT NULL," +
                "status TEXT NOT NULL," +
                "created_at INTEGER NOT NULL," +
                "expires_at INTEGER," +
                "world TEXT," +
                "x REAL," +
                "y REAL," +
                "z REAL," +
                "fee REAL DEFAULT 0.0," +
                "total_price REAL NOT NULL," +
                "description TEXT" +
                ")";
    }

    @Override
    protected String getCreateOrderItemsTableSQL() {
        return "CREATE TABLE IF NOT EXISTS " + ORDER_ITEMS_TABLE + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "order_id TEXT NOT NULL," +
                "item_type TEXT NOT NULL," +
                "amount INTEGER NOT NULL," +
                "price_per_item REAL NOT NULL," +
                "meta TEXT," +
                "deliverer_uuid TEXT," +
                "delivered_at INTEGER," +
                "delivered_amount INTEGER DEFAULT 0," +
                "FOREIGN KEY (order_id) REFERENCES " + ORDERS_TABLE + "(id) ON DELETE CASCADE" +
                ")";
    }

    @Override
    protected String getCreatePlayerDataTableSQL() {
        return "CREATE TABLE IF NOT EXISTS " + PLAYER_DATA_TABLE + " (" +
                "player_uuid TEXT PRIMARY KEY," +
                "orders_created INTEGER DEFAULT 0," +
                "orders_completed INTEGER DEFAULT 0," +
                "orders_delivered INTEGER DEFAULT 0," +
                "money_spent REAL DEFAULT 0.0," +
                "money_earned REAL DEFAULT 0.0," +
                "last_updated INTEGER NOT NULL" +
                ")";
    }

    @Override
    protected String getInsertOrderSQL() {
        return "INSERT INTO " + ORDERS_TABLE + " (id, player_uuid, status, created_at, expires_at, world, x, y, z, fee, total_price, description) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdateOrderSQL() {
        return "UPDATE " + ORDERS_TABLE + " SET status = ?, expires_at = ?, fee = ?, total_price = ?, description = ? WHERE id = ?";
    }

    @Override
    protected String getDeleteOrderSQL() {
        return "DELETE FROM " + ORDERS_TABLE + " WHERE id = ?";
    }

    @Override
    protected String getSelectOrderSQL() {
        return "SELECT * FROM " + ORDERS_TABLE + " WHERE id = ?";
    }

    @Override
    protected String getSelectPlayerOrdersSQL() {
        return "SELECT * FROM " + ORDERS_TABLE + " WHERE player_uuid = ? ORDER BY created_at DESC";
    }

    @Override
    protected String getSelectAllOrdersSQL() {
        return "SELECT * FROM " + ORDERS_TABLE + " ORDER BY created_at DESC";
    }

    @Override
    protected String getSelectExpiredOrdersSQL() {
        return "SELECT * FROM " + ORDERS_TABLE + " WHERE expires_at IS NOT NULL AND expires_at < ? AND status != 'EXPIRED'";
    }

    @Override
    protected String getInsertOrderItemSQL() {
        return "INSERT INTO " + ORDER_ITEMS_TABLE + " (order_id, item_type, amount, price_per_item, meta, deliverer_uuid, delivered_at, delivered_amount) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected String getDeleteOrderItemsSQL() {
        return "DELETE FROM " + ORDER_ITEMS_TABLE + " WHERE order_id = ?";
    }

    @Override
    protected String getSelectOrderItemsSQL() {
        return "SELECT * FROM " + ORDER_ITEMS_TABLE + " WHERE order_id = ?";
    }

    @Override
    protected String getInsertPlayerDataSQL() {
        return "INSERT OR REPLACE INTO " + PLAYER_DATA_TABLE + " (player_uuid, orders_created, orders_completed, orders_delivered, money_spent, money_earned, last_updated) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
    }

    @Override
    protected String getUpdatePlayerDataSQL() {
        return "UPDATE " + PLAYER_DATA_TABLE + " SET orders_created = ?, orders_completed = ?, orders_delivered = ?, " +
                "money_spent = ?, money_earned = ?, last_updated = ? WHERE player_uuid = ?";
    }

    @Override
    protected String getSelectPlayerDataSQL() {
        return "SELECT * FROM " + PLAYER_DATA_TABLE + " WHERE player_uuid = ?";
    }

    @Override
    protected String getDeleteExpiredOrdersSQL() {
        return "DELETE FROM " + ORDERS_TABLE + " WHERE expires_at IS NOT NULL AND expires_at < ?";
    }

    @Override
    protected String getDeleteOrphanedItemsSQL() {
        return "DELETE FROM " + ORDER_ITEMS_TABLE + " WHERE order_id NOT IN (SELECT id FROM " + ORDERS_TABLE + ")";
    }

    @Override
    protected void insertOrder(Connection connection, Order order) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(getInsertOrderSQL())) {
            statement.setInt(1, order.getId());
            statement.setString(2, order.getPlayerUuid().toString());
            statement.setString(3, order.getStatus().name());
            statement.setLong(4, order.getCreatedAt());
            if (order.getExpiresAt() > 0) {
                statement.setLong(5, order.getExpiresAt());
            } else {
                statement.setNull(5, Types.INTEGER);
            }
            statement.setString(6, order.getWorld());
            statement.setDouble(7, order.getX());
            statement.setDouble(8, order.getY());
            statement.setDouble(9, order.getZ());
            statement.setDouble(10, order.getFee());
            statement.setDouble(11, order.getTotalPrice());
            statement.setString(12, order.getDescription());
            statement.executeUpdate();
        }
    }

    @Override
    protected void insertOrderItems(Connection connection, Order order) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(getInsertOrderItemSQL())) {
            for (OrderItem item : order.getItems()) {
                statement.setInt(1, order.getId());
                statement.setString(2, item.getItemType());
                statement.setInt(3, item.getAmount());
                statement.setDouble(4, item.getPricePerItem());
                statement.setString(5, item.getMeta());
                if (item.getDelivererUuid() != null) {
                    statement.setString(6, item.getDelivererUuid().toString());
                } else {
                    statement.setNull(6, Types.VARCHAR);
                }
                if (item.getDeliveredAt() > 0) {
                    statement.setLong(7, item.getDeliveredAt());
                } else {
                    statement.setNull(7, Types.INTEGER);
                }
                statement.setInt(8, item.getDeliveredAmount());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    @Override
    protected List<Order> loadOrdersFromDatabase(Connection connection) throws SQLException {
        List<Order> orders = new ArrayList<>();
        
        try (PreparedStatement statement = connection.prepareStatement(getSelectAllOrdersSQL());
             ResultSet resultSet = statement.executeQuery()) {
            
            while (resultSet.next()) {
                Order order = createOrderFromResultSet(resultSet);
                loadOrderItems(connection, order);
                orders.add(order);
            }
        }
        
        return orders;
    }

    @Override
    protected void deleteOrderFromDatabase(Connection connection, String orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(getDeleteOrderSQL())) {
            statement.setString(1, orderId);
            statement.executeUpdate();
        }
    }

    @Override
    protected void deleteOrderItemsFromDatabase(Connection connection, String orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(getDeleteOrderItemsSQL())) {
            statement.setString(1, orderId);
            statement.executeUpdate();
        }
    }

    @Override
    protected void updateOrderInDatabase(Connection connection, Order order) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(getUpdateOrderSQL())) {
            statement.setString(1, order.getStatus().name());
            if (order.getExpiresAt() > 0) {
                statement.setLong(2, order.getExpiresAt());
            } else {
                statement.setNull(2, Types.INTEGER);
            }
            statement.setDouble(3, order.getFee());
            statement.setDouble(4, order.getTotalPrice());
            statement.setString(5, order.getDescription());
            statement.setInt(6, order.getId());
            statement.executeUpdate();
        }
    }

    @Override
    protected List<Order> getPlayerOrdersFromDatabase(Connection connection, UUID playerId) throws SQLException {
        List<Order> orders = new ArrayList<>();
        
        try (PreparedStatement statement = connection.prepareStatement(getSelectPlayerOrdersSQL())) {
            statement.setString(1, playerId.toString());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Order order = createOrderFromResultSet(resultSet);
                    loadOrderItems(connection, order);
                    orders.add(order);
                }
            }
        }
        
        return orders;
    }

    @Override
    protected List<String> getExpiredOrderIds(Connection connection) throws SQLException {
        List<String> expiredIds = new ArrayList<>();
        
        try (PreparedStatement statement = connection.prepareStatement(getSelectExpiredOrdersSQL())) {
            statement.setLong(1, System.currentTimeMillis());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    expiredIds.add(resultSet.getString("id"));
                }
            }
        }
        
        return expiredIds;
    }

    @Override
    protected int deleteExpiredOrderItems(Connection connection, List<String> expiredOrderIds) throws SQLException {
        if (expiredOrderIds.isEmpty()) {
            return 0;
        }
        
        StringBuilder sql = new StringBuilder("DELETE FROM ").append(ORDER_ITEMS_TABLE).append(" WHERE order_id IN (");
        for (int i = 0; i < expiredOrderIds.size(); i++) {
            if (i > 0) sql.append(",");
            sql.append("?");
        }
        sql.append(")");
        
        try (PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < expiredOrderIds.size(); i++) {
                statement.setString(i + 1, expiredOrderIds.get(i));
            }
            return statement.executeUpdate();
        }
    }

    @Override
    protected int deleteExpiredOrdersFromDatabase(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(getDeleteExpiredOrdersSQL())) {
            statement.setLong(1, System.currentTimeMillis());
            return statement.executeUpdate();
        }
    }

    @Override
    protected int deleteOrphanedItems(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(getDeleteOrphanedItemsSQL())) {
            return statement.executeUpdate();
        }
    }

    @Override
    protected int getTableCount(Connection connection, String tableName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName);
             ResultSet resultSet = statement.executeQuery()) {
            
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        }
    }

    @Override
    protected int getActiveOrdersCount(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + ORDERS_TABLE + " WHERE status IN ('ACTIVE', 'PENDING')");
             ResultSet resultSet = statement.executeQuery()) {
            
            if (resultSet.next()) {
                return resultSet.getInt(1);
            }
            return 0;
        }
    }

    @Override
    protected int getExpiredOrdersCount(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + ORDERS_TABLE + " WHERE expires_at IS NOT NULL AND expires_at < ?")) {
            statement.setLong(1, System.currentTimeMillis());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return 0;
            }
        }
    }

    private Order createOrderFromResultSet(ResultSet resultSet) throws SQLException {
        Order order = new Order();
        order.setId(resultSet.getInt("id"));
        order.setPlayerUuid(UUID.fromString(resultSet.getString("player_uuid")));
        order.setStatus(OrderStatus.valueOf(resultSet.getString("status")));
        order.setCreatedAt(resultSet.getLong("created_at"));
        
        Long expiresAt = resultSet.getLong("expires_at");
        if (!resultSet.wasNull()) {
            order.setExpiresAt(expiresAt);
        }
        
        order.setWorld(resultSet.getString("world"));
        order.setX(resultSet.getDouble("x"));
        order.setY(resultSet.getDouble("y"));
        order.setZ(resultSet.getDouble("z"));
        order.setFee(resultSet.getDouble("fee"));
        order.setTotalPrice(resultSet.getDouble("total_price"));
        order.setDescription(resultSet.getString("description"));
        
        return order;
    }

    private void loadOrderItems(Connection connection, Order order) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(getSelectOrderItemsSQL())) {
            statement.setInt(1, order.getId());
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    OrderItem item = new OrderItem();
                    item.setId(resultSet.getInt("id"));
                    item.setOrderId(order.getId());
                    item.setItemType(resultSet.getString("item_type"));
                    item.setAmount(resultSet.getInt("amount"));
                    item.setPricePerItem(resultSet.getDouble("price_per_item"));
                    item.setMeta(resultSet.getString("meta"));
                    
                    String delivererUuid = resultSet.getString("deliverer_uuid");
                    if (delivererUuid != null) {
                        item.setDelivererUuid(UUID.fromString(delivererUuid));
                    }
                    
                    long deliveredAt = resultSet.getLong("delivered_at");
                    if (!resultSet.wasNull()) {
                        item.setDeliveredAt(deliveredAt);
                    }
                    
                    item.setDeliveredAmount(resultSet.getInt("delivered_amount"));
                    order.addItem(item);
                }
            }
        }
    }
}