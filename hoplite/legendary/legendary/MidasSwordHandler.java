package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MidasSwordHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;

    public MidasSwordHandler(HoplitePlugin plugin) { this.plugin = plugin; }

    @Override
    public String getId() { return "midas_sword"; }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&6Midas Sword"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary weapon that gets stronger with each kill and turns its victims to gold.",
                    "",
                    ChatColor.WHITE + "KILL PLAYER: 50% chance to gain +1 Sharpness",
                    ChatColor.WHITE + "KILL: victims drop gold nuggets",
                    ChatColor.DARK_GRAY + "COOLDOWN: None"
            ));
                meta.setCustomModelData(1);
            meta.getPersistentDataContainer().set(new NamespacedKey(this.plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            meta.addEnchant(Enchantment.SHARPNESS, 1, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)) {
            return;
        }

        if (target.getHealth() - event.getFinalDamage() > 0.0) {
            return;
        }

        target.getWorld().dropItemNaturally(target.getLocation(), new ItemStack(Material.GOLD_NUGGET, 3));

        if (target instanceof Player) {
            if (ThreadLocalRandom.current().nextDouble() < 0.5) {
                ItemStack weapon = attacker.getInventory().getItemInMainHand();
                ItemMeta meta = weapon.getItemMeta();
                if (meta != null) {
                    int nextLevel = meta.getEnchantLevel(Enchantment.SHARPNESS) + 1;
                    meta.addEnchant(Enchantment.SHARPNESS, nextLevel, true);
                    weapon.setItemMeta(meta);
                    attacker.sendMessage("§6Midas Sword absorbed gold power: Sharpness " + nextLevel + ".");
                }
            }
        }
    }
}

