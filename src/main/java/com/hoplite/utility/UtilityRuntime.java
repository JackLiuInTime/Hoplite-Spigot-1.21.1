package com.hoplite.utility;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class UtilityRuntime {

    private static final Map<Material, ItemStack> SMELT_RESULTS = new EnumMap<>(Material.class);
    private static volatile double cooldownMultiplier = 1.0D;

    static {
        SMELT_RESULTS.put(Material.IRON_ORE, new ItemStack(Material.IRON_INGOT, 1));
        SMELT_RESULTS.put(Material.DEEPSLATE_IRON_ORE, new ItemStack(Material.IRON_INGOT, 1));
        SMELT_RESULTS.put(Material.GOLD_ORE, new ItemStack(Material.GOLD_INGOT, 1));
        SMELT_RESULTS.put(Material.DEEPSLATE_GOLD_ORE, new ItemStack(Material.GOLD_INGOT, 1));
        SMELT_RESULTS.put(Material.COPPER_ORE, new ItemStack(Material.COPPER_INGOT, 1));
        SMELT_RESULTS.put(Material.DEEPSLATE_COPPER_ORE, new ItemStack(Material.COPPER_INGOT, 1));
        SMELT_RESULTS.put(Material.ANCIENT_DEBRIS, new ItemStack(Material.NETHERITE_SCRAP, 1));
        SMELT_RESULTS.put(Material.SAND, new ItemStack(Material.GLASS, 1));
        SMELT_RESULTS.put(Material.RED_SAND, new ItemStack(Material.GLASS, 1));
        SMELT_RESULTS.put(Material.COBBLESTONE, new ItemStack(Material.STONE, 1));
        SMELT_RESULTS.put(Material.STONE, new ItemStack(Material.SMOOTH_STONE, 1));
        SMELT_RESULTS.put(Material.CLAY_BALL, new ItemStack(Material.BRICK, 1));
    }

    private UtilityRuntime() {
    }

    public static void setCooldownMultiplier(double multiplier) {
        cooldownMultiplier = Math.max(0.0D, multiplier);
    }

    private static long scaledCooldown(long cooldownMs) {
        return Math.max(0L, Math.round(cooldownMs * cooldownMultiplier));
    }

    public static boolean isRightClick(Action action) {
        return action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK;
    }

    public static long remainingSeconds(Map<UUID, Long> cooldowns, UUID id, long cooldownMs, long nowMs) {
        Long last = cooldowns.get(id);
        if (last == null) {
            return 0;
        }

        long effectiveCooldownMs = scaledCooldown(cooldownMs);
        if (effectiveCooldownMs <= 0) {
            return 0;
        }

        long elapsed = nowMs - last;
        if (elapsed >= effectiveCooldownMs) {
            return 0;
        }
        return (effectiveCooldownMs - elapsed + 999) / 1000;
    }

    public static void startCooldown(Map<UUID, Long> cooldowns, UUID id, long nowMs, long cooldownMs, Player player, ItemStack sourceItem) {
        cooldowns.put(id, nowMs);
        applyCooldownDisplay(player, sourceItem, scaledCooldown(cooldownMs));
    }

    public static void refreshCooldownDisplay(Player player, ItemStack sourceItem, long remainingSeconds) {
        if (player == null || sourceItem == null) {
            return;
        }

        Material material = sourceItem.getType();
        if (material.isAir()) {
            return;
        }

        int ticks = (int) Math.max(1, remainingSeconds * 20L);
        player.setCooldown(material, ticks);
    }

    public static void applyCooldownDisplay(Player player, ItemStack sourceItem, long cooldownMs) {
        if (player == null || sourceItem == null) {
            return;
        }

        Material material = sourceItem.getType();
        if (material.isAir()) {
            return;
        }

        int ticks = (int) Math.max(1, (cooldownMs + 49L) / 50L);
        player.setCooldown(material, ticks);
    }

    public static void consumeOne(ItemStack item, Player player) {
        if (item == null || player == null) {
            return;
        }

        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
    }

    public static ItemStack smeltResult(Material blockType, int multiplier) {
        ItemStack base = SMELT_RESULTS.get(blockType);
        if (base == null) {
            return null;
        }
        ItemStack out = base.clone();
        out.setAmount(Math.max(1, out.getAmount() * Math.max(1, multiplier)));
        return out;
    }

    public static boolean isProtectedBlock(Block block) {
        if (block == null) {
            return true;
        }

        Material type = block.getType();
        return type == Material.BEDROCK
                || type == Material.BARRIER
                || type == Material.CHEST
                || type == Material.TRAPPED_CHEST
                || type == Material.BARREL
                || type == Material.SHULKER_BOX
                || type == Material.SPAWNER
                || type == Material.REINFORCED_DEEPSLATE;
    }
}
