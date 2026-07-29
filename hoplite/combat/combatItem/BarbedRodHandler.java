package com.hoplite.combat.combatItem;

import com.hoplite.combat.CombatItemHandler;
import com.hoplite.HoplitePlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class BarbedRodHandler implements CombatItemHandler {

    private static final String ID = "barbed_rod";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("combat:" + ID)
                .orElseGet(() -> new ItemStack(Material.FISHING_ROD));
    }

    @Override
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_ENTITY) {
            return;
        }
        if (!(event.getCaught() instanceof Player target)) {
            return;
        }

        Player attacker = event.getPlayer();
        if (attacker.equals(target)) {
            return;
        }

        Vector knock = target.getLocation().toVector().subtract(attacker.getLocation().toVector());
        if (knock.lengthSquared() > 0.001) {
            knock.normalize().multiply(1.15).setY(0.35);
            target.setVelocity(target.getVelocity().add(knock));
        }

        target.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_STRONG, 0.8f, 0.7f);
    }
}
