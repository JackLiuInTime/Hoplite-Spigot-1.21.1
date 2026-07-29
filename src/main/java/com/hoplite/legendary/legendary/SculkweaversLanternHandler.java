package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SculkweaversLanternHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> prisonCooldown = new ConcurrentHashMap<>();
    private static final long PRISON_COOLDOWN_MS = 150_000;
    private static final int PRISON_DURATION_TICKS = 18 * 20;

    public SculkweaversLanternHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "sculkweavers_lantern";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&3Sculkweaver's Lantern"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary lantern imbued with the",
                    ChatColor.GRAY + "power of the Deep Dark.",
                    "",
                    ChatColor.WHITE + "RIGHT CLICK to launch the lantern. When",
                    ChatColor.WHITE + "it lands, a Deep Dark prison will form,",
                    ChatColor.WHITE + "which will last for 18 seconds. Friendly",
                    ChatColor.WHITE + "creatures will appear inside and hunt",
                    ChatColor.WHITE + "enemies for the duration of the ability.",
                    ChatColor.DARK_GRAY + "150s Cooldown"
            ));
                    meta.setCustomModelData(5);
            meta.getPersistentDataContainer().set(new NamespacedKey(this.plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
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
        long remaining = WeaponRuntime.remainingSeconds(prisonCooldown, id, PRISON_COOLDOWN_MS, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cDeep Dark Prison is on cooldown: " + remaining + "s");
            return;
        }

        var target = player.getTargetBlockExact(24);
        var center = target != null ? target.getLocation().add(0.5, 1.0, 0.5) : player.getLocation().add(player.getLocation().getDirection().normalize().multiply(8));

        int affected = 0;
        for (var entity : player.getWorld().getNearbyEntities(center, 6, 4, 6)) {
            if (!(entity instanceof LivingEntity living) || living.equals(player)) {
                continue;
            }
            living.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, PRISON_DURATION_TICKS, 0, true, true, true));
            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, PRISON_DURATION_TICKS, 1, true, true, true));
            living.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, PRISON_DURATION_TICKS, 0, true, true, true));
            affected++;
        }

        // Spawn temporary friendly hunters inside the prison area.
        for (int i = 0; i < 3; i++) {
            Wolf wolf = player.getWorld().spawn(center, Wolf.class);
            wolf.setOwner(player);
            wolf.setCustomName("Sculk Hunter");
            wolf.setCustomNameVisible(false);
            wolf.setAdult();
            wolf.setRemoveWhenFarAway(true);

            LivingEntity nearestEnemy = null;
            double nearest = Double.MAX_VALUE;
            for (var entity : player.getWorld().getNearbyEntities(center, 8, 4, 8)) {
                if (!(entity instanceof LivingEntity living) || living.equals(player) || living instanceof Wolf) {
                    continue;
                }
                double dist = living.getLocation().distanceSquared(center);
                if (dist < nearest) {
                    nearest = dist;
                    nearestEnemy = living;
                }
            }
            if (nearestEnemy != null) {
                wolf.setTarget(nearestEnemy);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (wolf.isValid() && !wolf.isDead()) {
                        wolf.remove();
                    }
                }
            }.runTaskLater(plugin, PRISON_DURATION_TICKS);
        }

        WeaponRuntime.startCooldown(prisonCooldown, id, now, PRISON_COOLDOWN_MS, player, player.getInventory().getItemInMainHand());
        player.getWorld().spawnParticle(org.bukkit.Particle.SCULK_CHARGE_POP, center, 40, 1.0, 0.7, 1.0, 0.05);
        player.getWorld().spawnParticle(org.bukkit.Particle.SONIC_BOOM, center, 1, 0.0, 0.0, 0.0, 0.0);
        player.getWorld().playSound(center, org.bukkit.Sound.BLOCK_SCULK_SHRIEKER_SHRIEK, 0.8f, 0.9f);
        if (affected > 0) {
            player.sendMessage("§3Deep Dark Prison trapped " + affected + " target(s) for 18s.");
        } else {
            player.sendMessage("§7Deep Dark Prison formed, but no enemies were nearby.");
        }
        event.setCancelled(true);
    }
}
