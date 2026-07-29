package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GuardianCannonHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> laserCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> boostCooldown = new ConcurrentHashMap<>();
    private static final long LASER_COOLDOWN_MS = 30_000;
    private static final long BOOST_COOLDOWN_MS = 25_000;

    public GuardianCannonHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "guardian_cannon";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&bGuardian Cannon"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary cannon that harnesses the power of the guardians.",
                    "",
                    ChatColor.WHITE + "RIGHT CLICK: fire a charged guardian beam",
                    ChatColor.DARK_GRAY + "COOLDOWN: 30s",
                    "",
                    ChatColor.WHITE + "RIGHT CLICK IN WATER: burst forward",
                    ChatColor.DARK_GRAY + "COOLDOWN: 25s"
            ));
            meta.setCustomModelData(10);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            meta.addEnchant(Enchantment.QUICK_CHARGE, 3, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!WeaponRuntime.isRightClick(event.getAction())) {
            return;
        }

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();

        if (player.isInWater() || player.getLocation().getBlock().isLiquid()) {
            long remainingBoost = WeaponRuntime.remainingSeconds(boostCooldown, id, BOOST_COOLDOWN_MS, now);
            if (remainingBoost > 0) {
                WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remainingBoost);
                player.sendMessage("§cGuardian Boost is on cooldown: " + remainingBoost + "s");
                return;
            }

            WeaponRuntime.startCooldown(boostCooldown, id, now, BOOST_COOLDOWN_MS, player, player.getInventory().getItemInMainHand());
            Vector push = player.getLocation().getDirection().normalize().multiply(1.7);
            push.setY(Math.max(push.getY(), 0.25));
            player.setVelocity(push);
            player.getWorld().spawnParticle(org.bukkit.Particle.BUBBLE_COLUMN_UP, player.getLocation().add(0, 1, 0), 22, 0.4, 0.3, 0.4, 0.02);
            player.sendMessage("§bGuardian Boost activated.");
            event.setCancelled(true);
            return;
        }

        long remainingLaser = WeaponRuntime.remainingSeconds(laserCooldown, id, LASER_COOLDOWN_MS, now);
        if (remainingLaser > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remainingLaser);
            player.sendMessage("§cLaser Charge is on cooldown: " + remainingLaser + "s");
            return;
        }

        var hit = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                20,
                entity -> entity instanceof LivingEntity && entity != player
        );

        WeaponRuntime.startCooldown(laserCooldown, id, now, LASER_COOLDOWN_MS, player, player.getInventory().getItemInMainHand());
        if (hit != null && hit.getHitEntity() instanceof LivingEntity target) {
            target.damage(8.0, player);
            target.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 80, 0, true, true, true));
            player.getWorld().spawnParticle(org.bukkit.Particle.GLOW, target.getLocation().add(0, 1, 0), 24, 0.25, 0.35, 0.25, 0.03);
            player.sendMessage("§bGuardian beam hit target.");
        } else {
            player.sendMessage("§7Guardian beam fired, but no target was hit.");
        }
        event.setCancelled(true);
    }
}

