package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.block.Block;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CorruptedCrossbowHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> corruptionCooldown = new ConcurrentHashMap<>();
    private final long corruptionCooldownMs;

    public CorruptedCrossbowHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.corruptionCooldownMs = plugin.getLegendaryCooldownMs("corrupted_crossbow.corruption_spread", 34_000L);
    }

    @Override
    public String getId() {
        return "corrupted_crossbow";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Corrupted Crossbow"));
            meta.setLore(List.of(
                ChatColor.GRAY + "A legendary crossbow that shoots a",
                ChatColor.GRAY + "deadly corrupted slime ball.",
                    "",
                ChatColor.WHITE + "SHOOT to fire a corrupted slime ball.",
                ChatColor.WHITE + "The area around where the slime lands",
                ChatColor.WHITE + "will begin to decay, leaving behind a",
                ChatColor.WHITE + "poisonous cloud that inflicts all",
                ChatColor.WHITE + "players inside with " + ChatColor.GREEN + "Poison" + ChatColor.WHITE + " while",
                ChatColor.WHITE + "breaking blocks in a large radius.",
                ChatColor.DARK_GRAY + "34s Cooldown"
            ));
            meta.setCustomModelData(4);
            meta.getPersistentDataContainer().set(new NamespacedKey(this.plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow) || !(arrow.getShooter() instanceof Player shooter)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        UUID id = shooter.getUniqueId();
        long now = System.currentTimeMillis();
        if (!WeaponRuntime.isOffCooldown(corruptionCooldown, id, corruptionCooldownMs, now)) {
            return;
        }

        WeaponRuntime.startCooldown(corruptionCooldown, id, now, corruptionCooldownMs, shooter, shooter.getInventory().getItemInMainHand());
        for (Entity raw : target.getNearbyEntities(4.0, 2.0, 4.0)) {
            if (!(raw instanceof LivingEntity nearby)) {
                continue;
            }
            nearby.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 0, true, true, true));
        }

        Block center = target.getLocation().getBlock();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Block b = center.getRelative(dx, dy, dz);
                    Material t = b.getType();
                        if (t == Material.SHORT_GRASS || t == Material.TALL_GRASS || t == Material.FERN || t == Material.LARGE_FERN
                            || t == Material.DANDELION || t == Material.POPPY || t == Material.SEAGRASS
                            || t == Material.KELP || t == Material.KELP_PLANT || t == Material.VINE
                            || t == Material.CAVE_VINES || t == Material.CAVE_VINES_PLANT) {
                        b.breakNaturally();
                    }
                }
            }
        }

        target.getWorld().spawnParticle(org.bukkit.Particle.SQUID_INK, target.getLocation().add(0, 1, 0), 24, 0.5, 0.6, 0.5, 0.02);
        target.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, target.getLocation().add(0, 1, 0), 28, 0.5, 0.6, 0.5, 0.01);
        shooter.sendMessage("§4Corruption spread to your target.");
    }
}
