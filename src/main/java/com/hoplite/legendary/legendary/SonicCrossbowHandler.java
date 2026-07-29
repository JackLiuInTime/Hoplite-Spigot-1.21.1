package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SonicCrossbowHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> sonicCooldown = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 22_000;

    public SonicCrossbowHandler(HoplitePlugin plugin) { this.plugin = plugin; }

    @Override
    public String getId() { return "sonic_crossbow"; }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&9Sonic Crossbow"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary crossbow originating from the Deep Dark.",
                    "",
                    ChatColor.WHITE + "SHOOT: launch a sonic arrow",
                    ChatColor.WHITE + "ARROW HIT: large sculk blast with heavy knockback",
                    ChatColor.WHITE + "ARROW HIT: deals true damage to players",
                    ChatColor.DARK_GRAY + "COOLDOWN: 22s"
            ));
                meta.setCustomModelData(1);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            meta.addEnchant(Enchantment.QUICK_CHARGE, 3, true);
            meta.addEnchant(Enchantment.PIERCING, 4, true);
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
        if (!WeaponRuntime.isOffCooldown(sonicCooldown, id, COOLDOWN_MS, now)) {
            return;
        }

        WeaponRuntime.startCooldown(sonicCooldown, id, now, COOLDOWN_MS, shooter, shooter.getInventory().getItemInMainHand());
        event.setDamage(0.0);
        var impact = target.getLocation();

        for (var entity : target.getWorld().getNearbyEntities(impact, 4.5, 3, 4.5)) {
            if (!(entity instanceof Player victim) || victim.equals(shooter)) {
                continue;
            }

            var push = victim.getLocation().toVector().subtract(shooter.getLocation().toVector()).normalize().multiply(1.2);
            push.setY(0.4);
            victim.setVelocity(push);

            // Simulate true damage by directly reducing health.
            double dealt = 5.0;
            if (victim.getHealth() <= dealt) {
                victim.setHealth(0.0);
            } else {
                victim.setHealth(victim.getHealth() - dealt);
            }
        }

        target.setVelocity(target.getVelocity().add(shooter.getLocation().getDirection().normalize().multiply(1.0)).setY(0.4));
        target.getWorld().spawnParticle(org.bukkit.Particle.SCULK_CHARGE_POP, impact.add(0, 1, 0), 36, 0.8, 0.6, 0.8, 0.04);
        target.getWorld().spawnParticle(org.bukkit.Particle.SONIC_BOOM, target.getLocation().add(0, 1, 0), 1, 0, 0, 0, 0);
        target.getWorld().createExplosion(target.getLocation().getX(), target.getLocation().getY(), target.getLocation().getZ(), 2.4f, false, false, shooter);
        target.getWorld().playSound(target.getLocation(), org.bukkit.Sound.ENTITY_WARDEN_SONIC_BOOM, 0.7f, 1.2f);
        shooter.sendMessage("§9Sonic Blast triggered.");
    }
}

