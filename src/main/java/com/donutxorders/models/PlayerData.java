package com.donutxorders.models;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Represents player data and statistics for DonutxOrders.
 */
public class PlayerData implements Serializable {

    private java.util.List<String> notifications = new java.util.ArrayList<>();

    private UUID uuid;
    private int activeOrders;
    private int totalOrdersCreated;
    private int totalOrdersFulfilled;
    private double totalMoneySpent;
    private double totalMoneyEarned;

    public PlayerData(UUID uuid, int activeOrders, int totalOrdersCreated, int totalOrdersFulfilled, double totalMoneySpent, double totalMoneyEarned) {
        this.notifications = new java.util.ArrayList<>();
        this.uuid = uuid;
        this.activeOrders = activeOrders;
        this.totalOrdersCreated = totalOrdersCreated;
        this.totalOrdersFulfilled = totalOrdersFulfilled;
        this.totalMoneySpent = totalMoneySpent;
        this.totalMoneyEarned = totalMoneyEarned;
    }

    public PlayerData(UUID uuid) {
        this(uuid, 0, 0, 0, 0.0, 0.0);
    }

    // Serialization for database storage (example: to/from ResultSet)
    public static PlayerData fromResultSet(ResultSet rs) throws SQLException {
        return new PlayerData(
            UUID.fromString(rs.getString("player_uuid")),
            0, // activeOrders is usually calculated at runtime
            rs.getInt("orders_created"),
            rs.getInt("orders_completed"),
            rs.getDouble("money_spent"),
            rs.getDouble("money_earned")
        );
    }

    // Getters and Setters
    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public int getActiveOrders() { return activeOrders; }
    public void setActiveOrders(int activeOrders) { this.activeOrders = activeOrders; }

    public int getTotalOrdersCreated() { return totalOrdersCreated; }
    public void setTotalOrdersCreated(int totalOrdersCreated) { this.totalOrdersCreated = totalOrdersCreated; }

    public int getTotalOrdersFulfilled() { return totalOrdersFulfilled; }
    public void setTotalOrdersFulfilled(int totalOrdersFulfilled) { this.totalOrdersFulfilled = totalOrdersFulfilled; }

    public double getTotalMoneySpent() { return totalMoneySpent; }
    public void setTotalMoneySpent(double totalMoneySpent) { this.totalMoneySpent = totalMoneySpent; }

    public double getTotalMoneyEarned() { return totalMoneyEarned; }
    public void setTotalMoneyEarned(double totalMoneyEarned) { this.totalMoneyEarned = totalMoneyEarned; }

    /**
     * Checks if the player can create a new order based on their current active orders and limit.
     * @param plugin The plugin instance for permission checks.
     * @return true if the player can create a new order.
     */
    public boolean canCreateOrder(JavaPlugin plugin) {
        int limit = getOrderLimit(plugin);
        return activeOrders < limit;
    }

    /**
     * Gets the order limit for the player, supporting permission-based overrides.
     * @param plugin The plugin instance for permission checks.
     * @return The maximum number of active orders allowed.
     */
    public int getOrderLimit(JavaPlugin plugin) {
        Player player = Bukkit.getPlayer(uuid);
        int defaultLimit = plugin.getConfig().getInt("orders.max-orders-per-player", 10);
        if (player != null && player.isOnline()) {
            for (int i = 100; i >= 1; i--) {
                if (player.hasPermission("donutxorders.orderlimit." + i)) {
                    return i;
                }
            }
        }
        return defaultLimit;
    }

    /**
     * Updates player statistics.
     * @param createdDelta Change in orders created.
     * @param fulfilledDelta Change in orders fulfilled.
     * @param spentDelta Change in money spent.
     * @param earnedDelta Change in money earned.
     */
    public void updateStats(int createdDelta, int fulfilledDelta, double spentDelta, double earnedDelta) {
        this.totalOrdersCreated += createdDelta;
        this.totalOrdersFulfilled += fulfilledDelta;
        this.totalMoneySpent += spentDelta;
        this.totalMoneyEarned += earnedDelta;
    }

    // Database operations (example: save/update)
    public void saveToDatabase(JavaPlugin plugin) {
        // Example implementation: save or update player data using DatabaseManager
        try {
            com.donutxorders.database.DatabaseManager db = ((com.donutxorders.core.DonutxOrders) plugin).getDatabaseManager();
            try (java.sql.Connection conn = db.getConnection()) {
                String sql = "REPLACE INTO player_data (player_uuid, orders_created, orders_completed, orders_delivered, money_spent, money_earned) VALUES (?, ?, ?, ?, ?, ?)";
                try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    ps.setInt(2, totalOrdersCreated);
                    ps.setInt(3, totalOrdersFulfilled);
                    ps.setInt(4, 0); // orders_delivered, update as needed
                    ps.setDouble(5, totalMoneySpent);
                    ps.setDouble(6, totalMoneyEarned);
                    ps.executeUpdate();
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save player data for " + uuid + ": " + e.getMessage());
        }
    }

    public void updateFromDatabase(JavaPlugin plugin) {
        // Example implementation: load player data using DatabaseManager
        try {
            com.donutxorders.database.DatabaseManager db = ((com.donutxorders.core.DonutxOrders) plugin).getDatabaseManager();
            try (java.sql.Connection conn = db.getConnection()) {
                String sql = "SELECT * FROM player_data WHERE player_uuid = ?";
                try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, uuid.toString());
                    try (java.sql.ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            this.totalOrdersCreated = rs.getInt("orders_created");
                            this.totalOrdersFulfilled = rs.getInt("orders_completed");
                            // this.activeOrders = ...; // Calculate as needed
                            this.totalMoneySpent = rs.getDouble("money_spent");
                            this.totalMoneyEarned = rs.getDouble("money_earned");
                        }
                    }
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load player data for " + uuid + ": " + e.getMessage());
        }
    }

    /**
     * Adds a notification message to the player's notification list.
     * @param notification The notification message to add.
     */
    public void addNotification(String notification) {
        if (notification != null && !notification.isEmpty()) {
            notifications.add(notification);
        }
    }

    /**
     * Gets the list of notifications for this player.
     * @return List of notification messages.
     */
    public java.util.List<String> getNotifications() {
        return notifications;
    }
}