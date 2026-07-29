package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HornOfWinterHandler implements LegendaryWeaponHandler, Listener {
    private final HoplitePlugin plugin;
    private static final int MIN_COOLDOWN_SECONDS = 30;
    private static final int MAX_COOLDOWN_SECONDS = 300;
    private static final long MIN_COOLDOWN_MS = MIN_COOLDOWN_SECONDS * 1000L;
    private static final long MAX_COOLDOWN_MS = MAX_COOLDOWN_SECONDS * 1000L;
    private static final int BLIZZARD_FREEZE_TICKS = 10 * 20;
    private static final double ICE_SHARD_SPEED = 3.2D;
    private static final double ICE_SHARD_BONUS_DAMAGE = 4.0D;

    private final Map<UUID, Long> hornCooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> ownerToHorse = new ConcurrentHashMap<>();
    private final Map<UUID, UUID> horseToOwner = new ConcurrentHashMap<>();
    private final NamespacedKey iceShardKey;
    private final NamespacedKey hornHorseOwnerKey;

    public HornOfWinterHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.iceShardKey = new NamespacedKey(plugin, "horn_of_winter_ice_shard");
        this.hornHorseOwnerKey = new NamespacedKey(plugin, "horn_of_winter_owner");
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getId() {
        return "horn_of_winter";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&bHorn of Winter"));
            meta.setLore(List.of(
                ChatColor.GRAY + "A legendary horn that summons a",
                ChatColor.GRAY + "powerful horse ally.",
                    "",
                ChatColor.WHITE + "USE to summon the Ice Horse.",
                ChatColor.WHITE + "Dismounting despawns the horse and",
                ChatColor.WHITE + "begins a cooldown to reuse the horn",
                ChatColor.WHITE + "ability. The cooldown ranges between 30",
                ChatColor.WHITE + "and 300 seconds based on the health of",
                ChatColor.WHITE + "the horse.",
                "",
                ChatColor.WHITE + "SHOOT while mounted to turn your arrow",
                ChatColor.WHITE + "into a deadly ice shard that will fly",
                ChatColor.WHITE + "in the direction you are looking.",
                "",
                ChatColor.WHITE + "RIGHT CLICK while riding the Ice Horse",
                ChatColor.WHITE + "to activate a Blizzard ability, turning",
                ChatColor.WHITE + "the surface in front of the horse's",
                ChatColor.WHITE + "feet into powdered snow. Enemies in",
                ChatColor.WHITE + "front of the horse will be damaged and",
                ChatColor.WHITE + "inflicted with freezing for 10s.",
                ChatColor.DARK_GRAY + "30s Cooldown"
            ));
                meta.setCustomModelData(1);
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
        if (isMountedOnOwnedHorse(player)) {
            activateBlizzard(player);
            event.setCancelled(true);
            return;
        }

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long remaining = WeaponRuntime.remainingSeconds(hornCooldowns, id, MIN_COOLDOWN_MS, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cHorn of Winter is on cooldown: " + remaining + "s");
            event.setCancelled(true);
            return;
        }

        if (ownerToHorse.containsKey(id)) {
            Horse existing = getOwnedHorse(id);
            if (existing != null && !existing.isDead()) {
                existing.addPassenger(player);
                player.sendMessage("§bYou mount your Ice Horse.");
                event.setCancelled(true);
                return;
            }
            ownerToHorse.remove(id);
        }

        summonIceHorse(player);
        event.setCancelled(true);
    }

    @EventHandler
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!(event.getProjectile() instanceof Arrow arrow)) {
            return;
        }
        if (!isMountedOnOwnedHorse(player)) {
            return;
        }

        arrow.getPersistentDataContainer().set(iceShardKey, PersistentDataType.BYTE, (byte) 1);
        arrow.setVelocity(player.getEyeLocation().getDirection().normalize().multiply(ICE_SHARD_SPEED));
        arrow.setCritical(true);
        arrow.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        player.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, player.getEyeLocation(), 16, 0.12, 0.12, 0.12, 0.02);
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) {
            return;
        }
        if (!arrow.getPersistentDataContainer().has(iceShardKey, PersistentDataType.BYTE)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (arrow.getShooter() instanceof Player shooter && target instanceof Player targetPlayer && !isEnemy(shooter, targetPlayer)) {
            return;
        }

        event.setDamage(event.getDamage() + ICE_SHARD_BONUS_DAMAGE);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, BLIZZARD_FREEZE_TICKS, 1, true, true, true));
        target.setFreezeTicks(Math.max(target.getFreezeTicks(), BLIZZARD_FREEZE_TICKS));
        target.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, target.getLocation().add(0, 1, 0), 24, 0.3, 0.5, 0.3, 0.02);
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getExited() instanceof Player player) || !(event.getVehicle() instanceof Horse horse)) {
            return;
        }

        UUID ownerId = horseToOwner.get(horse.getUniqueId());
        if (ownerId == null || !ownerId.equals(player.getUniqueId())) {
            return;
        }

        long cooldownMs = computeCooldownFromHorseHealth(horse);
        WeaponRuntime.startCooldown(
                hornCooldowns,
                player.getUniqueId(),
                System.currentTimeMillis(),
                cooldownMs,
                player,
                player.getInventory().getItemInMainHand()
        );

        ownerToHorse.remove(player.getUniqueId());
        horseToOwner.remove(horse.getUniqueId());
        horse.remove();
        long seconds = Math.max(1L, cooldownMs / 1000L);
        player.sendMessage("§bIce Horse dismissed. Horn cooldown: " + seconds + "s");
    }

    private void summonIceHorse(Player player) {
        Horse horse = player.getWorld().spawn(player.getLocation(), Horse.class);
        horse.setAdult();
        horse.setTamed(true);
        horse.setOwner(player);
        horse.setColor(Horse.Color.WHITE);
        horse.setStyle(Horse.Style.WHITE_DOTS);
        horse.setCustomName("§bIce Horse");
        horse.setCustomNameVisible(true);

        if (horse.getAttribute(Attribute.GENERIC_MAX_HEALTH) != null) {
            horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(40.0D);
            horse.setHealth(40.0D);
        }
        if (horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED) != null) {
            horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.27D);
        }
        horse.setJumpStrength(0.9D);

        UUID ownerId = player.getUniqueId();
        ownerToHorse.put(ownerId, horse.getUniqueId());
        horseToOwner.put(horse.getUniqueId(), ownerId);
        horse.getPersistentDataContainer().set(hornHorseOwnerKey, PersistentDataType.STRING, ownerId.toString());
        horse.addPassenger(player);

        player.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, horse.getLocation().add(0, 1, 0), 60, 0.6, 0.8, 0.6, 0.05);
        player.getWorld().playSound(horse.getLocation(), org.bukkit.Sound.ENTITY_HORSE_AMBIENT, 1.0f, 0.8f);
        player.sendMessage("§bIce Horse summoned.");
    }

    private void activateBlizzard(Player player) {
        Entity vehicle = player.getVehicle();
        if (!(vehicle instanceof Horse horse)) {
            return;
        }

        Vector direction = player.getLocation().getDirection().setY(0).normalize();
        org.bukkit.Location center = horse.getLocation().clone().add(direction.multiply(3.0D));

        int affected = 0;
        for (Entity entity : horse.getWorld().getNearbyEntities(center, 4, 3, 4)) {
            if (!(entity instanceof LivingEntity living) || living.equals(player)) {
                continue;
            }
            if (living instanceof Player targetPlayer && !isEnemy(player, targetPlayer)) {
                continue;
            }

            living.damage(4.0D, player);
            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, BLIZZARD_FREEZE_TICKS, 1, true, true, true));
            living.setFreezeTicks(Math.max(living.getFreezeTicks(), BLIZZARD_FREEZE_TICKS));
            affected++;
        }

        List<Block> changed = new ArrayList<>();
        Map<Block, Material> previous = new HashMap<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Block floor = center.clone().add(dx, -1, dz).getBlock();
                Block surface = floor.getLocation().add(0, 1, 0).getBlock();
                if (floor.getType().isAir() || !surface.getType().isAir()) {
                    continue;
                }
                previous.put(surface, surface.getType());
                surface.setType(Material.POWDER_SNOW, false);
                changed.add(surface);
            }
        }

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Block block : changed) {
                    if (block.getType() == Material.POWDER_SNOW) {
                        block.setType(previous.getOrDefault(block, Material.AIR), false);
                    }
                }
            }
        }.runTaskLater(plugin, 20L * 8L);

        horse.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, center.add(0, 1, 0), 60, 1.4, 0.8, 1.4, 0.05);
        horse.getWorld().playSound(center, org.bukkit.Sound.BLOCK_POWDER_SNOW_BREAK, 1.0f, 0.8f);
        if (affected > 0) {
            player.sendMessage("§bBlizzard hits " + affected + " target(s).");
        } else {
            player.sendMessage("§7Blizzard cast, but no enemies were in front.");
        }
    }

    private boolean isMountedOnOwnedHorse(Player player) {
        UUID horseId = ownerToHorse.get(player.getUniqueId());
        if (horseId == null) {
            return false;
        }
        Entity vehicle = player.getVehicle();
        return vehicle instanceof Horse horse && horse.getUniqueId().equals(horseId);
    }

    private Horse getOwnedHorse(UUID ownerId) {
        UUID horseId = ownerToHorse.get(ownerId);
        if (horseId == null) {
            return null;
        }
        Entity entity = plugin.getServer().getEntity(horseId);
        if (entity instanceof Horse horse) {
            return horse;
        }
        return null;
    }

    private long computeCooldownFromHorseHealth(Horse horse) {
        double maxHealth = WeaponRuntime.getMaxHealth(horse, 20.0D);
        double clampedHealth = Math.max(0.0D, Math.min(maxHealth, horse.getHealth()));
        double ratio = maxHealth <= 0.0D ? 0.0D : clampedHealth / maxHealth;
        double cooldownSec = MIN_COOLDOWN_SECONDS + (1.0D - ratio) * (MAX_COOLDOWN_SECONDS - MIN_COOLDOWN_SECONDS);
        long ms = Math.round(cooldownSec * 1000.0D);
        return Math.max(MIN_COOLDOWN_MS, Math.min(MAX_COOLDOWN_MS, ms));
    }

    private boolean isEnemy(Player source, Player target) {
        Team sourceTeam = source.getScoreboard().getEntryTeam(source.getName());
        Team targetTeam = target.getScoreboard().getEntryTeam(target.getName());
        if (sourceTeam == null || targetTeam == null) {
            return true;
        }
        return !sourceTeam.equals(targetTeam);
    }
}
