package com.hoplite.utility.UtilityItem;

import com.hoplite.HoplitePlugin;
import com.hoplite.utility.UtilityItemHandler;
import com.hoplite.utility.UtilityRuntime;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;

public class LightAnvilHandler implements UtilityItemHandler {

    private static final String ID = "light_anvil";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("utility:" + ID)
                .orElseGet(() -> new ItemStack(Material.ANVIL));
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!UtilityRuntime.isRightClick(event.getAction())) {
            return;
        }

        event.getPlayer().openInventory(Bukkit.createInventory(event.getPlayer(), InventoryType.ANVIL, "Light Anvil"));
        event.setCancelled(true);
    }
}
