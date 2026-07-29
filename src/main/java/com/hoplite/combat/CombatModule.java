package com.hoplite.combat;

import com.hoplite.combat.combatItem.BarbedRodHandler;
import com.hoplite.combat.combatItem.CrystallizationShardHandler;
import com.hoplite.combat.combatItem.GoldenHeadHandler;
import com.hoplite.combat.combatItem.AgonyHandler;
import com.hoplite.combat.combatItem.MaceHandler;
import com.hoplite.combat.combatItem.PanaceaHandler;
import com.hoplite.combat.combatItem.BanditLeggingsHandler;
import com.hoplite.combat.combatItem.SkeletonLeggingsHandler;
import com.hoplite.combat.combatItem.VerySuspiciousStewHandler;
import com.hoplite.combat.combatItem.BlazingCrossbowHandler;
import com.hoplite.combat.combatItem.AxolotlBootsHandler;
import com.hoplite.combat.combatItem.CowboyBootsHandler;
import com.hoplite.combat.combatItem.CactusChestplateHandler;
import com.hoplite.combat.combatItem.LightNetheriteSwordHandler;
import com.hoplite.combat.combatItem.ShortswordHandler;
import com.hoplite.combat.combatItem.ShortbowHandler;
import com.hoplite.combat.combatItem.TridentHandler;
import com.hoplite.combat.combatItem.TotemOfUndyingHandler;
import com.hoplite.HoplitePlugin;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Combat module bootstrap.
 * Keeps combat runtime wiring out of legendary-specific registration flow.
 */
public final class CombatModule {

    private final HoplitePlugin plugin;
    private final CombatItemManager combatItemManager;

    public CombatModule(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.combatItemManager = new CombatItemManager(plugin);
    }

    public void enable() {
        Map<String, Supplier<CombatItemHandler>> availableHandlers = availableHandlers();
        for (Supplier<CombatItemHandler> supplier : availableHandlers.values()) {
            combatItemManager.registerHandler(supplier.get());
        }

        plugin.getLogger().info("Combat handlers enabled: " + combatItemManager.availableTypes());

        plugin.getServer().getPluginManager().registerEvents(new CombatMainListener(plugin), plugin);
    }

    public CombatItemManager getCombatItemManager() {
        return combatItemManager;
    }

    private Map<String, Supplier<CombatItemHandler>> availableHandlers() {
        Map<String, Supplier<CombatItemHandler>> map = new LinkedHashMap<>();
        map.put("golden_head", GoldenHeadHandler::new);
        map.put("totem_of_undying", TotemOfUndyingHandler::new);
        map.put("panacea", () -> new PanaceaHandler(plugin));
        map.put("barbed_rod", BarbedRodHandler::new);
        map.put("very_suspicious_stew", VerySuspiciousStewHandler::new);
        map.put("crystallization_shard", CrystallizationShardHandler::new);
        map.put("agony", AgonyHandler::new);
        map.put("blazing_crossbow", BlazingCrossbowHandler::new);
        map.put("mace", MaceHandler::new);
        map.put("cactus_chestplate", CactusChestplateHandler::new);
        map.put("skeleton_leggings", SkeletonLeggingsHandler::new);
        map.put("bandit_leggings", BanditLeggingsHandler::new);
        map.put("cowboy_boots", CowboyBootsHandler::new);
        map.put("axolotl_boots", AxolotlBootsHandler::new);
        map.put("shortsword", ShortswordHandler::new);
        map.put("light_netherite_sword", LightNetheriteSwordHandler::new);
        map.put("shortbow", ShortbowHandler::new);
        map.put("trident", TridentHandler::new);
        return map;
    }
}
