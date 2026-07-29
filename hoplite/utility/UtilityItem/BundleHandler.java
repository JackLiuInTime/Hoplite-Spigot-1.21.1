package com.hoplite.utility.UtilityItem;

import com.hoplite.HoplitePlugin;
import com.hoplite.utility.UtilityItemHandler;
import com.hoplite.utility.UtilityRuntime;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class BundleHandler implements UtilityItemHandler {

    private static final String ID = "bundle";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("utility:" + ID)
                .orElseGet(() -> new ItemStack(Material.PAPER));
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!UtilityRuntime.isRightClick(event.getAction())) {
            return;
        }

        Player player = event.getPlayer();
        int moved = 0;
        for (Item nearbyItem : player.getWorld().getEntitiesByClass(Item.class)) {
            if (nearbyItem.getLocation().distanceSquared(player.getLocation()) > 6 * 6) {
                continue;
            }

            ItemStack stack = nearbyItem.getItemStack();
            var leftovers = player.getInventory().addItem(stack);
            if (leftovers.isEmpty()) {
                moved += stack.getAmount();
                nearbyItem.remove();
            } else {
                int left = leftovers.values().stream().mapToInt(ItemStack::getAmount).sum();
                int picked = stack.getAmount() - left;
                if (picked > 0) {
                    moved += picked;
                    stack.setAmount(left);
                    nearbyItem.setItemStack(stack);
                }
            }
        }

        if (moved > 0) {
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.8f, 1.2f);
            player.sendMessage("§6Bundle stored §f" + moved + "§6 items.");
        } else {
            player.sendMessage("§7No nearby items to store.");
        }
        event.setCancelled(true);
    }
}
