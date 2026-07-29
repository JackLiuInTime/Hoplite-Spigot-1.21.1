package com.hoplite.legendary;

import com.hoplite.HoplitePlugin;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MiningListener implements Listener {

    private final HoplitePlugin plugin;
    private static final Set<Location> controlledBreaks = ConcurrentHashMap.newKeySet();

    public MiningListener(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    public static void markControlledBreak(Location location) {
        controlledBreaks.add(location);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (controlledBreaks.remove(event.getBlock().getLocation())) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        Optional<com.hoplite.legendary.legendary.LegendaryWeaponHandler> handler = plugin.getWeaponManager().fromItem(item);
        if (handler.isEmpty()) {
            return;
        }

        // Delegate break handling to the weapon handler
        handler.get().onBlockBreak(event);
    }
}

