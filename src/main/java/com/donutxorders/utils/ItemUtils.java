package com.donutxorders.utils;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ItemUtils {

    // Create a GUI item with name, lore, and optional glow
    public static ItemStack createGuiItem(Material material, String name, List<String> lore, boolean glow) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(MessageUtils.colorize(name));
            if (lore != null) meta.setLore(MessageUtils.colorize(lore));
            if (glow) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    // Validate if a material is allowed for orders
    public static boolean isValidMaterial(Material material) {
        if (material == null || material == Material.AIR) return false;
        // Add blacklist/whitelist logic as needed
        return true;
    }

    // Get item category for GUI display
    public static String getItemCategory(ItemStack item) {
        if (item == null) return "other";
        Material mat = item.getType();
        if (mat.isEdible()) return "food";
        if (mat.isBlock()) return "block";
        if (mat.name().endsWith("_SWORD") || mat.name().endsWith("_AXE")) return "weapon";
        // Add more categories as needed
        return "other";
    }

    // Compare two ItemStacks, including NBT and custom model data
    public static boolean compareItemStacks(ItemStack a, ItemStack b) {
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
}