package com.donutxorders.listeners;

import com.donutxorders.core.DonutxOrders;
import com.donutxorders.models.PlayerData;
import com.donutxorders.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final DonutxOrders plugin;
    private final Map<UUID, PlayerData> playerDataCache = new HashMap<>();

    public PlayerListener(DonutxOrders plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Load player data (async if needed)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data = new PlayerData(player.getUniqueId());
            data.updateFromDatabase(plugin);
            synchronized (playerDataCache) {
                playerDataCache.put(player.getUniqueId(), data);
            }
            // Send order notifications if needed
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.sendMessage(MessageUtils.colorize("&aWelcome! You have " + data.getActiveOrders() + " active orders."));
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        // Save player data (async if needed)
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            PlayerData data;
            synchronized (playerDataCache) {
                data = playerDataCache.remove(player.getUniqueId());
            }
            if (data != null) {
                data.saveToDatabase(plugin);
            }
        });
    }

    // Example: handle permission changes (if using a permission plugin that fires events)
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        // This is a placeholder for actual permission change event
        // If your permission plugin fires a specific event, use that instead
        if (event.getMessage().toLowerCase().startsWith("/lp user") || event.getMessage().toLowerCase().startsWith("/pex")) {
            Player player = event.getPlayer();
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                PlayerData data;
                synchronized (playerDataCache) {
                    data = playerDataCache.get(player.getUniqueId());
                }
                if (data != null) {
                    // Recalculate order limits or permissions
                    player.sendMessage(MessageUtils.colorize("&eYour permissions have been updated."));
                }
            }, 40L); // Delay to allow permission update
        }
    }

    // Utility: get cached player data
    public PlayerData getPlayerData(UUID uuid) {
        synchronized (playerDataCache) {
            return playerDataCache.get(uuid);
        }
    }
}