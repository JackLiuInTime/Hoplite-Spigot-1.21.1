package com.hoplite.utility.UtilityItem;

import com.hoplite.HoplitePlugin;
import com.hoplite.utility.UtilityItemHandler;
import com.hoplite.utility.UtilityRuntime;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ExplosivePickaxeHandler implements UtilityItemHandler {

    private static final String ID = "explosive_pickaxe";
    private final long cooldownMs;

    private final Map<UUID, Long> cooldown = new ConcurrentHashMap<>();

    public ExplosivePickaxeHandler(HoplitePlugin plugin) {
        this.cooldownMs = plugin.getUtilityCooldownMs("explosive_pickaxe.blast", 3_500L);
    }

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
        Player player = event.getPlayer();
        ItemStack tool = player.getInventory().getItemInMainHand();
        UUID id = player.getUniqueId();
        long now = System.currentTimeMillis();
        long remaining = UtilityRuntime.remainingSeconds(cooldown, id, cooldownMs, now);
        if (remaining > 0) {
            UtilityRuntime.refreshCooldownDisplay(player, tool, remaining);
            return;
        }
        UtilityRuntime.startCooldown(cooldown, id, now, cooldownMs, player, tool);

        Block origin = event.getBlock();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    Block target = origin.getRelative(dx, dy, dz);
                    if (target.isEmpty() || UtilityRuntime.isProtectedBlock(target)) {
                        continue;
                    }
                    target.breakNaturally(tool);
                }
            }
        }

        origin.getWorld().playSound(origin.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7f, 1.2f);
    }
}
