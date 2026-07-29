package com.hoplite.combat.combatItem;

import com.hoplite.combat.CombatItemHandler;
import com.hoplite.HoplitePlugin;
import org.bukkit.Material;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class BanditLeggingsHandler implements CombatItemHandler {

    private static final String ID = "bandit_leggings";

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
    public void onEntityDeath(EntityDeathEvent event) {
        List<ItemStack> drops = event.getDrops();
        for (ItemStack drop : drops) {
            if (drop == null || drop.getType() != Material.GOLD_NUGGET) {
                continue;
            }
            int bonus = Math.max(1, (int) Math.floor(drop.getAmount() * 0.5d));
            drop.setAmount(Math.min(drop.getAmount() + bonus, drop.getMaxStackSize()));
        }
    }
}
