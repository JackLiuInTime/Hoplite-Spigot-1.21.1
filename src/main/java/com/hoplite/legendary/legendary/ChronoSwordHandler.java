package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChronoSwordHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> rewindCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Location> rewindPoint = new ConcurrentHashMap<>();
    private final Map<UUID, Long> rewindPointExpiry = new ConcurrentHashMap<>();
    private final long rewindCooldownMs;
    private static final long REWIND_POINT_WINDOW_MS = 7_000;

    public ChronoSwordHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.rewindCooldownMs = plugin.getLegendaryCooldownMs("chrono_sword.time_rewind", 45_000L);
    }

    @Override
    public String getId() { return "chrono_sword"; }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&bChrono Sword"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary sword capable of manipulating time itself.",
                    "",
                    ChatColor.WHITE + "RIGHT CLICK: set a rewind point",
                    ChatColor.WHITE + "RIGHT CLICK: rewind to point within 7s",
                    ChatColor.DARK_GRAY + "COOLDOWN: 45s"
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            meta.addEnchant(Enchantment.SHARPNESS, 5, true);
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

        long remaining = WeaponRuntime.remainingSeconds(rewindCooldown, id, rewindCooldownMs, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cTime Rewind is on cooldown: " + remaining + "s");
            return;
        }

        Long expiry = rewindPointExpiry.get(id);
        Location point = rewindPoint.get(id);
        if (expiry != null && point != null && now <= expiry) {
            Location back = point.clone();
            back.setYaw(player.getLocation().getYaw());
            back.setPitch(player.getLocation().getPitch());
            player.teleport(back);
            player.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, back.add(0, 1, 0), 30, 0.35, 0.45, 0.35, 0.03);
            player.getWorld().playSound(player.getLocation(), org.bukkit.Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.8f, 1.2f);
            rewindPoint.remove(id);
            rewindPointExpiry.remove(id);
            WeaponRuntime.startCooldown(rewindCooldown, id, now, rewindCooldownMs, player, player.getInventory().getItemInMainHand());
            player.sendMessage("§bTime rewound.");
            event.setCancelled(true);
            return;
        }

        rewindPoint.put(id, player.getLocation().clone());
        rewindPointExpiry.put(id, now + REWIND_POINT_WINDOW_MS);
        player.sendMessage("§bRewind point set for 7s.");
        event.setCancelled(true);
    }
}

