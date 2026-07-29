package com.hoplite.legendary;

import com.hoplite.HoplitePlugin;

import org.bukkit.ChatColor;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.CraftingInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Handles one-time crafting rules for legendary weapons.
 */
public class LegendaryCraftingListener implements Listener {

    private static final Set<String> DROP_ONLY_CUSTOM_ITEMS = Set.of(
            "spider silk sac",
            "vampire tooth"
    );

    private final HoplitePlugin plugin;

    public LegendaryCraftingListener(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        String weaponId = resolveLegendaryId(event.getRecipe(), event.getInventory().getResult());
        if (weaponId == null) {
            return;
        }

        if (plugin.getCraftTracker().hasCrafted(weaponId)) {
            CraftingInventory inventory = event.getInventory();
            inventory.setResult(new ItemStack(Material.AIR));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        ItemStack result = event.getCurrentItem();
        if (result == null && event.getRecipe() != null) {
            result = event.getRecipe().getResult();
        }
        if (result == null || result.getType().isAir()) {
            return;
        }

        if (isDropOnlyCustomItem(result)) {
            event.setCancelled(true);
            player.sendMessage("§cThis custom item is drop-only and cannot be crafted.");
            return;
        }

        String weaponId = resolveLegendaryId(event.getRecipe(), result);
        if (weaponId == null) {
            return;
        }

        String weaponName = result.hasItemMeta() && result.getItemMeta() != null && result.getItemMeta().hasDisplayName()
                ? ChatColor.stripColor(result.getItemMeta().getDisplayName())
                : weaponId;

        // Shift-click can move results fast; disallow it to keep one-craft behavior deterministic.
        if (event.isShiftClick()) {
            event.setCancelled(true);
            plugin.getCraftTracker().recordBlocked(player.getName(), weaponId, weaponName);
            player.sendMessage("§cLegendary items can only be crafted once. Please craft with a normal click.");
            return;
        }

        if (plugin.getCraftTracker().hasCrafted(weaponId)) {
            event.setCancelled(true);
            plugin.getCraftTracker().recordBlocked(player.getName(), weaponId, weaponName);
            player.sendMessage("§cThis legendary item has already been crafted and cannot be crafted again: §f" + weaponName);
            return;
        }

        boolean accepted = plugin.getCraftTracker().recordCraft(player.getName(), weaponId, weaponName);
        if (!accepted) {
            event.setCancelled(true);
            player.sendMessage("§cThis legendary item has already been crafted and cannot be crafted again: §f" + weaponName);
            return;
        }

        preserveDragonEggInWorkbench(event, weaponId);

        plugin.getServer().broadcastMessage("§6[Legendary] §e" + player.getName() + " §fcrafted §b" + weaponName);
    }

    private void preserveDragonEggInWorkbench(CraftItemEvent event, String weaponId) {
        if (!"dragon_katana".equalsIgnoreCase(weaponId)) {
            return;
        }

        CraftingInventory craftingInventory = event.getInventory();
        ItemStack[] beforeMatrix = craftingInventory.getMatrix();
        List<Integer> eggSlots = new ArrayList<>();
        for (int i = 0; i < beforeMatrix.length; i++) {
            ItemStack ingredient = beforeMatrix[i];
            if (ingredient != null && ingredient.getType() == Material.DRAGON_EGG) {
                eggSlots.add(i);
            }
        }

        if (eggSlots.isEmpty()) {
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            ItemStack[] afterMatrix = craftingInventory.getMatrix();
            for (Integer slot : eggSlots) {
                ItemStack item = afterMatrix[slot];
                if (item == null || item.getType().isAir()) {
                    afterMatrix[slot] = new ItemStack(Material.DRAGON_EGG, 1);
                } else if (item.getType() == Material.DRAGON_EGG) {
                    item.setAmount(Math.min(item.getAmount() + 1, item.getMaxStackSize()));
                }
            }
            craftingInventory.setMatrix(afterMatrix);
        });
    }

    private String resolveLegendaryId(Recipe recipe, ItemStack result) {
        // Prefer recipe key detection because crafting outputs may lose custom metadata
        // in some server implementations.
        if (recipe instanceof Keyed keyed) {
            String namespace = keyed.getKey().getNamespace();
            String expectedNamespace = plugin.getName().toLowerCase(Locale.ROOT);
            String key = keyed.getKey().getKey();
            if (expectedNamespace.equals(namespace) && key.endsWith("_recipe") && key.length() > 7) {
                return key.substring(0, key.length() - 7).toLowerCase(Locale.ROOT);
            }
        }

        if (result == null) {
            return null;
        }
        Optional<com.hoplite.legendary.legendary.LegendaryWeaponHandler> handler = plugin.getWeaponManager().fromItem(result);
        return handler.map(h -> h.getId().toLowerCase(Locale.ROOT)).orElse(null);
    }

    private boolean isDropOnlyCustomItem(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getItemMeta() == null || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        String raw = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        if (raw == null) {
            return false;
        }
        return DROP_ONLY_CUSTOM_ITEMS.contains(raw.trim().toLowerCase(Locale.ROOT));
    }
}

