package com.donutxorders.commands;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class OrdersTabCompleter implements TabCompleter {

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        // /orders <subcommand> [args...]
        if (args.length == 1) {
            if ("reload".startsWith(args[0].toLowerCase()) && sender.hasPermission("donutxorders.admin")) {
                completions.add("reload");
            }
            // Suggest online player names
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getName().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(player.getName());
                }
            }
            // Suggest item names
            for (Material mat : Material.values()) {
                if (mat.isItem() && mat.name().toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(mat.name().toLowerCase());
                }
            }
        } else if (args.length == 2) {
            // If first arg is a player, suggest item names
            if (Bukkit.getPlayerExact(args[0]) != null) {
                for (Material mat : Material.values()) {
                    if (mat.isItem() && mat.name().toLowerCase().startsWith(args[1].toLowerCase())) {
                        completions.add(mat.name().toLowerCase());
                    }
                }
            }
            // If first arg is an item, suggest quantity
            try {
                Material.valueOf(args[0].toUpperCase(Locale.ROOT));
                for (int i = 1; i <= 64; i++) {
                    String qty = String.valueOf(i);
                    if (qty.startsWith(args[1])) {
                        completions.add(qty);
                    }
                }
            } catch (IllegalArgumentException ignored) {}
        } else if (args.length == 3) {
            // If first arg is an item and second is a quantity, suggest price
            try {
                Material.valueOf(args[0].toUpperCase(Locale.ROOT));
                Integer.parseInt(args[1]);
                for (int i = 1; i <= 100; i++) {
                    String price = String.valueOf(i);
                    if (price.startsWith(args[2])) {
                        completions.add(price);
                    }
                }
            } catch (Exception ignored) {}
        }

        // Remove duplicates and sort
        return completions.stream().distinct().sorted().collect(Collectors.toList());
    }
}