package com.hoplite.utility.UtilityItem;

import com.hoplite.HoplitePlugin;
import com.hoplite.utility.UtilityItemHandler;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

import java.util.Comparator;

public class TrackerPackHandler implements UtilityItemHandler {

    private static final String ID = "tracker_pack";
    private static final int TRACK_RADIUS = 40;

    private final HoplitePlugin plugin;

    public TrackerPackHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("utility:" + ID)
                .orElseGet(() -> new ItemStack(Material.BONE));
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        Player target = findNearestEnemy(player);
        if (target == null) {
            player.sendMessage(ChatColor.GRAY + "No enemy player found nearby.");
            event.setCancelled(true);
            return;
        }

        for (int i = 0; i < 2; i++) {
            Wolf wolf = player.getWorld().spawn(player.getLocation(), Wolf.class);
            wolf.setAdult();
            wolf.setOwner(player);
            wolf.setCustomName(ChatColor.WHITE + "Tracker Wolf");
            wolf.setCustomNameVisible(false);
            wolf.setRemoveWhenFarAway(true);
            wolf.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 20, 1));
            wolf.setTarget(target);
        }

        consumeOne(player.getInventory().getItemInMainHand(), player);

        player.playSound(player.getLocation(), Sound.ENTITY_WOLF_HOWL, 1.0f, 1.0f);
        int x = target.getLocation().getBlockX();
        int y = target.getLocation().getBlockY();
        int z = target.getLocation().getBlockZ();
        player.sendMessage(ChatColor.GREEN + "Tracking target: " + target.getName() + " at [" + x + ", " + y + ", " + z + "]");
        event.setCancelled(true);
    }

    private Player findNearestEnemy(Player source) {
        Team sourceTeam = source.getScoreboard().getEntryTeam(source.getName());
        return source.getWorld().getPlayers().stream()
                .filter(p -> !p.equals(source))
                .filter(p -> p.getLocation().distanceSquared(source.getLocation()) <= TRACK_RADIUS * TRACK_RADIUS)
                .filter(p -> isEnemy(sourceTeam, p))
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(source.getLocation())))
                .orElse(null);
    }

    private boolean isEnemy(Team sourceTeam, Player target) {
        Team targetTeam = target.getScoreboard().getEntryTeam(target.getName());
        if (sourceTeam == null || targetTeam == null) {
            return true;
        }
        return !sourceTeam.equals(targetTeam);
    }

    private void consumeOne(ItemStack item, Player player) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
    }
}
