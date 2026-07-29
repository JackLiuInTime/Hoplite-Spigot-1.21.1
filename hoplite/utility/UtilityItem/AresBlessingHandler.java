package com.hoplite.utility.UtilityItem;

import com.hoplite.HoplitePlugin;
import com.hoplite.utility.UtilityItemHandler;
import com.hoplite.utility.UtilityRuntime;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Random;

public class AresBlessingHandler implements UtilityItemHandler {

    private static final String ID = "ares_blessing";
    private final Random random = new Random();

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
        List<ItemStack> rewards = List.of(
                new ItemStack(Material.GOLDEN_APPLE, 2),
                new ItemStack(Material.ENDER_PEARL, 4),
                new ItemStack(Material.ARROW, 24),
                new ItemStack(Material.DIAMOND, 2),
                new ItemStack(Material.EXPERIENCE_BOTTLE, 12),
                new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1)
        );

        int rolls = 2 + random.nextInt(2);
        for (int i = 0; i < rolls; i++) {
            ItemStack reward = rewards.get(random.nextInt(rewards.size())).clone();
            player.getWorld().dropItemNaturally(player.getLocation().add(0, 0.5, 0), reward);
        }

        UtilityRuntime.consumeOne(player.getInventory().getItemInMainHand(), player);
        player.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.8f, 1.3f);
        player.sendMessage("§6Ares Blessing spills war spoils around you.");
        event.setCancelled(true);
    }
}
