package com.donutxorders.gui;

import com.donutxorders.core.DonutxOrders;
import com.donutxorders.managers.EconomyManager;
import com.donutxorders.managers.ItemManager;
import com.donutxorders.models.Order;
import com.donutxorders.utils.ItemUtils;
import com.donutxorders.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class DeliveryGUI {

    private final DonutxOrders plugin;
    private final EconomyManager economyManager;
    private final ItemManager itemManager;
    private final int size = 36;

    public DeliveryGUI(DonutxOrders plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.itemManager = plugin.getItemManager();
    }

    // Open the delivery GUI for a player and order
    public void openGUI(Player player, Order order) {
        Inventory inv = Bukkit.createInventory(player, size, MessageUtils.colorize("&bDeliver Items - Order #" + order.getId()));
        // Fill with glass panes except delivery slots (e.g., 10-16, 19-25)
        for (int i = 0; i < size; i++) {
            inv.setItem(i, ItemUtils.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, false));
        }
        for (int i : getDeliverySlots()) {
            inv.setItem(i, new ItemStack(Material.AIR));
        }
        player.openInventory(inv);
    }

    // Validate items placed in the GUI against the order requirements
    public boolean validateItems(Inventory inv, Order order) {
        int total = 0;
        for (int i : getDeliverySlots()) {
            ItemStack item = inv.getItem(i);
            if (item != null && itemManager.compareItems(item, order.getItemStack())) {
                total += item.getAmount();
            }
        }
        return total > 0 && total <= (order.getQuantity() - order.getDeliveredAmount());
    }

    // Process the delivery: remove items, pay deliverer, update order
    public boolean processDelivery(Player deliverer, Inventory inv, Order order) {
        if (!validateItems(inv, order)) {
            deliverer.sendMessage(MessageUtils.colorize("&cInvalid items for delivery."));
            return false;
        }
        int deliverAmount = 0;
        for (int i : getDeliverySlots()) {
            ItemStack item = inv.getItem(i);
            if (item != null && itemManager.compareItems(item, order.getItemStack())) {
                deliverAmount += item.getAmount();
                inv.setItem(i, new ItemStack(Material.AIR));
            }
        }
        if (deliverAmount <= 0) {
            deliverer.sendMessage(MessageUtils.colorize("&cNo valid items delivered."));
            return false;
        }
        // Payment
        double payment = deliverAmount * order.getPricePerItem();
        economyManager.depositMoney(deliverer, payment);

        // Update order
        order.setDeliveredAmount(order.getDeliveredAmount() + deliverAmount);
        if (order.isFullyFulfilled()) {
            order.setStatus(com.donutxorders.models.OrderStatus.COMPLETED);
        }
        plugin.getDatabaseManager().updateOrder(order);

        deliverer.sendMessage(MessageUtils.colorize("&aDelivered " + deliverAmount + " items! You earned: " + payment));
        return true;
    }

    // Handle inventory click events (to be called from your listener)
    public void handleClick(Player player, InventoryClickEvent event, Order order) {
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= size) return;
        if (!isDeliverySlot(slot)) {
            event.setCancelled(true);
        }
    }

    // Delivery slots (e.g., center 3x2 area: 10-12, 19-21)
    private List<Integer> getDeliverySlots() {
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row <= 2; row++) {
            for (int col = 1; col <= 5; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots;
    }

    private boolean isDeliverySlot(int slot) {
        return getDeliverySlots().contains(slot);
    }
}