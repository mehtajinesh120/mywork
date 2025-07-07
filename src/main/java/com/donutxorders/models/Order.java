package com.donutxorders.models;

import org.bukkit.inventory.ItemStack;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import com.donutxorders.models.OrderStatus;

/**
 * Represents an order in DonutxOrders.
 */
public class Order implements Serializable {

    // Default constructor for deserialization
    public Order() {}

    // Aliases for compatibility
    public long getCreatedAt() { return createdTime; }
    public void setCreatedAt(long createdAt) { this.createdTime = createdAt; }
    public int getOrderId() { return getId(); }
    public void setPlayerUuid(UUID uuid) { setCreatorUUID(uuid); }
    public UUID getPlayerId() { return getCreatorUUID(); }
    public int getAmount() { return getQuantity(); }
    public void setAmount(int amount) { setQuantity(amount); }

    // Stubs for meta/item type
    public String getMeta() { return null; }
    public void setMeta(String meta) {}
    public String getItemType() { return null; }
    public void setItemType(String itemType) {}

    private int id;
    private UUID creatorUUID;
    private ItemStack itemStack;
    private int quantity;
    private double pricePerItem;
    private int deliveredAmount;
    private long createdTime;
    private long expiresAt;
    private OrderStatus status;

    // Additional fields for plugin features
    private String world;
    private double x, y, z;
    private double fee;
    private double totalPrice;
    private String description;
    private java.util.List<OrderItem> items = new java.util.ArrayList<>();

    public Order(int id, UUID creatorUUID, ItemStack itemStack, int quantity, double pricePerItem, int deliveredAmount, long createdTime, long expiresAt, OrderStatus status) {
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

    public Order(UUID creatorUUID, ItemStack itemStack, int quantity, double pricePerItem, long createdTime, long expiresAt, OrderStatus status) {
        this(-1, creatorUUID, itemStack, quantity, pricePerItem, 0, createdTime, expiresAt, status);
    }

    // Serialization for database storage (example: to/from ResultSet)
    public static Order fromResultSet(ResultSet rs, ItemStack itemStack) throws SQLException {
        Order order = new Order(
            rs.getInt("id"),
            UUID.fromString(rs.getString("player_uuid")),
            itemStack,
            rs.getInt("quantity"),
            rs.getDouble("price_per_item"),
            rs.getInt("delivered_amount"),
            rs.getLong("created_at"),
            rs.getLong("expires_at"),
            OrderStatus.valueOf(rs.getString("status"))
        );
        order.setWorld(rs.getString("world"));
        order.setX(rs.getDouble("x"));
        order.setY(rs.getDouble("y"));
        order.setZ(rs.getDouble("z"));
        order.setFee(rs.getDouble("fee"));
        order.setTotalPrice(rs.getDouble("total_price"));
        order.setDescription(rs.getString("description"));
        // Items should be set separately if needed
        return order;
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

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    // Deprecated String status methods for compatibility
    @Deprecated
    public String getStatusString() { return status != null ? status.name() : null; }
    @Deprecated
    public void setStatusString(String status) { this.status = status != null ? OrderStatus.valueOf(status) : null; }

    // Business logic methods

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }

    public boolean isFullyFulfilled() {
        return deliveredAmount >= quantity;
    }

    public boolean canBeCancelled() {
        return !isExpired() && !isFullyFulfilled() && status != OrderStatus.CANCELLED;
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

    // --- Additional fields and methods ---
    public String getWorld() { return world; }
    public void setWorld(String world) { this.world = world; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }

    public double getY() { return y; }
    public void setY(double y) { this.y = y; }

    public double getZ() { return z; }
    public void setZ(double z) { this.z = z; }

    public double getFee() { return fee; }
    public void setFee(double fee) { this.fee = fee; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public java.util.List<OrderItem> getItems() { return items; }
    public void setItems(java.util.List<OrderItem> items) { this.items = items; }
    public void addItem(OrderItem item) { this.items.add(item); }

    // Alias for compatibility
    public UUID getPlayerUuid() { return getCreatorUUID(); }
}