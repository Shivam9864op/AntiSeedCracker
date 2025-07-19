package me.gadse.antiseedcracker.listeners;

import me.gadse.antiseedcracker.AntiSeedCracker;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

/**
 * Listener to handle player respawn events for biome obfuscation tracking.
 * 
 * This listener notifies the BiomeObfuscator when a player respawns,
 * allowing it to track respawn timing for suspicious request detection.
 */
public class PlayerRespawnListener implements Listener {
    
    private final AntiSeedCracker plugin;
    
    public PlayerRespawnListener(AntiSeedCracker plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        // Notify the biome obfuscator about the respawn
        if (plugin.getBiomeObfuscator() != null) {
            plugin.getBiomeObfuscator().onPlayerRespawn(event.getPlayer());
        }
    }
}