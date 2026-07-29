package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class CloudSwordHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final long cooldownMs;
    private static final long ACTIVE_MS = 10_000;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Long> activeUntil = new ConcurrentHashMap<>();

    public CloudSwordHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.cooldownMs = plugin.getLegendaryCooldownMs("cloud_sword.cloud_form", 25_000L);
    }

    @Override
    public String getId() {
        return "cloud_sword";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&fCloud Sword"));
            meta.setLore(List.of(
                ChatColor.GRAY + "A legendary weapon that grants the user",
                ChatColor.GRAY + "the power of the clouds.",
                    "",
                ChatColor.WHITE + "RIGHT CLICK to summon clouds that can",
                ChatColor.WHITE + "only be traversed by the wielder of the",
                ChatColor.WHITE + "sword. This ability lasts for 10s and",
                ChatColor.WHITE + "can be toggled on and off throughout",
                ChatColor.WHITE + "the duration of the ability using right",
                ChatColor.WHITE + "click.",
                ChatColor.DARK_GRAY + "25s Cooldown",
                "",
                ChatColor.WHITE + "HOLD in main hand to prevent all fall",
                ChatColor.WHITE + "damage and to gain a small speed boost."
            ));
            meta.setCustomModelData(7);
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

        long activeEnd = activeUntil.getOrDefault(id, 0L);
        if (activeEnd > now) {
            activeUntil.remove(id);
            player.sendMessage("§7Cloud Sword ability toggled off.");
            event.setCancelled(true);
            return;
        }

        long remaining = WeaponRuntime.remainingSeconds(cooldowns, id, cooldownMs, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cCloud Sword is on cooldown: " + remaining + "s");
            event.setCancelled(true);
            return;
        }

        WeaponRuntime.startCooldown(cooldowns, id, now, cooldownMs, player, player.getInventory().getItemInMainHand());
        activeUntil.put(id, now + ACTIVE_MS);
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * 11, 0, true, false, true));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 11, 0, true, false, true));
        player.sendMessage("§fCloud Sword ability active for 10s.");
        event.setCancelled(true);
    }

    @Override
    public void onOwnerDamage(EntityDamageEvent event) {
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 40, 0, true, false, false));
    }
}
