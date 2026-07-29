package com.hoplite.legendary.legendary;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hoplite.HoplitePlugin;

public class EagleEyeBowHandler implements LegendaryWeaponHandler {
    private static final double FULL_DRAW_FORCE = 0.99D;
    private static final double HEADSHOT_DAMAGE_BONUS = 4.0D;
    private final HoplitePlugin plugin;
    private final NamespacedKey fullDrawArrowKey;

    public EagleEyeBowHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.fullDrawArrowKey = new NamespacedKey(plugin, "eagle_eye_full_draw");
    }

    @Override
    public String getId() { return "eagle_eye_bow"; }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.BOW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&eEagle Eye Bow"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A legendary bow infused with the spirit of an eagle.",
                    "",
                    ChatColor.GOLD + "Air Hover",
                    ChatColor.WHITE + "Draw bow in mid-air to hover briefly",
                    ChatColor.GOLD + "Headshot Bonus",
                    ChatColor.WHITE + "Full-charge headshots deal bonus damage",
                    ChatColor.DARK_GRAY + "Cooldown: None"
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(this.plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            meta.addEnchant(Enchantment.POWER, 3, true);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public void onEntityShootBow(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player) || !(event.getProjectile() instanceof Arrow arrow)) {
            return;
        }

        if (!player.isOnGround()) {
            player.setVelocity(player.getVelocity().setY(0.0D));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0, true, false, false));
        }

        if (event.getForce() >= FULL_DRAW_FORCE) {
            arrow.getPersistentDataContainer().set(fullDrawArrowKey, PersistentDataType.BYTE, (byte) 1);
        }
    }

    @Override
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)
                || !arrow.getPersistentDataContainer().has(fullDrawArrowKey, PersistentDataType.BYTE)) {
            return;
        }

        double targetBase = event.getEntity().getBoundingBox().getMinY();
        double targetHeight = event.getEntity().getBoundingBox().getHeight();
        double headThreshold = targetBase + targetHeight * 0.75D;
        if (arrow.getLocation().getY() >= headThreshold) {
            event.setDamage(event.getDamage() + HEADSHOT_DAMAGE_BONUS);
        }
    }
}

