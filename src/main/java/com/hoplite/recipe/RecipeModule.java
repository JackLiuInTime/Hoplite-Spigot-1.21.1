package com.hoplite.recipe;

import com.hoplite.HoplitePlugin;
import com.hoplite.recipe.loader.RecipeFileLoader;
import com.hoplite.recipe.manager.RecipeManager;

public class RecipeModule {
    private final HoplitePlugin plugin;
    private RecipeManager recipeManager;

    public RecipeModule(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    public void enable() {
        recipeManager = new RecipeManager(plugin);
        RecipeFileLoader loader = new RecipeFileLoader(plugin);
        loader.loadFromDataFolder();
    }

    public RecipeManager getRecipeManager() {
        return recipeManager;
    }
}
