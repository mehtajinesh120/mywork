package com.donutxorders.commands;

import com.donutxorders.core.DonutxOrders;
import com.donutxorders.core.ConfigManager;
import com.donutxorders.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderCommand implements CommandExecutor, TabCompleter {

    private final DonutxOrders plugin;
    private final ConfigManager configManager;

    public OrderCommand(DonutxOrders plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // /order or /orders
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Only players can use this command.");
                return true;
            }
            Player player = (Player) sender;
            // Open main GUI (implement your GUI logic here)
            player.sendMessage(ChatColor.GREEN + "Opening main order GUI...");
            // plugin.getOrderManager().openMainGUI(player); // Uncomment if implemented
            return true;
        }

        // /orders reload
        if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("donutxorders.admin")) {
                sender.sendMessage(ChatColor.RED + "You do not have permission to reload the plugin.");
                return true;
            }
            boolean reloaded = plugin.reload(sender);
            if (reloaded) {
                sender.sendMessage(ChatColor.GREEN + "DonutxOrders reloaded successfully.");
            } else {
                sender.sendMessage(ChatColor.RED + "Failed to reload DonutxOrders. Check the console for errors.");
            }
            return true;
        }

        // Unknown or invalid usage
        sender.sendMessage(ChatColor.RED + "Invalid usage. Try /order or /orders reload");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase()) && sender.hasPermission("donutxorders.admin")) {
                completions.add("reload");
            }
        }
        return completions;
    }
}