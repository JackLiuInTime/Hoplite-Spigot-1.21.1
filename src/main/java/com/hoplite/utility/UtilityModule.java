package com.hoplite.utility;

import com.hoplite.HoplitePlugin;
import com.hoplite.utility.UtilityItem.AresBlessingHandler;
import com.hoplite.utility.UtilityItem.AutoSmeltersPickaxeHandler;
import com.hoplite.utility.UtilityItem.BundleHandler;
import com.hoplite.utility.UtilityItem.BundledArrowsHandler;
import com.hoplite.utility.UtilityItem.EnderPackHandler;
import com.hoplite.utility.UtilityItem.ExplosivePickaxeHandler;
import com.hoplite.utility.UtilityItem.LightAnvilHandler;
import com.hoplite.utility.UtilityItem.LumberjacksAxeHandler;
import com.hoplite.utility.UtilityItem.NetherReactorCoreHandler;
import com.hoplite.utility.UtilityItem.ObsidianMakerHandler;
import com.hoplite.utility.UtilityItem.PortableVillagerHandler;
import com.hoplite.utility.UtilityItem.RevivalStarHandler;
import com.hoplite.utility.UtilityItem.SuperSmeltersPickaxeHandler;
import com.hoplite.utility.UtilityItem.TimTheEnchanterHandler;
import com.hoplite.utility.UtilityItem.TrackerPackHandler;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

public final class UtilityModule {

    private final HoplitePlugin plugin;
    private final UtilityItemManager utilityItemManager;

    public UtilityModule(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.utilityItemManager = new UtilityItemManager(plugin);
    }

    public void enable() {
        Map<String, Supplier<UtilityItemHandler>> availableHandlers = availableHandlers();
        for (Supplier<UtilityItemHandler> supplier : availableHandlers.values()) {
            utilityItemManager.registerHandler(supplier.get());
        }

        plugin.getLogger().info("Utility handlers enabled: " + utilityItemManager.availableTypes());
        plugin.getServer().getPluginManager().registerEvents(new UtilityMainListener(plugin), plugin);
    }

    public UtilityItemManager getUtilityItemManager() {
        return utilityItemManager;
    }

    private Map<String, Supplier<UtilityItemHandler>> availableHandlers() {
        Map<String, Supplier<UtilityItemHandler>> map = new LinkedHashMap<>();
        map.put("auto_smelters_pickaxe", AutoSmeltersPickaxeHandler::new);
        map.put("explosive_pickaxe", () -> new ExplosivePickaxeHandler(plugin));
        map.put("lumberjacks_axe", LumberjacksAxeHandler::new);
        map.put("bundle", BundleHandler::new);
        map.put("light_anvil", LightAnvilHandler::new);
        map.put("super_smelters_pickaxe", SuperSmeltersPickaxeHandler::new);
        map.put("obsidian_maker", ObsidianMakerHandler::new);
        map.put("tracker_pack", () -> new TrackerPackHandler(plugin));
        map.put("nether_reactor_core", () -> new NetherReactorCoreHandler(plugin));
        map.put("ares_blessing", AresBlessingHandler::new);
        map.put("bundled_arrows", BundledArrowsHandler::new);
        map.put("portable_villager", () -> new PortableVillagerHandler(plugin));
        map.put("ender_pack", () -> new EnderPackHandler(plugin));
        map.put("tim_the_enchanter", TimTheEnchanterHandler::new);
        map.put("revival_star", RevivalStarHandler::new);
        return map;
    }
}
