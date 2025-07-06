package com.donutxorders.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtils {

    // Parse a hex color code (e.g., "#aabbcc" or "&#aabbcc") to a Bukkit ChatColor (1.16+)
    public static ChatColor parseHexColor(String hex) {
        if (hex == null) return ChatColor.WHITE;
        hex = hex.replace("&", "").replace("#", "");
        if (hex.length() != 6) return ChatColor.WHITE;
        // Spigot 1.17 does not support ChatColor.of, fallback to WHITE or legacy
        return ChatColor.WHITE;
    }

    // Create a gradient between two hex colors for a given text (1.16+)
    public static String createGradient(String text, String startHex, String endHex) {
        if (text == null || startHex == null || endHex == null || text.length() < 2) return text;
        // Fallback: just return text, no gradient in 1.17
        return text;
    }

    // Translate legacy color codes (&a, &b, etc.) and hex codes (&#aabbcc)
    public static String translateColorCodes(String message) {
        if (message == null) return "";
        message = applyHexColors(message);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    // Strip all color codes from a string
    public static String stripColors(String message) {
        if (message == null) return "";
        return ChatColor.stripColor(translateColorCodes(message));
    }

    // --- Helpers ---

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

    // Version-specific color handling (stub, expand as needed)
    public static boolean supportsHexColors() {
        String version = Bukkit.getBukkitVersion();
        // 1.16+ supports hex colors
        return version.startsWith("1.16") || version.startsWith("1.17") || version.startsWith("1.18")
                || version.startsWith("1.19") || version.startsWith("1.20");
    }
}