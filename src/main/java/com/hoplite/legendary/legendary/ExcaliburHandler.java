package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ExcaliburHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> invocationCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> invulnerableUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> blockedHits = new ConcurrentHashMap<>();
    private final long invocationCooldownMs;
    private static final long INVULNERABLE_DURATION_MS = 10_000;
    private static final int MAX_BLOCKED_HITS = 3;

    public ExcaliburHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.invocationCooldownMs = plugin.getLegendaryCooldownMs("excalibur.invocation", 45_000L);
    }

    @Override
    public String getId() {
        return "excalibur";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.NETHERITE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Excalibur"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary blade that harnesses the",
                    ChatColor.GRAY + "power of the gods to protect the player",
                    ChatColor.GRAY + "from incoming attacks.",
                    "",
                    ChatColor.WHITE + "RIGHT CLICK to activate an invincibility",
                    ChatColor.WHITE + "period for 10s. The first 3 hits you",
                    ChatColor.WHITE + "take in this period will be cancelled.",
                    ChatColor.DARK_GRAY + "45s Cooldown"
            ));
                    meta.setCustomModelData(3);
            meta.getPersistentDataContainer().set(new NamespacedKey(this.plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onPlayerInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        if (!WeaponRuntime.isRightClick(event.getAction())) {
            return;
        }

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long remaining = WeaponRuntime.remainingSeconds(invocationCooldown, id, invocationCooldownMs, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cExcalibur is on cooldown: " + remaining + "s");
            return;
        }

        WeaponRuntime.startCooldown(invocationCooldown, id, now, invocationCooldownMs, player, player.getInventory().getItemInMainHand());
        invulnerableUntil.put(id, now + INVULNERABLE_DURATION_MS);
        blockedHits.put(id, 0);
        player.sendMessage("§6Excalibur invoked: invulnerable for 10s (up to 3 hits blocked).");
        event.setCancelled(true);
    }

    @Override
    public void onOwnerDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player defender)) {
            return;
        }

        UUID id = defender.getUniqueId();
        long now = System.currentTimeMillis();
        long until = invulnerableUntil.getOrDefault(id, 0L);
        if (now > until) {
            invulnerableUntil.remove(id);
            blockedHits.remove(id);
            return;
        }

        int count = blockedHits.getOrDefault(id, 0);
        if (count >= MAX_BLOCKED_HITS) {
            invulnerableUntil.remove(id);
            blockedHits.remove(id);
            return;
        }

        event.setCancelled(true);
        blockedHits.put(id, count + 1);
        defender.getWorld().spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING, defender.getLocation().add(0, 1, 0), 20, 0.3, 0.4, 0.3, 0.02);
        if (count + 1 >= MAX_BLOCKED_HITS) {
            defender.sendMessage("§eExcalibur protection expired (3/3 hits blocked).");
            invulnerableUntil.remove(id);
            blockedHits.remove(id);
        }
    }
}
