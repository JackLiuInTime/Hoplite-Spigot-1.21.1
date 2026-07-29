package com.hoplite.global.registry;

import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Global registry for non-legendary items that can be used as exact recipe ingredients.
 *
 * Supported category prefixes in recipe files:
 * - global:<id>
 * - combat:<id>
 * - utility:<id>
 * - custom:<id> (legacy alias)
 */
public class GlobalIngredientRegistry {

    private final Map<String, ItemStack> items = new LinkedHashMap<>();

    public void register(String id, ItemStack item) {
        if (id == null || id.isBlank() || item == null) {
            return;
        }
        items.put(normalize(id), item.clone());
    }

    public void registerCombat(String id, ItemStack item) {
        register("combat:" + id, item);
    }

    public void registerGlobal(String id, ItemStack item) {
        register("global:" + id, item);
    }

    public void registerUtility(String id, ItemStack item) {
        register("utility:" + id, item);
    }

    public void registerCustom(String id, ItemStack item) {
        register("custom:" + id, item);
    }

    public Optional<ItemStack> get(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        ItemStack item = items.get(normalize(id));
        if (item == null) {
            return Optional.empty();
        }
        return Optional.of(item.clone());
    }

    private String normalize(String id) {
        return id.trim().toLowerCase(Locale.ROOT);
    }
}
