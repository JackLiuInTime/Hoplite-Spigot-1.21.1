package com.hoplite.legendary;

import com.hoplite.HoplitePlugin;

import com.hoplite.legendary.legendary.LegendaryWeaponHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Startup integrity checks for legendary weapon metadata.
 * Ensures each handler creates an item with basic required data.
 */
public class LegendaryItemIntegrityValidator {
    private final HoplitePlugin plugin;

    public LegendaryItemIntegrityValidator(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    public void validateAllHandlers() {
        int passed = 0;
        int failed = 0;

        for (LegendaryWeaponHandler handler : plugin.getWeaponManager().getAllHandlers()) {
            List<String> problems = validateHandler(handler);
            if (problems.isEmpty()) {
                passed++;
                continue;
            }

            failed++;
            plugin.getLogger().warning("Integrity check failed for handler " + handler.getId() + ":");
            for (String problem : problems) {
                plugin.getLogger().warning(" - " + problem);
            }
        }

        plugin.getLogger().info("Legendary item integrity check completed. Passed=" + passed + ", Failed=" + failed);
    }

    private List<String> validateHandler(LegendaryWeaponHandler handler) {
        List<String> problems = new ArrayList<>();
        ItemStack item;

        try {
            item = handler.createItem(plugin);
        } catch (Exception ex) {
            problems.add("createItem threw exception: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            return problems;
        }

        if (item == null || item.getType().isAir()) {
            problems.add("createItem returned null or AIR item");
            return problems;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            problems.add("item meta is missing");
            return problems;
        }

        if (meta.getLore() == null || meta.getLore().isEmpty()) {
            problems.add("lore is missing");
        }

        if (meta.getEnchants().isEmpty()) {
            problems.add("enchantments are missing");
        }

        String id = meta.getPersistentDataContainer().get(plugin.getLegendaryKey(), PersistentDataType.STRING);
        if (id == null || !id.equalsIgnoreCase(handler.getId())) {
            problems.add("legendary_weapon PDC id is missing or mismatched");
        }

        return problems;
    }
}
