package com.hoplite.combat.combatItem;

import com.hoplite.combat.CombatItemHandler;
import com.hoplite.HoplitePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Warden;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public class SkeletonLeggingsHandler implements CombatItemHandler {

    private static final String ID = "skeleton_leggings";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("combat:" + ID)
                .orElseGet(() -> new ItemStack(Material.IRON_LEGGINGS));
    }

    @Override
    public void onEntityTargetLivingEntity(EntityTargetLivingEntityEvent event) {
        if (!(event.getEntity() instanceof Monster) || event.getEntity() instanceof Warden) {
            return;
        }
        event.setCancelled(true);
    }

    @Override
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (ThreadLocalRandom.current().nextDouble() <= 0.33d) {
            event.setConsumeItem(false);
        }
    }
}
