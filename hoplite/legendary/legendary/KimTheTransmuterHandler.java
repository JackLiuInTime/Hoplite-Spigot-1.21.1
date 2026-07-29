package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class KimTheTransmuterHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, UUID> activeKim = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> upgradeCount = new ConcurrentHashMap<>();
    private static final int MAX_UPGRADES = 3;

    public KimTheTransmuterHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "kim_the_transmuter";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.IRON_INGOT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aKim the Transmuter"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A magical villager that upgrades items",
                    ChatColor.GRAY + "and gear.",
                    "",
                    ChatColor.WHITE + "CRAFT to summon Kim the Transmuter.",
                    "",
                    ChatColor.WHITE + "GIVE an item to Kim and she will",
                    ChatColor.WHITE + "upgrade it to a stronger material.",
                    "",
                    ChatColor.WHITE + "Kim the Transmuter will upgrade 3 items",
                    ChatColor.WHITE + "before leaving."
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this.plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!WeaponRuntime.isRightClick(event.getAction())) {
            return;
        }

        Player player = event.getPlayer();
        UUID id = player.getUniqueId();

        if (activeKim.containsKey(id)) {
            player.sendMessage("§eKim is already here. Interact with Kim to upgrade your held item.");
            return;
        }

        Villager kim = player.getWorld().spawn(player.getLocation(), Villager.class, v -> {
            v.setCustomName("§aKim the Transmuter");
            v.setCustomNameVisible(true);
            v.setInvulnerable(true);
            v.setAI(false);
            v.setCollidable(false);
            v.setProfession(Villager.Profession.LIBRARIAN);
        });

        activeKim.put(id, kim.getUniqueId());
        upgradeCount.put(id, 0);
        player.sendMessage("§aKim has arrived. Give Kim an item in your main hand to upgrade its material.");
        event.setCancelled(true);
    }

    @Override
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        UUID id = player.getUniqueId();
        UUID kimId = activeKim.get(id);
        if (kimId == null) {
            return;
        }

        Entity clicked = event.getRightClicked();
        if (!(clicked instanceof Villager) || !clicked.getUniqueId().equals(kimId)) {
            return;
        }

        event.setCancelled(true);
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held.getType() == Material.AIR) {
            player.sendMessage("§eHold an item in your main hand to upgrade it.");
            return;
        }

        Material upgraded = getUpgradedMaterial(held.getType());
        if (upgraded == null) {
            player.sendMessage("§eKim cannot transmute this item into a stronger material.");
            return;
        }

        held.setType(upgraded);

        int used = upgradeCount.getOrDefault(id, 0) + 1;
        upgradeCount.put(id, used);
        player.sendMessage("§aKim upgraded your item to " + upgraded.name() + " (" + used + "/" + MAX_UPGRADES + ").");

        if (used >= MAX_UPGRADES) {
            clicked.remove();
            activeKim.remove(id);
            upgradeCount.remove(id);
            player.sendMessage("§7Kim completed 3 upgrades and leaves.");
        }
    }

    private Material getUpgradedMaterial(Material material) {
        return switch (material) {
            case WOODEN_SWORD -> Material.STONE_SWORD;
            case STONE_SWORD -> Material.IRON_SWORD;
            case IRON_SWORD -> Material.DIAMOND_SWORD;
            case DIAMOND_SWORD -> Material.NETHERITE_SWORD;

            case WOODEN_PICKAXE -> Material.STONE_PICKAXE;
            case STONE_PICKAXE -> Material.IRON_PICKAXE;
            case IRON_PICKAXE -> Material.DIAMOND_PICKAXE;
            case DIAMOND_PICKAXE -> Material.NETHERITE_PICKAXE;

            case WOODEN_AXE -> Material.STONE_AXE;
            case STONE_AXE -> Material.IRON_AXE;
            case IRON_AXE -> Material.DIAMOND_AXE;
            case DIAMOND_AXE -> Material.NETHERITE_AXE;

            case WOODEN_SHOVEL -> Material.STONE_SHOVEL;
            case STONE_SHOVEL -> Material.IRON_SHOVEL;
            case IRON_SHOVEL -> Material.DIAMOND_SHOVEL;
            case DIAMOND_SHOVEL -> Material.NETHERITE_SHOVEL;

            case LEATHER_HELMET, CHAINMAIL_HELMET, GOLDEN_HELMET -> Material.IRON_HELMET;
            case IRON_HELMET -> Material.DIAMOND_HELMET;
            case DIAMOND_HELMET -> Material.NETHERITE_HELMET;

            case LEATHER_CHESTPLATE, CHAINMAIL_CHESTPLATE, GOLDEN_CHESTPLATE -> Material.IRON_CHESTPLATE;
            case IRON_CHESTPLATE -> Material.DIAMOND_CHESTPLATE;
            case DIAMOND_CHESTPLATE -> Material.NETHERITE_CHESTPLATE;

            case LEATHER_LEGGINGS, CHAINMAIL_LEGGINGS, GOLDEN_LEGGINGS -> Material.IRON_LEGGINGS;
            case IRON_LEGGINGS -> Material.DIAMOND_LEGGINGS;
            case DIAMOND_LEGGINGS -> Material.NETHERITE_LEGGINGS;

            case LEATHER_BOOTS, CHAINMAIL_BOOTS, GOLDEN_BOOTS -> Material.IRON_BOOTS;
            case IRON_BOOTS -> Material.DIAMOND_BOOTS;
            case DIAMOND_BOOTS -> Material.NETHERITE_BOOTS;

            case BOW -> Material.CROSSBOW;
            default -> null;
        };
    }
}
