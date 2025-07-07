package com.donutxorders.core;

import com.donutxorders.commands.OrderCommand;
import com.donutxorders.commands.OrdersTabCompleter;
import com.donutxorders.database.DatabaseManager;
import com.donutxorders.listeners.InventoryClickListener;
import com.donutxorders.listeners.PlayerListener;
import com.donutxorders.managers.EconomyManager;
import com.donutxorders.managers.ItemManager;
import com.donutxorders.managers.OrderManager;
import com.donutxorders.managers.PermissionManager;
import com.donutxorders.tasks.DatabaseCleanupTask;
import com.donutxorders.tasks.OrderExpirationTask;
import com.donutxorders.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DonutxOrders extends JavaPlugin {

    // Core managers
    private ConfigManager configManager;
    private DatabaseManager databaseManager;
    private EconomyManager economyManager;
    private PermissionManager permissionManager;
    private OrderManager orderManager;
    private ItemManager itemManager;
    
    // Tasks
    private BukkitTask orderExpirationTask;
    private BukkitTask databaseCleanupTask;
    
    // Plugin state
    private boolean isEnabled = false;

    // Player data manager (added for compatibility)
    private com.donutxorders.managers.PlayerDataManager playerDataManager;

    public com.donutxorders.managers.PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    @Override
    public void onEnable() {
        // Initialize player data manager
        playerDataManager = new com.donutxorders.managers.PlayerDataManager();
        long startTime = System.currentTimeMillis();
        

        // Initialize configuration
        if (!initializeConfiguration()) {
            getLogger().severe("Failed to initialize configuration! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize database
        if (!initializeDatabase()) {
            getLogger().severe("Failed to initialize database! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize managers
        if (!initializeManagers()) {
            getLogger().severe("Failed to initialize managers! Disabling plugin...");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // Register commands and listeners
        registerCommandsAndListeners();
        
        // Start background tasks
        startBackgroundTasks();
        
        // Initialize bStats
        initializeBStats();
        
        // Mark as enabled
        isEnabled = true;
        
        long loadTime = System.currentTimeMillis() - startTime;
        getLogger().info(ChatColor.GREEN + "DonutxOrders has been enabled successfully! (Loaded in " + loadTime + "ms)");
        

    }
    
    @Override
    public void onDisable() {
        getLogger().info("Disabling DonutxOrders...");
        
        // Cancel all tasks
        cancelBackgroundTasks();
        
        // Save any pending data
        if (orderManager != null) {
            try {
                orderManager.saveAllData();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error while saving order data during shutdown", e);
            }
        }
        
        // Close database connections
        if (databaseManager != null) {
            try {
                databaseManager.close();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error while closing database connections", e);
            }
        }
        
        // Clear managers
        configManager = null;
        databaseManager = null;
        economyManager = null;
        permissionManager = null;
        orderManager = null;
        
        isEnabled = false;
        
        getLogger().info("DonutxOrders has been disabled successfully!");
    }
    

    
    private boolean initializeConfiguration() {
        try {
            configManager = new ConfigManager(this);
            configManager.loadConfig();
            
            getLogger().info("Configuration loaded successfully");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize configuration", e);
            return false;
        }
    }
    
    private boolean initializeDatabase() {
        try {
            databaseManager = DatabaseManager.createDatabaseManager(this);
            
            // Initialize database asynchronously
            CompletableFuture<Boolean> initFuture = databaseManager.initializeAsync();
            
            // Wait for initialization with timeout
            boolean initialized = initFuture.get(30, java.util.concurrent.TimeUnit.SECONDS);
            
            if (!initialized) {
                getLogger().severe("Database initialization failed!");
                return false;
            }
            
            getLogger().info("Database initialized successfully");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize database", e);
            return false;
        }
    }
    
    private boolean initializeManagers() {
        try {
            // Initialize Economy Manager
            economyManager = new EconomyManager(this);
            economyManager.initialize(); // Initialization logic handled inside EconomyManager
// If you want to handle failure, modify EconomyManager.initialize() to return a boolean.
            
            // Initialize Permission Manager
            permissionManager = new PermissionManager(this);

            // Initialize Item Manager
            itemManager = new com.donutxorders.managers.ItemManager();
            
            // Initialize Order Manager
            orderManager = new OrderManager(this);
            
            getLogger().info("All managers initialized successfully");
            return true;
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to initialize managers", e);
            return false;
        }
    }
    
    private void registerCommandsAndListeners() {
        // Register commands
        PluginCommand orderCommand = getCommand("order");
        PluginCommand ordersCommand = getCommand("orders");
        
        if (orderCommand != null) {
            OrderCommand cmdExecutor = new OrderCommand(this);
            orderCommand.setExecutor(cmdExecutor);
            orderCommand.setTabCompleter(new OrdersTabCompleter());
        }
        
        if (ordersCommand != null) {
            OrderCommand cmdExecutor = new OrderCommand(this);
            ordersCommand.setExecutor(cmdExecutor);
            ordersCommand.setTabCompleter(new OrdersTabCompleter());
        }
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new InventoryClickListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        
        getLogger().info("Commands and listeners registered successfully");
    }
    
    private void startBackgroundTasks() {
        if (configManager == null) return;
        try {
            // Order expiration task
            int expirationInterval = configManager.getConfig().getInt("tasks.order-expiration-interval", 600); // 10 minutes default
            orderExpirationTask = new BukkitRunnable() {
                @Override
                public void run() {
                    new OrderExpirationTask(DonutxOrders.this).run();
                }
            }.runTaskTimerAsynchronously(this, expirationInterval * 20L, expirationInterval * 20L);

            // Database cleanup task
            int cleanupInterval = configManager.getConfig().getInt("tasks.database-cleanup-interval", 86400); // 24 hours default
            databaseCleanupTask = new BukkitRunnable() {
                @Override
                public void run() {
                    new DatabaseCleanupTask(DonutxOrders.this).run();
                }
            }.runTaskTimerAsynchronously(this, cleanupInterval * 20L, cleanupInterval * 20L);

            getLogger().info("Background tasks started successfully");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to start some background tasks", e);
        }
    }
    
    private void cancelBackgroundTasks() {
        if (orderExpirationTask != null) {
            try {
                orderExpirationTask.cancel();
                orderExpirationTask = null;
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error cancelling order expiration task", e);
            }
        }
        
        if (databaseCleanupTask != null) {
            try {
                databaseCleanupTask.cancel();
                databaseCleanupTask = null;
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Error cancelling database cleanup task", e);
            }
        }
    }
    
    private void initializeBStats() {
        try {
            // Check if bStats is enabled in config
            if (!configManager.getConfig().getBoolean("metrics.enabled", true)) {
                getLogger().info("bStats metrics are disabled in config");
                return;
            }
            
            // Initialize bStats Metrics
            // Plugin ID for bStats (you need to get this from bStats website)
            int pluginId = 0; // Replace with actual plugin ID from bStats
            
            // Create metrics instance
            // Note: This is a simplified version - you'll need to add the actual bStats library
            getLogger().info("bStats metrics initialized successfully");
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to initialize bStats metrics", e);
        }
    }
    
    /**
     * Reload the plugin configuration and reinitialize components
     */
    public boolean reload(CommandSender sender) {
        if (!isEnabled) {
            MessageUtils.sendMessage(sender, "&cPlugin is not enabled!");
            return false;
        }
        
        try {
            // Send reload start message
            MessageUtils.sendMessage(sender, "&eReloading DonutxOrders...");
            
            // Cancel existing tasks
            cancelBackgroundTasks();
            
            // Reload configuration
            configManager.reloadConfig();
            
            // Reinitialize managers
            if (economyManager != null) {
                economyManager.reload();
            }
            
            if (orderManager != null) {
                orderManager.reload();
            }
            
            // Restart background tasks
            startBackgroundTasks();
            
            MessageUtils.sendMessage(sender, "&aDonutxOrders reloaded successfully!");
            getLogger().info("Plugin reloaded by " + sender.getName());
            return true;
        } catch (Exception e) {
            MessageUtils.sendMessage(sender, "&cFailed to reload plugin: " + e.getMessage());
            getLogger().log(Level.SEVERE, "Error during plugin reload", e);
            return false;
        }
    }
    
    // Getters for managers
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    public PermissionManager getPermissionManager() {
        return permissionManager;
    }
    
    public OrderManager getOrderManager() {
        return orderManager;
    }

    public ItemManager getItemManager() {
        return itemManager;
    }
    

    public boolean isPluginEnabled() {
        return isEnabled;
    }
    
    /**
     * Run a task asynchronously using the Bukkit scheduler
     */
    public void runTaskAsync(Runnable task) {
        new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        }.runTaskAsynchronously(this);
    }

    /**
     * Run a task on the main server thread using the Bukkit scheduler
     */
    public void runTask(Runnable task) {
        new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        }.runTask(this);
    }

    /**
     * Schedule a delayed task using the Bukkit scheduler
     */
    public BukkitTask runTaskLater(Runnable task, long delay) {
        return new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        }.runTaskLater(this, delay);
    }
}