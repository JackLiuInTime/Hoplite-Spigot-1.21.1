package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class VampireSabreHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> bloodSiphonCooldown = new ConcurrentHashMap<>();
    private final Map<UUID, Long> batFormCooldown = new ConcurrentHashMap<>();
    private final Set<UUID> armedSiphon = ConcurrentHashMap.newKeySet();
    private final Set<UUID> internalSiphonBonusHit = ConcurrentHashMap.newKeySet();
    private final Set<UUID> batFormActive = ConcurrentHashMap.newKeySet();
    private final Set<UUID> hadFlightBeforeBat = ConcurrentHashMap.newKeySet();

    private static final long BLOOD_SIPHON_COOLDOWN_MS = 45_000;
    private static final long BAT_FORM_COOLDOWN_MS = 60_000;
    private static final int BAT_FORM_TICKS = 8 * 20;

    public VampireSabreHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "vampire_sabre";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Vampire Sabre"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary sabre cursed with an insatiable thirst for blood.",
                    "",
                ChatColor.WHITE + "MELEE against enemies to siphon 12% of",
                ChatColor.WHITE + "the damage dealt as health.",
                    "",
                ChatColor.WHITE + "SNEAK + RIGHT CLICK to transform into a",
                ChatColor.WHITE + "bat for 8 seconds, granting flight at",
                ChatColor.WHITE + "the cost of being vulnerable in",
                ChatColor.WHITE + "sunlight.",
                ChatColor.DARK_GRAY + "60s Cooldown",
                    "",
                ChatColor.WHITE + "RIGHT CLICK to activate " + ChatColor.RED + "Blood Siphon" + ChatColor.WHITE + ".",
                ChatColor.WHITE + "Your next successful strike drains 3",
                ChatColor.WHITE + "hunger and 2 hearts from the victim and",
                ChatColor.WHITE + "gives it to you.",
                ChatColor.DARK_GRAY + "45s Cooldown"
            ));
            meta.setCustomModelData(22);
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
        UUID id = player.getUniqueId();

        if (player.isSneaking()) {
            tryActivateBatForm(player, id);
            event.setCancelled(true);
            return;
        }

        long now = System.currentTimeMillis();
        long remaining = WeaponRuntime.remainingSeconds(bloodSiphonCooldown, id, BLOOD_SIPHON_COOLDOWN_MS, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cBlood Siphon is on cooldown: " + remaining + "s");
            return;
        }

        WeaponRuntime.startCooldown(bloodSiphonCooldown, id, now, BLOOD_SIPHON_COOLDOWN_MS, player, player.getInventory().getItemInMainHand());
        armedSiphon.add(id);
        player.sendMessage("§aBlood Siphon armed. Your next hit will drain health and hunger.");
        event.setCancelled(true);
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        UUID id = attacker.getUniqueId();
        if (internalSiphonBonusHit.remove(id)) {
            return;
        }

        double healFromMelee = Math.max(0.0, event.getFinalDamage() * 0.12);
        double maxHealth = WeaponRuntime.getMaxHealth(attacker, 20.0);
        attacker.setHealth(Math.min(maxHealth, attacker.getHealth() + healFromMelee));

        if (!armedSiphon.remove(id)) {
            return;
        }

        internalSiphonBonusHit.add(id);
        target.damage(4.0, attacker);
        // Failsafe: ensure the guard flag cannot survive indefinitely.
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> internalSiphonBonusHit.remove(id), 1L);
        attacker.setHealth(Math.min(maxHealth, attacker.getHealth() + 4.0));

        if (target instanceof Player victimPlayer) {
            int drain = Math.min(3, victimPlayer.getFoodLevel());
            victimPlayer.setFoodLevel(victimPlayer.getFoodLevel() - drain);
            attacker.setFoodLevel(Math.min(20, attacker.getFoodLevel() + drain));
        }

        attacker.sendMessage("§cBlood Siphon consumed.");
    }

    private void tryActivateBatForm(Player player, UUID id) {
        if (batFormActive.contains(id)) {
            player.sendMessage("§eBat form is already active.");
            return;
        }

        long now = System.currentTimeMillis();
        long remaining = WeaponRuntime.remainingSeconds(batFormCooldown, id, BAT_FORM_COOLDOWN_MS, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cBat form is on cooldown: " + remaining + "s");
            return;
        }

        WeaponRuntime.startCooldown(batFormCooldown, id, now, BAT_FORM_COOLDOWN_MS, player, player.getInventory().getItemInMainHand());
        batFormActive.add(id);
        if (player.getAllowFlight()) {
            hadFlightBeforeBat.add(id);
        }

        player.setAllowFlight(true);
        player.setFlying(true);
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, BAT_FORM_TICKS, 0, true, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, BAT_FORM_TICKS, 1, true, false, false));
        player.sendMessage("§5You transformed into bat form for 8s.");

        new BukkitRunnable() {
            int ticks = BAT_FORM_TICKS;

            @Override
            public void run() {
                if (!player.isOnline() || !batFormActive.contains(id)) {
                    clearBatForm(player, id);
                    cancel();
                    return;
                }

                if (ticks % 20 == 0 && isInSunlight(player)) {
                    player.damage(1.0);
                    player.sendMessage("§6Sunlight burns your bat form.");
                }

                ticks--;
                if (ticks <= 0) {
                    clearBatForm(player, id);
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private void clearBatForm(Player player, UUID id) {
        batFormActive.remove(id);
        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.removePotionEffect(PotionEffectType.SPEED);

        boolean hadFlight = hadFlightBeforeBat.remove(id);
        if (!hadFlight && player.getGameMode() != GameMode.CREATIVE && player.getGameMode() != GameMode.SPECTATOR) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
    }

    private boolean isInSunlight(Player player) {
        if (player.getWorld().getEnvironment() != org.bukkit.World.Environment.NORMAL) {
            return false;
        }

        long time = player.getWorld().getTime();
        if (time > 12300 && time < 23850) {
            return false;
        }

        int highestY = player.getWorld().getHighestBlockYAt(player.getLocation());
        return player.getLocation().getY() >= highestY - 1;
    }
}
