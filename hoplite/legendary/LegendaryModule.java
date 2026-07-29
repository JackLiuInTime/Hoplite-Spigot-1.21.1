package com.hoplite.legendary;

import com.hoplite.HoplitePlugin;

public class LegendaryModule {
    private final HoplitePlugin plugin;
    private final LegendaryWeaponManager weaponManager;
    private final LegendaryCraftTracker craftTracker;

    public LegendaryModule(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.weaponManager = new LegendaryWeaponManager(plugin);
        this.craftTracker = new LegendaryCraftTracker(plugin);
    }

    public void enable(boolean enableWeaponRuntime) {
        registerHandlers();
        new LegendaryItemIntegrityValidator(plugin).validateAllHandlers();

        if (enableWeaponRuntime) {
            plugin.getServer().getPluginManager().registerEvents(new LegendaryWeaponListener(plugin), plugin);
            plugin.getServer().getPluginManager().registerEvents(new MiningListener(plugin), plugin);
            plugin.getServer().getPluginManager().registerEvents(new InteractionListener(plugin), plugin);
        } else {
            plugin.getLogger().warning("Weapon runtime listeners are temporarily disabled (framework-only mode).");
        }

        plugin.getServer().getPluginManager().registerEvents(new LegendaryCraftingListener(plugin), plugin);
    }

    public LegendaryWeaponManager getWeaponManager() {
        return weaponManager;
    }

    public LegendaryCraftTracker getCraftTracker() {
        return craftTracker;
    }

    private void registerHandlers() {
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.WarPickHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.EmeraldBladeHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.PoseidonsTridentHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.BeehiveBlasterHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.HypnosisStaffHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.GeraldTheSnifferHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.GuardianCannonHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.HarpoonLauncherHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.GolemHammerHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.ChronoSwordHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.EagleEyeBowHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.MagmaCannonHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.GruntildaHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.HeadhunterChestpieceHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.FreezingChakramHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.MidasSwordHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.ReapersScytheHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.SonicCrossbowHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.DragonKatanaHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.ShadowBladeHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.SceptreOfArachneHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.VampireSabreHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.MjolnirHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.EnderbowHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.WitherSicklesHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.CloudSwordHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.HornOfWinterHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.CorruptedCrossbowHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.ShrinkRayHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.ExcaliburHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.SculkweaversLanternHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.KimTheTransmuterHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.VillagerWandHandler(plugin));
        weaponManager.registerHandler(new com.hoplite.legendary.legendary.CrimsonChainswordHandler(plugin));
    }
}
