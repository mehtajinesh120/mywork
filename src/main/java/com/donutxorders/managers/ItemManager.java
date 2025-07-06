package com.donutxorders.managers;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ItemManager {

    // Compare two items, including NBT and custom model data
    public boolean compareItems(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (a.getType() != b.getType()) return false;
        ItemMeta metaA = a.getItemMeta();
        ItemMeta metaB = b.getItemMeta();
        if (metaA != null && metaB != null) {
            if (metaA.hasCustomModelData() != metaB.hasCustomModelData()) return false;
            if (metaA.hasCustomModelData() && metaB.hasCustomModelData()) {
                if (!metaA.getCustomModelData().equals(metaB.getCustomModelData())) return false;
            }
        } else if (metaA != null || metaB != null) {
            return false;
        }
        // NBT comparison using NBTAPI
        NBTItem nbtA = new NBTItem(a);
        NBTItem nbtB = new NBTItem(b);
        return nbtA.toString().equals(nbtB.toString());
    }

    // Serialize an ItemStack to a Base64 string
    public String serializeItem(ItemStack item) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ObjectOutputStream dataOutput = new ObjectOutputStream(outputStream);
            dataOutput.writeObject(item);
            dataOutput.close();
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            return null;
        }
    }

    // Deserialize an ItemStack from a Base64 string
    public ItemStack deserializeItem(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            ObjectInputStream inputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
            Object obj = inputStream.readObject();
            inputStream.close();
            if (obj instanceof ItemStack) {
                return (ItemStack) obj;
            }
        } catch (Exception e) {
            // Handle error
        }
        return null;
    }

    // Validate if an item is allowed for orders
    public boolean isValidItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        // Add more validation as needed (e.g., blacklist certain items)
        return true;
    }

    // Get a user-friendly item name
    public String getItemName(ItemStack item) {
        if (item == null) return "Unknown";
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return item.getType().name().replace("_", " ").toLowerCase();
    }

    // Example: categorize items for GUI display (stub)
    public String getItemCategory(ItemStack item) {
        if (item == null) return "other";
        Material mat = item.getType();
        if (mat.isEdible()) return "food";
        if (mat.isBlock()) return "block";
        if (mat.name().endsWith("_SWORD") || mat.name().endsWith("_AXE")) return "weapon";
        // Add more categories as needed
        return "other";
    }
}