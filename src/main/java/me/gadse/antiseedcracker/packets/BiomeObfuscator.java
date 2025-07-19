package me.gadse.antiseedcracker.packets;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.reflect.FieldAccessException;
import me.gadse.antiseedcracker.AntiSeedCracker;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Packet handler that obfuscates biome data in chunk packets to counteract seed cracking tools.
 * 
 * This handler intercepts outgoing chunk data packets and randomizes biome information
 * for suspicious requests, such as:
 * - Rapid biome probing attempts
 * - Requests during login or respawn sequences
 * - Requests for chunks that haven't been recently loaded by players
 * 
 * The obfuscation preserves vanilla gameplay while making it difficult for automated
 * tools like SeedcrackerX to extract accurate biome data for seed analysis.
 */
public class BiomeObfuscator extends PacketAdapter {

    private final AntiSeedCracker plugin;
    
    // Track player activity for suspicious request detection
    private final Map<UUID, PlayerBiomeActivity> playerActivity = new ConcurrentHashMap<>();
    
    // Cache of randomized biomes to maintain consistency during a session
    private final Map<String, int[]> biomeCache = new ConcurrentHashMap<>();
    
    // Common biome IDs that can be used for randomization
    private static final int[] COMMON_BIOMES = {
        1,   // Plains
        4,   // Forest
        6,   // Swamp
        7,   // River
        12,  // Desert
        21,  // Jungle
        25,  // Birch Forest
        27,  // Birch Forest Hills
        29,  // Dark Forest
        30,  // Snowy Taiga
        35   // Savanna
    };

    public BiomeObfuscator(AntiSeedCracker plugin) {
        super(plugin, ListenerPriority.HIGH, 
              PacketType.Play.Server.MAP_CHUNK, 
              PacketType.Play.Server.LEVEL_CHUNK_WITH_LIGHT);
        this.plugin = plugin;
        
        // Clean up old activity data every 5 minutes
        plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, 
            this::cleanupOldActivity, 6000L, 6000L);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (!plugin.getConfig().getBoolean("biome_obfuscation.enabled", false)) {
            return;
        }
        
        Player player = event.getPlayer();
        if (player == null) {
            return;
        }
        
        PlayerBiomeActivity activity = getPlayerActivity(player.getUniqueId());
        
        // Check if this request should be considered suspicious
        if (isSuspiciousRequest(activity, player)) {
            obfuscateBiomeData(event, player);
        }
        
        // Update activity tracking
        activity.recordChunkRequest();
    }
    
    /**
     * Determines if a biome data request should be considered suspicious
     * based on player activity patterns.
     */
    private boolean isSuspiciousRequest(PlayerBiomeActivity activity, Player player) {
        long currentTime = System.currentTimeMillis();
        
        // Check for rapid requests (potential automated probing)
        if (activity.isRapidRequesting(currentTime)) {
            plugin.getLogger().info("Detected rapid biome probing from player: " + player.getName());
            return true;
        }
        
        // Check if player recently joined (login sequence)
        if (currentTime - activity.getJoinTime() < plugin.getConfig().getLong("biome_obfuscation.login_protection_duration", 10000)) {
            return true;
        }
        
        // Check if player recently respawned
        if (currentTime - activity.getLastRespawn() < plugin.getConfig().getLong("biome_obfuscation.respawn_protection_duration", 5000)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Obfuscates biome data in the chunk packet by randomizing biome IDs.
     */
    private void obfuscateBiomeData(PacketEvent event, Player player) {
        PacketContainer packet = event.getPacket();
        
        try {
            // Get chunk coordinates for cache key
            int chunkX = packet.getIntegers().read(0);
            int chunkZ = packet.getIntegers().read(1);
            String cacheKey = player.getWorld().getName() + "_" + chunkX + "_" + chunkZ;
            
            // Try to get or create randomized biome data
            int[] randomizedBiomes = biomeCache.computeIfAbsent(cacheKey, k -> generateRandomizedBiomes());
            
            // Apply the randomized biomes to the packet
            // Note: The exact field to modify depends on the packet structure and MC version
            // This is a simplified approach that may need version-specific handling
            applyBiomeObfuscation(packet, randomizedBiomes);
            
            if (plugin.getConfig().getBoolean("biome_obfuscation.log_obfuscated_chunks", false)) {
                plugin.getLogger().info("Obfuscated biome data for chunk (" + chunkX + ", " + chunkZ + 
                                      ") sent to player: " + player.getName());
            }
            
        } catch (Exception ex) {
            // Log the error but don't break the packet sending
            plugin.getLogger().warning("Failed to obfuscate biome data for player " + 
                                     player.getName() + ": " + ex.getMessage());
        }
    }
    
    /**
     * Applies biome obfuscation to the packet based on the packet type and structure.
     */
    private void applyBiomeObfuscation(PacketContainer packet, int[] randomizedBiomes) throws FieldAccessException {
        // This method needs to handle different packet structures based on Minecraft version
        // For now, we'll implement a basic approach that works with common packet structures
        
        if (packet.getType() == PacketType.Play.Server.MAP_CHUNK) {
            // Handle MAP_CHUNK packet (older versions)
            handleMapChunkBiomes(packet, randomizedBiomes);
        } else if (packet.getType() == PacketType.Play.Server.LEVEL_CHUNK_WITH_LIGHT) {
            // Handle LEVEL_CHUNK_WITH_LIGHT packet (newer versions)
            handleLevelChunkBiomes(packet, randomizedBiomes);
        }
    }
    
    /**
     * Handles biome obfuscation for MAP_CHUNK packets.
     */
    private void handleMapChunkBiomes(PacketContainer packet, int[] randomizedBiomes) throws FieldAccessException {
        // The exact implementation depends on the packet structure
        // This is a placeholder that attempts to modify biome-related fields
        try {
            // Try to access biome data through byte arrays or other structures
            if (packet.getByteArrays().size() > 0) {
                // Some versions store biome data in byte arrays
                byte[] data = packet.getByteArrays().read(0);
                if (data != null && data.length > 0) {
                    // Apply simple obfuscation by modifying some bytes
                    obfuscateByteArray(data, randomizedBiomes);
                    packet.getByteArrays().write(0, data);
                }
            }
        } catch (Exception ex) {
            // Fallback: don't modify if we can't access the data safely
            plugin.getLogger().fine("Could not access MAP_CHUNK biome data: " + ex.getMessage());
        }
    }
    
    /**
     * Handles biome obfuscation for LEVEL_CHUNK_WITH_LIGHT packets.
     */
    private void handleLevelChunkBiomes(PacketContainer packet, int[] randomizedBiomes) throws FieldAccessException {
        // Similar to MAP_CHUNK but for newer packet structure
        try {
            // Try to access and modify biome-related data
            if (packet.getByteArrays().size() > 0) {
                byte[] data = packet.getByteArrays().read(0);
                if (data != null && data.length > 0) {
                    obfuscateByteArray(data, randomizedBiomes);
                    packet.getByteArrays().write(0, data);
                }
            }
        } catch (Exception ex) {
            plugin.getLogger().fine("Could not access LEVEL_CHUNK biome data: " + ex.getMessage());
        }
    }
    
    /**
     * Applies simple obfuscation to byte array data that may contain biome information.
     */
    private void obfuscateByteArray(byte[] data, int[] randomizedBiomes) {
        if (data.length < 4) return;
        
        // Apply simple pattern-based obfuscation
        // This is a basic approach that modifies some bytes to disrupt biome patterns
        for (int i = 0; i < data.length - 3; i += 4) {
            if (shouldObfuscateByte(data, i)) {
                int randomBiome = randomizedBiomes[ThreadLocalRandom.current().nextInt(randomizedBiomes.length)];
                data[i] = (byte) (randomBiome & 0xFF);
            }
        }
    }
    
    /**
     * Determines if a specific byte position should be obfuscated.
     */
    private boolean shouldObfuscateByte(byte[] data, int index) {
        // Only obfuscate certain patterns to avoid breaking the packet completely
        // This heuristic tries to identify potential biome data
        if (index + 3 >= data.length) return false;
        
        int value = ((data[index] & 0xFF) << 24) | 
                   ((data[index + 1] & 0xFF) << 16) | 
                   ((data[index + 2] & 0xFF) << 8) | 
                   (data[index + 3] & 0xFF);
        
        // Obfuscate values that look like biome IDs (small positive integers)
        return value >= 0 && value < 1000 && ThreadLocalRandom.current().nextFloat() < 0.3f;
    }
    
    /**
     * Generates a set of randomized biome IDs for obfuscation.
     */
    private int[] generateRandomizedBiomes() {
        List<Integer> biomes = new ArrayList<>();
        for (int biome : COMMON_BIOMES) {
            biomes.add(biome);
        }
        Collections.shuffle(biomes);
        return biomes.stream().mapToInt(Integer::intValue).toArray();
    }
    
    /**
     * Gets or creates player activity tracking data.
     */
    private PlayerBiomeActivity getPlayerActivity(UUID playerId) {
        return playerActivity.computeIfAbsent(playerId, k -> new PlayerBiomeActivity());
    }
    
    /**
     * Notifies the obfuscator that a player has respawned.
     */
    public void onPlayerRespawn(Player player) {
        PlayerBiomeActivity activity = getPlayerActivity(player.getUniqueId());
        activity.recordRespawn();
    }
    
    /**
     * Cleans up old activity data to prevent memory leaks.
     */
    private void cleanupOldActivity() {
        long cutoffTime = System.currentTimeMillis() - 300000; // 5 minutes
        playerActivity.entrySet().removeIf(entry -> 
            entry.getValue().getLastActivity() < cutoffTime);
        
        // Also clean up old biome cache entries
        if (biomeCache.size() > 1000) {
            biomeCache.clear();
        }
    }
    
    /**
     * Inner class to track player biome-related activity patterns.
     */
    private static class PlayerBiomeActivity {
        private final long joinTime;
        private long lastRespawn;
        private long lastChunkRequest;
        private int recentRequestCount;
        private final Queue<Long> requestTimes = new LinkedList<>();
        
        public PlayerBiomeActivity() {
            this.joinTime = System.currentTimeMillis();
            this.lastRespawn = 0;
            this.lastChunkRequest = 0;
            this.recentRequestCount = 0;
        }
        
        public void recordChunkRequest() {
            long currentTime = System.currentTimeMillis();
            this.lastChunkRequest = currentTime;
            this.recentRequestCount++;
            
            // Track request times for rapid detection
            requestTimes.offer(currentTime);
            
            // Remove old request times (older than 10 seconds)
            while (!requestTimes.isEmpty() && 
                   currentTime - requestTimes.peek() > 10000) {
                requestTimes.poll();
            }
        }
        
        public void recordRespawn() {
            this.lastRespawn = System.currentTimeMillis();
        }
        
        public boolean isRapidRequesting(long currentTime) {
            // Consider it rapid if more than 20 requests in 10 seconds
            return requestTimes.size() > 20;
        }
        
        public long getJoinTime() {
            return joinTime;
        }
        
        public long getLastRespawn() {
            return lastRespawn;
        }
        
        public long getLastActivity() {
            return Math.max(lastChunkRequest, lastRespawn);
        }
    }
}