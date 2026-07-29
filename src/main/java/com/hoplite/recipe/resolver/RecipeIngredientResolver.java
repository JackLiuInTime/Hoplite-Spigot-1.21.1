package com.hoplite.recipe.resolver;

import com.hoplite.HoplitePlugin;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;

import java.util.List;
import java.util.Locale;

/**
 * Resolves raw ingredient tokens from recipe files into Bukkit recipe ingredients.
 *
 * Supported syntaxes:
 * - minecraft:material_name
 * - material_name
 * - material_name|exact
 * - legendary:<weapon_id>
 * - global:<item_id>
 * - combat:<item_id>
 * - utility:<item_id>
 * - custom:<item_id> (legacy alias)
 */
public class RecipeIngredientResolver {

    private final HoplitePlugin plugin;

    public RecipeIngredientResolver(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    public Object resolve(String rawValue, String recipeId, List<String> problems) {
        if (rawValue == null || rawValue.isBlank()) {
            problems.add("Empty ingredient value in recipe: " + recipeId);
            return null;
        }

        String value = rawValue.trim();
        String lower = value.toLowerCase(Locale.ROOT);

        if (lower.startsWith("legendary:")) {
            String id = value.substring("legendary:".length()).trim().toLowerCase(Locale.ROOT);
            var handlerOpt = plugin.getWeaponManager().getHandlerById(id);
            if (handlerOpt.isEmpty()) {
                problems.add("Unknown legendary ingredient id: " + id);
                return null;
            }
            ItemStack item = handlerOpt.get().createItem(plugin);
            return new RecipeChoice.ExactChoice(item);
        }

        if (lower.startsWith("global:") || lower.startsWith("combat:") || lower.startsWith("utility:") || lower.startsWith("custom:")) {
            String scopedId = buildScopedId(value, lower);
            var customItemOpt = plugin.getGlobalIngredientRegistry().get(scopedId);
            if (customItemOpt.isEmpty()) {
                problems.add("Unknown custom ingredient id: " + scopedId);
                return null;
            }
            return new RecipeChoice.ExactChoice(customItemOpt.get());
        }

        boolean exact = lower.endsWith("|exact");
        String materialToken = exact ? value.substring(0, value.length() - 6).trim() : value;
        String materialName = normalizeMaterialName(materialToken);

        if (exact) {
            try {
                return new RecipeChoice.ExactChoice(new ItemStack(Material.valueOf(materialName)));
            } catch (Exception ex) {
                problems.add("Invalid exact material: " + materialName);
                return null;
            }
        }

        try {
            return Material.valueOf(materialName);
        } catch (Exception ex) {
            if (materialName.endsWith("S")) {
                String singular = materialName.substring(0, materialName.length() - 1);
                try {
                    Material fixed = Material.valueOf(singular);
                    plugin.getLogger().info("Auto-corrected material " + materialName + " -> " + singular + " for recipe " + recipeId);
                    return fixed;
                } catch (Exception ignored) {
                    // fall through to problem record
                }
            }
            problems.add("Invalid material: " + materialName);
            return null;
        }
    }

    private String buildScopedId(String raw, String lower) {
        if (lower.startsWith("global:")) {
            return "global:" + raw.substring("global:".length()).trim().toLowerCase(Locale.ROOT);
        }
        if (lower.startsWith("combat:")) {
            return "combat:" + raw.substring("combat:".length()).trim().toLowerCase(Locale.ROOT);
        }
        if (lower.startsWith("utility:")) {
            return "utility:" + raw.substring("utility:".length()).trim().toLowerCase(Locale.ROOT);
        }
        return "custom:" + raw.substring("custom:".length()).trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeMaterialName(String rawName) {
        String name = rawName.trim();
        if (name.toLowerCase(Locale.ROOT).startsWith("minecraft:")) {
            name = name.substring(name.indexOf(':') + 1);
        }
        return name.toUpperCase(Locale.ROOT).trim();
    }
}
