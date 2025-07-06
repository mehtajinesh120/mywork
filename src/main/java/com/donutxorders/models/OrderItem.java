package com.donutxorders.models;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;
import java.util.UUID;

/**
 * Represents an item in an order, including delivery and payment details.
 */
public class OrderItem implements Serializable {

    private int id;
    private int orderId;
    private UUID delivererUUID;
    private ItemStack itemStack;
    private int quantity;
    private long deliveredTime;
    private double paymentAmount;

    public OrderItem(int id, int orderId, UUID delivererUUID, ItemStack itemStack, int quantity, long deliveredTime, double paymentAmount) {
        this.id = id;
        this.orderId = orderId;
        this.delivererUUID = delivererUUID;
        this.itemStack = itemStack;
        this.quantity = quantity;
        this.deliveredTime = deliveredTime;
        this.paymentAmount = paymentAmount;
    }

    public OrderItem(int orderId, UUID delivererUUID, ItemStack itemStack, int quantity, long deliveredTime, double paymentAmount) {
        this(-1, orderId, delivererUUID, itemStack, quantity, deliveredTime, paymentAmount);
    }

    // Serialization for database storage (example: to/from ResultSet)
    public static OrderItem fromResultSet(ResultSet rs, ItemStack itemStack) throws SQLException {
        return new OrderItem(
            rs.getInt("id"),
            rs.getInt("order_id"),
            rs.getString("deliverer_uuid") != null ? UUID.fromString(rs.getString("deliverer_uuid")) : null,
            itemStack,
            rs.getInt("quantity"),
            rs.getLong("delivered_at"),
            rs.getDouble("payment_amount")
        );
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getOrderId() { return orderId; }
    public void setOrderId(int orderId) { this.orderId = orderId; }

    public UUID getDelivererUUID() { return delivererUUID; }
    public void setDelivererUUID(UUID delivererUUID) { this.delivererUUID = delivererUUID; }

    public ItemStack getItemStack() { return itemStack; }
    public void setItemStack(ItemStack itemStack) { this.itemStack = itemStack; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public long getDeliveredTime() { return deliveredTime; }
    public void setDeliveredTime(long deliveredTime) { this.deliveredTime = deliveredTime; }

    public double getPaymentAmount() { return paymentAmount; }
    public void setPaymentAmount(double paymentAmount) { this.paymentAmount = paymentAmount; }

    // Validation: Check if the item is valid for the order
    public boolean isValid() {
        return itemStack != null && quantity > 0;
    }

    // Comparison: Match item type, NBT, and custom model data
    public boolean matches(ItemStack other) {
        if (other == null) return false;
        if (!itemStack.getType().equals(other.getType())) return false;

        ItemMeta meta1 = itemStack.getItemMeta();
        ItemMeta meta2 = other.getItemMeta();

        // Compare custom model data
        if (meta1 != null && meta2 != null) {
            if (meta1.hasCustomModelData() != meta2.hasCustomModelData()) return false;
            if (meta1.hasCustomModelData() && meta2.hasCustomModelData()) {
                if (meta1.getCustomModelData() != meta2.getCustomModelData()) return false;
            }
            // Compare NBT (PersistentDataContainer)
            PersistentDataContainer pdc1 = meta1.getPersistentDataContainer();
            PersistentDataContainer pdc2 = meta2.getPersistentDataContainer();
            if (!comparePersistentData(pdc1, pdc2)) return false;
        } else if (meta1 != null || meta2 != null) {
            return false;
        }

        return true;
    }

    // Helper: Compare PersistentDataContainer (NBT)
    private boolean comparePersistentData(PersistentDataContainer pdc1, PersistentDataContainer pdc2) {
        if (pdc1 == null && pdc2 == null) return true;
        if (pdc1 == null || pdc2 == null) return false;
        if (!pdc1.getKeys().equals(pdc2.getKeys())) return false;
        for (NamespacedKey key : pdc1.getKeys()) {
            Object v1 = pdc1.has(key) ? pdc1.get(key, pdc1.get(key).getClass()) : null;
            Object v2 = pdc2.has(key) ? pdc2.get(key, pdc2.get(key).getClass()) : null;
            if (!Objects.equals(v1, v2)) return false;
        }
        return true;
    }

    // Equality based on itemStack, quantity, and orderId
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem)) return false;
        OrderItem that = (OrderItem) o;
        return orderId == that.orderId &&
                quantity == that.quantity &&
                Objects.equals(itemStack, that.itemStack);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, itemStack, quantity);
    }
}