package com.hoplite.recipe.loader;

import com.hoplite.HoplitePlugin;
import com.hoplite.recipe.resolver.RecipeIngredientResolver;

import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.RecipeChoice;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loads crafting recipes from the plugin data folder.
 * Uses JSON recipes as the only source of truth.
 */
public class RecipeFileLoader {
    private final HoplitePlugin plugin;
    private final RecipeIngredientResolver ingredientResolver;

    public RecipeFileLoader(HoplitePlugin plugin) {
        this.plugin = plugin;
        this.ingredientResolver = new RecipeIngredientResolver(plugin);
    }

    public void loadFromDataFolder() {
        try {
            File data = plugin.getDataFolder();
            if (!data.exists()) data.mkdirs();
            File recipeRoot = new File(data, "recipe");
            if (!recipeRoot.exists()) recipeRoot.mkdirs();
            loadCategoryRecipes(data, recipeRoot, "legendary");
            loadCategoryRecipes(data, recipeRoot, "combat");
            loadCategoryRecipes(data, recipeRoot, "utility");
        } catch (IOException ex) {
            plugin.getLogger().severe("Failed to load recipes file: " + ex.getMessage());
        }
    }

    private void loadCategoryRecipes(File data, File recipeRoot, String category) throws IOException {
        File categoryDir = new File(recipeRoot, category);
        if (!categoryDir.exists()) categoryDir.mkdirs();

        File jsonFile = new File(categoryDir, "recipes.json");

        // Migrate from previous layout: <data>/<category>/recipes.json -> <data>/recipe/<category>/recipes.json
        File legacyCategoryJson = new File(new File(data, category), "recipes.json");
        if (!jsonFile.exists() && legacyCategoryJson.exists()) {
            Files.move(legacyCategoryJson.toPath(), jsonFile.toPath());
            plugin.getLogger().info("Migrated legacy recipe file to " + jsonFile.getAbsolutePath());
        }

        if (!jsonFile.exists()) {
            extractDefaultResource("recipe/" + category + "/recipes.json", jsonFile);
        }

        if (!jsonFile.exists()) {
            plugin.getLogger().warning("Recipe file not found for category " + category + ": " + jsonFile.getAbsolutePath());
            return;
        }

        String content = readTextWithEncodingDetection(jsonFile);
        Map<String, Map<String, String>> recipeDefinitions = parseJson(content);
        if (recipeDefinitions == null) {
            plugin.getLogger().severe("Invalid recipes JSON in " + jsonFile.getAbsolutePath() + " (category: " + category + ")");
            return;
        }
        if (recipeDefinitions.isEmpty()) {
            plugin.getLogger().warning("No recipe entries found in " + jsonFile.getAbsolutePath() + " (category: " + category + ")");
        }

        registerSections(recipeDefinitions, category);
    }

    private Map<String, Map<String, String>> parseJson(String content) {
        // Lightweight JSON parsing for recipe definitions.
        // Only object keys and string values are supported, which is sufficient for recipe data.
        JsonParser parser = new JsonParser(content);
        Map<String, Object> root = parser.parseObject();
        if (root == null || !parser.isFullyConsumed()) return null;

        Map<String, Map<String, String>> sections = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            if (!(entry.getValue() instanceof Map)) continue;
            Map<String, Object> raw = (Map<String, Object>) entry.getValue();
            Map<String, String> section = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : raw.entrySet()) {
                section.put(e.getKey(), String.valueOf(e.getValue()));
            }
            sections.put(entry.getKey(), section);
        }
        return sections;
    }

    private boolean extractDefaultResource(String resourceName, File target) {
        try (var in = plugin.getResource(resourceName)) {
            if (in == null) return false;
            Files.copy(in, target.toPath());
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    private String readTextWithEncodingDetection(File file) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());
        if (bytes.length >= 2) {
            int b0 = bytes[0] & 0xFF;
            int b1 = bytes[1] & 0xFF;
            if (b0 == 0xFF && b1 == 0xFE) {
                return new String(bytes, Charset.forName("UTF-16LE"));
            }
            if (b0 == 0xFE && b1 == 0xFF) {
                return new String(bytes, Charset.forName("UTF-16BE"));
            }
        }

        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static class JsonParser {
        private final String json;
        private int idx;

        public JsonParser(String json) {
            this.json = normalizeJson(json);
            this.idx = 0;
        }

        private String normalizeJson(String raw) {
            if (raw == null) {
                return "";
            }

            String normalized = raw;
            while (!normalized.isEmpty() && normalized.charAt(0) == '\uFEFF') {
                normalized = normalized.substring(1);
            }
            return normalized.trim();
        }

        public Map<String, Object> parseObject() {
            skipWhitespace();
            if (!hasNext() || nextChar() != '{') return null;
            idx++;
            Map<String, Object> object = new LinkedHashMap<>();
            while (true) {
                skipWhitespace();
                if (!hasNext()) return null;
                if (nextChar() == '}') {
                    idx++;
                    return object;
                }
                String key = parseString();
                if (key == null) return null;
                skipWhitespace();
                if (!hasNext() || nextChar() != ':') return null;
                idx++;
                skipWhitespace();
                Object value = parseValue();
                if (value == null) return null;
                object.put(key, value);
                skipWhitespace();
                if (!hasNext()) return null;
                char c = nextChar();
                if (c == ',') {
                    idx++;
                    continue;
                }
                if (c == '}') {
                    idx++;
                    return object;
                }
                return null;
            }
        }

        private Object parseValue() {
            skipWhitespace();
            if (!hasNext()) return null;
            char c = nextChar();
            if (c == '"') return parseString();
            if (c == '{') return parseObject();
            return parseLiteral();
        }

        private String parseLiteral() {
            int start = idx;
            while (hasNext()) {
                char c = nextChar();
                if (c == ',' || c == '}' || Character.isWhitespace(c)) {
                    break;
                }
                idx++;
            }

            if (idx <= start) {
                return null;
            }

            return json.substring(start, idx);
        }

        private String parseString() {
            if (!hasNext() || nextChar() != '"') return null;
            idx++;
            StringBuilder sb = new StringBuilder();
            while (hasNext()) {
                char c = nextChar();
                idx++;
                if (c == '"') {
                    return sb.toString();
                }
                if (c == '\\' && hasNext()) {
                    char esc = nextChar();
                    idx++;
                    switch (esc) {
                        case '"', '\\', '/' -> sb.append(esc);
                        case 'b' -> sb.append('\b');
                        case 'f' -> sb.append('\f');
                        case 'n' -> sb.append('\n');
                        case 'r' -> sb.append('\r');
                        case 't' -> sb.append('\t');
                        default -> sb.append(esc);
                    }
                } else {
                    sb.append(c);
                }
            }
            return null;
        }

        private void skipWhitespace() {
            while (hasNext() && Character.isWhitespace(nextChar())) {
                idx++;
            }
        }

        public boolean isFullyConsumed() {
            skipWhitespace();
            return !hasNext();
        }

        private boolean hasNext() {
            return idx < json.length();
        }

        private char nextChar() {
            return json.charAt(idx);
        }
    }

    private void registerSections(Map<String, Map<String, String>> sections, String category) {
        // Register recipe entries using weapon handlers first, then fall back to
        // category-scoped global custom items (for combat/utility style items).
        sections.forEach((id, map) -> {
            ItemStack result;
            var handlerOpt = plugin.getWeaponManager().getHandlerById(id);
            if (handlerOpt.isPresent()) {
                var handler = handlerOpt.get();
                result = handler.createItem(plugin);
            } else {
                String scopedId = category.toLowerCase() + ":" + id.toLowerCase();
                var customResult = plugin.getGlobalIngredientRegistry().get(scopedId);
                if (customResult.isPresent()) {
                    result = customResult.get();
                } else {
                    plugin.getLogger().warning("Skipping recipe " + id + " in category " + category + ": no handler or scoped custom item found.");
                    return;
                }
            }

            int resultCount = parseResultCount(map);
            if (resultCount > 1) {
                result.setAmount(Math.min(resultCount, result.getMaxStackSize()));
            }

            NamespacedKey key = new NamespacedKey(plugin, category + "_" + id + "_recipe");

            String shapeRaw = map.getOrDefault("shape", "   ,   ,   ");
            String[] rows = shapeRaw.split(",");
            String r0 = rows.length > 0 ? padRow(rows[0]) : "   ";
            String r1 = rows.length > 1 ? padRow(rows[1]) : "   ";
            String r2 = rows.length > 2 ? padRow(rows[2]) : "   ";
            // Validate that all characters used in the shape have mappings
            java.util.Set<Character> used = new java.util.HashSet<>();
            for (char c : r0.toCharArray()) if (c != ' ') used.add(c);
            for (char c : r1.toCharArray()) if (c != ' ') used.add(c);
            for (char c : r2.toCharArray()) if (c != ' ') used.add(c);

            // Prepare resolved ingredient map
            java.util.Map<Character, Object> resolved = new java.util.HashMap<>();
            java.util.List<String> problems = new java.util.ArrayList<>();

            for (Map.Entry<String, String> e : map.entrySet()) {
                String k = e.getKey();
                if (k.length() != 1) continue;
                char ch = k.charAt(0);
                String rawVal = e.getValue().trim();

                Object resolvedIngredient = ingredientResolver.resolve(rawVal, id, problems);
                if (resolvedIngredient != null) {
                    resolved.put(ch, resolvedIngredient);
                }
            }

            // Check used chars
            for (char c : used) {
                if (!resolved.containsKey(c)) {
                    problems.add("Missing mapping for character '" + c + "' used in shape");
                }
            }

            if (!problems.isEmpty()) {
                plugin.getLogger().warning("Skipping recipe " + id + " due to problems:");
                for (String p : problems) plugin.getLogger().warning(" - " + p + " for recipe " + id);
                return; // skip this recipe
            }

            ShapedRecipe recipe = new ShapedRecipe(key, result);
            recipe.shape(r0, r1, r2);

            // apply resolved ingredients
            for (var entry : resolved.entrySet()) {
                char ch = entry.getKey();
                Object val = entry.getValue();
                if (val instanceof Material) {
                    recipe.setIngredient(ch, (Material) val);
                } else if (val instanceof RecipeChoice) {
                    recipe.setIngredient(ch, (RecipeChoice) val);
                }
            }

            plugin.getServer().addRecipe(recipe);
            plugin.getLogger().info("Registered recipe: " + category + "/" + id);
        });
    }

    private int parseResultCount(Map<String, String> map) {
        String raw = map.getOrDefault("result-count", map.getOrDefault("result_count", "1"));
        try {
            return Math.max(1, Integer.parseInt(raw.trim()));
        } catch (Exception ignored) {
            return 1;
        }
    }

    private String padRow(String row) {
        String value = row == null ? "" : row;
        if (value.length() >= 3) return value.substring(0, 3);
        return String.format("%-3s", value);
    }
}
