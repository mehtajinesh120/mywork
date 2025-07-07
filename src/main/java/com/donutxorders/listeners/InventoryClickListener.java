package com.donutxorders.listeners;

import com.donutxorders.core.DonutxOrders;
import com.donutxorders.gui.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class InventoryClickListener implements Listener {

    private final DonutxOrders plugin;
    private final MainOrderGUI mainOrderGUI;
    private final DeliveryGUI deliveryGUI;
    private final YourOrdersGUI yourOrdersGUI;
    private final NewOrderGUI newOrderGUI;
    private final ItemSelectionGUI itemSelectionGUI;
    private final SearchGUI searchGUI;

    public InventoryClickListener(DonutxOrders plugin) {
        this.plugin = plugin;
        this.mainOrderGUI = plugin.getOrderManager() != null ? (MainOrderGUI) plugin.getOrderManager().getMainOrderGUI() : null;
        this.deliveryGUI = plugin.getOrderManager() != null ? (DeliveryGUI) plugin.getOrderManager().getDeliveryGUI() : null;
        this.yourOrdersGUI = plugin.getOrderManager() != null ? (YourOrdersGUI) plugin.getOrderManager().getYourOrdersGUI() : null;
        this.newOrderGUI = plugin.getOrderManager() != null ? (NewOrderGUI) plugin.getOrderManager().getNewOrderGUI() : null;
        this.itemSelectionGUI = plugin.getOrderManager() != null ? (ItemSelectionGUI) plugin.getOrderManager().getItemSelectionGUI() : null;
        this.searchGUI = plugin.getOrderManager() != null ? (SearchGUI) plugin.getOrderManager().getSearchGUI() : null;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getInventory();
        String title = ((Player) event.getWhoClicked()).getOpenInventory().getTitle();

        // Prevent shift-clicks and double-clicks for all plugin GUIs
        if (event.isShiftClick() || event.getClick().isKeyboardClick() || event.getClick().isCreativeAction()) {
            event.setCancelled(true);
            return;
        }

        // Route to the correct GUI handler based on inventory title
        if (title.contains("DonutxOrders - Orders") && mainOrderGUI != null) {
            mainOrderGUI.handleClick(player, event);
        } else if (title.contains("Deliver Items") && deliveryGUI != null) {
            // You need to get the order context for the player/session
            // deliveryGUI.handleClick(player, event, order);
            event.setCancelled(true); // Placeholder
        } else if (title.contains("Your Orders") && yourOrdersGUI != null) {
            // You need to track the current page for the player/session
            // yourOrdersGUI.handleClick(player, event, page);
            event.setCancelled(true); // Placeholder
        } else if (title.contains("Create New Order") && newOrderGUI != null) {
            newOrderGUI.handleClick(player, event);
        } else if (title.contains("Select Item") && itemSelectionGUI != null) {
            itemSelectionGUI.handleSelection(player, event);
        } else if (title.contains("Search Results") && searchGUI != null) {
            searchGUI.handleClick(player, event);
        } else if (title.contains("Order Search") && searchGUI != null) {
            searchGUI.handleClick(player, event);
        } else {
            // Not a plugin GUI, do nothing
        }

        // Prevent item duplication and edge cases
        if (event.getCurrentItem() != null && event.getCurrentItem().getAmount() > 0) {
            if (event.getSlotType() == org.bukkit.event.inventory.InventoryType.SlotType.CONTAINER) {
                event.setCancelled(true);
            }
        }
    }
}