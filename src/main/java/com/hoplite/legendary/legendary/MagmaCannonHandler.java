package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MagmaCannonHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> toggleCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> normalShotCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> superShotCooldown = new ConcurrentHashMap<>();
    private final Set<UUID> superMode = ConcurrentHashMap.newKeySet();
    private static final long TOGGLE_COOLDOWN_MS = 2_000;
    private static final long NORMAL_SHOT_COOLDOWN_MS = 8_000;
    private static final long SUPER_SHOT_COOLDOWN_MS = 24_000;

    public MagmaCannonHandler(HoplitePlugin plugin) { this.plugin = plugin; }

    @Override
    public String getId() { return "magma_cannon"; }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cMagma Cannon"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary cannon with explosive magma rounds.",
                    "",
                    ChatColor.WHITE + "LEFT CLICK: swap Normal and Super mode",
                    ChatColor.WHITE + "ARROW HIT: Normal mode fires faster",
                    ChatColor.WHITE + "ARROW HIT: Super mode has stronger impact",
                    ChatColor.DARK_GRAY + "COOLDOWN: Toggle 2s, Super reload 24s"
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            meta.addEnchant(Enchantment.QUICK_CHARGE, 3, true);
            meta.addEnchant(Enchantment.MULTISHOT, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_AIR
                && event.getAction() != org.bukkit.event.block.Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long remaining = WeaponRuntime.remainingSeconds(toggleCooldown, id, TOGGLE_COOLDOWN_MS, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cMode toggle is on cooldown: " + remaining + "s");
            return;
        }

        WeaponRuntime.startCooldown(toggleCooldown, id, now, TOGGLE_COOLDOWN_MS, player, player.getInventory().getItemInMainHand());
        if (superMode.contains(id)) {
            superMode.remove(id);
            player.sendMessage("§6Magma Cannon set to §fNormal §6mode.");
        } else {
            superMode.add(id);
            player.sendMessage("§6Magma Cannon set to §cSuper §6mode.");
        }
        event.setCancelled(true);
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow) || !(arrow.getShooter() instanceof Player shooter)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        UUID id = shooter.getUniqueId();
        long now = System.currentTimeMillis();
        if (superMode.contains(id)) {
            if (!WeaponRuntime.isOffCooldown(superShotCooldown, id, SUPER_SHOT_COOLDOWN_MS, now)) {
                return;
            }
            WeaponRuntime.startCooldown(superShotCooldown, id, now, SUPER_SHOT_COOLDOWN_MS, shooter, shooter.getInventory().getItemInMainHand());
            event.setDamage(event.getDamage() + 4.0);
            target.setFireTicks(Math.max(target.getFireTicks(), 6 * 20));
            target.getWorld().createExplosion(target.getLocation().getX(), target.getLocation().getY(), target.getLocation().getZ(), 2.0f, false, false, shooter);
            shooter.sendMessage("§cSuper magma round triggered.");
            return;
        }

        if (!WeaponRuntime.isOffCooldown(normalShotCooldown, id, NORMAL_SHOT_COOLDOWN_MS, now)) {
            return;
        }
        WeaponRuntime.startCooldown(normalShotCooldown, id, now, NORMAL_SHOT_COOLDOWN_MS, shooter, shooter.getInventory().getItemInMainHand());
        event.setDamage(event.getDamage() + 1.5);
        target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 40, 0, true, true, true));
        target.setFireTicks(Math.max(target.getFireTicks(), 3 * 20));
    }
}

