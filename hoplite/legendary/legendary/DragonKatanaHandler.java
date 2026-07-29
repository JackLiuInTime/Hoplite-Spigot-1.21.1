package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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

public class DragonKatanaHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> lastBlinkUse = new ConcurrentHashMap<>();
    private final long blinkCooldownMillis;

    public DragonKatanaHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.blinkCooldownMillis = plugin.getLegendaryCooldownMs("dragon_katana.dragon_blink", 12_000L);
    }

    @Override
    public String getId() { return "dragon_katana"; }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cDragon Katana"));
            meta.setLore(List.of(
                ChatColor.GRAY + "A legendary weapon that harnesses the",
                ChatColor.GRAY + "swiftness of a great dragon.",
                    "",
                ChatColor.WHITE + "RIGHT CLICK to teleport in the",
                ChatColor.WHITE + "direction you are looking.",
                ChatColor.DARK_GRAY + "12s Cooldown"
            ));
            meta.setCustomModelData(1);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
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
        long now = System.currentTimeMillis();
        UUID id = player.getUniqueId();
        long remaining = WeaponRuntime.remainingSeconds(lastBlinkUse, id, blinkCooldownMillis, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cDragon Blink is on cooldown: " + remaining + "s");
            return;
        }

        Vector dir = player.getLocation().getDirection().normalize();
        Location target = player.getLocation().clone().add(dir.multiply(8.0));
        target.setYaw(player.getLocation().getYaw());
        target.setPitch(player.getLocation().getPitch());
        target = getSafeBlinkTarget(player, target);

        WeaponRuntime.startCooldown(lastBlinkUse, id, now, blinkCooldownMillis, player, player.getInventory().getItemInMainHand());
        player.teleport(target);
        player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, player.getLocation(), 30, 0.4, 0.6, 0.4, 0.02);
        player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.1f);
        event.setCancelled(true);
    }

    private Location getSafeBlinkTarget(Player player, Location target) {
        // Keep blink simple but avoid teleporting into blocked two-block player space.
        if (isBlocked(target)) {
            target = target.clone().add(0, 1, 0);
        }
        if (isBlocked(target)) {
            target = target.clone().add(0, 1, 0);
        }
        if (isBlocked(target)) {
            return player.getLocation();
        }
        return target;
    }

    private boolean isBlocked(Location location) {
        return location.getBlock().getType().isSolid()
                || location.clone().add(0, 1, 0).getBlock().getType().isSolid();
    }
}

