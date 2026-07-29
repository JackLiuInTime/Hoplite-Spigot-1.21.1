package com.hoplite.legendary;

import com.hoplite.HoplitePlugin;

import org.bukkit.inventory.ItemStack;

import com.hoplite.legendary.legendary.LegendaryWeaponHandler;

import java.util.*;
import java.util.stream.Collectors;

public class LegendaryWeaponManager {

    private final HoplitePlugin plugin;
    private final Map<String, LegendaryWeaponHandler> handlers = new LinkedHashMap<>();

    public LegendaryWeaponManager(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    public void registerHandler(LegendaryWeaponHandler handler) {
        handlers.put(handler.getId().toLowerCase(Locale.ROOT), handler);
    }

    public Optional<LegendaryWeaponHandler> getHandlerById(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(handlers.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<LegendaryWeaponHandler> fromItem(ItemStack item) {
        if (item == null || item.getType() == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }

        var meta = item.getItemMeta();
        if (meta == null) return Optional.empty();
        var container = meta.getPersistentDataContainer();
        var key = plugin.getLegendaryKey();
        if (!container.has(key, org.bukkit.persistence.PersistentDataType.STRING)) return Optional.empty();
        String id = container.get(key, org.bukkit.persistence.PersistentDataType.STRING);
        return getHandlerById(id);
    }

    public String availableTypes() {
        return handlers.keySet().stream().collect(Collectors.joining(", "));
    }

    public Collection<LegendaryWeaponHandler> getAllHandlers() {
        return Collections.unmodifiableCollection(handlers.values());
    }

}

