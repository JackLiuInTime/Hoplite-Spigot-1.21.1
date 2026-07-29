package com.hoplite.combat.combatItem;

import com.hoplite.combat.CombatItemHandler;
import com.hoplite.HoplitePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class VerySuspiciousStewHandler implements CombatItemHandler {

    private static final String ID = "very_suspicious_stew";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("combat:" + ID)
                .orElseGet(() -> new ItemStack(Material.SUSPICIOUS_STEW));
    }

    @Override
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();

        // Regen II for 10s is close to the lore target of healing over time.
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 10 * 20, 1));

        List<PotionEffect> pool = List.of(
                new PotionEffect(PotionEffectType.SPEED, 10 * 20, 0),
            new PotionEffect(PotionEffectType.RESISTANCE, 8 * 20, 0),
                new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 12 * 20, 0),
            new PotionEffect(PotionEffectType.STRENGTH, 8 * 20, 0),
            new PotionEffect(PotionEffectType.SLOWNESS, 8 * 20, 0),
                new PotionEffect(PotionEffectType.WEAKNESS, 8 * 20, 0)
        );

        PotionEffect randomEffect = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
        player.addPotionEffect(randomEffect);
    }
}
