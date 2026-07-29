package com.hoplite;

import com.hoplite.DeathEffect.PlayerDeathEffectListener;
import com.hoplite.combat.CombatItemManager;
import com.hoplite.combat.CombatModule;
import com.hoplite.global.registry.GlobalIngredientRegistry;
import com.hoplite.global.registry.GlobalItemDefinitionLoader;
import com.hoplite.global.resourcepack.LocalResourcePackServer;
import com.hoplite.global.resourcepack.ResourcePackDispatcher;
import com.hoplite.legendary.HopliteCommand;
import com.hoplite.legendary.LegendaryCraftTracker;
import com.hoplite.legendary.LegendaryModule;
import com.hoplite.legendary.LegendaryWeaponManager;
import com.hoplite.recipe.RecipeModule;
import com.hoplite.utility.UtilityItemManager;
import com.hoplite.utility.UtilityModule;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;

/**
 * Main plugin entry point for Hoplite.
 */
public class HoplitePlugin extends JavaPlugin {
	private boolean enableWeaponRuntime = true;
	private YamlConfiguration cooldownConfig;
	private YamlConfiguration resourcePackConfig;
	private LocalResourcePackServer localResourcePackServer;
	private volatile String resolvedResourcePackUrl = "";
	private volatile boolean resourcePackUrlWarningLogged = false;
	private int resourcePackRefreshTaskId = -1;

	private static HoplitePlugin instance;
	private NamespacedKey legendaryKey;
	private LegendaryModule legendaryModule;
	private RecipeModule recipeModule;
	private GlobalIngredientRegistry globalIngredientRegistry;
	private CombatModule combatModule;
	private UtilityModule utilityModule;

	@Override
	public void onEnable() {
		instance = this;
		loadLegendarySettings();
		loadCooldownSettings();
		loadResourcePackSettings();
		startLocalResourcePackServer();
		startResourcePackUrlRefreshTask();
		legendaryKey = new NamespacedKey(this, "legendary_weapon");
		legendaryModule = new LegendaryModule(this);
		globalIngredientRegistry = new GlobalIngredientRegistry();
		combatModule = new CombatModule(this);
		utilityModule = new UtilityModule(this);
		recipeModule = new RecipeModule(this);

		new GlobalItemDefinitionLoader(this).loadFromDataFolder();

		legendaryModule.enable(enableWeaponRuntime);
		getServer().getPluginManager().registerEvents(new PlayerDeathEffectListener(this), this);
		ResourcePackDispatcher resourcePackDispatcher = new ResourcePackDispatcher(this);
		getServer().getPluginManager().registerEvents(resourcePackDispatcher, this);
		combatModule.enable();
		utilityModule.enable();
		recipeModule.enable();
		resourcePackDispatcher.dispatchToOnlinePlayers();

		PluginCommand hopliteCommand = getCommand("hoplite");
		if (hopliteCommand != null) {
			HopliteCommand rootCommand = new HopliteCommand(this);
			hopliteCommand.setExecutor(rootCommand);
			hopliteCommand.setTabCompleter(rootCommand);
		}
	}

	@Override
	public void onDisable() {
		if (resourcePackRefreshTaskId != -1) {
			getServer().getScheduler().cancelTask(resourcePackRefreshTaskId);
			resourcePackRefreshTaskId = -1;
		}
		if (localResourcePackServer != null) {
			localResourcePackServer.stop();
			localResourcePackServer = null;
		}
		instance = null;
	}

	public NamespacedKey getLegendaryKey() {
		return legendaryKey;
	}

	public LegendaryWeaponManager getWeaponManager() {
		return legendaryModule.getWeaponManager();
	}

	public static HoplitePlugin getInstance() {
		return instance;
	}

	public LegendaryCraftTracker getCraftTracker() {
		return legendaryModule.getCraftTracker();
	}

	public GlobalIngredientRegistry getCustomIngredientRegistry() {
		return globalIngredientRegistry;
	}

	public GlobalIngredientRegistry getGlobalIngredientRegistry() {
		return globalIngredientRegistry;
	}

	public CombatItemManager getCombatItemManager() {
		return combatModule.getCombatItemManager();
	}

	public UtilityItemManager getUtilityItemManager() {
		return utilityModule.getUtilityItemManager();
	}

	public RecipeModule getRecipeModule() {
		return recipeModule;
	}

	private void loadLegendarySettings() {
		File dataFolder = getDataFolder();
		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}

		File configRoot = new File(dataFolder, "config");
		if (!configRoot.exists()) {
			configRoot.mkdirs();
		}

		File legendaryDir = new File(configRoot, "legendary");
		if (!legendaryDir.exists()) {
			legendaryDir.mkdirs();
		}

		File settingsFile = new File(legendaryDir, "settings.yml");
		if (!settingsFile.exists()) {
			extractDefaultResource("config/legendary/settings.yml", settingsFile);
		}

		YamlConfiguration config = YamlConfiguration.loadConfiguration(settingsFile);
		enableWeaponRuntime = config.getBoolean("enable-weapon-runtime", true);
	}

	private void loadCooldownSettings() {
		File dataFolder = getDataFolder();
		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}

		File configRoot = new File(dataFolder, "config");
		if (!configRoot.exists()) {
			configRoot.mkdirs();
		}

		File cooldownFile = new File(configRoot, "cooldowns.yml");
		if (!cooldownFile.exists()) {
			extractDefaultResource("config/cooldowns.yml", cooldownFile);
		}

		cooldownConfig = YamlConfiguration.loadConfiguration(cooldownFile);
	}

	private void loadResourcePackSettings() {
		File dataFolder = getDataFolder();
		if (!dataFolder.exists()) {
			dataFolder.mkdirs();
		}

		File configRoot = new File(dataFolder, "config");
		if (!configRoot.exists()) {
			configRoot.mkdirs();
		}

		File resourcePackFile = new File(configRoot, "resource-pack.yml");
		if (!resourcePackFile.exists()) {
			extractDefaultResource("config/resource-pack.yml", resourcePackFile);
		}

		resourcePackConfig = YamlConfiguration.loadConfiguration(resourcePackFile);
	}

	private void startLocalResourcePackServer() {
		if (resourcePackConfig == null || !resourcePackConfig.getBoolean("enabled", false)) {
			return;
		}

		if (!resourcePackConfig.getBoolean("local-serve-enabled", false)) {
			return;
		}

		String localFilePath = resourcePackConfig.getString("local-file-path", "").trim();
		if (localFilePath.isEmpty()) {
			getLogger().warning("local-serve-enabled is true but local-file-path is empty in config/resource-pack.yml.");
			return;
		}

		String bindHost = resourcePackConfig.getString("local-bind-host", "0.0.0.0").trim();
		int port = Math.max(1, Math.min(65535, resourcePackConfig.getInt("local-port", 18181)));
		String route = resourcePackConfig.getString("local-route", "/hoplite-pack.zip").trim();
		String publicHost = resourcePackConfig.getString("local-public-host", "").trim();
		String publicScheme = resourcePackConfig.getString("local-public-scheme", "http").trim();

		try {
			localResourcePackServer = LocalResourcePackServer.start(
					this,
					localFilePath,
					bindHost,
					port,
					route,
					publicHost,
					publicScheme
			);
		} catch (IOException ex) {
			getLogger().warning("Failed to start local resource-pack server: " + ex.getMessage());
		}
	}

	private void startResourcePackUrlRefreshTask() {
		if (resourcePackConfig == null || !resourcePackConfig.getBoolean("enabled", false)) {
			return;
		}

		String sourceUrl = resourcePackConfig.getString("url-source", "").trim();
		if (sourceUrl.isEmpty()) {
			return;
		}

		long refreshMinutes = Math.max(1L, resourcePackConfig.getLong("url-source-refresh-minutes", 10L));
		long refreshTicks = refreshMinutes * 60L * 20L;

		resourcePackRefreshTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, this::refreshResourcePackUrlFromSource, 1L, refreshTicks);
		getLogger().info("Resource-pack URL source enabled; refresh interval=" + refreshMinutes + " minute(s).");
	}

	private void refreshResourcePackUrlFromSource() {
		String sourceUrl = resourcePackConfig.getString("url-source", "").trim();
		if (sourceUrl.isEmpty()) {
			return;
		}

		try {
			HttpClient client = HttpClient.newBuilder()
					.connectTimeout(Duration.ofSeconds(5))
					.build();

			HttpRequest request = HttpRequest.newBuilder(URI.create(sourceUrl))
					.GET()
					.timeout(Duration.ofSeconds(8))
					.build();

			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			if (response.statusCode() < 200 || response.statusCode() >= 300) {
				getLogger().warning("Failed to refresh resource-pack URL from source (HTTP " + response.statusCode() + ").");
				return;
			}

			String nextUrl = firstHttpLine(response.body());
			if (nextUrl.isEmpty()) {
				getLogger().warning("Resource-pack URL source did not return a valid URL line.");
				return;
			}

			if (!nextUrl.equals(resolvedResourcePackUrl)) {
				resolvedResourcePackUrl = nextUrl;
				getLogger().info("Updated resource-pack URL from source.");
			}
		} catch (Exception ex) {
			getLogger().warning("Failed to refresh resource-pack URL from source: " + ex.getMessage());
		}
	}

	private String firstHttpLine(String body) {
		if (body == null || body.isBlank()) {
			return "";
		}

		String[] lines = body.split("\\R");
		for (String line : lines) {
			if (line == null) {
				continue;
			}
			String candidate = line.trim();
			if (candidate.startsWith("https://") || candidate.startsWith("http://")) {
				return candidate;
			}
		}
		return "";
	}

	private String resolveResourcePackUrl() {
		if (localResourcePackServer != null) {
			return localResourcePackServer.getPublicUrl();
		}

		if (!resolvedResourcePackUrl.isBlank()) {
			return resolvedResourcePackUrl;
		}

		String direct = resourcePackConfig.getString("url", "").trim();
		if (!direct.isBlank()) {
			return direct;
		}

		List<String> fallbacks = resourcePackConfig.getStringList("fallback-urls");
		for (String fallback : fallbacks) {
			if (fallback != null && !fallback.isBlank()) {
				return fallback.trim();
			}
		}

		return "";
	}

	public void sendConfiguredResourcePack(Player player) {
		if (resourcePackConfig == null) {
			return;
		}

		if (!resourcePackConfig.getBoolean("enabled", false)) {
			return;
		}

		String url = resolveResourcePackUrl();
		if (url.isEmpty()) {
			if (!resourcePackUrlWarningLogged) {
				resourcePackUrlWarningLogged = true;
				getLogger().warning("Resource-pack dispatch is enabled but no usable URL was found (url, fallback-urls, or url-source). ");
			}
			return;
		}
		resourcePackUrlWarningLogged = false;

		String prompt = resourcePackConfig.getString("prompt", "");
		boolean required = resourcePackConfig.getBoolean("required", false);
		player.setResourcePack(url, null, prompt, required);
	}

	public boolean isResourcePackDispatchEnabled() {
		return resourcePackConfig != null && resourcePackConfig.getBoolean("enabled", false);
	}

	public boolean shouldKickOnResourcePackFailure() {
		if (resourcePackConfig == null) {
			return false;
		}
		return resourcePackConfig.getBoolean("kick-on-failure", resourcePackConfig.getBoolean("required", false));
	}

	public String getResourcePackFailureKickMessage() {
		if (resourcePackConfig == null) {
			return "Resource pack is required.";
		}
		return resourcePackConfig.getString("kick-message-on-failure", "Resource pack is required.");
	}

	public int getResourcePackLoadTimeoutSeconds() {
		if (resourcePackConfig == null) {
			return 0;
		}
		return Math.max(0, resourcePackConfig.getInt("kick-on-timeout-seconds", 0));
	}

	public String getResourcePackTimeoutKickMessage() {
		if (resourcePackConfig == null) {
			return "Resource pack load timed out.";
		}
		return resourcePackConfig.getString("kick-message-on-timeout", "Resource pack load timed out.");
	}

	public long getLegendaryCooldownMs(String key, long defaultMs) {
		return getCooldownMs("legendary." + key, defaultMs, "legendary.default-multiplier");
	}

	public long getUtilityCooldownMs(String key, long defaultMs) {
		return getCooldownMs("utility." + key, defaultMs, "utility.default-multiplier");
	}

	private long getCooldownMs(String valuePath, long defaultMs, String multiplierPath) {
		if (cooldownConfig == null) {
			return defaultMs;
		}

		long configured = cooldownConfig.getLong(valuePath, -1L);
		if (configured >= 0) {
			return configured;
		}
		double multiplier = Math.max(0.0D, cooldownConfig.getDouble(multiplierPath, 1.0D));
		return Math.max(0L, Math.round(defaultMs * multiplier));
	}

	private void extractDefaultResource(String resourceName, File target) {
		try (var in = getResource(resourceName)) {
			if (in == null) {
				return;
			}
			Files.copy(in, target.toPath());
		} catch (IOException ex) {
			getLogger().warning("Failed to extract default resource " + resourceName + ": " + ex.getMessage());
		}
	}
}
