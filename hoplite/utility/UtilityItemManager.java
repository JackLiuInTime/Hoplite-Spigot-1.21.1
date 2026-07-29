package com.hoplite.utility;

import com.hoplite.HoplitePlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class UtilityItemManager {
    private final Map<String, UtilityItemHandler> handlers = new LinkedHashMap<>();
    private final NamespacedKey customItemIdKey;

    public UtilityItemManager(HoplitePlugin plugin) {
        this.customItemIdKey = new NamespacedKey(plugin, "custom_item_id");
    }

    public void registerHandler(UtilityItemHandler handler) {
        handlers.put(handler.getId().toLowerCase(Locale.ROOT), handler);
    }

    public Optional<UtilityItemHandler> getHandlerById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(handlers.get(id.toLowerCase(Locale.ROOT)));
    }

    public Optional<UtilityItemHandler> fromItem(ItemStack item) {
        if (item == null || item.getType() == null || item.getType().isAir() || !item.hasItemMeta()) {
            return Optional.empty();
        }

        var meta = item.getItemMeta();
        if (meta == null) {
            return Optional.empty();
        }

        var container = meta.getPersistentDataContainer();
        if (!container.has(customItemIdKey, PersistentDataType.STRING)) {
            return Optional.empty();
        }

        String id = container.get(customItemIdKey, PersistentDataType.STRING);
        return getHandlerById(id);
    }

    public String availableTypes() {
        return handlers.keySet().stream().collect(Collectors.joining(", "));
    }

    public Collection<UtilityItemHandler> getAllHandlers() {
        return Collections.unmodifiableCollection(handlers.values());
    }

    public Set<UtilityItemHandler> fromArmor(Player player) {
        if (player == null || player.getInventory() == null) {
            return Collections.emptySet();
        }

        var result = new java.util.LinkedHashSet<UtilityItemHandler>();
        ItemStack[] armor = player.getInventory().getArmorContents();
        if (armor == null) {
            return result;
        }

        for (ItemStack piece : armor) {
            fromItem(piece).ifPresent(result::add);
        }
        return result;
    }
}
