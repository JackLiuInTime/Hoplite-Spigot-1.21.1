package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
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

public class HypnosisStaffHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();
    private final long cooldownMillis = 5000;
    private static final String CONTROL_KEY = "hypnosis_staff_controlled";

    public HypnosisStaffHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "hypnosis_staff";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&5Hypnosis Staff"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary staff that can convert any mob into a companion.",
                    "",
                    ChatColor.GOLD + "Mind Control",
                    ChatColor.WHITE + "Sneak + right click to cast on a mob",
                    ChatColor.WHITE + "Recast on controlled mobs to heal them",
                    ChatColor.DARK_GRAY + "Cooldown: 5s"
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!WeaponRuntime.isRightClick(event.getAction())) {
            return;
        }
        if (!event.getPlayer().isSneaking()) {
            return;
        }

        Player player = event.getPlayer();
        UUID pid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long remaining = WeaponRuntime.remainingSeconds(lastUse, pid, cooldownMillis, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cHypnosis Staff is on cooldown: " + remaining + "s");
            return;
        }

        var ray = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                10,
                entity -> entity instanceof LivingEntity && !(entity instanceof Player) && entity != player
        );
        if (ray == null || !(ray.getHitEntity() instanceof LivingEntity target)) {
            player.sendMessage("§cNo valid mob found to hypnotize.");
            return;
        }

        WeaponRuntime.startCooldown(lastUse, pid, now, cooldownMillis, player, player.getInventory().getItemInMainHand());
        if (target.getPersistentDataContainer().has(new NamespacedKey(plugin, CONTROL_KEY), PersistentDataType.STRING)) {
            target.setHealth(Math.min(WeaponRuntime.getMaxHealth(target, target.getHealth()), target.getHealth() + 10));
            player.sendMessage("§aHealed an already hypnotized target.");
        } else {
            target.getPersistentDataContainer().set(new NamespacedKey(plugin, CONTROL_KEY), PersistentDataType.STRING, player.getUniqueId().toString());
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 200, 1, true, false, false));
            target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 200, 1, true, false, false));
            target.setCustomName("§dHypnotized");
            target.setCustomNameVisible(true);
            player.sendMessage("§aTarget mob has been hypnotized.");
        }
        event.setCancelled(true);
    }
}

