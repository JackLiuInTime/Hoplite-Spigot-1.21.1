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
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class HarpoonLauncherHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> grappleCooldown = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 22_000;

    public HarpoonLauncherHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "harpoon_launcher";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&3Harpoon Launcher"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary launcher that fires powerful harpoons...",
                    "",
                    ChatColor.WHITE + "RIGHT CLICK: grapple terrain to pull yourself",
                    ChatColor.WHITE + "RIGHT CLICK: hook enemies to pull and damage",
                    ChatColor.DARK_GRAY + "COOLDOWN: 22s"
            ));
                meta.setCustomModelData(11);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            meta.addEnchant(Enchantment.QUICK_CHARGE, 2, true);
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
        long remaining = WeaponRuntime.remainingSeconds(grappleCooldown, id, COOLDOWN_MS, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cGrapple is on cooldown: " + remaining + "s");
            return;
        }

        var entityHit = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                20,
                entity -> entity instanceof LivingEntity && entity != player
        );

        WeaponRuntime.startCooldown(grappleCooldown, id, now, COOLDOWN_MS, player, player.getInventory().getItemInMainHand());
        if (entityHit != null && entityHit.getHitEntity() instanceof LivingEntity target) {
            Vector pull = player.getLocation().toVector().subtract(target.getLocation().toVector()).normalize().multiply(1.1);
            pull.setY(0.2);
            target.setVelocity(pull);
            target.damage(4.0, player);
            player.getWorld().spawnParticle(org.bukkit.Particle.CRIT, target.getLocation().add(0, 1, 0), 12, 0.2, 0.3, 0.2, 0.02);
            player.sendMessage("§3Harpoon hooked an enemy.");
            event.setCancelled(true);
            return;
        }

        var blockHit = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 22);
        if (blockHit == null) {
            player.sendMessage("§7No valid grapple point found.");
            return;
        }

        Vector toward = blockHit.getHitPosition().subtract(player.getLocation().toVector()).normalize().multiply(1.45);
        toward.setY(Math.max(0.3, toward.getY()));
        player.setVelocity(toward);
        player.getWorld().spawnParticle(org.bukkit.Particle.SPLASH, player.getLocation().add(0, 1, 0), 14, 0.25, 0.35, 0.25, 0.03);
        player.sendMessage("§3Harpoon grapple engaged.");
        event.setCancelled(true);
    }
}

