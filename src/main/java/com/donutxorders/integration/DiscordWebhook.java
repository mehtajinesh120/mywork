package com.donutxorders.integration;

import com.donutxorders.core.ConfigManager;
import com.donutxorders.core.DonutxOrders;
import com.donutxorders.models.Order;
import org.bukkit.Bukkit;

import javax.net.ssl.HttpsURLConnection;
import java.awt.*;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhook {

    private final DonutxOrders plugin;
    private final ConfigManager configManager;

    public DiscordWebhook(DonutxOrders plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
    }

    public void sendOrderCreated(Order order) {
        if (!configManager.getBoolean("discord.enabled", false)) return;
        String webhookUrl = configManager.getString("discord.webhook-url", "");
        if (webhookUrl.isEmpty()) return;
        String title = configManager.getString("messages.discord.new-order-title", "New Order Created");
        String description = configManager.getString("messages.discord.new-order-description", "A new order has been created!");
        String color = configManager.getString("discord.embed-color", "#4ecdc4");
        sendWebhookAsync(webhookUrl, title, description, color, order);
    }

    public void sendOrderFulfilled(Order order) {
        if (!configManager.getBoolean("discord.enabled", false)) return;
        String webhookUrl = configManager.getString("discord.webhook-url", "");
        if (webhookUrl.isEmpty()) return;
        String title = configManager.getString("messages.discord.completed-order-title", "Order Completed");
        String description = configManager.getString("messages.discord.completed-order-description", "An order has been completed!");
        String color = "#43b581";
        sendWebhookAsync(webhookUrl, title, description, color, order);
    }

    public void sendOrderCancelled(Order order) {
        if (!configManager.getBoolean("discord.enabled", false)) return;
        String webhookUrl = configManager.getString("discord.webhook-url", "");
        if (webhookUrl.isEmpty()) return;
        String title = "Order Cancelled";
        String description = "An order has been cancelled!";
        String color = "#e74c3c";
        sendWebhookAsync(webhookUrl, title, description, color, order);
    }

    public void sendOrderExpired(Order order) {
        if (!configManager.getBoolean("discord.enabled", false)) return;
        String webhookUrl = configManager.getString("discord.webhook-url", "");
        if (webhookUrl.isEmpty()) return;
        String title = configManager.getString("messages.discord.expired-order-title", "Order Expired");
        String description = configManager.getString("messages.discord.expired-order-description", "An order has expired!");
        String color = "#f1c40f";
        sendWebhookAsync(webhookUrl, title, description, color, order);
    }

    private void sendWebhookAsync(String webhookUrl, String title, String description, String color, Order order) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL(webhookUrl);
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");

                String json = buildEmbedJson(title, description, color, order);
                try (OutputStream os = connection.getOutputStream()) {
                    os.write(json.getBytes(StandardCharsets.UTF_8));
                }

                int responseCode = connection.getResponseCode();
                if (responseCode != 204 && responseCode != 200) {
                    plugin.getLogger().warning("Discord webhook failed: HTTP " + responseCode);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }

    private String buildEmbedJson(String title, String description, String color, Order order) {
        int rgb = parseColor(color);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"embeds\":[{");
        sb.append("\"title\":\"").append(escapeJson(title)).append("\",");
        sb.append("\"description\":\"").append(escapeJson(description)).append("\\nOrder ID: ").append(order.getId()).append("\",");
        sb.append("\"color\":").append(rgb).append(",");
        sb.append("\"fields\":[");
        sb.append("{\"name\":\"Player\",\"value\":\"").append(escapeJson(order.getCreatorUUID().toString())).append("\",\"inline\":true},");
        sb.append("{\"name\":\"Item\",\"value\":\"").append(escapeJson(order.getItemStack() != null ? order.getItemStack().getType().name() : "N/A")).append("\",\"inline\":true},");
        sb.append("{\"name\":\"Quantity\",\"value\":\"").append(order.getQuantity()).append("\",\"inline\":true}");
        sb.append("]");
        sb.append("}]}");
        return sb.toString();
    }

    private int parseColor(String hex) {
        try {
            return Integer.parseInt(hex.replace("#", ""), 16);
        } catch (Exception e) {
            return Color.CYAN.getRGB();
        }
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
    }