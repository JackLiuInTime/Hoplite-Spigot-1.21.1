package com.hoplite.combat;

import com.hoplite.HoplitePlugin;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;

public interface CombatItemHandler {
    String getId();

    ItemStack createItem(HoplitePlugin plugin);

    default void onPlayerInteract(PlayerInteractEvent event) {
    }

    default void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
    }

    default void onEntityShootBow(EntityShootBowEvent event) {
    }

    default void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
    }

    default void onEntityDeath(EntityDeathEvent event) {
    }

    default void onPlayerMove(PlayerMoveEvent event) {
    }

    default void onVehicleEnter(VehicleEnterEvent event) {
    }

    default void onVehicleExit(VehicleExitEvent event) {
    }

    default void onPlayerItemConsume(PlayerItemConsumeEvent event) {
    }

    default void onPlayerFish(PlayerFishEvent event) {
    }
}
