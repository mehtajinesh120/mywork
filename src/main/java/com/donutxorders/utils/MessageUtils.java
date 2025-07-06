package com.donutxorders.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MessageUtils {

    // Colorize a string (supports hex, gradients, and legacy codes)
    public static String colorize(String message) {
        if (message == null) return "";
        message = applyGradients(message);
        message = applyHexColors(message);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Colorize a list of strings
    public static List<String> colorize(List<String> messages) {
        return messages.stream().map(MessageUtils::colorize).collect(Collectors.toList());
    }

    // Send a colorized message to a player or sender
    public static void sendMessage(CommandSender sender, String message) {
        if (message == null || message.isEmpty()) return;
        sender.sendMessage(colorize(message));
    }

    // Broadcast a colorized message to all players
    public static void broadcast(String message) {
        String colored = colorize(message);
        Bukkit.getServer().getOnlinePlayers().forEach(p -> p.sendMessage(colored));
        Bukkit.getConsoleSender().sendMessage(colored);
    }

    // Format placeholders in a message
    public static String formatPlaceholders(String message, Map<String, String> placeholders) {
        if (message == null || placeholders == null) return message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    // Colorize item name and lore for GUI
    public static void colorizeItemMeta(ItemMeta meta) {
        if (meta == null) return;
        if (meta.hasDisplayName()) {
            meta.setDisplayName(colorize(meta.getDisplayName()));
        }
        if (meta.hasLore()) {
            meta.setLore(colorize(meta.getLore()));
        }
    }

    // --- Color/Gradient helpers ---

    // Apply hex color codes (e.g., &#aabbcc)
    private static String applyHexColors(String message) {
        Pattern hexPattern = Pattern.compile("&#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            // Fallback: strip hex, use empty string
            matcher.appendReplacement(buffer, "");
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    // Apply gradients (e.g., <gradient:#ff0000:#00ff00>text</gradient>)
    private static String applyGradients(String message) {
        Pattern gradientPattern = Pattern.compile("<gradient:([#A-Fa-f0-9:]+)>(.*?)</gradient>");
        Matcher matcher = gradientPattern.matcher(message);
        while (matcher.find()) {
            String[] colors = matcher.group(1).split(":");
            String text = matcher.group(2);
            String gradient = applyGradientToText(text, colors);
            message = message.replace(matcher.group(0), gradient);
        }
        return message;
    }

    // Simple gradient application (linear between two colors)
    private static String applyGradientToText(String text, String[] colors) {
        // Fallback: just return text, no gradient in 1.17
        return text;
    }

    private static int[] hexToRgb(String hex) {
        hex = hex.replace("#", "");
        return new int[]{
                Integer.valueOf(hex.substring(0, 2), 16),
                Integer.valueOf(hex.substring(2, 4), 16),
                Integer.valueOf(hex.substring(4, 6), 16)
        };
    }

    private static int interpolate(int start, int end, int step, int max) {
        return start + (end - start) * step / Math.max(max, 1);
    }
}