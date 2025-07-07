package com.donutxorders.tasks;

import com.donutxorders.core.DonutxOrders;
import com.donutxorders.managers.OrderManager;
import com.donutxorders.managers.EconomyManager;
import com.donutxorders.models.Order;
import com.donutxorders.models.OrderStatus;
import com.donutxorders.models.PlayerData;
import com.donutxorders.utils.MessageUtils;
import com.donutxorders.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.UUID;

/**
 * Periodically checks for expired orders, processes refunds, notifies players, and cleans up the database.
 * Expiration time is configurable via config.yml ("order-expiration-minutes").
 */
public class OrderExpirationTask extends BukkitRunnable {

    private final DonutxOrders plugin;
    private final OrderManager orderManager;
    private final EconomyManager economyManager;
    private final long expirationMillis;

    public OrderExpirationTask(DonutxOrders plugin) {
        this.plugin = plugin;
        this.orderManager = plugin.getOrderManager();
        this.economyManager = plugin.getEconomyManager();
        // Get expiration time from config (default: 1440 minutes = 24 hours)
        long minutes = plugin.getConfig().getLong("order-expiration-minutes", 1440L);
        this.expirationMillis = minutes * 60 * 1000;
    }

    @Override
    public void run() {
        checkExpiredOrders();
    }

    /**
     * Checks for expired orders and processes them.
     */
    public void checkExpiredOrders() {
        long now = System.currentTimeMillis();
        List<Order> allOrders = orderManager.getAllActiveOrders();
        for (Order order : allOrders) {
            if (isExpired(order, now)) {
                processExpiration(order);
            }
        }
    }

    /**
     * Determines if an order is expired.
     */
    private boolean isExpired(Order order, long now) {
        return order.getStatus() == OrderStatus.PENDING
                && (now - order.getCreatedAt()) >= expirationMillis;
    }

    /**
     * Handles refund, notification, and cleanup for an expired order.
     */
    public void processExpiration(Order order) {
        // Refund the player
        UUID playerId = order.getPlayerId();
        double refundAmount = order.getTotalPrice();
        boolean refunded = economyManager.deposit(playerId, refundAmount);

        // Update order status and database
        order.setStatus(OrderStatus.EXPIRED);
        orderManager.updateOrder(order);
        orderManager.removeOrder(order.getOrderId());

        // Notify player if online or store for later
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerId);
        String timeStr = TimeUtils.formatDuration(expirationMillis);
        String msg = MessageUtils.color("&cYour order &e#" + order.getOrderId() + " &chas expired after " + timeStr + ". "
                + (refunded ? "&aYou have been refunded &e" + refundAmount + "&a!" : "&cRefund failed. Contact staff."));

        if (player.isOnline() && player.getPlayer() != null) {
            player.getPlayer().sendMessage(msg);
        } else {
            // Store notification for next login
            PlayerData data = plugin.getPlayerDataManager().getOrLoadPlayerData(playerId);
            data.addNotification(msg);
            plugin.getPlayerDataManager().savePlayerData(data);
        }

        // Optionally: Log to console
        plugin.getLogger().info("Order #" + order.getOrderId() + " expired and processed for player " + player.getName());
    }
}