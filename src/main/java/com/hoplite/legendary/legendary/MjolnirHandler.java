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

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MjolnirHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> thunderCooldown = new ConcurrentHashMap<>();
    private static final long THUNDER_COOLDOWN_MS = 16_000;

    public MjolnirHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "mjolnir";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.STONE_AXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&bMjolnir"));
            meta.setLore(List.of(
                ChatColor.GRAY + "A legendary weapon that harnesses the",
                ChatColor.GRAY + "power of the skies.",
                    "",
                ChatColor.WHITE + "THROW or ATTACK to deal lightning",
                ChatColor.WHITE + "damage.",
                ChatColor.DARK_GRAY + "16s Cooldown"
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

        UUID id = attacker.getUniqueId();
        long now = System.currentTimeMillis();
        if (!WeaponRuntime.isOffCooldown(thunderCooldown, id, THUNDER_COOLDOWN_MS, now)) {
            return;
        }

        WeaponRuntime.startCooldown(thunderCooldown, id, now, THUNDER_COOLDOWN_MS, attacker, attacker.getInventory().getItemInMainHand());
        event.setDamage(event.getDamage() + 3.0);
        target.getWorld().strikeLightningEffect(target.getLocation());
        target.setVelocity(target.getVelocity().setY(Math.max(target.getVelocity().getY(), 0.45)));
        target.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK, target.getLocation().add(0, 1, 0), 20, 0.25, 0.35, 0.25, 0.02);
        attacker.sendMessage("§bThunder Impact triggered.");
    }
}
