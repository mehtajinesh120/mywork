package com.donutxorders.gui;

import com.donutxorders.core.DonutxOrders;
import com.donutxorders.managers.ItemManager;
import com.donutxorders.utils.ItemUtils;
import com.donutxorders.utils.MessageUtils;
import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class ItemSelectionGUI {

    private final DonutxOrders plugin;
    private final ItemManager itemManager;
    private final int size = 54;
    private final int itemsPerPage = 45;
    private final Map<UUID, GUIState> guiStates = new HashMap<>();

    public ItemSelectionGUI(DonutxOrders plugin) {
        this.plugin = plugin;
        this.itemManager = plugin.getItemManager();
    }

    // Open the item selection GUI for a player
    public void openGUI(Player player) {
        GUIState state = guiStates.computeIfAbsent(player.getUniqueId(), k -> new GUIState());
        state.currentPage = 0;
        state.filter = "all";
        state.search = "";
        refreshItems(state);
        updateDisplay(player, state);
    }

    // Display items in the GUI
    private void updateDisplay(Player player, GUIState state) {
        Inventory inv = Bukkit.createInventory(player, size, MessageUtils.colorize("&bSelect Item"));
        int start = state.currentPage * itemsPerPage;
        int end = Math.min(start + itemsPerPage, state.filteredItems.size());
        List<ItemStack> pageItems = state.filteredItems.subList(start, end);

        for (int i = 0; i < itemsPerPage; i++) {
            int itemIndex = start + i;
            if (itemIndex < end) {
                inv.setItem(i, pageItems.get(i));
            } else {
                inv.setItem(i, new ItemStack(Material.AIR));
            }
        }

        // Controls
        inv.setItem(47, ItemUtils.createGuiItem(Material.ARROW, "&aPrevious Page", Collections.singletonList("&7Go to previous page"), false));
        inv.setItem(48, ItemUtils.createGuiItem(Material.HOPPER, "&bFilter: &f" + state.filter, Collections.singletonList("&7Click to change filter"), false));
        inv.setItem(49, ItemUtils.createGuiItem(Material.PAPER, "&bSearch", Collections.singletonList("&7Click to search items"), false));
        inv.setItem(50, ItemUtils.createGuiItem(Material.COMPASS, "&bCategory", Collections.singletonList("&7Click to change category"), false));
        inv.setItem(51, ItemUtils.createGuiItem(Material.ARROW, "&aNext Page", Collections.singletonList("&7Go to next page"), false));
        inv.setItem(52, ItemUtils.createGuiItem(Material.BARRIER, "&cClose", Collections.singletonList("&7Close the menu"), false));

        player.openInventory(inv);
    }

    // Handle inventory click events
    public void handleSelection(Player player, InventoryClickEvent event) {
        GUIState state = guiStates.get(player.getUniqueId());
        if (state == null) return;
        int slot = event.getRawSlot();
        if (slot < 0 || slot >= size) return;
        event.setCancelled(true);

        if (slot == 47 && state.currentPage > 0) {
            state.currentPage--;
            updateDisplay(player, state);
            return;
        }
        if (slot == 51 && (state.currentPage + 1) * itemsPerPage < state.filteredItems.size()) {
            state.currentPage++;
            updateDisplay(player, state);
            return;
        }
        if (slot == 48) {
            // Cycle filter
            state.filter = nextFilter(state.filter);
            refreshItems(state);
            updateDisplay(player, state);
            return;
        }
        if (slot == 49) {
            // Prompt for search (implement chat input or anvil GUI as needed)
            player.closeInventory();
            player.sendMessage(MessageUtils.colorize("&eType your search query in chat:"));
            // Listen for AsyncPlayerChatEvent to capture search input
            return;
        }
        if (slot == 50) {
            // Cycle category (stub, expand as needed)
            state.category = nextCategory(state.category);
            refreshItems(state);
            updateDisplay(player, state);
            return;
        }
        // Item selection (slots 0-44)
        if (slot >= 0 && slot < itemsPerPage) {
            int index = state.currentPage * itemsPerPage + slot;
            if (index < state.filteredItems.size()) {
                ItemStack selected = state.filteredItems.get(index);
                // Handle item selection (e.g., pass to NewOrderGUI)
                player.sendMessage(MessageUtils.colorize("&aSelected item: " + ItemUtils.getItemCategory(selected)));
                // Implement callback or state update as needed
            }
        }
    }

    // Refresh the list of items for the current state
    private void refreshItems(GUIState state) {
        List<ItemStack> allItems = getAllSelectableItems();
        // Filter by category
        if (!state.category.equals("all")) {
            allItems = allItems.stream()
                    .filter(item -> ItemUtils.getItemCategory(item).equalsIgnoreCase(state.category))
                    .collect(Collectors.toList());
        }
        // Filter by filter string
        if (!state.filter.equals("all")) {
            allItems = allItems.stream()
                    .filter(item -> item.getType().name().toLowerCase().contains(state.filter.toLowerCase()))
                    .collect(Collectors.toList());
        }
        // Search
        if (!state.search.isEmpty()) {
            allItems = allItems.stream()
                    .filter(item -> item.getType().name().toLowerCase().contains(state.search.toLowerCase()))
                    .collect(Collectors.toList());
        }
        state.filteredItems = allItems;
    }

    // Get all selectable items (vanilla + custom + NBT)
    private List<ItemStack> getAllSelectableItems() {
        List<ItemStack> items = new ArrayList<>();
        for (Material mat : Material.values()) {
            if (mat.isItem() && mat != Material.AIR) {
                items.add(new ItemStack(mat));
            }
        }
        // Add custom/NBT items as needed
        // Example: items.add(NBTItemUtils.createCustomNBTItem(...));
        return items;
    }

    // Apply filter (stub, expand as needed)
    public void applyFilter(Player player, String filter) {
        GUIState state = guiStates.get(player.getUniqueId());
        if (state != null) {
            state.filter = filter;
            refreshItems(state);
            updateDisplay(player, state);
        }
    }

    // Helper: get next filter option
    private String nextFilter(String current) {
        List<String> filters = Arrays.asList("all", "sword", "block", "food", "tool");
        int idx = filters.indexOf(current);
        return filters.get((idx + 1) % filters.size());
    }

    // Helper: get next category option
    private String nextCategory(String current) {
        List<String> categories = Arrays.asList("all", "weapon", "armor", "block", "misc");
        int idx = categories.indexOf(current);
        return categories.get((idx + 1) % categories.size());
    }

    // State for each player's GUI
    private static class GUIState {
        int currentPage = 0;
        String filter = "all";
        String category = "all";
        String search = "";
        List<ItemStack> filteredItems = new ArrayList<>();
    }
}