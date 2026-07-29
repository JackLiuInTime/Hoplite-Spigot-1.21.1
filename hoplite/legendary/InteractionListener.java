package com.hoplite.legendary;

import com.hoplite.HoplitePlugin;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;

public class InteractionListener implements Listener {

    private final HoplitePlugin plugin;

    public InteractionListener(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Optional<com.hoplite.legendary.legendary.LegendaryWeaponHandler> handler = plugin.getWeaponManager().fromItem(item);
        if (handler.isEmpty()) {
            return;
        }

        handler.get().onPlayerInteract(event);
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Optional<com.hoplite.legendary.legendary.LegendaryWeaponHandler> handler = plugin.getWeaponManager().fromItem(item);
        handler.ifPresent(h -> h.onPlayerInteractEntity(event));
    }
}

