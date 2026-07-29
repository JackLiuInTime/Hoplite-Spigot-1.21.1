package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Random;

public class GeraldTheSnifferHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Random random = new Random();

    public GeraldTheSnifferHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getId() {
        return "gerald_the_sniffer";
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.SNIFFER_SPAWN_EGG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Gerald the Sniffer"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary tracker, renowned for its",
                    ChatColor.GRAY + "exceptional prowess in locating",
                    ChatColor.GRAY + "legendary artifacts.",
                    "",
                    ChatColor.WHITE + "CRAFT to summon Gerald, which will roam",
                    ChatColor.WHITE + "for 8 seconds before digging and",
                    ChatColor.WHITE + "summoning a randomly selected legendary",
                    ChatColor.WHITE + "weapon."
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
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
        ItemStack hand = player.getInventory().getItemInMainHand();

        player.sendMessage("§6Gerald starts digging for treasure...");
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            ItemStack reward = rollReward();
            player.getWorld().dropItemNaturally(player.getLocation().add(0, 0.6, 0), reward);
            player.sendMessage("§eGerald found a legendary weapon.");
        }, 8L * 20L);

        consumeOne(hand, player);
        event.setCancelled(true);
    }

    private ItemStack rollReward() {
        List<ItemStack> pool = plugin.getWeaponManager().getAllHandlers().stream()
                .map(handler -> handler.createItem(plugin))
                .filter(item -> item != null && item.getType() != Material.AIR)
                .toList();
        if (pool.isEmpty()) {
            return new ItemStack(Material.SNIFFER_EGG);
        }
        return pool.get(random.nextInt(pool.size())).clone();
    }

    private void consumeOne(ItemStack item, Player player) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }
        player.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
    }
}

