package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class EmeraldBladeHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private static final int MAX_LEVEL = 5;
    private static final String LEVEL_KEY = "emerald_blade_level";

    public EmeraldBladeHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "emerald_blade";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aEmerald Blade"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary blade that when upgraded, can prove viable in combat.",
                    "",
                    ChatColor.GOLD + "Economy Upgrade",
                    ChatColor.WHITE + "Sneak + right click to consume emeralds",
                    ChatColor.WHITE + "Increase Sharpness level with each upgrade",
                    ChatColor.DARK_GRAY + "Cooldown: None"
            ));
            meta.setCustomModelData(3);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, LEVEL_KEY), PersistentDataType.INTEGER, 1);
            meta.addEnchant(Enchantment.SHARPNESS, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private int getLevel(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return 0;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return 0;
        Integer lvl = meta.getPersistentDataContainer().get(new NamespacedKey(plugin, LEVEL_KEY), PersistentDataType.INTEGER);
        return lvl == null ? 0 : lvl;
    }

    private ItemStack upgrade(ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;
        int cur = getLevel(item);
        int next = Math.min(cur + 1, MAX_LEVEL);
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, LEVEL_KEY), PersistentDataType.INTEGER, next);
        meta.addEnchant(Enchantment.SHARPNESS, next, true);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getPlayer().isSneaking()) return;
        if (event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_AIR && event.getAction() != org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK) return;
        Player player = event.getPlayer();
        if (!player.getInventory().contains(Material.EMERALD)) {
            player.sendMessage("§cYou need one emerald to upgrade Emerald Blade.");
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        ItemStack upgraded = upgrade(item);
        player.getInventory().setItemInMainHand(upgraded);
        int level = getLevel(upgraded);
        player.sendMessage("§aEmerald Blade upgraded. Current Sharpness level: " + level);
        event.setCancelled(true);
    }
}

