package com.donutxorders.managers;

import com.donutxorders.core.DonutxOrders;
import com.donutxorders.core.ConfigManager;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.logging.Level;

public class EconomyManager {

    private final DonutxOrders plugin;
    private final ConfigManager configManager;
    private Economy economy;

    public EconomyManager(DonutxOrders plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        setupEconomy();
    }

    private void setupEconomy() {
        if (Bukkit.getServer().getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().severe("Vault not found! Economy features will be disabled.");
            return;
        }
        var rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().severe("No economy provider found! Economy features will be disabled.");
            return;
        }
        economy = rsp.getProvider();
        plugin.getLogger().info("Vault economy hooked: " + economy.getName());
    }

    public boolean hasBalance(OfflinePlayer player, double amount) {
        if (economy == null) return false;
        return economy.has(player, amount);
    }

    public boolean withdrawMoney(OfflinePlayer player, double amount) {
        if (economy == null) {
            plugin.getLogger().warning("Economy not available for withdrawal.");
            return false;
        }
        if (amount < 0) {
            plugin.getLogger().warning("Attempted to withdraw negative amount: " + amount);
            return false;
        }
        var response = economy.withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().log(Level.WARNING, "Failed to withdraw {0} from {1}: {2}",
                    new Object[]{amount, player.getName(), response.errorMessage});
        }
        return response.transactionSuccess();
    }

    public boolean depositMoney(OfflinePlayer player, double amount) {
        if (economy == null) {
            plugin.getLogger().warning("Economy not available for deposit.");
            return false;
        }
        if (amount < 0) {
            plugin.getLogger().warning("Attempted to deposit negative amount: " + amount);
            return false;
        }
        var response = economy.depositPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().log(Level.WARNING, "Failed to deposit {0} to {1}: {2}",
                    new Object[]{amount, player.getName(), response.errorMessage});
        }
        return response.transactionSuccess();
    }

    public double getBalance(OfflinePlayer player) {
        if (economy == null) return 0.0;
        return economy.getBalance(player);
    }

    // Support for custom money commands from config (if needed)
    public boolean runCustomMoneyCommand(Player player, String command, double amount) {
        if (command == null || command.isEmpty()) return false;
        String cmd = command.replace("{player}", player.getName()).replace("{amount}", String.valueOf(amount));
        try {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to execute custom money command: " + cmd, e);
            return false;
        }
    }

    // Support for multiple currency types (stub, expand as needed)
    public String getCurrencySymbol() {
        return configManager.getString("economy.currency-symbol", "$");
    }

    public int getDecimalPlaces() {
        return configManager.getInt("economy.decimal-places", 2);
    }
}