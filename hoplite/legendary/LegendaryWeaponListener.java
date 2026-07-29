package com.hoplite.legendary;

import com.hoplite.HoplitePlugin;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class LegendaryWeaponListener implements Listener {

    private final HoplitePlugin plugin;
    public LegendaryWeaponListener(HoplitePlugin plugin) {
        this.plugin = plugin;
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
        Optional<com.hoplite.legendary.legendary.LegendaryWeaponHandler> handler = plugin.getWeaponManager().fromItem(item);
        handler.ifPresent(h -> h.onEntityDamageByEntity(event));

        if (event.getEntity() instanceof Player defender) {
            ItemStack defenderItem = defender.getInventory().getItemInMainHand();
            Optional<com.hoplite.legendary.legendary.LegendaryWeaponHandler> defenderHandler = plugin.getWeaponManager().fromItem(defenderItem);
            defenderHandler.ifPresent(h -> h.onOwnerDamaged(event));
        }
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        ItemStack weapon = event.getBow();
        Optional<com.hoplite.legendary.legendary.LegendaryWeaponHandler> handler = plugin.getWeaponManager().fromItem(weapon);
        handler.ifPresent(h -> h.onEntityShootBow(event));
    }

    @EventHandler
    public void onOwnerDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player defender)) {
            return;
        }

        ItemStack defenderItem = defender.getInventory().getItemInMainHand();
        Optional<com.hoplite.legendary.legendary.LegendaryWeaponHandler> defenderHandler = plugin.getWeaponManager().fromItem(defenderItem);
        defenderHandler.ifPresent(h -> h.onOwnerDamage(event));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Optional<com.hoplite.legendary.legendary.LegendaryWeaponHandler> handler = plugin.getWeaponManager().fromItem(item);
        handler.ifPresent(h -> h.onPlayerMove(event));
    }

}

