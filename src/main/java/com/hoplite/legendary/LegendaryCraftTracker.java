package com.hoplite.legendary;

import com.hoplite.HoplitePlugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks one-time legendary crafts and logs craft events to JSON.
 */
public class LegendaryCraftTracker {

    private static final Pattern EVENT_PATTERN = Pattern.compile(
            "\\{\\s*\\\"timestamp\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\\\"])*)\\\"\\s*,"
                    + "\\s*\\\"player\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\\\"])*)\\\"\\s*,"
                    + "\\s*\\\"weaponId\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\\\"])*)\\\"\\s*,"
                    + "\\s*\\\"weaponName\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\\\"])*)\\\"\\s*,"
                    + "\\s*\\\"action\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\\\"])*)\\\"\\s*\\}"
    );

    private static final String ACTION_CRAFTED = "crafted";
    private static final String ACTION_BLOCKED = "blocked_already_crafted";

    private static final class CraftEvent {
        private final String timestamp;
        private final String player;
        private final String weaponId;
        private final String weaponName;
        private final String action;

        private CraftEvent(String timestamp, String player, String weaponId, String weaponName, String action) {
            this.timestamp = timestamp;
            this.player = player;
            this.weaponId = weaponId;
            this.weaponName = weaponName;
            this.action = action;
        }
    }

    private final HoplitePlugin plugin;
    private final File logFile;
    private final Set<String> craftedWeaponIds = new LinkedHashSet<>();
    private final List<CraftEvent> events = new ArrayList<>();

    public LegendaryCraftTracker(HoplitePlugin plugin) {
        this.plugin = plugin;
        File data = plugin.getDataFolder();
        if (!data.exists()) {
            data.mkdirs();
        }
        this.logFile = new File(data, "legendary-craft-events.json");
        loadFromDisk();
    }

    public synchronized boolean hasCrafted(String weaponId) {
        if (weaponId == null || weaponId.isBlank()) {
            return false;
        }
        return craftedWeaponIds.contains(weaponId.toLowerCase(Locale.ROOT));
    }

    public synchronized boolean recordCraft(String player, String weaponId, String weaponName) {
        if (weaponId == null || weaponId.isBlank()) {
            return false;
        }
        String normalized = weaponId.toLowerCase(Locale.ROOT);
        if (craftedWeaponIds.contains(normalized)) {
            appendEvent(player, normalized, weaponName, ACTION_BLOCKED);
            saveToDisk();
            return false;
        }

        craftedWeaponIds.add(normalized);
        appendEvent(player, normalized, weaponName, ACTION_CRAFTED);
        saveToDisk();
        return true;
    }

    public synchronized void recordBlocked(String player, String weaponId, String weaponName) {
        if (weaponId == null || weaponId.isBlank()) {
            return;
        }
        appendEvent(player, weaponId.toLowerCase(Locale.ROOT), weaponName, ACTION_BLOCKED);
        saveToDisk();
    }

    private void appendEvent(String player, String weaponId, String weaponName, String action) {
        events.add(new CraftEvent(
                Instant.now().toString(),
                player == null || player.isBlank() ? "unknown" : player,
                weaponId,
                weaponName == null || weaponName.isBlank() ? weaponId : weaponName,
                action
        ));
    }

    private void loadFromDisk() {
        if (!logFile.exists()) {
            saveToDisk();
            return;
        }

        try {
            String json = Files.readString(logFile.toPath(), StandardCharsets.UTF_8);
            parseEvents(json);
            rebuildCraftedSet();
        } catch (Exception ex) {
            plugin.getLogger().warning("Failed to read legendary-craft-events.json, file will be reset: " + ex.getMessage());
            craftedWeaponIds.clear();
            events.clear();
            saveToDisk();
        }
    }

    private void parseEvents(String json) {
        events.clear();
        Matcher matcher = EVENT_PATTERN.matcher(json);
        while (matcher.find()) {
            events.add(new CraftEvent(
                    unescapeJson(matcher.group(1)),
                    unescapeJson(matcher.group(2)),
                    unescapeJson(matcher.group(3)).toLowerCase(Locale.ROOT),
                    unescapeJson(matcher.group(4)),
                    unescapeJson(matcher.group(5))
            ));
        }
    }

    private void rebuildCraftedSet() {
        craftedWeaponIds.clear();
        for (CraftEvent event : events) {
            if (ACTION_CRAFTED.equals(event.action)) {
                craftedWeaponIds.add(event.weaponId);
            }
        }
    }

    private void saveToDisk() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"events\": [\n");
        for (int i = 0; i < events.size(); i++) {
            CraftEvent e = events.get(i);
            sb.append("    {")
                    .append("\"timestamp\":\"").append(escapeJson(e.timestamp)).append("\",")
                    .append("\"player\":\"").append(escapeJson(e.player)).append("\",")
                    .append("\"weaponId\":\"").append(escapeJson(e.weaponId)).append("\",")
                    .append("\"weaponName\":\"").append(escapeJson(e.weaponName)).append("\",")
                    .append("\"action\":\"").append(escapeJson(e.action)).append("\"")
                    .append("}");
            if (i < events.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ]\n");
        sb.append("}\n");

        try {
            Files.writeString(logFile.toPath(), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            plugin.getLogger().warning("Failed to write legendary-craft-events.json: " + ex.getMessage());
        }
    }

    private String escapeJson(String input) {
        String s = input == null ? "" : input;
        return s
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String input) {
        String s = input == null ? "" : input;
        return s
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
