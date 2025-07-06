package com.donutxorders.utils;

public class TimeUtils {

    // Format a duration in milliseconds to a human-readable string (e.g., "2h 5m 10s")
    public static String formatDuration(long millis) {
        if (millis < 0) millis = 0;
        long seconds = millis / 1000;
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    // Check if a timestamp is expired
    public static boolean isExpired(long expiresAt) {
        return System.currentTimeMillis() > expiresAt;
    }

    // Get remaining time in milliseconds until expiration
    public static long getRemainingTime(long expiresAt) {
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    // Format remaining time for GUI/messages (e.g., "Expires in: 1h 5m")
    public static String formatTimeRemaining(long expiresAt) {
        long remaining = getRemainingTime(expiresAt);
        if (remaining <= 0) {
            return "Expired";
        }
        return "Expires in: " + formatDuration(remaining);
    }
}