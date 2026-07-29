package com.hoplite.utility.UtilityItem;

import com.hoplite.HoplitePlugin;
import com.hoplite.utility.UtilityItemHandler;
import com.hoplite.utility.UtilityRuntime;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NetherReactorCoreHandler implements UtilityItemHandler {

    private static final String ID = "nether_reactor_core";
    private final long cooldownMs;

    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public NetherReactorCoreHandler(HoplitePlugin plugin) {
        this.cooldownMs = plugin.getUtilityCooldownMs("nether_reactor_core.activation", 60_000L);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("utility:" + ID)
                .orElseGet(() -> new ItemStack(Material.BARRIER));
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!UtilityRuntime.isRightClick(event.getAction()) || event.getClickedBlock() == null) {
            return;
        }

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        ItemStack held = player.getInventory().getItemInMainHand();
        long now = System.currentTimeMillis();
        long remain = UtilityRuntime.remainingSeconds(cooldowns, id, cooldownMs, now);
        if (remain > 0) {
            UtilityRuntime.refreshCooldownDisplay(player, held, remain);
            player.sendMessage("§cNether Reactor Core cooldown: " + remain + "s");
            event.setCancelled(true);
            return;
        }

        Block center = event.getClickedBlock();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                Block target = center.getRelative(dx, 0, dz);
                Material type = target.getType();
                if (type == Material.STONE || type == Material.COBBLESTONE || type == Material.DIRT || type == Material.GRASS_BLOCK || type == Material.SAND) {
                    target.setType(random.nextBoolean() ? Material.NETHERRACK : Material.BLACKSTONE);
                }
                Block above = target.getRelative(0, 1, 0);
                if (above.getType().isAir() && random.nextDouble() < 0.08) {
                    above.setType(Material.FIRE);
                }
            }
        }

        UtilityRuntime.startCooldown(cooldowns, id, now, cooldownMs, player, held);
        UtilityRuntime.consumeOne(held, player);
        player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.0f, 0.8f);
        event.setCancelled(true);
    }
}
