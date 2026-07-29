package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
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

public class ShrinkRayHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private static final long COOLDOWN_MS = 50_000;
    private static final int EFFECT_TICKS = 12 * 20;
    private static final double SHRUNK_SCALE = 0.56;
    private static final double ENLARGED_SCALE = 2.22;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> effectUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Double> originalScale = new ConcurrentHashMap<>();

    public ShrinkRayHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "shrink_ray";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&dShrink Ray"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary blaster that allows the",
                    ChatColor.GRAY + "user to change the size of enemies and",
                    ChatColor.GRAY + "themselves.",
                    "",
                    ChatColor.WHITE + "LEFT CLICK to shrink yourself down to",
                    ChatColor.WHITE + "the size of 1 block tall for 12 seconds.",
                    ChatColor.WHITE + "While you are smaller, you will have an",
                    ChatColor.WHITE + "increased movement speed.",
                    "",
                    ChatColor.WHITE + "SHOOT to fire a beam that increases the",
                    ChatColor.WHITE + "size of enemy players to 4 blocks tall",
                    ChatColor.WHITE + "for 12 seconds. While larger, players",
                    ChatColor.WHITE + "will have a slower movement speed.",
                    "",
                    ChatColor.WHITE + "Only one of these abilities can be used",
                    ChatColor.WHITE + "at once.",
                    ChatColor.DARK_GRAY + "50s Cooldown"
            ));
                meta.setCustomModelData(12);
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
        long remaining = WeaponRuntime.remainingSeconds(cooldowns, id, COOLDOWN_MS, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cShrink Ray is on cooldown: " + remaining + "s");
            event.setCancelled(true);
            return;
        }

        if (effectUntil.getOrDefault(id, 0L) > now) {
            player.sendMessage("§eOnly one Shrink Ray ability can be active at once.");
            event.setCancelled(true);
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
            applyScaleForDuration(player, SHRUNK_SCALE, EFFECT_TICKS);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, EFFECT_TICKS, 1, true, true, true));
            WeaponRuntime.startCooldown(cooldowns, id, now, COOLDOWN_MS, player, player.getInventory().getItemInMainHand());
            effectUntil.put(id, now + EFFECT_TICKS * 50L);
            player.sendMessage("§dShrink Ray: you are now mini-sized for 12s.");
            event.setCancelled(true);
            return;
        }

        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        var result = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                20.0,
                0.5,
                e -> e instanceof Player && !e.getUniqueId().equals(id)
        );

        if (result == null || !(result.getHitEntity() instanceof Player target)) {
            player.sendMessage("§7No enemy player in beam range.");
            event.setCancelled(true);
            return;
        }

        applyScaleForDuration(target, ENLARGED_SCALE, EFFECT_TICKS);
        target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, EFFECT_TICKS, 1, true, true, true));
        target.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, target.getLocation().add(0, 1, 0), 25, 0.4, 0.6, 0.4, 0.02);

        WeaponRuntime.startCooldown(cooldowns, id, now, COOLDOWN_MS, player, player.getInventory().getItemInMainHand());
        effectUntil.put(id, now + EFFECT_TICKS * 50L);
        player.sendMessage("§dShrink Ray beam enlarged " + target.getName() + " for 12s.");
        event.setCancelled(true);
    }

    private void applyScaleForDuration(Player player, double scale, int durationTicks) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_SCALE);
        if (attr == null) {
            return;
        }

        UUID id = player.getUniqueId();
        originalScale.putIfAbsent(id, attr.getBaseValue());
        attr.setBaseValue(scale);

        new BukkitRunnable() {
            @Override
            public void run() {
                AttributeInstance current = player.getAttribute(Attribute.GENERIC_SCALE);
                if (current == null) {
                    originalScale.remove(id);
                    return;
                }
                double restore = originalScale.getOrDefault(id, 1.0);
                current.setBaseValue(restore);
                originalScale.remove(id);
            }
        }.runTaskLater(plugin, durationTicks);
    }
}
