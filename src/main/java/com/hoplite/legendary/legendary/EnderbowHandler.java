package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Endermite;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class EnderbowHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Boolean> teleportMode = new ConcurrentHashMap<>();
    private final Map<UUID, Long> teleportShotCooldown = new ConcurrentHashMap<>();
    private final Set<UUID> arrowModeProjectiles = ConcurrentHashMap.newKeySet();
    private final long teleportCooldownMs;

    public EnderbowHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.teleportCooldownMs = plugin.getLegendaryCooldownMs("enderbow.teleport_shot", 55_000L);
    }

    @Override
    public String getId() {
        return "enderbow";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&5Enderbow"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary bow smithed in the darkest",
                    ChatColor.GRAY + "depths of the End dimension.",
                    "",
                    ChatColor.WHITE + "LEFT CLICK to change modes.",
                    "",
                    ChatColor.GREEN + "Teleport Mode",
                    ChatColor.WHITE + "SHOOT to fire an ender pearl.",
                    ChatColor.DARK_GRAY + "55s Cooldown",
                    "",
                    ChatColor.GREEN + "Arrow Mode",
                    ChatColor.WHITE + "SHOOT to fire a regular arrow. Arrows",
                    ChatColor.WHITE + "have a chance of spawning an endermite",
                    ChatColor.WHITE + "where they land."
            ));
                    meta.setCustomModelData(1);
            meta.getPersistentDataContainer().set(new NamespacedKey(this.plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            meta.addEnchant(Enchantment.POWER, 3, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.LEFT_CLICK_AIR && action != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        boolean nextTeleportMode = !teleportMode.getOrDefault(id, true);
        teleportMode.put(id, nextTeleportMode);
        player.sendMessage(nextTeleportMode ? "§aEnderbow mode: Teleport" : "§aEnderbow mode: Arrow");
        event.setCancelled(true);
    }

    @Override
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        UUID id = player.getUniqueId();
        boolean isTeleportMode = teleportMode.getOrDefault(id, true);
        if (!isTeleportMode) {
            if (event.getProjectile() instanceof Arrow arrow) {
                arrowModeProjectiles.add(arrow.getUniqueId());
            }
            return;
        }

        long now = System.currentTimeMillis();
        long remaining = WeaponRuntime.remainingSeconds(teleportShotCooldown, id, teleportCooldownMs, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cTeleport shot is on cooldown: " + remaining + "s");
            event.setCancelled(true);
            return;
        }

        WeaponRuntime.startCooldown(teleportShotCooldown, id, now, teleportCooldownMs, player, player.getInventory().getItemInMainHand());
        event.setCancelled(true);

        EnderPearl pearl = player.launchProjectile(EnderPearl.class);
        pearl.setVelocity(event.getProjectile().getVelocity());
        player.sendMessage("§5Teleport shot fired.");
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow) || !(arrow.getShooter() instanceof Player)) {
            return;
        }
        if (!arrowModeProjectiles.remove(arrow.getUniqueId())) {
            return;
        }

        if (Math.random() < 0.25) {
            Endermite endermite = arrow.getWorld().spawn(arrow.getLocation(), Endermite.class);
            endermite.setRemoveWhenFarAway(true);
        }
    }
}
