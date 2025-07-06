package com.donutxorders.gui;

import com.donutxorders.core.DonutxOrders;
import com.donutxorders.managers.EconomyManager;
import com.donutxorders.managers.PermissionManager;
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

import java.util.Collections;

public class NewOrderGUI {

    private final DonutxOrders plugin;
    private final EconomyManager economyManager;
    private final PermissionManager permissionManager;
    private final ItemManager itemManager;
    private final int size = 27;

    // State for each player
    private static class OrderState {
        ItemStack item;
        int amount = 1;
        double price = 0.0;
    }

    public NewOrderGUI(DonutxOrders plugin) {
        this.plugin = plugin;
        this.economyManager = plugin.getEconomyManager();
        this.permissionManager = plugin.getPermissionManager();
        this.itemManager = plugin.getItemManager();
    }

    // Open the new order GUI for a player
    public void openGUI(Player player) {
        Inventory inv = Bukkit.createInventory(player, size, MessageUtils.colorize("&aCreate New Order"));
        // Fill with glass panes except input slots
        for (int i = 0; i < size; i++) {
            inv.setItem(i, ItemUtils.createGuiItem(Material.GRAY_STAINED_GLASS_PANE, " ", null, false));
        }
        // Item slot (13), amount (11), price (15), confirm (26)
        inv.setItem(13, new ItemStack(Material.AIR));
        inv.setItem(11, ItemUtils.createGuiItem(Material.PAPER, "&bSet Amount", Collections.singletonList("&7Click to set amount"), false));
        inv.setItem(15, ItemUtils.createGuiItem(Material.GOLD_INGOT, "&bSet Price", Collections.singletonList("&7Click to set price"), false));
        inv.setItem(26, ItemUtils.createGuiItem(Material.LIME_CONCRETE, "&aConfirm", Collections.singletonList("&7Click to create order"), false));
        player.openInventory(inv);
    }

    // Set the item for the order
    public void setItem(Player player, Inventory inv, ItemStack item) {
        if (!itemManager.isValidItem(item)) {
            player.sendMessage(MessageUtils.colorize("&cInvalid item for order."));
            return;
        }
        inv.setItem(13, item);
        player.sendMessage(MessageUtils.colorize("&aItem set for order."));
    }

    // Set the amount for the order
    public void setAmount(Player player, Inventory inv, int amount) {
        if (amount <= 0 || amount > 64) {
            player.sendMessage(MessageUtils.colorize("&cInvalid amount."));
            return;
        }
        ItemStack item = inv.getItem(13);
        if (item != null) {
            item.setAmount(amount);
            inv.setItem(13, item);
        }
        player.sendMessage(MessageUtils.colorize("&aAmount set to " + amount));
    }

    // Set the price for the order
    public void setPrice(Player player, Inventory inv, double price) {
        if (price < 0) {
            player.sendMessage(MessageUtils.colorize("&cInvalid price."));
            return;
        }
        // Store price in item meta/lore or state as needed
        player.sendMessage(MessageUtils.colorize("&aPrice set to " + price));
    }

    // Create the order after validation
    public void createOrder(Player player, Inventory inv) {
        ItemStack item = inv.getItem(13);
        if (!itemManager.isValidItem(item)) {
            player.sendMessage(MessageUtils.colorize("&cPlease select a valid item."));
            return;
        }
        int amount = item.getAmount();
        double price = 0.0; // Retrieve from state or input
        // For demonstration, you may want to store price in a map or use anvil GUI for input

        // Permission and economy checks
        if (!permissionManager.canCreateOrder(player, 0)) { // Replace 0 with actual active order count
            player.sendMessage(MessageUtils.colorize("&cYou cannot create more orders."));
            return;
        }
        double totalCost = price * amount;
        if (!economyManager.hasBalance(player, totalCost)) {
            player.sendMessage(MessageUtils.colorize("&cYou do not have enough money."));
            return;
        }

        // Withdraw money
        if (!economyManager.withdrawMoney(player, totalCost)) {
            player.sendMessage(MessageUtils.colorize("&cFailed to withdraw money for order."));
            return;
        }

        // Create order
        plugin.getOrderManager().createOrder(player, item, amount, price, System.currentTimeMillis() + 86400000L); // 24h default
        player.sendMessage(MessageUtils.colorize("&aOrder created successfully!"));
        player.closeInventory();
    }

    // Handle inventory click events (to be called from your listener)
    public void handleClick(Player player, InventoryClickEvent event) {
        int slot = event.getRawSlot();
        Inventory inv = event.getInventory();
        if (slot < 0 || slot >= size) return;
        event.setCancelled(true);

        if (slot == 11) {
            // Prompt for amount (implement chat or anvil input)
            player.sendMessage(MessageUtils.colorize("&eType the amount in chat:"));
        } else if (slot == 15) {
            // Prompt for price (implement chat or anvil input)
            player.sendMessage(MessageUtils.colorize("&eType the price in chat:"));
        } else if (slot == 26) {
            createOrder(player, inv);
        } else if (slot == 13) {
            // Let player place item (implement drag/drop or click logic)
            player.sendMessage(MessageUtils.colorize("&ePlace the item you want to order."));
        }