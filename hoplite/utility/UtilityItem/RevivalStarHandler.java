package com.hoplite.utility.UtilityItem;

import com.hoplite.HoplitePlugin;
import com.hoplite.utility.UtilityItemHandler;
import com.hoplite.utility.UtilityRuntime;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

import java.util.Comparator;

public class RevivalStarHandler implements UtilityItemHandler {

    private static final String ID = "revival_star";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("utility:" + ID)
                .orElseGet(() -> new ItemStack(org.bukkit.Material.PAPER));
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!UtilityRuntime.isRightClick(event.getAction())) {
            return;
        }

        Player caster = event.getPlayer();
        Team casterTeam = caster.getScoreboard().getEntryTeam(caster.getName());
        if (casterTeam == null) {
            caster.sendMessage("§cRevival Star requires team assignment before it can revive teammates.");
            event.setCancelled(true);
            return;
        }

        Player target = caster.getWorld().getPlayers().stream()
                .filter(p -> !p.equals(caster))
                .filter(p -> p.getGameMode() == GameMode.SPECTATOR)
                .filter(p -> p.getLocation().distanceSquared(caster.getLocation()) <= 32 * 32)
                .filter(p -> sameTeam(casterTeam, caster, p))
                .min(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(caster.getLocation())))
                .orElse(null);

        if (target == null) {
            caster.sendMessage("§7No nearby fallen teammate to revive.");
            event.setCancelled(true);
            return;
        }

        target.teleport(caster.getLocation().clone().add(0, 1, 0));
        target.setGameMode(GameMode.SURVIVAL);
        target.setHealth(Math.min(target.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue(), 12.0));
        target.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 20 * 8, 1));
        target.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 20 * 30, 1));

        UtilityRuntime.consumeOne(caster.getInventory().getItemInMainHand(), caster);
        caster.getWorld().playSound(caster.getLocation(), Sound.ITEM_TOTEM_USE, 1.0f, 1.0f);
        caster.sendMessage("§eRevival Star restored " + target.getName() + " to battle.");
        target.sendMessage("§aYou have been revived by " + caster.getName() + ".");
        event.setCancelled(true);
    }

    private boolean sameTeam(Team casterTeam, Player caster, Player target) {
        Team targetTeam = target.getScoreboard().getEntryTeam(target.getName());
        return targetTeam != null && targetTeam.equals(casterTeam);
    }
}
