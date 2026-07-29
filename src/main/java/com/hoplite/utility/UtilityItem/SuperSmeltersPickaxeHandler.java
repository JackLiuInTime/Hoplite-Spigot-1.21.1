package com.hoplite.utility.UtilityItem;

import com.hoplite.HoplitePlugin;
import com.hoplite.utility.UtilityItemHandler;
import com.hoplite.utility.UtilityRuntime;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public class SuperSmeltersPickaxeHandler implements UtilityItemHandler {

    private static final String ID = "super_smelters_pickaxe";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("utility:" + ID)
                .orElseGet(() -> new ItemStack(Material.DIAMOND_PICKAXE));
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        ItemStack smelted = UtilityRuntime.smeltResult(event.getBlock().getType(), 2);
        if (smelted == null) {
            return;
        }

        event.setDropItems(false);
        event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(), smelted);
        event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_BLASTFURNACE_FIRE_CRACKLE, 1.0f, 0.95f);
    }
}
