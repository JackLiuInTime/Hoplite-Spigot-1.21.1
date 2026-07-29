package com.hoplite.combat.combatItem;

import com.hoplite.combat.CombatItemHandler;
import com.hoplite.HoplitePlugin;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class CactusChestplateHandler implements CombatItemHandler {

    private static final String ID = "cactus_chestplate";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("combat:" + ID)
                .orElseGet(() -> new ItemStack(Material.IRON_CHESTPLATE));
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player defender)) {
            return;
        }
        if (!(event.getDamager() instanceof LivingEntity attacker)) {
            return;
        }

        ItemStack chest = defender.getInventory().getChestplate();
        if (chest == null || chest.getType().isAir()) {
            return;
        }

        double thornDamage = Math.min(3.0, Math.max(1.0, event.getFinalDamage() * 0.2));
        attacker.damage(thornDamage, defender);
        defender.getWorld().playSound(defender.getLocation(), Sound.BLOCK_GRASS_BREAK, 0.7f, 1.6f);
    }
}
