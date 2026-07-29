package com.hoplite.combat.combatItem;

import com.hoplite.combat.CombatItemHandler;
import com.hoplite.HoplitePlugin;
import org.bukkit.Material;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;

public class BlazingCrossbowHandler implements CombatItemHandler {

    private static final String ID = "blazing_crossbow";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("combat:" + ID)
                .orElseGet(() -> new ItemStack(Material.CROSSBOW));
    }

    @Override
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (event.getProjectile() instanceof AbstractArrow arrow) {
            arrow.setFireTicks(100);
        }
    }
}
