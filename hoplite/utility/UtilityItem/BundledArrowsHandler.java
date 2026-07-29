package com.hoplite.utility.UtilityItem;

import com.hoplite.HoplitePlugin;
import com.hoplite.utility.UtilityItemHandler;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class BundledArrowsHandler implements UtilityItemHandler {

    private static final String ID = "bundled_arrows";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("utility:" + ID)
                .orElseGet(() -> {
                    ItemStack fallback = new ItemStack(Material.ARROW);
                    fallback.setAmount(20);
                    return fallback;
                });
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        ItemStack arrows = new ItemStack(Material.ARROW, 20);
        var leftovers = player.getInventory().addItem(arrows);
        leftovers.values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));

        consumeOne(hand, player);
        player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.9f, 1.2f);
        event.setCancelled(true);
    }

    private void consumeOne(ItemStack item, Player player) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
    }
}
