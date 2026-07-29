package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
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

public class ShadowBladeHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> shadowCooldown = new ConcurrentHashMap<>();
    private final Set<UUID> stealthActive = ConcurrentHashMap.newKeySet();
    private static final long COOLDOWN_MS = 30_000;
    private static final int STEALTH_TICKS = 10 * 20;

    public ShadowBladeHandler(HoplitePlugin plugin) { this.plugin = plugin; }

    @Override
    public String getId() { return "shadow_blade"; }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&0Shadow Blade"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "Strike from the shadows.",
                    "",
                    ChatColor.WHITE + "RIGHT CLICK: enter shadow stealth",
                    ChatColor.WHITE + "RIGHT CLICK: gain invisibility and Speed III for 10s",
                    ChatColor.WHITE + "HIT PLAYER: stealth is canceled instantly",
                    ChatColor.DARK_GRAY + "COOLDOWN: 30s"
            ));
                meta.setCustomModelData(4);
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
        long remaining = WeaponRuntime.remainingSeconds(shadowCooldown, id, COOLDOWN_MS, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cShadow Sneak is on cooldown: " + remaining + "s");
            return;
        }

        WeaponRuntime.startCooldown(shadowCooldown, id, now, COOLDOWN_MS, player, player.getInventory().getItemInMainHand());
        stealthActive.add(id);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, STEALTH_TICKS, 0, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, STEALTH_TICKS, 2, true, false, false));
        player.sendMessage("§8You vanish into the shadows.");
        event.setCancelled(true);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> stealthActive.remove(id), STEALTH_TICKS);
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        UUID id = attacker.getUniqueId();
        if (!stealthActive.remove(id)) {
            return;
        }

        attacker.removePotionEffect(PotionEffectType.INVISIBILITY);
        attacker.sendMessage("§7Stealth broken.");
    }
}

