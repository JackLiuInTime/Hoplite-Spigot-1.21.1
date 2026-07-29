package com.hoplite.utility.UtilityItem;

import com.hoplite.HoplitePlugin;
import com.hoplite.utility.UtilityItemHandler;
import com.hoplite.utility.UtilityRuntime;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Levelled;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class ObsidianMakerHandler implements UtilityItemHandler {

    private static final String ID = "obsidian_maker";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("utility:" + ID)
                .orElseGet(() -> new ItemStack(Material.OBSIDIAN));
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!UtilityRuntime.isRightClick(event.getAction()) || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (!isFullLavaSource(block)) {
            player.sendMessage("§7Obsidian Maker can only convert a full lava source block.");
            event.setCancelled(true);
            return;
        }

        block.setType(Material.OBSIDIAN);
        UtilityRuntime.consumeOne(player.getInventory().getItemInMainHand(), player);
        player.playSound(player.getLocation(), Sound.BLOCK_LAVA_EXTINGUISH, 1.0f, 1.0f);
        event.setCancelled(true);
    }

    private boolean isFullLavaSource(Block block) {
        if (block.getType() != Material.LAVA || !(block.getBlockData() instanceof Levelled levelled)) {
            return false;
        }
        return levelled.getLevel() == 0;
    }
}
