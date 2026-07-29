package com.hoplite.utility.UtilityItem;

import com.hoplite.HoplitePlugin;
import com.hoplite.utility.UtilityItemHandler;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public class LumberjacksAxeHandler implements UtilityItemHandler {

    private static final String ID = "lumberjacks_axe";
    private static final int MAX_BREAK = 64;

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("utility:" + ID)
                .orElseGet(() -> new ItemStack(Material.IRON_AXE));
    }

    @Override
    public void onBlockBreak(BlockBreakEvent event) {
        Block origin = event.getBlock();
        if (!isLog(origin.getType())) {
            return;
        }

        ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();
        queue.add(origin);

        int broken = 0;
        while (!queue.isEmpty() && broken < MAX_BREAK) {
            Block current = queue.poll();
            if (!visited.add(current) || !isLog(current.getType())) {
                continue;
            }

            if (!current.equals(origin)) {
                current.breakNaturally(tool);
                broken++;
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        Block near = current.getRelative(dx, dy, dz);
                        if (!visited.contains(near) && isLog(near.getType())) {
                            queue.add(near);
                        }
                    }
                }
            }
        }

        if (broken > 0) {
            event.getPlayer().playSound(event.getPlayer().getLocation(), Sound.BLOCK_WOOD_BREAK, 0.9f, 0.9f);
        }
    }

    private boolean isLog(Material material) {
        return material.name().endsWith("_LOG") || material.name().endsWith("_STEM") || material == Material.MANGROVE_ROOTS;
    }
}
