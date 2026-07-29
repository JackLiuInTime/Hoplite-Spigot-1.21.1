package com.hoplite.legendary.legendary;

import org.bukkit.attribute.Attribute;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;

public final class WeaponRuntime {
    private static volatile double cooldownMultiplier = 1.0D;

    private WeaponRuntime() {
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

    public static boolean isOffCooldown(Map<UUID, Long> cooldowns, UUID id, long cooldownMs, long nowMs) {
        return remainingSeconds(cooldowns, id, cooldownMs, nowMs) == 0;
    }

    public static void startCooldown(Map<UUID, Long> cooldowns, UUID id, long nowMs) {
        cooldowns.put(id, nowMs);
    }

    public static void startCooldown(
            Map<UUID, Long> cooldowns,
            UUID id,
            long nowMs,
            long cooldownMs,
            Player player,
            ItemStack sourceItem
    ) {
        startCooldown(cooldowns, id, nowMs);
        applyCooldownDisplay(player, sourceItem, scaledCooldown(cooldownMs));
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

    public static double getMaxHealth(LivingEntity entity, double fallback) {
        var attr = entity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attr != null ? attr.getValue() : fallback;
    }
}