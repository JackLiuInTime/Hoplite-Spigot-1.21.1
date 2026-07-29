package com.hoplite.legendary.legendary;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GruntildaHandler implements LegendaryWeaponHandler {
    private final HoplitePlugin plugin;
    private final Map<UUID, Long> brewCooldown = new ConcurrentHashMap<>();
    private final long brewCooldownMs;

    public GruntildaHandler(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.brewCooldownMs = plugin.getLegendaryCooldownMs("gruntilda.alchemy_brew", 12_000L);
    }

    @Override
    public String getId() { return "gruntilda"; }

    @Override
    public ItemStack createItem(HoplitePlugin plugin) {
        ItemStack item = new ItemStack(Material.POTION);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&5Gruntilda"));
            meta.setLore(List.of(
                    ChatColor.GRAY + "A mysterious witch skilled in the art of alchemy.",
                    "",
                    ChatColor.WHITE + "RIGHT CLICK: brew a witch mix from water bottle",
                    ChatColor.WHITE + "RIGHT CLICK: output random enhanced potion",
                    ChatColor.DARK_GRAY + "COOLDOWN: 12s"
            ));
            meta.getPersistentDataContainer().set(new NamespacedKey(plugin, "legendary_weapon"), PersistentDataType.STRING, getId());
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.LUCK_OF_THE_SEA, 1, true);
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
        long now = System.currentTimeMillis();
        long remaining = WeaponRuntime.remainingSeconds(brewCooldown, id, brewCooldownMs, now);
        if (remaining > 0) {
            WeaponRuntime.refreshCooldownDisplay(player, player.getInventory().getItemInMainHand(), remaining);
            player.sendMessage("§cAlchemy brew is on cooldown: " + remaining + "s");
            return;
        }

        PlayerInventory inv = player.getInventory();
        int slot = findWaterBottleSlot(inv);
        if (slot < 0) {
            player.sendMessage("§eYou need a water bottle to brew.");
            return;
        }

        ItemStack water = inv.getItem(slot);
        if (water == null) {
            return;
        }
        water.setAmount(water.getAmount() - 1);
        if (water.getAmount() <= 0) {
            inv.setItem(slot, null);
        } else {
            inv.setItem(slot, water);
        }

        WeaponRuntime.startCooldown(brewCooldown, id, now, brewCooldownMs, player, player.getInventory().getItemInMainHand());
        ItemStack output = rollPotion();
        inv.addItem(output);
        player.getWorld().spawnParticle(org.bukkit.Particle.CRIT, player.getLocation().add(0, 1, 0), 26, 0.35, 0.4, 0.35, 0.02);
        player.sendMessage("§5Gruntilda brewed a potion for you.");
        event.setCancelled(true);
    }

    private int findWaterBottleSlot(PlayerInventory inv) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack == null || stack.getType() != Material.POTION) {
                continue;
            }
            if (!(stack.getItemMeta() instanceof PotionMeta pm)) {
                continue;
            }
            if (pm.getBasePotionType() == PotionType.WATER) {
                return i;
            }
        }
        return -1;
    }

    private ItemStack rollPotion() {
        ItemStack potion = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) potion.getItemMeta();
        if (meta == null) {
            return potion;
        }

        int roll = (int) (Math.random() * 3);
        if (roll == 0) {
            meta.setDisplayName("§dWitch Brew of Swiftness");
            meta.addCustomEffect(new PotionEffect(PotionEffectType.SPEED, 90 * 20, 1), true);
        } else if (roll == 1) {
            meta.setDisplayName("§dWitch Brew of Fortitude");
            meta.addCustomEffect(new PotionEffect(PotionEffectType.RESISTANCE, 60 * 20, 0), true);
        } else {
            meta.setDisplayName("§dWitch Brew of Fangs");
            meta.addCustomEffect(new PotionEffect(PotionEffectType.STRENGTH, 60 * 20, 0), true);
        }
        potion.setItemMeta(meta);
        return potion;
    }
}

