package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class HeadhunterChestpieceHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;

    public HeadhunterChestpieceHandler(HoplitePlugin plugin) { this.plugin = plugin; }

    @Override
    public String getId() { return "headhunter_chestpiece"; }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.DIAMOND_CHESTPLATE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Headhunter's Chestpiece"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary chestplate emblazoned with the fury of the Headhunter.",
                    "",
                    ChatColor.GOLD + "Rage System",
                    ChatColor.WHITE + "Gain rage by dealing or taking damage",
                    ChatColor.WHITE + "Max rage grants Strength III and Speed II",
                    ChatColor.DARK_GRAY + "Cooldown: 120s after trigger"
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this.plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            meta.addEnchant(Enchantment.PROTECTION, 3, true);
            item.setItemMeta(meta);
        }
        return item;
    }
}

