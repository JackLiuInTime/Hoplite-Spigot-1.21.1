package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ReapersScytheHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> soulReapCooldown = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 45_000;

    public ReapersScytheHandler(HoplitePlugin plugin) { this.plugin = plugin; }

    @Override
    public String getId() { return "reapers_scythe"; }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.DIAMOND_HOE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&8Reaper's Scythe"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "Drain life and steal the souls of your enemies.",
                    "",
                    ChatColor.WHITE + "ATTACK PLAYER: drain 25% HP and heal that amount",
                    ChatColor.WHITE + "ATTACK PLAYER: copy all potion effects",
                    ChatColor.DARK_GRAY + "COOLDOWN: 45s"
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            meta.addEnchant(Enchantment.SHARPNESS, 5, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof Player target)) {
            return;
        }

        UUID id = attacker.getUniqueId();
        long now = System.currentTimeMillis();
        if (!WeaponRuntime.isOffCooldown(soulReapCooldown, id, COOLDOWN_MS, now)) {
            return;
        }

        WeaponRuntime.startCooldown(soulReapCooldown, id, now, COOLDOWN_MS, attacker, attacker.getInventory().getItemInMainHand());
        double heal = Math.max(1.0, target.getHealth() * 0.25);
        target.damage(heal, attacker);
        double maxHealth = WeaponRuntime.getMaxHealth(attacker, 20.0);
        attacker.setHealth(Math.min(maxHealth, attacker.getHealth() + heal));

        for (PotionEffect effect : target.getActivePotionEffects()) {
            attacker.addPotionEffect(new PotionEffect(effect.getType(), Math.min(effect.getDuration(), 8 * 20), effect.getAmplifier(), true, true, true));
        }

        target.getWorld().spawnParticle(org.bukkit.Particle.SOUL, target.getLocation().add(0, 1, 0), 20, 0.3, 0.4, 0.3, 0.02);
        attacker.sendMessage("§8Soul Reap triggered.");
    }
}

