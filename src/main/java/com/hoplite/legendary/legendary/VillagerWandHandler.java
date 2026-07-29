package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VillagerWandHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> beamCooldown = new ConcurrentHashMap<>();
    private static final long BEAM_COOLDOWN_MS = 60_000;

    public VillagerWandHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "villager_wand";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.STONE_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&2Villager Wand"));
            meta.setLore(List.of(
                ChatColor.GRAY + "A legendary wand that harnesses the",
                ChatColor.GRAY + "power of the villagers.",
                    "",
                ChatColor.WHITE + "RIGHT CLICK to summon a powerful beam",
                ChatColor.WHITE + "causing a large explosion.",
                ChatColor.DARK_GRAY + "60s Cooldown",
                    "",
                ChatColor.WHITE + "LEFT CLICK mobs to transmute them into",
                ChatColor.WHITE + "pure emeralds essence."
            ));
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
        long remaining = WeaponRuntime.remainingSeconds(beamCooldown, id, BEAM_COOLDOWN_MS, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cVillager beam is on cooldown: " + remaining + "s");
            return;
        }

        var hit = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 24.0);
        org.bukkit.Location blastCenter = hit != null ? hit.getHitPosition().toLocation(player.getWorld()) : player.getLocation().add(player.getLocation().getDirection().multiply(12));

        WeaponRuntime.startCooldown(beamCooldown, id, now, BEAM_COOLDOWN_MS, player, player.getInventory().getItemInMainHand());
        player.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, blastCenter, 45, 0.5, 0.5, 0.5, 0.02);
        player.getWorld().createExplosion(blastCenter.getX(), blastCenter.getY(), blastCenter.getZ(), 3.0f, false, false, player);
        player.sendMessage("§aVillager beam unleashed.");
        event.setCancelled(true);
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target) || event.getEntity() instanceof Player) {
            return;
        }

        target.getWorld().spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER, target.getLocation().add(0, 1, 0), 16, 0.25, 0.35, 0.25, 0.01);
        target.remove();
        target.getWorld().dropItemNaturally(target.getLocation(), new ItemStack(Material.EMERALD, 3));
        attacker.sendMessage("§aMob transmuted into pure emerald essence.");
        event.setCancelled(true);
    }
}
