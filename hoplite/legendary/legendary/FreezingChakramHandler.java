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
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FreezingChakramHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Integer> charges = new ConcurrentHashMap<>();
    private static final int MAX_CHARGES = 4;
    private static final long RECHARGE_TICKS = 15 * 20L;

    public FreezingChakramHandler(HoplitePlugin plugin) { this.plugin = plugin; }

    @Override
    public String getId() { return "freezing_chakram"; }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.IRON_HOE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&bFreezing Chakram"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary pair of chakrams imbued with arctic power.",
                    "",
                    ChatColor.WHITE + "RIGHT CLICK: throw a freezing chakram",
                    ChatColor.WHITE + "RIGHT CLICK: outbound and return both strike",
                    ChatColor.DARK_GRAY + "CHARGES: 4 (15s per recharge)"
            ));
                meta.setCustomModelData(13);
                meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            meta.addEnchant(Enchantment.SHARPNESS, 4, true);
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
        int current = charges.getOrDefault(id, MAX_CHARGES);
        if (current <= 0) {
            player.sendMessage("§cNo chakram charges available.");
            return;
        }

        charges.put(id, current - 1);
        castChakramPulse(player, true);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> castChakramPulse(player, false), 12L);

        // Each use schedules exactly one charge regen after 15s.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            int restored = Math.min(MAX_CHARGES, charges.getOrDefault(id, 0) + 1);
            charges.put(id, restored);
        }, RECHARGE_TICKS);

        player.sendMessage("§bFreezing Chakram cast. Charges left: " + (current - 1));
        event.setCancelled(true);
    }

    private void castChakramPulse(Player player, boolean outbound) {
        var center = player.getLocation().add(player.getLocation().getDirection().normalize().multiply(outbound ? 4.0 : 1.0));
        int affected = 0;
        for (var entity : player.getWorld().getNearbyEntities(center, 2.5, 2, 2.5)) {
            if (!(entity instanceof LivingEntity living) || living.equals(player)) {
                continue;
            }
            living.damage(outbound ? 3.0 : 2.0, player);
            living.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 50, 1, true, true, true));
            affected++;
        }

        player.getWorld().spawnParticle(org.bukkit.Particle.SNOWFLAKE, center.add(0, 1, 0), outbound ? 24 : 16, 0.5, 0.35, 0.5, 0.03);
        if (!outbound && affected > 0) {
            player.sendMessage("§bChakram returned through " + affected + " target(s).");
        }
    }
}

