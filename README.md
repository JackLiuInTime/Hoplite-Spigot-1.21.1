# Hoplite Legendary Weapons

This is a Spigot 1.21.1 plugin for implementing Hoplite-style legendary weapons.

## Current Weapon List

See `docs/legendary/legendary.md` for the full legendary weapon table (stats, skills, lore, and recipes).

### Items With Core Gameplay Logic Implemented

Legendary

- `war_pick` (War Pick)
- `emerald_blade` (Emerald Blade)
- `poseidon_trident` (Poseidon's Trident)
- `hypnosis_staff` (Hypnosis Staff)
- `golem_hammer` (Golem Hammer)
- `midas_sword` (Midas Sword)

CombatItem

- `agony`
- `cactus_chestplate`
- `crystallization_shard`
- `golden_head`
- `mace`
- `panacea`

UtilityItem

- `ares_blessing`
- `auto_smelters_pickaxe`
- `bundled_arrows`
- `bundle`
- `ender_pack`
- `explosive_pickaxe`
- `light_anvil`
- `lumberjacks_axe`
- `nether_reactor_core`
- `obsidian_maker`
- `portable_villager`
- `revival_star`
- `super_smelters_pickaxe`
- `tim_the_enchanter`
- `tracker_pack`

Other items are currently focused on item creation, base enchantments, and recipe registration.

## Plugin Features

- Defines legendary weapons with unique identifiers
- Detects legendary weapons in combat-related events
- Supports root command `/hoplite <legendary|combat|utility> <give|recipes> ...`
- Externalized cooldown settings in `config/cooldowns.yml` (global multiplier and selected per-skill overrides)
- Official resource-pack CMD values are set directly in relevant legendary handlers
- Combat and utility item handlers are registered directly in code (no handlers.yml toggle list)
- Optional plugin-managed resource-pack dispatch via `config/resource-pack.yml` (no server.properties `resource-pack` required)
- Global death feedback effect (visual lightning + wither spawn sound)

## Hoplite Categories

Hoplite content is organized into three categories:

- `combat`
- `utility`
- `legendary` (this repository's current focus)

## Recipe Ingredient Notes

You can use Hoplite custom legendary items as ingredients in both `recipes.ini` and `recipes.json`.

- Syntax: `legendary:<weapon_id>`
- Example: `R=legendary:war_pick`

The loader resolves this syntax as `RecipeChoice.ExactChoice`, so only the exact custom item can match.

You can also use shared non-legendary Hoplite items as ingredients.

- Preferred global syntax: `global:<item_id>`
- Category syntax: `combat:<item_id>` or `utility:<item_id>`
- Legacy alias still supported: `custom:<item_id>`
- Example: `V=global:spider_silk_sac`

These IDs are resolved through `GlobalIngredientRegistry`.

## Build and Run

1. Go to the project root:

   ```powershell
   cd c:\Users\login\Desktop\Hoplite
   ```

2. Ensure `spigot-api.jar` exists in the project root.
3. Build with Maven:

   ```powershell
   mvn package
   ```

4. Copy `target/hoplite-legendary-1.0.0.jar` into your server `plugins` folder.

## Project Structure

- `pom.xml` - Maven build file
- `src/main/java` - plugin source code
- `src/main/java/com/hoplite/HoplitePlugin.java` - single plugin entry point and module bootstrap
- `src/main/java/com/hoplite/DeathEffect/` - global death-effect listeners
- `src/main/resources/plugin.yml` - plugin descriptor
- `src/main/resources/config/cooldowns.yml` - cooldown externalization config
- `src/main/resources/config/resource-pack.yml` - plugin-managed resource-pack dispatch settings
- `docs/legendary/legendary.md` - legendary weapon list and effect descriptions
- `docs/combat/` - combat module documentation
- `docs/utility/` - utility module documentation
- `docs/global/death-effect.md` - global death effect documentation
- `docs/legendary/old/` - archived legendary weapon lists not present in current screenshot set
- `src/main/resources/legendary/` - legendary recipe data
- `src/main/resources/combat/` - combat recipe data
- `src/main/resources/utility/` - utility recipe data
- `src/main/resources/legendary/old/` - archived recipe data for legendary-archived weapons
- `src/main/resources/global/custom-items.yml` - global special item definitions (e.g. Spider Silk Sac, Vampire Tooth)

## Official Resource Pack Compatibility

This project uses direct code mapping: each relevant handler sets `ItemMeta#setCustomModelData(...)` in `createItem(...)`.

Current note: the currently checked external resource pack is known to be problematic and is not used as authoritative reference.
Use `docs/legendary/cmd-baseline.md` as the current source-of-truth baseline.

- Internal behavior routing still uses `legendary_weapon` PDC ID.
- `custom-model-data` must match the item's base `Material` mapping in the pack (e.g. `diamond_sword` and `netherite_sword` use different CMD tables).

## Resource-Pack Dispatch Without Self-Hosting

If you do not have your own file server and official URLs change frequently, use `config/resource-pack.yml` with `url-source`.

- `url`: static direct zip URL (optional)
- `url-source`: HTTP(S) text endpoint; plugin uses the first `http://` or `https://` line as current pack URL
- `fallback-urls`: optional backup URL list
- `url-source-refresh-minutes`: polling interval for URL refresh

The plugin then dispatches this resolved URL to players on join, so you do not need `server.properties` resource-pack settings.

If your Minecraft server is running on your own machine, you can also let the plugin serve a local zip directly:

- Set `local-serve-enabled: true`
- Set `local-file-path` to your local `.zip` absolute path
- Set `local-public-host` to an IP/DNS reachable by players (for LAN, use your LAN IP)
- Keep `local-port` open in firewall/router when needed

When local serving is enabled and started successfully, plugin dispatch uses this local URL first.

To remove players from the server early when resource-pack loading is not completed:

- `required: true` to enforce acceptance
- `kick-on-failure: true` to kick immediately on decline/download failure/invalid URL
- `kick-on-timeout-seconds: <seconds>` to kick if loading does not complete in time

Customize messages with:

- `kick-message-on-failure`
- `kick-message-on-timeout`
