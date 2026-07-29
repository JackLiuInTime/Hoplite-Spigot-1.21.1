package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CrimsonChainswordHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> curseCooldown = new ConcurrentHashMap<>();
    private final Set<UUID> armedCurse = ConcurrentHashMap.newKeySet();
    private final Set<UUID> bleedingTargets = ConcurrentHashMap.newKeySet();

    private final long curseCooldownMs;

    public CrimsonChainswordHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.curseCooldownMs = plugin.getLegendaryCooldownMs("crimson_chainsword.rending_chain", 45_000L);
    }

    @Override
    public String getId() {
        return "crimson_chainsword";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&4Crimson Chainsword"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A brutal weapon, heralded for its",
                    ChatColor.GRAY + "ability to strike down hordes of foes.",
                    "",
                    ChatColor.WHITE + "RIGHT CLICK to apply a curse to the",
                    ChatColor.WHITE + "blade, making your next attack apply a",
                    ChatColor.WHITE + "0.75 second bleed dealing 1.5 hearts of",
                    ChatColor.WHITE + "damage over the duration. This bleed",
                    ChatColor.WHITE + "does not stack.",
                    ChatColor.DARK_GRAY + "45s Cooldown",
                    "",
                    ChatColor.WHITE + "KILL players to gain " + ChatColor.RED + "Strength II " + ChatColor.WHITE + "for 15",
                    ChatColor.WHITE + "seconds.",
                    "",
                    ChatColor.WHITE + "This weapon deals 1.5x damage to",
                    ChatColor.WHITE + "enemies under 20% health."
            ));
                    meta.setCustomModelData(8);
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
        long remaining = WeaponRuntime.remainingSeconds(curseCooldown, id, curseCooldownMs, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cRending Chain is on cooldown: " + remaining + "s");
            return;
        }

        WeaponRuntime.startCooldown(curseCooldown, id, now, curseCooldownMs, player, player.getInventory().getItemInMainHand());
        armedCurse.add(id);
        player.sendMessage("§4Curse applied. Your next attack will inflict bleed.");
        event.setCancelled(true);
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        double maxHealth = WeaponRuntime.getMaxHealth(target, target.getHealth());
        if (maxHealth > 0 && target.getHealth() / maxHealth <= 0.20) {
            event.setDamage(event.getDamage() * 1.5);
        }

        UUID attackerId = attacker.getUniqueId();
        if (armedCurse.remove(attackerId)) {
            applyBleed(target, attacker);
        }

        if (target instanceof Player && target.getHealth() - event.getFinalDamage() <= 0.0) {
            attacker.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 15 * 20, 1, true, true, true));
            attacker.sendMessage("§cCrimson power surges through you (Strength II, 15s).");
        }
    }

    private void applyBleed(LivingEntity target, Player attacker) {
        UUID targetId = target.getUniqueId();
        if (!bleedingTargets.add(targetId)) {
            attacker.sendMessage("§eTarget is already bleeding.");
            return;
        }

        attacker.sendMessage("§cBleed applied.");
        target.getWorld().spawnParticle(org.bukkit.Particle.DAMAGE_INDICATOR, target.getLocation().add(0, 1, 0), 8, 0.2, 0.3, 0.2, 0.02);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!target.isValid() || target.isDead()) {
                    bleedingTargets.remove(targetId);
                    cancel();
                    return;
                }

                // Apply bleed as neutral DOT so it is not re-processed as a direct melee strike.
                // We tick at 5, 10, 15 to match a 0.75s total duration.
                ticks += 5;
                target.damage(1.0);
                if (ticks >= 15) {
                    bleedingTargets.remove(targetId);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 5L, 5L);
    }
}
