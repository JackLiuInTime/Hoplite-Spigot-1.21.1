package com.hoplite.recipe.manager;

import com.hoplite.HoplitePlugin;

public class RecipeManager {
    private final HoplitePlugin plugin;

    public RecipeManager(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    public void registerAll() {
        // Legacy no-op: recipes are loaded by RecipeFileLoader from JSON.
        // Keep this method only for backward compatibility with older call sites.
        plugin.getLogger().fine("RecipeManager.registerAll() skipped (JSON recipe loader is active)");
    }
}
