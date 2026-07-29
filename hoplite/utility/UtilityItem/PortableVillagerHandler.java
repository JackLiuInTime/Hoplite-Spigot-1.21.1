package com.hoplite.utility.UtilityItem;

import com.hoplite.HoplitePlugin;
import com.hoplite.utility.UtilityItemHandler;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PortableVillagerHandler implements UtilityItemHandler {

    private static final String ID = "portable_villager";
    private static final List<Villager.Profession> PROFESSIONS = Arrays.stream(Villager.Profession.values())
            .filter(p -> p != Villager.Profession.NONE)
            .toList();

    private final NamespacedKey selectedProfessionKey;

    public PortableVillagerHandler(HoplitePlugin plugin) {
        this.selectedProfessionKey = new NamespacedKey(plugin, "portable_villager_profession");
    }

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
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (player.isSneaking()) {
            Villager.Profession next = cycleProfession(hand);
            player.sendMessage(ChatColor.LIGHT_PURPLE + "Portable Villager profession: " + formatProfession(next));
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            event.setCancelled(true);
            return;
        }

        if (event.getClickedBlock() == null) {
            player.sendMessage(ChatColor.GRAY + "Right-click a block to spawn the selected villager.");
            event.setCancelled(true);
            return;
        }

        Location spawnAt = event.getClickedBlock().getRelative(event.getBlockFace()).getLocation().add(0.5, 0, 0.5);
        Villager villager = player.getWorld().spawn(spawnAt, Villager.class);
        Villager.Profession selected = getSelectedProfession(hand);
        villager.setProfession(selected);
        villager.setVillagerLevel(1);

        consumeOne(hand, player);
        player.sendMessage(ChatColor.GREEN + "Spawned " + formatProfession(selected) + " villager.");
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_TRADE, 1.0f, 1.0f);
        event.setCancelled(true);
    }

    private Villager.Profession cycleProfession(ItemStack item) {
        Villager.Profession current = getSelectedProfession(item);
        int idx = PROFESSIONS.indexOf(current);
        int nextIdx = (idx + 1) % PROFESSIONS.size();
        Villager.Profession next = PROFESSIONS.get(nextIdx);
        setSelectedProfession(item, next);
        return next;
    }

    private Villager.Profession getSelectedProfession(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return PROFESSIONS.get(0);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return PROFESSIONS.get(0);
        }

        String raw = meta.getPersistentDataContainer().get(selectedProfessionKey, PersistentDataType.STRING);
        if (raw == null || raw.isBlank()) {
            Villager.Profession first = PROFESSIONS.get(0);
            setSelectedProfession(item, first);
            return first;
        }

        try {
            return Villager.Profession.valueOf(raw);
        } catch (IllegalArgumentException ex) {
            Villager.Profession first = PROFESSIONS.get(0);
            setSelectedProfession(item, first);
            return first;
        }
    }

    private void setSelectedProfession(ItemStack item, Villager.Profession profession) {
        if (item == null || !item.hasItemMeta()) {
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.getPersistentDataContainer().set(selectedProfessionKey, PersistentDataType.STRING, profession.name());

        List<String> lore = meta.getLore();
        String selectorLine = ChatColor.DARK_AQUA + "Selected Profession: " + formatProfession(profession);
        if (lore == null || lore.isEmpty()) {
            meta.setLore(List.of(selectorLine));
        } else {
            boolean replaced = false;
            for (int i = 0; i < lore.size(); i++) {
                String stripped = ChatColor.stripColor(lore.get(i));
                if (stripped != null && stripped.startsWith("Selected Profession:")) {
                    lore.set(i, selectorLine);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) {
                lore.add(selectorLine);
            }
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
    }

    private String formatProfession(Villager.Profession profession) {
        String[] parts = profession.name().toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }

    private void consumeOne(ItemStack item, Player player) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
    }
}
