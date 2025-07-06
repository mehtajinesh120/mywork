package com.donutxorders.models;

import org.bukkit.inventory.ItemStack;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Represents an order in DonutxOrders.
 */
public class Order implements Serializable {

    private int id;
    private UUID creatorUUID;
    private ItemStack itemStack;
    private int quantity;
    private double pricePerItem;
    private int deliveredAmount;
    private long createdTime;
    private long expiresAt;
    private String status;

    public Order(int id, UUID creatorUUID, ItemStack itemStack, int quantity, double pricePerItem, int deliveredAmount, long createdTime, long expiresAt, String status) {
        this.id = id;
        this.creatorUUID = creatorUUID;
        this.itemStack = itemStack;
        this.quantity = quantity;
        this.pricePerItem = pricePerItem;
        this.deliveredAmount = deliveredAmount;
        this.createdTime = createdTime;
        this.expiresAt = expiresAt;
        this.status = status;
    }

    public Order(UUID creatorUUID, ItemStack itemStack, int quantity, double pricePerItem, long createdTime, long expiresAt, String status) {
        this(-1, creatorUUID, itemStack, quantity, pricePerItem, 0, createdTime, expiresAt, status);
    }

    // Serialization for database storage (example: to/from ResultSet)
    public static Order fromResultSet(ResultSet rs, ItemStack itemStack) throws SQLException {
        return new Order(
            rs.getInt("id"),
            UUID.fromString(rs.getString("player_uuid")),
            itemStack,
            rs.getInt("quantity"),
            rs.getDouble("price_per_item"),
            rs.getInt("delivered_amount"),
            rs.getLong("created_at"),
            rs.getLong("expires_at"),
            rs.getString("status")
        );
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public UUID getCreatorUUID() { return creatorUUID; }
    public void setCreatorUUID(UUID creatorUUID) { this.creatorUUID = creatorUUID; }

    public ItemStack getItemStack() { return itemStack; }
    public void setItemStack(ItemStack itemStack) { this.itemStack = itemStack; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getPricePerItem() { return pricePerItem; }
    public void setPricePerItem(double pricePerItem) { this.pricePerItem = pricePerItem; }

    public int getDeliveredAmount() { return deliveredAmount; }
    public void setDeliveredAmount(int deliveredAmount) { this.deliveredAmount = deliveredAmount; }

    public long getCreatedTime() { return createdTime; }
    public void setCreatedTime(long createdTime) { this.createdTime = createdTime; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // Business logic methods

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public boolean isFullyFulfilled() {
        return deliveredAmount >= quantity;
    }

    public boolean canBeCancelled() {
        return !isExpired() && !isFullyFulfilled() && !"cancelled".equalsIgnoreCase(status);
    }

    public long getTimeRemaining() {
        long remaining = expiresAt - System.currentTimeMillis();
        return Math.max(remaining, 0);
    }

    public double getTotalValue() {
        return pricePerItem * quantity;
    }

    public double getAmountPaid() {
        return pricePerItem * deliveredAmount;
    }
}