package com.donutxorders.core;

import com.donutxorders.utils.MessageUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Level;

public class ConfigManager {
    
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private File configFile;
    private final String configVersion = "1.0.0";
    
    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
    }
    
    /**
     * Load the configuration file
     */
    public void loadConfig() {
        // Create plugin data folder if it doesn't exist
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        
        // Create config file if it doesn't exist
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        
        // Load configuration
        config = YamlConfiguration.loadConfiguration(configFile);
        
        // Check version and update if necessary
        checkConfigVersion();
        
        plugin.getLogger().info("Configuration loaded successfully");
    }
    
    /**
     * Save the current configuration
     */
    public void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save configuration", e);
        }
    }
    
    /**
     * Reload the configuration
     */
    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        checkConfigVersion();
        plugin.getLogger().info("Configuration reloaded successfully");
    }
    
    /**
     * Get string value from config with color support
     */
    public String getString(String path, String defaultValue) {
        String value = config.getString(path, defaultValue);
        return MessageUtils.colorize(value);
    }
    
    /**
     * Get string value from config without color processing
     */
    public String getRawString(String path, String defaultValue) {
        return config.getString(path, defaultValue);
    }
    
    /**
     * Get integer value from config
     */
    public int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }
    
    /**
     * Get boolean value from config
     */
    public boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }
    
    /**
     * Get double value from config
     */
    public double getDouble(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }
    
    /**
     * Get string list from config with color support
     */
    public List<String> getStringList(String path) {
        List<String> list = config.getStringList(path);
        list.replaceAll(MessageUtils::colorize);
        return list;
    }
    
    /**
     * Get string list from config without color processing
     */
    public List<String> getRawStringList(String path) {
        return config.getStringList(path);
    }
    
    /**
     * Set a value in the configuration
     */
    public void set(String path, Object value) {
        config.set(path, value);
    }
    
    /**
     * Get the raw configuration object
     */
    public FileConfiguration getConfig() {
        return config;
    }
    
    /**
     * Check if a path exists in the configuration
     */
    public boolean contains(String path) {
        return config.contains(path);
    }
    
    /**
     * Create default configuration file
     */
    private void createDefaultConfig() {
        try {
            // Try to copy from resources
            InputStream inputStream = plugin.getResource("config.yml");
            if (inputStream != null) {
                Files.copy(inputStream, configFile.toPath());
                inputStream.close();
            } else {
                // Create default config if resource doesn't exist
                createHardcodedConfig();
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to create default config from resources, creating hardcoded version", e);
            createHardcodedConfig();
        }
    }
    
    /**
     * Create hardcoded default configuration
     */
    private void createHardcodedConfig() {
        config = new YamlConfiguration();
        
        // Config metadata
        config.set("config-version", configVersion);
        config.set("debug", false);
        
        // Database settings
        config.set("database.type", "SQLite");
        config.set("database.mysql.host", "localhost");
        config.set("database.mysql.port", 3306);
        config.set("database.mysql.database", "donutxorders");
        config.set("database.mysql.username", "root");
        config.set("database.mysql.password", "password");
        config.set("database.mysql.ssl", false);
        config.set("database.connection-pool.maximum-pool-size", 10);
        config.set("database.connection-pool.minimum-idle", 2);
        config.set("database.connection-pool.connection-timeout", 30000);
        config.set("database.connection-pool.idle-timeout", 600000);
        config.set("database.connection-pool.max-lifetime", 1800000);
        
        // Economy settings
        config.set("economy.enabled", true);
        config.set("economy.currency-symbol", "$");
        config.set("economy.decimal-places", 2);
        config.set("economy.order-fee", 0.0);
        config.set("economy.delivery-fee", 0.0);
        
        // Order settings
        config.set("orders.max-orders-per-player", 10);
        config.set("orders.max-items-per-order", 64);
        config.set("orders.default-expiration-hours", 72);
        config.set("orders.allow-cross-world", true);
        config.set("orders.require-payment", false);
        config.set("orders.auto-cleanup-expired", true);
        config.set("orders.notification-sound", "BLOCK_NOTE_BLOCK_PLING");
        
        // GUI settings
        config.set("gui.title-color", "<gradient:#ff6b6b:#4ecdc4>");
        config.set("gui.size", 54);
        config.set("gui.update-interval", 20);
        config.set("gui.close-on-click-outside", true);
        
        // GUI Items
        createGUIItemDefaults();
        
        // Messages
        createMessageDefaults();
        
        // Discord Integration
        config.set("discord.enabled", false);
        config.set("discord.webhook-url", "");
        config.set("discord.embed-color", "#4ecdc4");
        config.set("discord.notify-new-orders", true);
        config.set("discord.notify-completed-orders", true);
        config.set("discord.notify-expired-orders", false);
        
        // Sounds
        config.set("sounds.order-created", "BLOCK_NOTE_BLOCK_PLING");
        config.set("sounds.order-completed", "ENTITY_PLAYER_LEVELUP");
        config.set("sounds.order-cancelled", "BLOCK_NOTE_BLOCK_BASS");
        config.set("sounds.error", "ENTITY_VILLAGER_NO");
        config.set("sounds.gui-click", "UI_BUTTON_CLICK");
        config.set("sounds.page-turn", "ITEM_BOOK_PAGE_TURN");
        
        // Tasks
        config.set("tasks.order-expiration-interval", 600);
        config.set("tasks.database-cleanup-interval", 86400);
        config.set("tasks.auto-save-interval", 300);
        
        // Metrics
        config.set("metrics.enabled", true);
        
        // World settings
        config.set("worlds.blacklisted", List.of());
        config.set("worlds.whitelisted", List.of());
        config.set("worlds.mode", "blacklist");
        
        // Permissions
        config.set("permissions.use-vault", true);
        config.set("permissions.default-group", "default");
        
        // Save the configuration
        saveConfig();
    }
    
    /**
     * Create default GUI item configurations
     */
    private void createGUIItemDefaults() {
        // Main GUI Items
        config.set("gui.items.new-order.material", "EMERALD");
        config.set("gui.items.new-order.name", "<gradient:#00ff00:#00cc00>&lCreate New Order");
        config.set("gui.items.new-order.lore", List.of(
            "",
            "&7Click to create a new order",
            "&7for items you need!",
            "",
            "&aClick to continue"
        ));
        config.set("gui.items.new-order.slot", 11);
        config.set("gui.items.new-order.glow", true);
        
        config.set("gui.items.your-orders.material", "CHEST");
        config.set("gui.items.your-orders.name", "<gradient:#ff6b6b:#ff4757>&lYour Orders");
        config.set("gui.items.your-orders.lore", List.of(
            "",
            "&7View and manage your",
            "&7active orders",
            "",
            "&e{active_orders} &7active orders",
            "",
            "&aClick to view"
        ));
        config.set("gui.items.your-orders.slot", 13);
        
        config.set("gui.items.delivery.material", "MINECART");
        config.set("gui.items.delivery.name", "<gradient:#4ecdc4:#44bd32>&lDelivery Orders");
        config.set("gui.items.delivery.lore", List.of(
            "",
            "&7View orders that you",
            "&7can deliver for rewards!",
            "",
            "&e{available_orders} &7available orders",
            "",
            "&aClick to view"
        ));
        config.set("gui.items.delivery.slot", 15);
        
        config.set("gui.items.search.material", "COMPASS");
        config.set("gui.items.search.name", "<gradient:#ffa726:#ff9800>&lSearch Orders");
        config.set("gui.items.search.lore", List.of(
            "",
            "&7Search for specific",
            "&7orders by item or player",
            "",
            "&aClick to search"
        ));
        config.set("gui.items.search.slot", 29);
        
        config.set("gui.items.statistics.material", "BOOK");
        config.set("gui.items.statistics.name", "<gradient:#9c27b0:#673ab7>&lStatistics");
        config.set("gui.items.statistics.lore", List.of(
            "",
            "&7View your order statistics",
            "&7and achievements",
            "",
            "&7Orders Created: &e{orders_created}",
            "&7Orders Completed: &e{orders_completed}",
            "&7Orders Delivered: &e{orders_delivered}",
            "",
            "&aClick to view details"
        ));
        config.set("gui.items.statistics.slot", 31);
        
        config.set("gui.items.settings.material", "REDSTONE");
        config.set("gui.items.settings.name", "<gradient:#e74c3c:#c0392b>&lSettings");
        config.set("gui.items.settings.lore", List.of(
            "",
            "&7Configure your order",
            "&7preferences and notifications",
            "",
            "&aClick to configure"
        ));
        config.set("gui.items.settings.slot", 33);
        
        // Navigation items
        config.set("gui.items.back.material", "ARROW");
        config.set("gui.items.back.name", "&c&lBack");
        config.set("gui.items.back.lore", List.of(
            "",
            "&7Go back to the previous menu",
            "",
            "&cClick to go back"
        ));
        config.set("gui.items.back.slot", 45);
        
        config.set("gui.items.next-page.material", "PAPER");
        config.set("gui.items.next-page.name", "&a&lNext Page");
        config.set("gui.items.next-page.lore", List.of(
            "",
            "&7Page {current_page} of {total_pages}",
            "",
            "&aClick for next page"
        ));
        config.set("gui.items.next-page.slot", 53);
        
        config.set("gui.items.previous-page.material", "PAPER");
        config.set("gui.items.previous-page.name", "&c&lPrevious Page");
        config.set("gui.items.previous-page.lore", List.of(
            "",
            "&7Page {current_page} of {total_pages}",
            "",
            "&cClick for previous page"
        ));
        config.set("gui.items.previous-page.slot", 45);
        
        config.set("gui.items.close.material", "BARRIER");
        config.set("gui.items.close.name", "&c&lClose");
        config.set("gui.items.close.lore", List.of(
            "",
            "&7Close this menu",
            "",
            "&cClick to close"
        ));
        config.set("gui.items.close.slot", 49);
        
        // Decorative items
        config.set("gui.items.filler.material", "GRAY_STAINED_GLASS_PANE");
        config.set("gui.items.filler.name", " ");
        config.set("gui.items.filler.lore", List.of());
        
        // Order status items
        config.set("gui.items.order-active.material", "LIME_STAINED_GLASS");
        config.set("gui.items.order-active.name", "&a&lActive Order");
        
        config.set("gui.items.order-completed.material", "GREEN_STAINED_GLASS");
        config.set("gui.items.order-completed.name", "&2&lCompleted Order");
        
        config.set("gui.items.order-expired.material", "RED_STAINED_GLASS");
        config.set("gui.items.order-expired.name", "&c&lExpired Order");
        
        config.set("gui.items.order-cancelled.material", "ORANGE_STAINED_GLASS");
        config.set("gui.items.order-cancelled.name", "&6&lCancelled Order");
    }
    
    /**
     * Create default message configurations
     */
    private void createMessageDefaults() {
        // General messages
        config.set("messages.prefix", "<gradient:#ff6b6b:#4ecdc4>[DonutxOrders]</gradient>");
        config.set("messages.no-permission", "&cYou don't have permission to use this command!");
        config.set("messages.player-only", "&cThis command can only be used by players!");
        config.set("messages.invalid-usage", "&cInvalid usage! Use: {usage}");
        config.set("messages.plugin-reloaded", "&aPlugin reloaded successfully!");
        config.set("messages.database-error", "&cDatabase error occurred! Please try again later.");
        config.set("messages.economy-error", "&cEconomy error occurred! Please try again later.");
        
        // Order messages
        config.set("messages.order-created", "&aOrder created successfully! ID: &e{order_id}");
        config.set("messages.order-cancelled", "&cOrder cancelled successfully!");
        config.set("messages.order-completed", "&aOrder completed successfully! You received &e{reward}");
        config.set("messages.order-expired", "&cYour order has expired: &e{order_id}");
        config.set("messages.order-not-found", "&cOrder not found!");
        config.set("messages.order-not-yours", "&cThis order doesn't belong to you!");
        config.set("messages.order-already-completed", "&cThis order is already completed!");
        config.set("messages.order-limit-reached", "&cYou have reached the maximum number of orders ({max_orders})!");
        config.set("messages.insufficient-funds", "&cYou don't have enough money! Required: &e{amount}");
        config.set("messages.insufficient-items", "&cYou don't have enough items to fulfill this order!");
        config.set("messages.inventory-full", "&cYour inventory is full! Please make some space.");
        
        // Delivery messages
        config.set("messages.delivery-started", "&aYou started delivering order &e{order_id}");
        config.set("messages.delivery-completed", "&aDelivery completed! You earned &e{reward}");
        config.set("messages.delivery-cancelled", "&cDelivery cancelled!");
        config.set("messages.no-deliveries-available", "&cNo deliveries available at the moment.");
        config.set("messages.already-delivering", "&cYou are already delivering an order!");
        
        // GUI messages
        config.set("messages.gui-opened", "&aGUI opened successfully!");
        config.set("messages.gui-closed", "&cGUI closed.");
        config.set("messages.page-changed", "&aPage changed to &e{page}");
        config.set("messages.search-started", "&aSearch started for: &e{query}");
        config.set("messages.search-no-results", "&cNo results found for: &e{query}");
        config.set("messages.search-results", "&aFound &e{count} &aresults for: &e{query}");
        
        // Item messages
        config.set("messages.item-added", "&aItem added to order: &e{item} x{amount}");
        config.set("messages.item-removed", "&cItem removed from order: &e{item} x{amount}");
        config.set("messages.item-not-allowed", "&cThis item is not allowed in orders!");
        config.set("messages.item-limit-reached", "&cYou have reached the maximum number of items per order ({max_items})!");
        
        // Time messages
        config.set("messages.time-remaining", "&eTime remaining: &a{time}");
        config.set("messages.expires-in", "&eExpires in: &a{time}");
        config.set("messages.expired-ago", "&cExpired &e{time} &cago");
        
        // Notification messages
        config.set("messages.notification-new-order", "&aNew order available: &e{order_id} &aby &e{player}");
        config.set("messages.notification-order-completed", "&aOrder completed: &e{order_id} &aby &e{player}");
        config.set("messages.notification-order-cancelled", "&cOrder cancelled: &e{order_id} &aby &e{player}");
        config.set("messages.notification-delivery-request", "&e{player} &awants to deliver your order &e{order_id}");
        
        // Error messages
        config.set("messages.error-generic", "&cAn error occurred! Please try again.");
        config.set("messages.error-world-not-allowed", "&cOrders are not allowed in this world!");
        config.set("messages.error-cooldown", "&cYou must wait &e{time} &cbefore creating another order!");
        config.set("messages.error-maintenance", "&cThe order system is currently under maintenance!");
        
        // Success messages
        config.set("messages.success-generic", "&aOperation completed successfully!");
        config.set("messages.money-received", "&aYou received &e{amount} &afor completing the order!");
        config.set("messages.money-refunded", "&aYou were refunded &e{amount} &afor the cancelled order!");
        
        // Help messages
        config.set("messages.help-header", "&6&l=== DonutxOrders Help ===");
        config.set("messages.help-footer", "&6&l========================");
        config.set("messages.help-commands", List.of(
            "&e/order &7- Open the main order menu",
            "&e/order create &7- Create a new order",
            "&e/order list &7- View your orders",
            "&e/order cancel <id> &7- Cancel an order",
            "&e/order reload &7- Reload the plugin &c(Admin only)"
        ));
        
        // Discord messages
        config.set("messages.discord.new-order-title", "New Order Created");
        config.set("messages.discord.new-order-description", "A new order has been created!");
        config.set("messages.discord.completed-order-title", "Order Completed");
        config.set("messages.discord.completed-order-description", "An order has been completed!");
        config.set("messages.discord.expired-order-title", "Order Expired");
        config.set("messages.discord.expired-order-description", "An order has expired!");
        
        // Placeholders help
        config.set("messages.placeholders-info", List.of(
            "&6Available placeholders:",
            "&e{player} &7- Player name",
            "&e{order_id} &7- Order ID",
            "&e{amount} &7- Amount/Money",
            "&e{item} &7- Item name",
            "&e{time} &7- Time remaining/elapsed",
            "&e{world} &7- World name",
            "&e{x} {y} {z} &7- Coordinates"
        ));
    }
    
    /**
     * Check configuration version and update if necessary
     */
    private void checkConfigVersion() {
        String currentVersion = config.getString("config-version", "0.0.0");
        
        if (!currentVersion.equals(configVersion)) {
            plugin.getLogger().warning("Configuration version mismatch! Current: " + currentVersion + ", Expected: " + configVersion);
            plugin.getLogger().warning("Some features may not work correctly. Consider updating your configuration.");
            
            // Set the current version
            config.set("config-version", configVersion);
            saveConfig();
        }
    }
    
    /**
     * Get configuration file
     */
    public File getConfigFile() {
        return configFile;
    }
    
    /**
     * Get configuration version
     */
    public String getConfigVersion() {
        return configVersion;
    }
    
    /**
     * Check if debug mode is enabled
     */
    public boolean isDebugMode() {
        return config.getBoolean("debug", false);
    }
    
    /**
     * Get formatted message with prefix
     */
    public String getMessage(String path, String defaultValue) {
        String prefix = getString("messages.prefix", "");
        String message = getString(path, defaultValue);
        
        if (prefix.isEmpty()) {
            return message;
        }
        
        return prefix + " " + message;
    }
    
    /**
     * Get formatted message list with prefix
     */
    public List<String> getMessageList(String path) {
        List<String> messages = getStringList(path);
        String prefix = getString("messages.prefix", "");
        
        if (!prefix.isEmpty()) {
            messages.replaceAll(message -> prefix + " " + message);
        }
        
        return messages;
    }
}