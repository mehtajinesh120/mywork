package com.donutxorders.integration;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class NBTApiIntegration {

    // Check if NBTApi is available
    public static boolean hasNBTApi() {
        return Bukkit.getPluginManager().getPlugin("NBTAPI") != null;
    }

    // Compare NBT data of two ItemStacks
    public static boolean compareNBT(ItemStack a, ItemStack b) {
        if (a == null || b == null) return false;
        if (!hasNBTApi()) return true; // fallback: treat as equal if NBTApi not present
        NBTItem nbtA = new NBTItem(a);
        NBTItem nbtB = new NBTItem(b);
        return nbtA.toString().equals(nbtB.toString());
    }

    // Get a string value from NBT
    public static String getNBTString(ItemStack item, String key) {
        if (item == null || key == null || !hasNBTApi()) return null;
        NBTItem nbt = new NBTItem(item);
        return nbt.getString(key);
    }

    // Set a string value in NBT
    public static ItemStack setNBT(ItemStack item, String key, String value) {
        if (item == null || key == null || value == null || !hasNBTApi()) return item;
        NBTItem nbt = new NBTItem(item);
        nbt.setString(key, value);
        return nbt.getItem();
    }

    // Remove a key from NBT
    public static ItemStack removeNBT(ItemStack item, String key) {
        if (item == null || key == null || !hasNBTApi()) return item;
        NBTItem nbt = new NBTItem(item);
        nbt.removeKey(key);
        return nbt.getItem();
    }
}