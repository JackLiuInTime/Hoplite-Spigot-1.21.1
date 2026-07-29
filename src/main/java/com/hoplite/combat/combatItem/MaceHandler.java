package com.hoplite.combat.combatItem;

import com.hoplite.combat.CombatItemHandler;
import com.hoplite.HoplitePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class MaceHandler implements CombatItemHandler {

    private static final String ID = "mace";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("combat:" + ID)
                .orElseGet(() -> new ItemStack(Material.DIAMOND_SWORD));
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }

        float fallDistance = Math.max(0.0f, player.getFallDistance() - 1.5f);
        if (fallDistance <= 0.0f) {
            return;
        }

        double bonus = Math.min(10.0, fallDistance * 1.35);
        event.setDamage(event.getDamage() + bonus);
    }
}
