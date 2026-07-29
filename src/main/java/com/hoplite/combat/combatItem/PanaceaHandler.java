package com.hoplite.combat.combatItem;

import com.hoplite.combat.CombatItemHandler;
import com.hoplite.HoplitePlugin;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PanaceaHandler implements CombatItemHandler {

    private static final String ID = "panacea";
    private static final int MAX_USES = 5;
    private final NamespacedKey useKey;

    public PanaceaHandler(HoplitePlugin plugin) {
        this.useKey = new NamespacedKey(plugin, "panacea_uses");
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        return plugin.getGlobalIngredientRegistry()
                .get("combat:" + ID)
                .orElseGet(() -> new ItemStack(Material.POTION));
    }

    @Override
    public void onPlayerItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        ItemStack held = resolveHeldPanacea(player);
        if (held == null) {
            return;
        }

        event.setCancelled(true);

        int uses = getUses(held);
        if (uses >= MAX_USES) {
            player.sendMessage("Panacea is empty. Refill it at a bee nest with honey.");
            return;
        }

        double maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH) != null
                ? player.getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH).getValue()
                : 20.0;
        player.setHealth(Math.min(maxHealth, player.getHealth() + 8.0));

        uses++;
        setUses(held, uses);
        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_DRINK, 1.0f, 1.1f);

        if (uses >= MAX_USES) {
            consumeOne(held, player.getInventory());
            player.sendMessage("Panacea has been fully consumed.");
        }
    }

    @Override
    public void onPlayerInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null || clicked.getType() != Material.BEE_NEST) {
            return;
        }

        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();

        if (!inv.contains(Material.HONEY_BOTTLE)) {
            player.sendMessage("You need a honey bottle to refill Panacea.");
            return;
        }

        inv.removeItem(new ItemStack(Material.HONEY_BOTTLE, 1));
        ItemStack held = resolveHeldPanacea(player);
        if (held != null) {
            setUses(held, 0);
        }

        player.playSound(player.getLocation(), Sound.BLOCK_BEEHIVE_WORK, 1.0f, 1.0f);
        player.sendMessage("Panacea has been refilled.");
        event.setCancelled(true);
    }

    private int getUses(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return 0;
        }
        PersistentDataContainer container = meta.getPersistentDataContainer();
        Integer value = container.get(useKey, PersistentDataType.INTEGER);
        return value == null ? 0 : Math.max(0, value);
    }

    private void setUses(ItemStack item, int uses) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(useKey, PersistentDataType.INTEGER, Math.max(0, uses));
        item.setItemMeta(meta);
    }

    private ItemStack resolveHeldPanacea(Player player) {
        ItemStack main = player.getInventory().getItemInMainHand();
        if (isPanacea(main)) {
            return main;
        }

        ItemStack off = player.getInventory().getItemInOffHand();
        if (isPanacea(off)) {
            return off;
        }
        return null;
    }

    private boolean isPanacea(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        String id = meta.getPersistentDataContainer().get(new NamespacedKey(HoplitePlugin.getInstance(), "custom_item_id"), PersistentDataType.STRING);
        return ID.equalsIgnoreCase(id);
    }

    private void consumeOne(ItemStack item, PlayerInventory inventory) {
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            return;
        }

        ItemStack main = inventory.getItemInMainHand();
        if (main == item) {
            inventory.setItemInMainHand(new ItemStack(Material.AIR));
            return;
        }

        ItemStack off = inventory.getItemInOffHand();
        if (off == item) {
            inventory.setItemInOffHand(new ItemStack(Material.AIR));
        }
    }
}
