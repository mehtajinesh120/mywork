package com.donutxorders.managers;

import com.donutxorders.core.DonutxOrders;
import com.donutxorders.database.DatabaseManager;
import com.donutxorders.models.Order;
import com.donutxorders.models.OrderItem;
import com.donutxorders.models.OrderStatus;
import com.donutxorders.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class OrderManager {

    private final DonutxOrders plugin;
    private final DatabaseManager databaseManager;

    public OrderManager(DonutxOrders plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
    }

    // Create a new order
    public CompletableFuture<Boolean> createOrder(Player player, ItemStack itemStack, int quantity, double pricePerItem, long expiresAt) {
        return CompletableFuture.supplyAsync(() -> {
            PlayerData playerData = new PlayerData(player.getUniqueId());
            playerData.updateFromDatabase(plugin);

            if (!playerData.canCreateOrder(plugin)) {
                player.sendMessage("You have reached your order limit.");
                return false;
            }
            if (quantity <= 0 || pricePerItem < 0) {
                player.sendMessage("Invalid quantity or price.");
                return false;
            }
            if (itemStack == null || itemStack.getType() == Material.AIR) {
                player.sendMessage("Invalid item.");
                return false;
            }

            // Payment processing (if required)
            double totalCost = pricePerItem * quantity;
            // Implement your payment logic here (e.g., Vault/EconomyManager)
            // if (!plugin.getEconomyManager().withdraw(player, totalCost)) { ... }

            Order order = new Order(
    playerData.getUuid(),
    itemStack,
    quantity,
    pricePerItem,
    System.currentTimeMillis(),
    expiresAt,
    OrderStatus.PENDING
);
            boolean saved = databaseManager.saveOrder(order).join();
            if (saved) {
                playerData.updateStats(1, 0, totalCost, 0);
                playerData.saveToDatabase(plugin);
                player.sendMessage("Order created successfully!");
                return true;
            } else {
                player.sendMessage("Failed to create order.");
                return false;
            }
        });
    }

    // Fulfill an order (partial or full)
    public CompletableFuture<Boolean> fulfillOrder(Player deliverer, Order order, ItemStack deliveredItem, int amount) {
        return CompletableFuture.supplyAsync(() -> {
            if (order == null || deliveredItem == null || amount <= 0) {
                deliverer.sendMessage("Invalid order or item.");
                return false;
            }
            if (order.isExpired() || order.isFullyFulfilled()) {
                deliverer.sendMessage("Order is expired or already fulfilled.");
                return false;
            }
            if (!deliveredItem.isSimilar(order.getItemStack())) {
                deliverer.sendMessage("Delivered item does not match order.");
                return false;
            }
            int fulfillable = Math.min(amount, order.getQuantity() - order.getDeliveredAmount());
            if (fulfillable <= 0) {
                deliverer.sendMessage("Order already fulfilled.");
                return false;
            }

            // Payment distribution
            double payment = fulfillable * order.getPricePerItem();
            // plugin.getEconomyManager().deposit(deliverer, payment);

            // Update order
            order.setDeliveredAmount(order.getDeliveredAmount() + fulfillable);
            if (order.isFullyFulfilled()) {
                order.setStatus(OrderStatus.COMPLETED);
            }
            databaseManager.updateOrder(order);

            // Track delivery
            OrderItem orderItem = new OrderItem(
                    order.getId(),
                    deliverer.getUniqueId(),
                    deliveredItem,
                    fulfillable,
                    System.currentTimeMillis(),
                    payment
            );
            // Save order item to DB if needed

            deliverer.sendMessage("Order fulfilled! You earned: " + payment);
            return true;
        });
    }

    // Cancel an order
    public CompletableFuture<Boolean> cancelOrder(Player player, Order order) {
        return CompletableFuture.supplyAsync(() -> {
            if (order == null) {
                player.sendMessage("Order not found.");
                return false;
            }
            if (!order.getCreatorUUID().equals(player.getUniqueId())) {
                player.sendMessage("You can only cancel your own orders.");
                return false;
            }
            if (!order.canBeCancelled()) {
                player.sendMessage("Order cannot be cancelled.");
                return false;
            }
            order.setStatus(OrderStatus.CANCELLED);
            boolean updated = databaseManager.updateOrder(order).join();
            if (updated) {
                // Refund logic if needed
                player.sendMessage("Order cancelled.");
                return true;
            } else {
                player.sendMessage("Failed to cancel order.");
                return false;
            }
        });
    }

    // Search orders by player, item, or status
    public List<Order> searchOrders(List<Order> orders, String query) {
        String q = query.toLowerCase();
        return orders.stream()
                .filter(order ->
                        order.getCreatorUUID().toString().toLowerCase().contains(q) ||
                        (order.getItemStack() != null && order.getItemStack().getType().name().toLowerCase().contains(q)) ||
                        order.getStatus().name().toLowerCase().contains(q)
                )
                .collect(Collectors.toList());
    }

    // Sort orders by creation time, price, or quantity
    public List<Order> sortOrders(List<Order> orders, String sortBy) {
        Comparator<Order> comparator;
        switch (sortBy.toLowerCase()) {
            case "price":
                comparator = Comparator.comparingDouble(Order::getPricePerItem);
                break;
            case "quantity":
                comparator = Comparator.comparingInt(Order::getQuantity);
                break;
            case "created":
            default:
                comparator = Comparator.comparingLong(Order::getCreatedTime);
                break;
        }
        return orders.stream().sorted(comparator).collect(Collectors.toList());
    }

    // Filter orders by status
    public List<Order> filterOrders(List<Order> orders, String status) {
        return orders.stream()
                .filter(order -> order.getStatus().name().equalsIgnoreCase(status))
                .collect(Collectors.toList());
    }

    // Cleanup expired orders
    public void cleanupExpiredOrders() {
        // Implement logic to find and mark expired orders, refund if needed
        // List<Order> expired = databaseManager.getExpiredOrders();
        // for (Order order : expired) { ... }
    }

    // Additional utility methods as needed

    // --- ADDED: Stub methods to fix compilation errors ---
    public void saveAllData() {
        // TODO: Implement saving all data
        // Placeholder stub
    }

    public void reload() {
        // TODO: Implement reload logic
        // Placeholder stub
    }

    // GUI accessors (stubs)
    public Object getMainOrderGUI() { throw new UnsupportedOperationException("Not implemented"); }
    public Object getDeliveryGUI() { throw new UnsupportedOperationException("Not implemented"); }
    public Object getYourOrdersGUI() { throw new UnsupportedOperationException("Not implemented"); }
    public Object getNewOrderGUI() { throw new UnsupportedOperationException("Not implemented"); }
    public Object getItemSelectionGUI() { throw new UnsupportedOperationException("Not implemented"); }
    public Object getSearchGUI() { throw new UnsupportedOperationException("Not implemented"); }

}