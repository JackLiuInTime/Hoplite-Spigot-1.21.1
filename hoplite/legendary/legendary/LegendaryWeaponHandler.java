package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.inventory.ItemStack;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public interface LegendaryWeaponHandler {
    String getId();
    ItemStack createItem(HoplitePlugin plugin);

    default void onEntityDamageByEntity(EntityDamageByEntityEvent event) {}
    default void onOwnerDamaged(EntityDamageByEntityEvent event) {}
    default void onOwnerDamage(EntityDamageEvent event) {}
    default void onEntityShootBow(EntityShootBowEvent event) {}
    default void onBlockBreak(BlockBreakEvent event) {}
    default void onPlayerInteractEntity(PlayerInteractEntityEvent event) {}
    default void onPlayerInteract(PlayerInteractEvent event) {}
    default void onPlayerMove(PlayerMoveEvent event) {}
}

