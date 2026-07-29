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

public class AgonyHandler implements CombatItemHandler {

    private static final String ID = "agony";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("combat:" + ID)
                .orElseGet(() -> new ItemStack(Material.SPLASH_POTION));
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player attacker = event.getPlayer();
        Team team = attacker.getScoreboard().getEntryTeam(attacker.getName());

        for (Entity nearby : attacker.getNearbyEntities(4, 2, 4)) {
            if (!(nearby instanceof Player target) || target.equals(attacker)) {
                continue;
            }

            if (team != null) {
                Team targetTeam = target.getScoreboard().getEntryTeam(target.getName());
                if (targetTeam != null && targetTeam.equals(team)) {
                    continue;
                }
            }

            target.addPotionEffect(new PotionEffect(PotionEffectType.INSTANT_DAMAGE, 1, 1));
            target.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 60 * 20, 0));
        }

        ItemStack item = attacker.getInventory().getItemInMainHand();
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            attacker.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
        }

        event.setCancelled(true);
    }
}
