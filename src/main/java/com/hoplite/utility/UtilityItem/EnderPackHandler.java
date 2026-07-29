package com.hoplite.utility.UtilityItem;

import com.hoplite.HoplitePlugin;
import com.hoplite.utility.UtilityItemHandler;
import com.hoplite.utility.UtilityRuntime;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class EnderPackHandler implements UtilityItemHandler {

    private static final String ID = "ender_pack";
    private final HoplitePlugin plugin;

    public EnderPackHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("utility:" + ID)
                .orElseGet(() -> new ItemStack(Material.BONE));
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!UtilityRuntime.isRightClick(event.getAction())) {
            return;
        }

        Player player = event.getPlayer();
        for (int i = 0; i < 2; i++) {
            Wolf wolf = player.getWorld().spawn(player.getLocation(), Wolf.class);
            wolf.setOwner(player);
            wolf.setCustomName(ChatColor.DARK_PURPLE + "Ender Wolf");
            wolf.setRemoveWhenFarAway(true);
            wolf.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 20 * 25, 1));
            wolf.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 20 * 25, 1));
        }

        Location endCity = tryLocateEndCity(player);
        if (endCity != null) {
            int x = endCity.getBlockX();
            int z = endCity.getBlockZ();
            player.sendMessage("§dEnder Pack sniffs an End City near: [" + x + ", " + z + "]");
        } else if (player.getWorld().getEnvironment() == World.Environment.THE_END) {
            player.sendMessage("§dEnder Pack is searching... no nearby End City found.");
        } else {
            player.sendMessage("§7Ender Pack works best in The End.");
        }

        UtilityRuntime.consumeOne(player.getInventory().getItemInMainHand(), player);
        player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 0.8f, 1.5f);
        event.setCancelled(true);
    }

    private Location tryLocateEndCity(Player player) {
        try {
            World world = player.getWorld();
            if (world.getEnvironment() != World.Environment.THE_END) {
                return null;
            }

            Class<?> structureTypeClass = Class.forName("org.bukkit.StructureType");
            Object endCityType = structureTypeClass.getField("END_CITY").get(null);
            Object result = World.class
                    .getMethod("locateNearestStructure", Location.class, structureTypeClass, int.class, boolean.class)
                    .invoke(world, player.getLocation(), endCityType, 180, false);
            if (result instanceof Location location) {
                return location;
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }
}
