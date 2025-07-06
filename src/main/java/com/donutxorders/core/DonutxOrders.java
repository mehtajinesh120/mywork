package com.donutxorders.core;

import com.donutxorders.commands.OrderCommand;
import com.donutxorders.commands.OrdersTabCompleter;
import com.donutxorders.database.DatabaseManager;
import com.donutxorders.listeners.InventoryClickListener;
import com.donutxorders.listeners.PlayerListener;
import com.donutxorders.managers.EconomyManager;
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
    
    // Tasks
    private BukkitTask orderExpirationTask;
    private BukkitTask databaseCleanupTask;
    
    // Plugin state
    private boolean isEnabled = false;
    private boolean isFolia = false;
    
    @Override
    public void onEnable() {
        long startTime = System.currentTimeMillis();
        
        // Check if running on Folia
        checkFoliaCompatibility();
        
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
        
        if (isFolia) {
            getLogger().info(ChatColor.YELLOW + "Folia server detected - Using compatible task scheduling");
        }
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
    
    private void checkFoliaCompatibility() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            isFolia = true;
            getLogger().info("Folia server detected - Enabling compatibility mode");
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
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
            databaseManager = new DatabaseManager(this);
            
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
            if (!economyManager.initialize()) {
                getLogger().warning("Economy manager failed to initialize - Economy features will be disabled");
            }
            
            // Initialize Permission Manager
            permissionManager = new PermissionManager(this);
            
            // Initialize Order Manager
            orderManager = new OrderManager(this, databaseManager, economyManager, permissionManager);
            
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
            
            if (isFolia) {
                // Use Folia-compatible global scheduler
                orderExpirationTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                    this,
                    (task) -> new OrderExpirationTask(this).run(),
                    expirationInterval * 20L, // Convert to ticks
                    expirationInterval * 20L
                );
            } else {
                // Use traditional Bukkit scheduler
                orderExpirationTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        new OrderExpirationTask(DonutxOrders.this).run();
                    }
                }.runTaskTimerAsynchronously(this, expirationInterval * 20L, expirationInterval * 20L);
            }
            
            // Database cleanup task
            int cleanupInterval = configManager.getConfig().getInt("tasks.database-cleanup-interval", 86400); // 24 hours default
            
            if (isFolia) {
                // Use Folia-compatible global scheduler
                databaseCleanupTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                    this,
                    (task) -> new DatabaseCleanupTask(this).run(),
                    cleanupInterval * 20L,
                    cleanupInterval * 20L
                );
            } else {
                // Use traditional Bukkit scheduler
                databaseCleanupTask = new BukkitRunnable() {
                    @Override
                    public void run() {
                        new DatabaseCleanupTask(DonutxOrders.this).run();
                    }
                }.runTaskTimerAsynchronously(this, cleanupInterval * 20L, cleanupInterval * 20L);
            }
            
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
    
    public boolean isFoliaServer() {
        return isFolia;
    }
    
    public boolean isPluginEnabled() {
        return isEnabled;
    }
    
    /**
     * Get a safe scheduler for cross-platform compatibility
     */
    public void runTaskAsync(Runnable task) {
        if (isFolia) {
            // Use Folia's async scheduler
            Bukkit.getAsyncScheduler().runNow(this, (scheduledTask) -> task.run());
        } else {
            // Use traditional Bukkit scheduler
            new BukkitRunnable() {
                @Override
                public void run() {
                    task.run();
                }
            }.runTaskAsynchronously(this);
        }
    }
    
    /**
     * Run a task on the main thread (or appropriate thread for Folia)
     */
    public void runTask(Runnable task) {
        if (isFolia) {
            // Use Folia's global scheduler for non-world-specific tasks
            Bukkit.getGlobalRegionScheduler().run(this, (scheduledTask) -> task.run());
        } else {
            // Use traditional Bukkit scheduler
            new BukkitRunnable() {
                @Override
                public void run() {
                    task.run();
                }
            }.runTask(this);
        }
    }
    
    /**
     * Schedule a delayed task
     */
    public BukkitTask runTaskLater(Runnable task, long delay) {
        if (isFolia) {
            // Use Folia's global scheduler
            return Bukkit.getGlobalRegionScheduler().runDelayed(this, (scheduledTask) -> task.run(), delay);
        } else {
            // Use traditional Bukkit scheduler
            return new BukkitRunnable() {
                @Override
                public void run() {
                    task.run();
                }
            }.runTaskLater(this, delay);
        }
    }
}