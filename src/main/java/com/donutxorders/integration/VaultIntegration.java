package com.donutxorders.integration;

import com.donutxorders.core.DonutxOrders;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.logging.Level;

public class VaultIntegration {

    private final DonutxOrders plugin;
    private Economy economy;

    public VaultIntegration(DonutxOrders plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    // Setup Vault economy provider
    public boolean setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault plugin not found! Economy features will be disabled.");
            return false;
        }
        org.bukkit.plugin.RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().severe("No economy provider found via Vault!");
            return false;
        }
        economy = rsp.getProvider();
        plugin.getLogger().info("Vault economy hooked: " + economy.getName());
        return true;
    }

    // Check if a player has an account
    public boolean hasAccount(OfflinePlayer player) {
        if (economy == null) return false;
        return economy.hasAccount(player);
    }

    // Get a player's balance
    public double getBalance(OfflinePlayer player) {
        if (economy == null) return 0.0;
        return economy.getBalance(player);
    }

    // Withdraw money from a player
    public boolean withdrawPlayer(OfflinePlayer player, double amount) {
        if (economy == null) {
            plugin.getLogger().warning("Economy not available for withdrawal.");
            return false;
        }
        if (amount < 0) {
            plugin.getLogger().warning("Attempted to withdraw negative amount: " + amount);
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().log(Level.WARNING, "Failed to withdraw {0} from {1}: {2}",
                    new Object[]{amount, player.getName(), response.errorMessage});
        }
        return response.transactionSuccess();
    }

    // Deposit money to a player
    public boolean depositPlayer(OfflinePlayer player, double amount) {
        if (economy == null) {
            plugin.getLogger().warning("Economy not available for deposit.");
            return false;
        }
        if (amount < 0) {
            plugin.getLogger().warning("Attempted to deposit negative amount: " + amount);
            return false;
        }
        EconomyResponse response = economy.depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().log(Level.WARNING, "Failed to deposit {0} to {1}: {2}",
                    new Object[]{amount, player.getName(), response.errorMessage});
        }
        return response.transactionSuccess();
    }

    // Get the economy provider (if needed elsewhere)
    public Economy getEconomy() {
        org.bukkit.plugin.RegisteredServiceProvider<Economy> response = economy == null ? null : plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (response != null && !response.getProvider().equals(economy)) {
            economy = response.getProvider();
        }
        return economy;
    }
}