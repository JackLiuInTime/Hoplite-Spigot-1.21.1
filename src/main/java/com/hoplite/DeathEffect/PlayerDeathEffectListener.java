package com.hoplite.DeathEffect;

import com.hoplite.HoplitePlugin;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathEffectListener implements Listener {

    private final HoplitePlugin plugin;

    public PlayerDeathEffectListener(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        var location = event.getEntity().getLocation();

        // Visual-only lightning: no direct damage.
        location.getWorld().strikeLightningEffect(location);

        // Play the wither-spawn sound to all online players.
        for (var player : plugin.getServer().getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 1.0f);
        }
    }
}
