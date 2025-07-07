package com.donutxorders.tasks;

import com.donutxorders.core.DonutxOrders;
import com.donutxorders.database.DatabaseManager;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.logging.Logger;

/**
 * Periodically performs database maintenance: removes old records and optimizes tables.
 * Cleanup interval and retention period are configurable in config.yml.
 */
public class DatabaseCleanupTask extends BukkitRunnable {

    private final DonutxOrders plugin;
    private final DatabaseManager databaseManager;
    private final Logger logger;
    private final long retentionMillis;

    public DatabaseCleanupTask(DonutxOrders plugin) {
        this.plugin = plugin;
        this.databaseManager = plugin.getDatabaseManager();
        this.logger = plugin.getLogger();
        // Retention period for completed/expired orders (default: 30 days)
        long days = plugin.getConfig().getLong("database-cleanup-retention-days", 30L);
        this.retentionMillis = days * 24 * 60 * 60 * 1000;
    }

    @Override
    public void run() {
        cleanupDatabase();
        optimizeDatabase();
    }

    /**
     * Removes old completed/expired orders and related data from the database.
     */
    public void cleanupDatabase() {
        long cutoff = System.currentTimeMillis() - retentionMillis;
        // TODO: Implement removeOldOrders in DatabaseManager
        // int removed = databaseManager.removeOldOrders(cutoff);
        // logger.info("[DonutxOrders] Database cleanup: removed " + removed + " old orders (older than " + (retentionMillis / (24 * 60 * 60 * 1000)) + " days).");
    }

    /**
     * Optimizes database tables for better performance.
     */
    public void optimizeDatabase() {
        // TODO: Implement optimizeTables in DatabaseManager
        // databaseManager.optimizeTables();
        // logger.info("[DonutxOrders] Database optimization completed.");
    }
}