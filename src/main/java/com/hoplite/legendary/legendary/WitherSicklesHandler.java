package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;

public class WitherSicklesHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;

    public WitherSicklesHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "wither_sickles";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.STONE_HOE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&8Wither Sickles"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A pair of sickles imbued with the",
                    ChatColor.GRAY + "powers of the Wither.",
                    "",
                    ChatColor.WHITE + "Double clicking with a fully charged",
                    ChatColor.WHITE + "attack triggers a DOUBLE HIT.",
                    "",
                    ChatColor.WHITE + "A DOUBLE HIT has a 10% chance to knock",
                    ChatColor.WHITE + "back and apply Wither II for 5 seconds."
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this.plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (attacker.getAttackCooldown() < 0.95f) {
            return;
        }

        event.setDamage(event.getDamage() * 2.0);
        target.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, target.getLocation().add(0, 1, 0), 18, 0.2, 0.35, 0.2, 0.01);

        if (Math.random() < 0.10) {
            Vector velocity = target.getVelocity();
            velocity.setY(Math.max(velocity.getY(), 0.5));
            target.setVelocity(velocity);
            target.addPotionEffect(new PotionEffect(PotionEffectType.WITHER, 5 * 20, 1, true, true, true));
        }
    }
}
