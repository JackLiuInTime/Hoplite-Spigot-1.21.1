package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Bee;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Team;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class BeehiveBlasterHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final long cooldownMs;
    private static final int TRACK_RADIUS = 28;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public BeehiveBlasterHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.cooldownMs = plugin.getLegendaryCooldownMs("beehive_blaster.swarm_shot", 32_000L);
    }

    @Override
    public String getId() {
        return "beehive_blaster";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eBeehive Blaster"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary blaster that launches enraged bees.",
                    "",
                    ChatColor.WHITE + "SHOOT to launch a cluster of 4 bees that",
                    ChatColor.WHITE + "will sting enemy players.",
                    ChatColor.DARK_GRAY + "32s Cooldown"
            ));
                    meta.setCustomModelData(9);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        // Beehive Blaster is shoot-activated by design.
    }

    @Override
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long remaining = WeaponRuntime.remainingSeconds(cooldowns, id, cooldownMs, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cSwarm Shot is on cooldown: " + remaining + "s");
            event.setCancelled(true);
            return;
        }

        Player target = findNearestEnemy(player);
        if (target == null) {
            player.sendMessage("§7No enemy player found in range.");
            event.setCancelled(true);
            return;
        }

        for (int i = 0; i < 4; i++) {
            Bee bee = player.getWorld().spawn(player.getLocation().add(0, 1, 0), Bee.class);
            bee.setTarget(target);
            bee.setAnger(20 * 20);
            bee.setRemoveWhenFarAway(true);
            bee.setHasNectar(false);
            bee.setCustomName("§eSwarm Bee");
        }

        WeaponRuntime.startCooldown(cooldowns, id, now, cooldownMs, player, player.getInventory().getItemInMainHand());
        player.sendMessage("§eBee swarm launched toward " + target.getName() + ".");
    }

    private Player findNearestEnemy(Player source) {
        Team sourceTeam = source.getScoreboard().getEntryTeam(source.getName());
        return source.getWorld().getPlayers().stream()
                .filter(p -> !p.equals(source))
                .filter(p -> p.getLocation().distanceSquared(source.getLocation()) <= TRACK_RADIUS * TRACK_RADIUS)
                .filter(p -> isEnemy(sourceTeam, p))
                .min(java.util.Comparator.comparingDouble(p -> p.getLocation().distanceSquared(source.getLocation())))
                .orElse(null);
    }

    private boolean isEnemy(Team sourceTeam, Player target) {
        Team targetTeam = target.getScoreboard().getEntryTeam(target.getName());
        if (sourceTeam == null || targetTeam == null) {
            return true;
        }
        return !sourceTeam.equals(targetTeam);
    }
}

