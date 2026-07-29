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
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GolemHammerHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();
    private final long cooldownMillis;

    public GolemHammerHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.cooldownMillis = plugin.getLegendaryCooldownMs("golem_hammer.leap_smash", 30_000L);
    }

    @Override
    public String getId() { return "golem_hammer"; }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.IRON_AXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Golem Hammer"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary hammer that allows you to harness Iron Golem abilities.",
                    "",
                    ChatColor.GOLD + "Ground Slam",
                    ChatColor.WHITE + "Sneak + right click to leap and slam",
                    ChatColor.WHITE + "Landing creates AOE knockback and slow",
                    ChatColor.DARK_GRAY + "Cooldown: 30s"
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            meta.addEnchant(Enchantment.SHARPNESS, 4, true);
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
        if (!event.getPlayer().isSneaking()) {
            return;
        }

        Player player = event.getPlayer();
        UUID pid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long remaining = WeaponRuntime.remainingSeconds(lastUse, pid, cooldownMillis, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cGolem Hammer 冷却中: " + remaining + "s");
            return;
        }

        WeaponRuntime.startCooldown(lastUse, pid, now, cooldownMillis, player, player.getInventory().getItemInMainHand());
        Vector velocity = player.getLocation().getDirection().normalize().multiply(0.6);
        velocity.setY(0.8);
        player.setVelocity(velocity);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 100, 0, true, false, false));
        player.sendMessage("§aGolem Hammer 已触发，准备落地冲击!");

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || player.isDead()) {
                    cancel();
                    return;
                }

                ticks++;
                if (!player.isOnGround() && ticks < 60) {
                    return;
                }

                int affected = 0;
                for (var entity : player.getNearbyEntities(4.0, 2.5, 4.0)) {
                    if (!(entity instanceof LivingEntity living) || living.equals(player)) {
                        continue;
                    }

                    if (living instanceof Player targetPlayer) {
                        var teamA = player.getScoreboard().getEntryTeam(player.getName());
                        var teamB = targetPlayer.getScoreboard().getEntryTeam(targetPlayer.getName());
                        if (teamA != null && teamB != null && teamA.equals(teamB)) {
                            continue;
                        }
                    }

                    Vector knockback = living.getLocation().toVector().subtract(player.getLocation().toVector());
                    if (knockback.lengthSquared() > 0.0001) {
                        knockback.normalize().multiply(0.8).setY(0.35);
                        living.setVelocity(knockback);
                    }
                    living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 60, 1, true, true, true));
                    affected++;
                }

                player.getWorld().spawnParticle(org.bukkit.Particle.EXPLOSION, player.getLocation().add(0, 0.1, 0), 1, 0.0, 0.0, 0.0, 0.0);
                player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_IRON_GOLEM_ATTACK, 1.0f, 0.8f);
                if (affected > 0) {
                    player.sendMessage("§6Ground Slam 命中 " + affected + " 个目标。");
                }
                cancel();
            }
        }.runTaskTimer(plugin, 1L, 1L);
        event.setCancelled(true);
    }
}

