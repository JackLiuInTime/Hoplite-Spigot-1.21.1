package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PoseidonsTridentHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> lastDash = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lightningCooldown = new ConcurrentHashMap<>();
    private final long dashCooldownMillis = 30_000;
    private final long lightningCooldownMillis = 10_000;

    public PoseidonsTridentHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "poseidon_trident";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.TRIDENT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&3Poseidon's Trident"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary trident holding powers of",
                    ChatColor.GRAY + "the aquatic god.",
                    "",
                    ChatColor.WHITE + "SNEAK + THROW to launch yourself",
                    ChatColor.WHITE + "forwards with Riptide powers.",
                    ChatColor.DARK_GRAY + "30s Cooldown",
                    "",
                    ChatColor.WHITE + "50% chance to deal lightning damage on",
                    ChatColor.WHITE + "Riptide impact, 10% chance on throw.",
                    ChatColor.DARK_GRAY + "10s Cooldown",
                    "",
                    ChatColor.WHITE + "HOLD to gain " + ChatColor.AQUA + "Dolphin's Grace" + ChatColor.WHITE + "."
            ));
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
        player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 20 * 5, 0, true, false, true));

        if (!player.isSneaking()) {
            return;
        }

        UUID pid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long remaining = WeaponRuntime.remainingSeconds(lastDash, pid, dashCooldownMillis, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cPoseidon's Dash is on cooldown: " + remaining + "s");
            return;
        }

        WeaponRuntime.startCooldown(lastDash, pid, now, dashCooldownMillis, player, player.getInventory().getItemInMainHand());
        Vector direction = player.getLocation().getDirection().normalize().multiply(1.8);
        direction.setY(0.6);
        player.setVelocity(direction);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_1, 1f, 1f);
        player.sendMessage("§aPoseidon's Dash activated!");
        event.setCancelled(true);
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Trident trident) || !(trident.getShooter() instanceof Player player)) {
            return;
        }

        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (!WeaponRuntime.isOffCooldown(lightningCooldown, id, lightningCooldownMillis, now)) {
            return;
        }

        double chance = player.isRiptiding() ? 0.5 : 0.1;
        if (Math.random() >= chance) {
            return;
        }

        WeaponRuntime.startCooldown(lightningCooldown, id, now, lightningCooldownMillis, player, player.getInventory().getItemInMainHand());
        event.getEntity().getWorld().strikeLightningEffect(event.getEntity().getLocation());
        event.setDamage(event.getDamage() * 1.5);
    }
}
