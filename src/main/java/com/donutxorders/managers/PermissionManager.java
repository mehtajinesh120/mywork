package com.donutxorders.managers;

import com.donutxorders.core.DonutxOrders;
import com.donutxorders.core.ConfigManager;
import org.bukkit.entity.Player;

public class PermissionManager {

    private final DonutxOrders plugin;
    private final ConfigManager configManager;

    public PermissionManager(DonutxOrders plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    // Get the order limit for a player based on permissions
    public int getOrderLimit(Player player) {
        int defaultLimit = configManager.getInt("orders.max-orders-per-player", 10);
        int maxLimit = defaultLimit;
        // Check for highest permission-based limit
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("donutxorders.limit." + i)) {
                maxLimit = i;
                break;
            }
        }
        return maxLimit;
    }

    // Check if a player has a specific permission
    public boolean hasPermission(Player player, String permission) {
        return player != null && player.hasPermission(permission);
    }

    // Check if a player can create an order (limit + permission)
    public boolean canCreateOrder(Player player, int currentActiveOrders) {
        int limit = getOrderLimit(player);
        return currentActiveOrders < limit && hasPermission(player, "donutxorders.create");
    }

    // Check if a player can access admin features
    public boolean canAccessAdmin(Player player) {
        return hasPermission(player,