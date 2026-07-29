package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.CaveSpider;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SceptreOfArachneHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> empowerCooldown = new ConcurrentHashMap<>();
    private final Set<UUID> empowered = ConcurrentHashMap.newKeySet();
    private static final long EMPOWER_COOLDOWN_MS = 45_000;

    public SceptreOfArachneHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "sceptre_of_arachne";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Sceptre of Arachne"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary sceptre woven with the wrath of Arachne.",
                    "",
                ChatColor.WHITE + "SNEAK + LEFT CLICK to empower the",
                ChatColor.WHITE + "sceptre. The next throw while empowered",
                ChatColor.WHITE + "spawns several cobwebs around the",
                ChatColor.WHITE + "landing area of the sceptre and spawns",
                ChatColor.WHITE + "6 tiny and fast Arachne babies that",
                ChatColor.WHITE + "poison the nearest enemy for 2 seconds",
                ChatColor.WHITE + "on hit.",
                ChatColor.DARK_GRAY + "45s Cooldown",
                "",
                ChatColor.WHITE + "25% chance to spawn a cobweb on enemy",
                ChatColor.WHITE + "hit when thrown.",
                "",
                ChatColor.WHITE + "HOLD to be immune to cobwebs."
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }
        if (!event.getPlayer().isSneaking()) {
            return;
        }

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long remaining = WeaponRuntime.remainingSeconds(empowerCooldown, id, EMPOWER_COOLDOWN_MS, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cEmpower Throw is on cooldown: " + remaining + "s");
            return;
        }

        WeaponRuntime.startCooldown(empowerCooldown, id, now, EMPOWER_COOLDOWN_MS, player, player.getInventory().getItemInMainHand());
        empowered.add(id);
        player.sendMessage("§aArachne power armed for your next hit.");
        event.setCancelled(true);
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Player attacker = resolveAttacker(event.getDamager());
        if (attacker == null) {
            return;
        }

        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        UUID id = attacker.getUniqueId();
        if (!empowered.remove(id)) {
            if (event.getDamager() instanceof Arrow) {
                maybeSpawnPassiveCobweb(target.getLocation());
            }
            return;
        }

        Location center = target.getLocation().getBlock().getLocation();
        spawnCobwebBurst(center, 7);
        spawnArachneBabies(center.add(0.5, 1, 0.5), 6, attacker);
        attacker.sendMessage("§6Arachne burst triggered.");
    }

    private void maybeSpawnPassiveCobweb(Location hitLocation) {
        if (Math.random() >= 0.25) {
            return;
        }
        Block block = hitLocation.getBlock();
        if (block.getType().isAir()) {
            block.setType(Material.COBWEB);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (block.getType() == Material.COBWEB) {
                    block.setType(Material.AIR);
                }
            }, 80L);
        }
    }

    private Player resolveAttacker(Entity damager) {
        if (damager instanceof Player player) {
            return player;
        }
        if (damager instanceof Arrow arrow && arrow.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }

    private void spawnCobwebBurst(Location center, int maxWebs) {
        List<Block> candidates = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block b = center.clone().add(dx, 0, dz).getBlock();
                if (b.getType().isAir()) {
                    candidates.add(b);
                }
            }
        }
        Collections.shuffle(candidates);
        int placed = 0;
        for (Block block : candidates) {
            block.setType(Material.COBWEB);
            placed++;
            if (placed >= maxWebs) {
                break;
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Block block : candidates) {
                    if (block.getType() == Material.COBWEB) {
                        block.setType(Material.AIR);
                    }
                }
            }
        }.runTaskLater(plugin, 100L);
    }

    private void spawnArachneBabies(Location location, int count, Player owner) {
        for (int i = 0; i < count; i++) {
            CaveSpider spider = location.getWorld().spawn(location, CaveSpider.class);
            spider.setCustomName("Arachne Baby");
            spider.setCustomNameVisible(false);
            spider.setRemoveWhenFarAway(true);
            spider.addPotionEffect(new org.bukkit.potion.PotionEffect(org.bukkit.potion.PotionEffectType.SPEED, 12 * 20, 1, true, false, false));
            LivingEntity nearestEnemy = nearestEnemy(owner, spider.getLocation(), 14);
            if (nearestEnemy != null) {
                spider.setTarget(nearestEnemy);
            }

            new BukkitRunnable() {
                @Override
                public void run() {
                    if (spider.isValid() && !spider.isDead()) {
                        spider.remove();
                    }
                }
            }.runTaskLater(plugin, 12 * 20L);
        }
    }

    private LivingEntity nearestEnemy(Player owner, Location origin, double radius) {
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity raw : origin.getWorld().getNearbyEntities(origin, radius, radius, radius)) {
            if (!(raw instanceof LivingEntity living) || raw.equals(owner)) {
                continue;
            }
            if (raw instanceof Player player) {
                var teamA = owner.getScoreboard().getEntryTeam(owner.getName());
                var teamB = player.getScoreboard().getEntryTeam(player.getName());
                if (teamA != null && teamB != null && teamA.equals(teamB)) {
                    continue;
                }
            }
            double dist = raw.getLocation().distanceSquared(origin);
            if (dist < bestDist) {
                bestDist = dist;
                best = living;
            }
        }
        return best;
    }
}
