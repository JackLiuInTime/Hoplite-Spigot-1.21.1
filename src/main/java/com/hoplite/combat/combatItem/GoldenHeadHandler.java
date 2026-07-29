package com.hoplite.combat.combatItem;

import com.hoplite.combat.CombatItemHandler;
import com.hoplite.HoplitePlugin;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;

public class GoldenHeadHandler implements CombatItemHandler {

    private static final String ID = "golden_head";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("combat:" + ID)
                .orElseGet(() -> new ItemStack(Material.PLAYER_HEAD));
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        applyEffects(player);
        consumeOne(player.getInventory().getItemInMainHand(), player);
        event.setCancelled(true);
    }

    @Override
    public void onPlayerItemConsume(org.bukkit.event.player.PlayerItemConsumeEvent event) {
        applyEffects(event.getPlayer());
    }

    private void applyEffects(Player player) {

        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 2));
        player.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120 * 20, 1));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 14 * 20, 1));

        Team team = player.getScoreboard().getEntryTeam(player.getName());
        if (team == null) {
            return;
        }

        for (Entity nearby : player.getNearbyEntities(8, 8, 8)) {
            if (!(nearby instanceof Player teammate) || teammate.equals(player)) {
                continue;
            }
            Team nearbyTeam = teammate.getScoreboard().getEntryTeam(teammate.getName());
            if (nearbyTeam != null && nearbyTeam.equals(team)) {
                teammate.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 2));
                break;
            }
        }
    }

    private void consumeOne(ItemStack item, Player player) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
    }
}
