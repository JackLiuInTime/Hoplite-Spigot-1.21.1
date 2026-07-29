package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
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

public class HornOfWinterHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> winterCooldown = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 30_000;

    public HornOfWinterHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "horn_of_winter";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&bHorn of Winter"));
            meta.setLore(List.of(
                ChatColor.GRAY + "A legendary horn that summons a",
                ChatColor.GRAY + "powerful horse ally.",
                    "",
                ChatColor.WHITE + "USE to summon the Ice Horse.",
                ChatColor.WHITE + "Dismounting despawns the horse and",
                ChatColor.WHITE + "begins a cooldown to reuse the horn",
                ChatColor.WHITE + "ability. The cooldown ranges between 30",
                ChatColor.WHITE + "and 300 seconds based on the health of",
                ChatColor.WHITE + "the horse.",
                "",
                ChatColor.WHITE + "SHOOT while mounted to turn your arrow",
                ChatColor.WHITE + "into a deadly ice shard that will fly",
                ChatColor.WHITE + "in the direction you are looking.",
                "",
                ChatColor.WHITE + "RIGHT CLICK while riding the Ice Horse",
                ChatColor.WHITE + "to activate a Blizzard ability, turning",
                ChatColor.WHITE + "the surface in front of the horse's",
                ChatColor.WHITE + "feet into powdered snow. Enemies in",
                ChatColor.WHITE + "front of the horse will be damaged and",
                ChatColor.WHITE + "inflicted with freezing for 10s.",
                ChatColor.DARK_GRAY + "30s Cooldown"
            ));
                meta.setCustomModelData(1);
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
        long remaining = WeaponRuntime.remainingSeconds(winterCooldown, id, COOLDOWN_MS, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cWinter Call is on cooldown: " + remaining + "s");
            return;
        }

        var hit = player.getWorld().rayTraceBlocks(player.getEyeLocation(), player.getEyeLocation().getDirection(), 18.0);
        org.bukkit.Location center = hit != null
                ? hit.getHitPosition().toLocation(player.getWorld())
                : player.getLocation().add(player.getLocation().getDirection().multiply(8.0));

        int affected = 0;
        for (var entity : player.getWorld().getNearbyEntities(center, 4, 3, 4)) {
            if (!(entity instanceof LivingEntity living) || living.equals(player)) {
                continue;
            }
            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 1, true, true, true));
            living.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 80, 0, true, true, true));
            affected++;
        }

        WeaponRuntime.startCooldown(winterCooldown, id, now, COOLDOWN_MS, player, player.getInventory().getItemInMainHand());
        player.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, center.add(0, 1, 0), 35, 1.2, 0.6, 1.2, 0.03);
        player.getWorld().playSound(center, org.bukkit.Sound.BLOCK_POWDER_SNOW_BREAK, 1f, 0.8f);
        if (affected > 0) {
            player.sendMessage("§bWinter Call affected " + affected + " target(s).");
        } else {
            player.sendMessage("§7Winter Call released, but no targets were nearby.");
        }
        event.setCancelled(true);
    }
}
