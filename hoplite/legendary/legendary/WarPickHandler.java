package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import com.hoplite.legendary.MiningListener;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.Sound;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class WarPickHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> lastUse = new ConcurrentHashMap<>();
    private final long cooldownMillis = 5_000; // 5 seconds cooldown
    private final long critCooldownMillis = 20_000; // 20 seconds cooldown for crit effect
    private final Map<UUID, Long> lastCrit = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public WarPickHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "war_pick";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.DIAMOND_PICKAXE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&cWar Pick"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary pickaxe that deals devastating blows.",
                    "",
                    ChatColor.WHITE + "MINE a block to trigger a large",
                    ChatColor.WHITE + "explosion that destroys blocks in a",
                    ChatColor.WHITE + "3x3x3 area.",
                    ChatColor.DARK_GRAY + "5s Cooldown",
                    "",
                    ChatColor.WHITE + "CRITICAL HIT a player to have a chance",
                    ChatColor.WHITE + "to slightly knock them back and reduce",
                    ChatColor.WHITE + "the durability of a random armor piece.",
                    ChatColor.DARK_GRAY + "20s Cooldown"
            ));
                    meta.setCustomModelData(1);
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            item.setItemMeta(meta);
        }
        item.addUnsafeEnchantment(Enchantment.SHARPNESS, 10);
        item.addUnsafeEnchantment(Enchantment.EFFICIENCY, 1);
        return item;
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        UUID pid = player.getUniqueId();
        long now = System.currentTimeMillis();
        long remainingSeconds = WeaponRuntime.remainingSeconds(lastUse, pid, cooldownMillis, now);
        if (remainingSeconds > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, item, remainingSeconds);
            player.sendMessage("§cWar Pick ability is on cooldown: " + remainingSeconds + "s");
            return;
        }
        WeaponRuntime.startCooldown(lastUse, pid, now, cooldownMillis, player, item);

        event.getPlayer().getWorld().playSound(event.getPlayer().getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.2f, 0.8f);
        event.getPlayer().sendMessage("§aWar Pick ability triggered: 3x3x3 mining blast activated!");

        Block originBlock = event.getBlock();
        int radius = 1; // 3x3x3

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue; // keep original block break behavior and drops
                    }

                    Block target = originBlock.getWorld().getBlockAt(
                            originBlock.getX() + dx,
                            originBlock.getY() + dy,
                            originBlock.getZ() + dz
                    );
                    if (target.isEmpty() || target.getType() == Material.BEDROCK) continue;

                    org.bukkit.Material t = target.getType();
                    if (t == Material.CHEST || t == Material.TRAPPED_CHEST || t == Material.SPAWNER || t == Material.ANVIL || t == Material.BARREL) {
                        continue;
                    }

                    MiningListener.markControlledBreak(target.getLocation());
                    target.breakNaturally(item);
                }
            }
        }

        // Damage the pick a bit for using the ability (Damageable API)
        try {
            ItemMeta meta = item.getItemMeta();
            if (meta instanceof Damageable dmg) {
                dmg.setDamage(dmg.getDamage() + 1);
                item.setItemMeta((ItemMeta) dmg);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.DIAMOND_PICKAXE) {
            return;
        }
        if (!plugin.getWeaponManager().fromItem(item).map(h -> h.getId().equals(getId())).orElse(false)) {
            return;
        }

        UUID pid = player.getUniqueId();
        long now = System.currentTimeMillis();
        if (!WeaponRuntime.isOffCooldown(lastCrit, pid, critCooldownMillis, now)) {
            return;
        }

        boolean isCritical = player.getFallDistance() > 0.0f
            && !player.isOnGround()
            && !player.isInsideVehicle()
            && !player.hasPotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS)
            && !player.isSprinting();
        if (!isCritical) {
            return;
        }

        double chance = 0.3;
        if (random.nextDouble() >= chance) {
            return;
        }

        WeaponRuntime.startCooldown(lastCrit, pid, now, critCooldownMillis, player, item);
        event.getDamager().getWorld().playSound(event.getEntity().getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
        event.getEntity().setVelocity(event.getEntity().getVelocity().add(player.getLocation().getDirection().normalize().multiply(0.25)).setY(0.15));

        if (event.getEntity() instanceof Player targetPlayer) {
            ItemStack[] armor = targetPlayer.getInventory().getArmorContents();
            int slot = random.nextInt(armor.length);
            ItemStack piece = armor[slot];
            if (piece != null && piece.getType() != Material.AIR && piece.hasItemMeta() && piece.getItemMeta() instanceof Damageable dmgPiece) {
                dmgPiece.setDamage(Math.min(dmgPiece.getDamage() + 5, piece.getType().getMaxDurability()));
                piece.setItemMeta((ItemMeta) dmgPiece);
            }
            targetPlayer.getInventory().setArmorContents(armor);
            player.sendMessage("§aCritical effect triggered: target knocked back and one armor piece damaged.\n§bNext crit cooldown: 20s.");
        }
    }
}

