package com.hoplite.global.registry;

import com.hoplite.HoplitePlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Loads global item definitions from global/custom-items.yml
 * and registers them into GlobalIngredientRegistry.
 */
public class GlobalItemDefinitionLoader {

    private final HoplitePlugin plugin;

    public GlobalItemDefinitionLoader(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    public void loadFromDataFolder() {
        try {
            File data = plugin.getDataFolder();
            if (!data.exists()) {
                data.mkdirs();
            }

            File globalDir = new File(data, "global");
            if (!globalDir.exists()) {
                globalDir.mkdirs();
            }

            File itemsFile = new File(globalDir, "custom-items.yml");

            // Legacy compatibility: migrate old custom/items.yml into global/custom-items.yml.
            File legacyDir = new File(data, "custom");
            File legacyFile = new File(legacyDir, "items.yml");
            if (!itemsFile.exists() && legacyFile.exists()) {
                Files.move(legacyFile.toPath(), itemsFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }

            if (!itemsFile.exists()) {
                extractDefaultResource("global/custom-items.yml", itemsFile);
            }

            YamlConfiguration config = YamlConfiguration.loadConfiguration(itemsFile);
            ConfigurationSection items = config.getConfigurationSection("items");
            if (items == null) {
                plugin.getLogger().warning("No global item definitions found in global/custom-items.yml");
                return;
            }

            int loaded = 0;
            Set<String> loadedIds = new LinkedHashSet<>();
            for (String id : items.getKeys(false)) {
                ConfigurationSection section = items.getConfigurationSection(id);
                if (section == null) {
                    continue;
                }

                String normalizedId = id.toLowerCase(Locale.ROOT);
                String materialName = section.getString("material", "PAPER");
                Material material = Material.matchMaterial(materialName);
                if (material == null) {
                    plugin.getLogger().warning("Invalid global item material: " + materialName + " for id " + id);
                    continue;
                }

                ItemStack item = new ItemStack(material);
                ItemMeta meta = item.getItemMeta();
                if (meta != null) {
                    String displayName = section.getString("display-name");
                    if (displayName != null && !displayName.isBlank()) {
                        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
                    }

                    List<String> lore = section.getStringList("lore");
                    if (!lore.isEmpty()) {
                        meta.setLore(lore.stream()
                                .map(line -> ChatColor.translateAlternateColorCodes('&', line))
                                .toList());
                    }

                    ConfigurationSection enchants = section.getConfigurationSection("enchantments");
                    if (enchants != null) {
                        for (String enchantKey : enchants.getKeys(false)) {
                            Enchantment enchantment = Enchantment.getByName(enchantKey.toUpperCase(Locale.ROOT));
                            if (enchantment == null) {
                                plugin.getLogger().warning("Invalid enchantment key " + enchantKey + " for custom item " + id);
                                continue;
                            }
                            int level = Math.max(1, enchants.getInt(enchantKey, 1));
                            meta.addEnchant(enchantment, level, true);
                        }
                    }

                    meta.getPersistentDataContainer().set(
                            new NamespacedKey(plugin, "custom_item_id"),
                            PersistentDataType.STRING,
                            normalizedId
                    );
                    item.setItemMeta(meta);
                }

                String category = section.getString("category", "global").toLowerCase(Locale.ROOT);
                switch (category) {
                    case "combat" -> plugin.getGlobalIngredientRegistry().registerCombat(normalizedId, item);
                    case "utility" -> plugin.getGlobalIngredientRegistry().registerUtility(normalizedId, item);
                    case "global" -> {
                        plugin.getGlobalIngredientRegistry().registerGlobal(normalizedId, item);
                        // Compatibility alias for old recipe files using custom:<id>.
                        plugin.getGlobalIngredientRegistry().registerCustom(normalizedId, item);
                    }
                    default -> plugin.getGlobalIngredientRegistry().registerCustom(normalizedId, item);
                }

                loadedIds.add(normalizedId);
                loaded++;
            }

            ensureRequiredSpecialItems(loadedIds);
            plugin.getLogger().info("Loaded global item definitions: " + loaded);
        } catch (Exception ex) {
            plugin.getLogger().severe("Failed to load global item definitions: " + ex.getMessage());
        }
    }

    private void extractDefaultResource(String resourceName, File target) {
        try (var in = plugin.getResource(resourceName)) {
            if (in == null) {
                return;
            }
            Files.copy(in, target.toPath());
        } catch (IOException ignored) {
        }
    }

    private void ensureRequiredSpecialItems(Set<String> loadedIds) {
        if (!loadedIds.contains("spider_silk_sac")) {
            ItemStack spiderSilkSac = buildSpecialPaper(
                    "spider_silk_sac",
                    "&fSpider Silk Sac",
                    List.of(
                            "&7The silk sac of a cave spider, used for",
                            "&7crafting the &6Sceptre of Arachne&7.",
                            "",
                            "&7Obtained rarely from killing Cave Spiders."
                    )
            );
            plugin.getGlobalIngredientRegistry().registerGlobal("spider_silk_sac", spiderSilkSac);
            plugin.getGlobalIngredientRegistry().registerCustom("spider_silk_sac", spiderSilkSac);
            plugin.getLogger().warning("Missing spider_silk_sac in global/custom-items.yml; fallback item was auto-registered.");
        }

        if (!loadedIds.contains("vampire_tooth")) {
            ItemStack vampireTooth = buildSpecialPaper(
                    "vampire_tooth",
                    "&fVampire Tooth",
                    List.of(
                            "&7The tooth of a vampire, used for",
                            "&7crafting the &6Vampire Sabre&7.",
                            "",
                            "&7Obtained from a chest in the Manor."
                    )
            );
            plugin.getGlobalIngredientRegistry().registerGlobal("vampire_tooth", vampireTooth);
            plugin.getGlobalIngredientRegistry().registerCustom("vampire_tooth", vampireTooth);
            plugin.getLogger().warning("Missing vampire_tooth in global/custom-items.yml; fallback item was auto-registered.");
        }
    }

    private ItemStack buildSpecialPaper(String id, String displayName, List<String> loreLines) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
            meta.setLore(loreLines.stream().map(line -> ChatColor.translateAlternateColorCodes('&', line)).toList());
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, "custom_item_id"),
                    PersistentDataType.STRING,
                    id.toLowerCase(Locale.ROOT)
            );
            item.setItemMeta(meta);
        }
        return item;
    }
}
