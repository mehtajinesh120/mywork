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

public class SearchGUI {

    private final DonutxOrders plugin;
    private final OrderManager orderManager;
    private final int size = 54;
    private final int resultsPerPage = 45;
    private final Map<UUID, SearchState> searchStates = new HashMap<>();

    public SearchGUI(DonutxOrders plugin) {
        this.plugin = plugin;
        this.orderManager = plugin.getOrderManager();
    }

    // Open the search GUI for a player
    public void openSearchGUI(Player player) {
        SearchState state = searchStates.computeIfAbsent(player.getUniqueId(), k -> new SearchState());
        state.currentPage = 0;
        state.query = "";
        state.history = new LinkedList<>();
        state.results = new ArrayList<>();
        updateDisplay(player, state);
    }

    // Handle search input from player (e.g., via chat or anvil GUI)
    public void processInput(Player player, String input) {
        SearchState state = searchStates.computeIfAbsent(player.getUniqueId(), k -> new SearchState());
        state.query = input;
        if (!input.isEmpty()) {
            state.history.addFirst(input);
            if (state.history.size() > 10) state.history.removeLast();
        }
        handleSearch(player, input);
    }

    // Perform the search and update results
    public void handleSearch(Player player, String query) {
        List<Order> allOrders = getAllOrders();
        List<Order> results = allOrders.stream()
                .filter(order -> matchesQuery(order, query))
                .collect(Collectors.toList());
        SearchState state = searchStates.get(player.getUniqueId());
        if (state != null) {
            state.results = results;
            state.currentPage = 0;
            displayResults(player, state);
        }
    }

    // Display search results in the GUI
    public void displayResults(Player player, SearchState state) {
        Inventory inv = Bukkit.createInventory(player, size, MessageUtils.colorize("&bSearch Results"));
        int start = state.currentPage * resultsPerPage;
        int end = Math.min(start + resultsPerPage, state.results.size());
        List<Order> pageResults = state.results.subList(start, end);

        for (int i = 0; i < resultsPerPage; i++) {
            int resultIndex = start + i;
            if (resultIndex < end) {
                Order order = pageResults.get(i);
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
        inv.setItem(48, ItemUtils.createGuiItem(Material.PAPER, "&bSearch", Collections.singletonList("&7Click to search again"), false));
        inv.setItem(49, ItemUtils.createGuiItem(Material.BOOK, "&bHistory", state.history.stream().limit(5).collect(Collectors.toList()), false));
        inv.setItem(51, ItemUtils.createGuiItem(Material.ARROW, "&aNext Page", Collections.singletonList("&7Go to next page"), false));
        inv.setItem(52, ItemUtils.createGuiItem(Material.BARRIER, "&cClose", Collections.singletonList("&7Close the menu"), false));

        player.openInventory(inv);
    }

    // Update the GUI display (initial or after search)
    private void updateDisplay(Player player, SearchState state) {
        Inventory inv = Bukkit.createInventory(player, size, MessageUtils.colorize("&bOrder Search"));
        inv.setItem(22, ItemUtils.createGuiItem(Material.PAPER, "&bEnter search query", Collections.singletonList("&7Type in chat to search orders"), false));
        inv.setItem(26, ItemUtils.createGuiItem(Material.BARRIER, "&cClose", Collections.singletonList("&7Close the menu"), false));
        player.openInventory(inv);
    }

    // Handle inventory click events
    public void handleClick(Player player, InventoryClickEvent event) {
        SearchState state = searchStates.get(player.getUniqueId());
        if (state == null) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= size) return;
        event.setCancelled(true);

        if (slot == 47 && state.currentPage > 0) {
            state.currentPage--;
            displayResults(player, state);
            return;
        }
        if (slot == 51 && (state.currentPage + 1) * resultsPerPage < state.results.size()) {
            state.currentPage++;
            displayResults(player, state);
            return;
        }
        if (slot == 48) {
            // Prompt for new search
            player.closeInventory();
            player.sendMessage(MessageUtils.colorize("&eType your search query in chat:"));
            return;
        }
        if (slot == 49 && !state.history.isEmpty()) {
            // Show search history (could open a new GUI or send as chat)
            player.sendMessage(MessageUtils.colorize("&bSearch History:"));
            for (String h : state.history) {
                player.sendMessage(MessageUtils.colorize("&7- &f" + h));
            }
            return;
        }
        if (slot == 52) {
            player.closeInventory();
            return;
        }
        // Result slots
        if (slot < resultsPerPage) {
            int resultIndex = state.currentPage * resultsPerPage + slot;
            if (resultIndex < state.results.size()) {
                Order order = state.results.get(resultIndex);
                player.sendMessage(MessageUtils.colorize("&aOrder ID: " + order.getId() + " selected."));
                // Implement further actions as needed
            }
        }
    }

    // Helper: get all orders (replace with async DB call as needed)
    private List<Order> getAllOrders() {
        // Example: return plugin.getDatabaseManager().loadOrders().join();
        return new ArrayList<>(); // Replace with actual order loading
    }

    // Helper: check if an order matches the search query
    private boolean matchesQuery(Order order, String query) {
        if (query == null || query.isEmpty()) return true;
        String q = query.toLowerCase();
        if (order.getCreatorUUID().toString().toLowerCase().contains(q)) return true;
        if (order.getStatus().name().toLowerCase().contains(q)) return true;
        if (order.getItemStack() != null && order.getItemStack().getType().name().toLowerCase().contains(q)) return true;
        // Add custom/NBT item search as needed
        return false;
    }

    // State for each player's search session
    private static class SearchState {
        int currentPage = 0;
        String query = "";
        List<Order> results = new ArrayList<>();
        Deque<String> history = new LinkedList<>();
    }
}