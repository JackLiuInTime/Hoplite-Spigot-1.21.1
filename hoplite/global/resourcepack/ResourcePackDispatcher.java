package com.hoplite.global.resourcepack;

import com.hoplite.HoplitePlugin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Sends the configured resource pack on join so server.properties does not need resource-pack settings.
 */
public final class ResourcePackDispatcher implements Listener {

    private final HoplitePlugin plugin;
    private final Map<UUID, Integer> timeoutTasks = new ConcurrentHashMap<>();

    public ResourcePackDispatcher(HoplitePlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.sendConfiguredResourcePack(player);

        int timeoutSeconds = plugin.getResourcePackLoadTimeoutSeconds();
        if (timeoutSeconds <= 0 || !plugin.isResourcePackDispatchEnabled()) {
            return;
        }

        int taskId = plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            Player online = plugin.getServer().getPlayer(player.getUniqueId());
            if (online == null || !online.isOnline()) {
                timeoutTasks.remove(player.getUniqueId());
                return;
            }

            timeoutTasks.remove(player.getUniqueId());
            online.kickPlayer(plugin.getResourcePackTimeoutKickMessage());
        }, timeoutSeconds * 20L);

        timeoutTasks.put(player.getUniqueId(), taskId);
    }

    @EventHandler
    public void onResourcePackStatus(PlayerResourcePackStatusEvent event) {
        String status = event.getStatus().name();
        UUID id = event.getPlayer().getUniqueId();

        // Pack loaded/downloaded successfully: cancel pending timeout.
        if ("SUCCESSFULLY_LOADED".equals(status) || "DOWNLOADED".equals(status)) {
            cancelTimeout(id);
            return;
        }

        if ("DECLINED".equals(status) || status.startsWith("FAILED") || "INVALID_URL".equals(status) || "DISCARDED".equals(status)) {
            cancelTimeout(id);
            if (plugin.shouldKickOnResourcePackFailure()) {
                event.getPlayer().kickPlayer(plugin.getResourcePackFailureKickMessage());
            }
        }
    }

    public void dispatchToOnlinePlayers() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            plugin.sendConfiguredResourcePack(player);
        }
    }

    private void cancelTimeout(UUID playerId) {
        Integer taskId = timeoutTasks.remove(playerId);
        if (taskId != null) {
            plugin.getServer().getScheduler().cancelTask(taskId);
        }
    }
}
