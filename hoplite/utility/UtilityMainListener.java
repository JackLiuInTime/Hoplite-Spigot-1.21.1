package com.hoplite.utility;

import com.hoplite.HoplitePlugin;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
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
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class UtilityMainListener implements Listener {
    private final HoplitePlugin plugin;

    public UtilityMainListener(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        ItemStack item = event.getItem();
        Optional<UtilityItemHandler> handler = plugin.getUtilityItemManager().fromItem(item);
        handler.ifPresent(h -> h.onPlayerInteract(event));
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Optional<UtilityItemHandler> handler = plugin.getUtilityItemManager().fromItem(item);
        handler.ifPresent(h -> h.onBlockBreak(event));
    }

    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player attacker = null;
        if (event.getDamager() instanceof Player player) {
            attacker = player;
        } else if (event.getDamager() instanceof Arrow arrow && arrow.getShooter() instanceof Player player) {
            attacker = player;
        }

        if (attacker == null) {
            return;
        }

        ItemStack item = attacker.getInventory().getItemInMainHand();
        Optional<UtilityItemHandler> handler = plugin.getUtilityItemManager().fromItem(item);
        handler.ifPresent(h -> h.onEntityDamageByEntity(event));

        if (event.getEntity() instanceof Player defender) {
            for (UtilityItemHandler armorHandler : plugin.getUtilityItemManager().fromArmor(defender)) {
                armorHandler.onEntityDamageByEntity(event);
            }
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack weapon = event.getBow();
        Optional<UtilityItemHandler> handler = plugin.getUtilityItemManager().fromItem(weapon);
        handler.ifPresent(h -> h.onEntityShootBow(event));

        for (UtilityItemHandler armorHandler : plugin.getUtilityItemManager().fromArmor(player)) {
            armorHandler.onEntityShootBow(event);
        }
    }

    @EventHandler
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player player)) {
            return;
        }

        for (UtilityItemHandler armorHandler : plugin.getUtilityItemManager().fromArmor(player)) {
            armorHandler.onEntityTargetLivingEntity(event);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) {
            return;
        }

        for (UtilityItemHandler armorHandler : plugin.getUtilityItemManager().fromArmor(killer)) {
            armorHandler.onEntityDeath(event);
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        for (UtilityItemHandler armorHandler : plugin.getUtilityItemManager().fromArmor(event.getPlayer())) {
            armorHandler.onPlayerMove(event);
        }
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getEntered() instanceof Player player)) {
            return;
        }

        for (UtilityItemHandler armorHandler : plugin.getUtilityItemManager().fromArmor(player)) {
            armorHandler.onVehicleEnter(event);
        }
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player player)) {
            return;
        }

        for (UtilityItemHandler armorHandler : plugin.getUtilityItemManager().fromArmor(player)) {
            armorHandler.onVehicleExit(event);
        }
    }

    @EventHandler
    public void onPlayerConsume(PlayerItemConsumeEvent event) {
        ItemStack consumed = event.getItem();
        Optional<UtilityItemHandler> handler = plugin.getUtilityItemManager().fromItem(consumed);
        handler.ifPresent(h -> h.onPlayerItemConsume(event));
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        ItemStack held = event.getPlayer().getInventory().getItemInMainHand();
        Optional<UtilityItemHandler> handler = plugin.getUtilityItemManager().fromItem(held);
        handler.ifPresent(h -> h.onPlayerFish(event));
    }
}
