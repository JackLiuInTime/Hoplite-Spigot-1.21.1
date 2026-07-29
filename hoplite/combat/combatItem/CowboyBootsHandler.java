package com.hoplite.combat.combatItem;

import com.hoplite.combat.CombatItemHandler;
import com.hoplite.HoplitePlugin;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CowboyBootsHandler implements CombatItemHandler {

    private static final String ID = "cowboy_boots";
    private static final double SPEED_BONUS = 0.06d;
    private final Map<UUID, Double> originalHorseSpeeds = new HashMap<>();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("combat:" + ID)
                .orElseGet(() -> new ItemStack(Material.LEATHER_BOOTS));
    }

    @Override
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (!(event.getVehicle() instanceof AbstractHorse horse)) {
            return;
        }

        AttributeInstance movement = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }

        UUID horseId = horse.getUniqueId();
        originalHorseSpeeds.putIfAbsent(horseId, movement.getBaseValue());
        movement.setBaseValue(originalHorseSpeeds.get(horseId) + SPEED_BONUS);
    }

    @Override
    public void onVehicleExit(VehicleExitEvent event) {
        if (!(event.getVehicle() instanceof AbstractHorse horse)) {
            return;
        }

        AttributeInstance movement = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (movement == null) {
            return;
        }

        UUID horseId = horse.getUniqueId();
        Double original = originalHorseSpeeds.remove(horseId);
        if (original != null) {
            movement.setBaseValue(original);
        }
    }
}
