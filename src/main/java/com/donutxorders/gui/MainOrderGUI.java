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

import java.util.*;
import java.util.stream.Collectors;

public class MainOrderGUI {

    private final DonutxOrders plugin;
    private final OrderManager orderManager;
    private final int size = 54;
    private final int ordersPerPage = 45;
    private final Map<UUID, GUIState> guiStates = new HashMap<>();

    public MainOrderGUI(DonutxOrders plugin) {
        this.plugin = plugin;
        this.orderManager = plugin.getOrderManager();
    }

    // Open the main order GUI for a player
    public void openGUI(Player player) {
        GUIState state = guiStates.computeIfAbsent(player.getUniqueId(), k -> new GUIState());
        state.currentPage = 0;
        state.sortBy = "created";
        state.filter = "all";
        state.search = "";
        refreshOrders(player, state);
        updateDisplay(player, state);
    }

    // Refresh the list of orders for the current state
    public void refreshOrders(Player player, GUIState state) {
        // Fetch all orders (replace with async DB call if needed)
        List<Order> allOrders = orderManager.sortOrders(orderManager.filterOrders(getAllOrders(), state.filter), state.sortBy);
        if (!state.search.isEmpty()) {
            allOrders = orderManager.searchOrders(allOrders, state.search);
        }
        state.filteredOrders = allOrders;
    }

    // Handle inventory click events
    public void handleClick(Player player, InventoryClickEvent event) {
        GUIState state = guiStates.get(player.getUniqueId());
        if (state == null) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= size) return;
        event.setCancelled(true);

        // Pagination controls
        if (slot == 47 && state.currentPage > 0) {
            state.currentPage--;
            updateDisplay(player, state);
            return;
        }
        if (slot == 51 && (state.currentPage + 1) * ordersPerPage < state.filteredOrders.size()) {
            state.currentPage++;
            updateDisplay(player, state);
            return;
        }
        // Sorting/filtering/search controls (slots 48-50)
        if (slot == 48) {
            // Cycle sort
            state.sortBy = nextSort(state.sortBy);
            refreshOrders(player, state);
            updateDisplay(player, state);
            return;
        }
        if (slot == 49) {
            // Cycle filter
            state.filter = nextFilter(state.filter);
            refreshOrders(player, state);
            updateDisplay(player, state);
            return;
        }
        if (slot == 50) {
            // Prompt for search (implement chat input or anvil GUI as needed)
            player.closeInventory();
            player.sendMessage(MessageUtils.colorize("&eType your search query in chat:"));
            // You would need to listen for AsyncPlayerChatEvent to capture the search input
            return;
        }
        // Order click (slots 0-44)
        if (slot >= 0 && slot < ordersPerPage) {
            int index = state.currentPage * ordersPerPage + slot;
            if (index < state.filteredOrders.size()) {
                Order order = state.filteredOrders.get(index);
                // Open order details or handle order action
                player.sendMessage(MessageUtils.colorize("&aOrder ID: " + order.getId() + " selected."));
            }
        }
    }

    // Update the GUI display for the player
    public void updateDisplay(Player player, GUIState state) {
        Inventory inv = Bukkit.createInventory(player, size, MessageUtils.colorize("&bDonutxOrders - Orders"));
        int start = state.currentPage * ordersPerPage;
        int end = Math.min(start + ordersPerPage, state.filteredOrders.size());
        List<Order> pageOrders = state.filteredOrders.subList(start, end);

        // Display orders
        for (int i = 0; i < ordersPerPage; i++) {
            int orderIndex = start + i;
            if (orderIndex < end) {
                Order order = pageOrders.get(i);
                ItemStack item = ItemUtils.createGuiItem(
                        order.getItemStack() != null ? order.getItemStack().getType() : Material.PAPER,
                        "&eOrder #" + order.getId(),
                        Arrays.asList(
                                "&7Player: &f" + order.getCreatorUUID(),
                                "&7Item: &f" + (order.getItemStack() != null ? order.getItemStack().getType().name() : "N/A"),
                                "&7Qty: &f" + order.getQuantity(),
                                "&7Price: &f" + order.getPricePerItem(),
                                "&7Status: &f" + order.getStatus()
                        ),
                        false
                );
                inv.setItem(i, item);
            } else {
                inv.setItem(i, new ItemStack(Material.AIR));
            }
        }

        // Controls
        inv.setItem(47, ItemUtils.createGuiItem(Material.ARROW, "&aPrevious Page", Collections.singletonList("&7Go to previous page"), false));
        inv.setItem(48, ItemUtils.createGuiItem(Material.COMPASS, "&bSort: &f" + state.sortBy, Collections.singletonList("&7Click to change sort"), false));
        inv.setItem(49, ItemUtils.createGuiItem(Material.HOPPER, "&bFilter: &f" + state.filter, Collections.singletonList("&7Click to change filter"), false));
        inv.setItem(50, ItemUtils.createGuiItem(Material.PAPER, "&bSearch", Collections.singletonList("&7Click to search orders"), false));
        inv.setItem(51, ItemUtils.createGuiItem(Material.ARROW, "&aNext Page", Collections.singletonList("&7Go to next page"), false));
        inv.setItem(52, ItemUtils.createGuiItem(Material.BARRIER, "&cClose", Collections.singletonList("&7Close the menu"), false));

        player.openInventory(inv);
    }

    // Helper: get all orders (replace with async DB call as needed)
    private List<Order> getAllOrders() {
        // Example: return plugin.getDatabaseManager().loadOrders().join();
        return new ArrayList<>(); // Replace with actual order loading
    }

    // Helper: get next sort option
    private String nextSort(String current) {
        List<String> sorts = Arrays.asList("created", "price", "quantity");
        int idx = sorts.indexOf(current);
        return sorts.get((idx + 1) % sorts.size());
    }

    // Helper: get next filter option
    private String nextFilter(String current) {
        List<String> filters = Arrays.asList("all", "active", "completed", "expired", "cancelled");
        int idx = filters.indexOf(current);
        return filters.get((idx + 1) % filters.size());
    }

    // State for each player's GUI
    private static class GUIState {
        int currentPage = 0;
        String sortBy = "created";
        String filter = "all";
        String search = "";
        List<Order> filteredOrders = new ArrayList<>();
    }
}