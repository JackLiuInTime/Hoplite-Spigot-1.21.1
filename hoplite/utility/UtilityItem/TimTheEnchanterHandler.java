package com.hoplite.utility.UtilityItem;

import com.hoplite.HoplitePlugin;
import com.hoplite.utility.UtilityItemHandler;
import com.hoplite.utility.UtilityRuntime;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class TimTheEnchanterHandler implements UtilityItemHandler {

    private static final String ID = "tim_the_enchanter";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("utility:" + ID)
                .orElseGet(() -> new ItemStack(Material.VILLAGER_SPAWN_EGG));
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!UtilityRuntime.isRightClick(event.getAction()) || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        var spawnLocation = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0, 0.5);
        Villager villager = player.getWorld().spawn(spawnLocation, Villager.class);
        villager.setCustomName("§6Tim the Enchanter");
        villager.setCustomNameVisible(true);
        villager.setProfession(Villager.Profession.LIBRARIAN);
        villager.setVillagerLevel(4);
        villager.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 20 * 20, 0));

        UtilityRuntime.consumeOne(player.getInventory().getItemInMainHand(), player);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_CELEBRATE, 1.0f, 1.1f);
        player.sendMessage("§6Tim the Enchanter has arrived.");
        event.setCancelled(true);
    }
}
