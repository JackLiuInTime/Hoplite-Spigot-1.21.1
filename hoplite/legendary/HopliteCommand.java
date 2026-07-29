package com.hoplite.legendary;

import com.hoplite.HoplitePlugin;

import com.hoplite.combat.CombatItemHandler;
import com.hoplite.utility.UtilityItemHandler;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class HopliteCommand implements CommandExecutor, TabCompleter {

    private static final List<String> CATEGORIES = List.of("legendary", "combat", "utility");
    private static final List<String> ACTIONS = List.of("give", "recipes");

    private final HoplitePlugin plugin;

    public HopliteCommand(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sendRootUsage(sender);
            return true;
        }

        String category = args[0].toLowerCase(java.util.Locale.ROOT);
        if (!CATEGORIES.contains(category)) {
            sendRootUsage(sender);
            return true;
        }

        if (args.length < 2) {
            sendCategoryUsage(sender, category);
            return true;
        }

        String action = args[1].toLowerCase(java.util.Locale.ROOT);
        if ("recipes".equals(action)) {
            listRecipes(sender, category);
            return true;
        }

        if (!"give".equals(action) || args.length < 3) {
            sendCategoryUsage(sender, category);
            return true;
        }

        String type = args[2];
        Player target;
        if (args.length >= 4) {
            target = Bukkit.getPlayer(args[3]);
            if (target == null) {
                sender.sendMessage("§cPlayer is not online: " + args[3]);
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            sender.sendMessage("§cPlease specify a player.");
            return true;
        }

        return giveItem(sender, target, category, type);
    }

    private boolean giveItem(CommandSender sender, Player target, String category, String type) {
        switch (category) {
            case "legendary" -> {
                var handlerOptional = plugin.getWeaponManager().getHandlerById(type);
                if (handlerOptional.isEmpty()) {
                    sender.sendMessage("§cLegendary item not found: " + type);
                    sender.sendMessage("§7Available types: " + plugin.getWeaponManager().availableTypes());
                    return true;
                }

                ItemStack item = handlerOptional.get().createItem(plugin);
                target.getInventory().addItem(item);
                sender.sendMessage("§aGiven " + target.getName() + " legendary item: " + handlerOptional.get().getId());
                target.sendMessage("§aYou received legendary item: " + handlerOptional.get().getId());
                return true;
            }
            case "combat" -> {
                var handlerOptional = plugin.getCombatItemManager().getHandlerById(type);
                if (handlerOptional.isEmpty()) {
                    sender.sendMessage("§cCombat item not found: " + type);
                    sender.sendMessage("§7Available types: " + plugin.getCombatItemManager().availableTypes());
                    return true;
                }

                ItemStack item = handlerOptional.get().createItem(plugin);
                target.getInventory().addItem(item);
                sender.sendMessage("§aGiven " + target.getName() + " combat item: " + handlerOptional.get().getId());
                target.sendMessage("§aYou received combat item: " + handlerOptional.get().getId());
                return true;
            }
            case "utility" -> {
                var handlerOptional = plugin.getUtilityItemManager().getHandlerById(type);
                if (handlerOptional.isEmpty()) {
                    sender.sendMessage("§cUtility item not found: " + type);
                    sender.sendMessage("§7Available types: " + plugin.getUtilityItemManager().availableTypes());
                    return true;
                }

                ItemStack item = handlerOptional.get().createItem(plugin);
                target.getInventory().addItem(item);
                sender.sendMessage("§aGiven " + target.getName() + " utility item: " + handlerOptional.get().getId());
                target.sendMessage("§aYou received utility item: " + handlerOptional.get().getId());
                return true;
            }
            default -> {
                sendRootUsage(sender);
                return true;
            }
        }
    }

    private void listRecipes(CommandSender sender, String category) {
        sender.sendMessage("§eRegistered " + category + " recipes:");

        switch (category) {
            case "legendary" -> {
                for (var handler : plugin.getWeaponManager().getAllHandlers()) {
                    ItemStack result = handler.createItem(plugin);
                    boolean has = plugin.getServer().getRecipesFor(result).iterator().hasNext();
                    sender.sendMessage(" - " + handler.getId() + ": " + (has ? "registered" : "not registered"));
                }
            }
            case "combat" -> {
                for (CombatItemHandler handler : plugin.getCombatItemManager().getAllHandlers()) {
                    ItemStack result = handler.createItem(plugin);
                    boolean has = plugin.getServer().getRecipesFor(result).iterator().hasNext();
                    sender.sendMessage(" - " + handler.getId() + ": " + (has ? "registered" : "not registered"));
                }
            }
            case "utility" -> {
                for (UtilityItemHandler handler : plugin.getUtilityItemManager().getAllHandlers()) {
                    ItemStack result = handler.createItem(plugin);
                    boolean has = plugin.getServer().getRecipesFor(result).iterator().hasNext();
                    sender.sendMessage(" - " + handler.getId() + ": " + (has ? "registered" : "not registered"));
                }
            }
            default -> sendRootUsage(sender);
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return partialMatches(args[0], CATEGORIES);
        }

        if (args.length == 2) {
            return partialMatches(args[1], ACTIONS);
        }

        String category = args[0].toLowerCase(java.util.Locale.ROOT);
        String action = args[1].toLowerCase(java.util.Locale.ROOT);

        if (!CATEGORIES.contains(category) || !"give".equals(action)) {
            return Collections.emptyList();
        }

        if (args.length == 3) {
            return partialMatches(args[2], availableTypes(category));
        }

        if (args.length == 4) {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            return partialMatches(args[3], players);
        }

        return Collections.emptyList();
    }

    private List<String> availableTypes(String category) {
        Collection<String> result = switch (category) {
            case "legendary" -> plugin.getWeaponManager().getAllHandlers().stream().map(h -> h.getId()).toList();
            case "combat" -> plugin.getCombatItemManager().getAllHandlers().stream().map(h -> h.getId()).toList();
            case "utility" -> plugin.getUtilityItemManager().getAllHandlers().stream().map(h -> h.getId()).toList();
            default -> List.of();
        };
        return new ArrayList<>(result);
    }

    private void sendRootUsage(CommandSender sender) {
        sender.sendMessage("§cUsage: /hoplite <legendary|combat|utility> <give|recipes> ...");
    }

    private void sendCategoryUsage(CommandSender sender, String category) {
        sender.sendMessage("§cUsage: /hoplite " + category + " give <type> [player]");
        sender.sendMessage("§cUsage: /hoplite " + category + " recipes");
    }

    private List<String> partialMatches(String token, List<String> candidates) {
        List<String> matches = new ArrayList<>();
        StringUtil.copyPartialMatches(token, candidates, matches);
        Collections.sort(matches);
        return matches;
    }
}