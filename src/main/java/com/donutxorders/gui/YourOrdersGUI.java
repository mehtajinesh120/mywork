package com.donutxorders.gui;

import com.donutxorders.core.DonutxOrders;
import com.donutxorders.managers.OrderManager;
import com.donutxorders.models.Order;
import com.donutxorders.utils.ItemUtils;
import com.donutxorders.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class YourOrdersGUI {

    private final DonutxOrders plugin;
    private final OrderManager orderManager;
    private final int size = 27;
    private final int ordersPerPage = 18;

    public YourOrdersGUI(DonutxOrders plugin) {
        this.plugin = plugin;
        this.orderManager = plugin.getOrderManager();
    }

    // Open the "Your Orders" GUI for a player
    public void openGUI(Player player, int page) {
        List<Order> orders = getPlayerOrders(player.getUniqueId());
        int maxPage = Math.max(0, (orders.size() - 1) / ordersPerPage);
        int currentPage = Math.max(0, Math.min(page, maxPage));
        Inventory inv = Bukkit.createInventory(player, size, MessageUtils.colorize("&eYour Orders - Page " + (currentPage + 1)));

        displayOrders(inv, orders, currentPage);

        // Pagination controls
        if (currentPage > 0)
            inv.setItem(21, ItemUtils.createGuiItem(Material.ARROW, "&aPrevious", Collections.singletonList("&7Previous page"), false));
        if (currentPage < maxPage)
            inv.setItem(23, ItemUtils.createGuiItem(Material.ARROW, "&aNext", Collections.singletonList("&7Next page"), false));

        // Close button
        inv.setItem(26, ItemUtils.createGuiItem(Material.BARRIER, "&cClose", Collections.singletonList("&7Close the menu"), false));

        player.openInventory(inv);
    }

    // Display orders in the GUI
    private void displayOrders(Inventory inv, List<Order> orders, int page) {
        int start = page * ordersPerPage;
        int end = Math.min(start + ordersPerPage, orders.size());
        List<Order> pageOrders = orders.subList(start, end);

        for (int i = 0; i < ordersPerPage; i++) {
            int orderIndex = start + i;
            if (orderIndex < end) {
                Order order = pageOrders.get(i);
                ItemStack item = ItemUtils.createGuiItem(
                        order.getItemStack() != null ? order.getItemStack().getType() : Material.PAPER,
                        "&eOrder #" + order.getId(),
                        List.of(
                                "&7Status: &f" + order.getStatus(),
                                "&7Item: &f" + (order.getItemStack() != null ? order.getItemStack().getType().name() : "N/A"),
                                "&7Qty: &f" + order.getQuantity(),
                                "&7Delivered: &f" + order.getDeliveredAmount(),
                                "&7Price: &f" + order.getPricePerItem(),
                                "&aClick to collect items",
                                "&cRight-click to cancel"
                        ),
                        false
                );
                inv.setItem(i, item);
            } else {
                inv.setItem(i, new ItemStack(Material.AIR));
            }
        }
    }

    // Handle order cancellation
    public void handleCancel(Player player, Order order) {
        if (order == null) {
            player.sendMessage(MessageUtils.colorize("&cOrder not found."));
            return;
        }
        orderManager.cancelOrder(player, order).thenAccept(success -> {
            if (success) {
                player.sendMessage(MessageUtils.colorize("&aOrder cancelled successfully!"));
                openGUI(player, 0);
            } else {
                player.sendMessage(MessageUtils.colorize("&cFailed to cancel order."));
            }
        });
    }

    // Handle item collection for completed/expired orders
    public void collectItems(Player player, Order order) {
        if (order == null) {
            player.sendMessage(MessageUtils.colorize("&cOrder not found."));
            return;
        }
        if (!order.isFullyFulfilled() && !order.isExpired()) {
            player.sendMessage(MessageUtils.colorize("&cOrder is not ready for collection."));
            return;
        }
        // Give items to player (implement actual item giving logic)
        ItemStack item = order.getItemStack();
        int amount = order.getQuantity() - order.getDeliveredAmount();
        if (item != null && amount > 0) {
            item.setAmount(amount);
            player.getInventory().addItem(item);
            player.sendMessage(MessageUtils.colorize("&aCollected " + amount + " items from order #" + order.getId()));
        } else {
            player.sendMessage(MessageUtils.colorize("&cNo items to collect."));
        }
    }

    // Handle inventory click events (to be called from your listener)
    public void handleClick(Player player, InventoryClickEvent event, int page) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= size) return;
        event.setCancelled(true);

        if (slot == 21) {
            openGUI(player, page - 1);
            return;
        }
        if (slot == 23) {
            openGUI(player, page + 1);
            return;
        }
        if (slot == 26) {
            player.closeInventory();
            return;
        }
        // Order slots
        if (slot < ordersPerPage) {
            List<Order> orders = getPlayerOrders(player.getUniqueId());
            int orderIndex = page * ordersPerPage + slot;
            if (orderIndex < orders.size()) {
                Order order = orders.get(orderIndex);
                if (event.isRightClick()) {
                    handleCancel(player, order);
                } else {
                    collectItems(player, order);
                }
            }
        }
    }

    // Helper: get all orders for a player (replace with async DB call as needed)
    private List<Order> getPlayerOrders(UUID uuid) {
        // Example: return plugin.getDatabaseManager().getPlayerOrders(uuid).join();
        return List.of(); // Replace with actual order loading
    }
}