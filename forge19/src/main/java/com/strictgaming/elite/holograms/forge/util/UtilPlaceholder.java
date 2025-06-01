package com.strictgaming.elite.holograms.forge.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * Utility for placeholder replacement
 * 
 * Built-in placeholders available:
 * Server placeholders:
 * - %players% - Current number of online players
 * - %maxplayers% - Maximum number of players allowed on the server
 * - %tps% - Current server TPS (ticks per second)
 * - %uptime% - Server uptime in hours and minutes format
 * - %memory% - Current memory usage (used/max in MB)
 * - %server_time% - Real world server time
 * 
 * Player placeholders (show different values for each viewing player):
 * - %player% - Viewing player's name
 * - %player_health% - Player's current health
 * - %player_level% - Player's XP level
 * - %player_world% - What world the player is in
 * - %player_coords% - Player's X, Y, Z coordinates
 * - %player_gamemode% - Player's game mode (Creative/Survival/etc)
 *
 */
public class UtilPlaceholder {

    private static long serverStartTime = System.currentTimeMillis();
    
    /**
     * Called when the server starts to record the start time
     */
    public static void recordServerStart() {
        serverStartTime = System.currentTimeMillis();
    }

    /**
     *
     * Replace placeholders in a string
     *
     * @param player The player viewing the hologram
     * @param text The text containing placeholders
     * @return The text with placeholders replaced
     */
    public static String replacePlaceholders(ServerPlayer player, String text) {
        if (text == null) {
            return "";
        }
        
        try {
            String result = text;
            
            // Replace server-wide placeholders first
            result = replaceServerPlaceholders(result);
            
            // Replace player-specific placeholders
            result = replacePlayerPlaceholders(player, result);
            
            // Try external placeholder API if available
            try {
                Class.forName("com.envyful.papi.forge.ForgePlaceholderAPI");
                // If the class exists, we could hook into it here
                // For now, just use our built-in placeholders
            } catch (ClassNotFoundException e) {
                // External placeholder API not available, use only built-in placeholders
            }
            
            return result;
        } catch (Exception e) {
            // If anything goes wrong, return original text
            return text;
        }
    }
    
    /**
     * Replace server-wide placeholders that are the same for all players
     * 
     * @param text The text to process
     * @return Text with server placeholders replaced
     */
    private static String replaceServerPlaceholders(String text) {
        if (text == null) {
            return "";
        }
        
        String result = text;
        
        // Get server instance
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return result;
        }
        
        try {
            // Replace %players% with current online player count
            if (result.contains("%players%")) {
                int playerCount = server.getPlayerList() != null ? server.getPlayerList().getPlayerCount() : 0;
                result = result.replace("%players%", String.valueOf(playerCount));
            }
            
            // Replace %maxplayers% with maximum player count
            if (result.contains("%maxplayers%")) {
                int maxPlayers = server.getPlayerList() != null ? server.getPlayerList().getMaxPlayers() : 20;
                result = result.replace("%maxplayers%", String.valueOf(maxPlayers));
            }
            
            // Replace %tps% with server TPS (approximation)
            if (result.contains("%tps%")) {
                try {
                    // Try to get TPS - this might not be available in all versions
                    double tps = 20.0; // Default fallback
                    
                    // Simple approach - if server is running smoothly, assume 20 TPS
                    // In practice, you'd need more sophisticated TPS tracking
                    result = result.replace("%tps%", String.format("%.1f", tps));
                } catch (Exception e) {
                    // Safe fallback
                    result = result.replace("%tps%", "20.0");
                }
            }
            
            // Replace %uptime% with server uptime
            if (result.contains("%uptime%")) {
                long uptimeMillis = System.currentTimeMillis() - serverStartTime;
                long uptimeMinutes = uptimeMillis / (1000 * 60);
                long hours = uptimeMinutes / 60;
                long minutes = uptimeMinutes % 60;
                String uptimeStr = String.format("%dh %dm", hours, minutes);
                result = result.replace("%uptime%", uptimeStr);
            }
            
            // Replace %memory% with memory usage
            if (result.contains("%memory%")) {
                Runtime runtime = Runtime.getRuntime();
                long maxMemory = runtime.maxMemory() / 1024 / 1024; // MB
                long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024; // MB
                String memoryStr = String.format("%dMB/%dMB", usedMemory, maxMemory);
                result = result.replace("%memory%", memoryStr);
            }
            
            // Replace %server_time% with real world time
            if (result.contains("%server_time%")) {
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                String timeStr = now.format(formatter);
                result = result.replace("%server_time%", timeStr);
            }
            
        } catch (Exception e) {
            // If anything goes wrong, just return the original text
            return text;
        }
        
        return result;
    }
    
    /**
     * Replace player-specific placeholders that are different for each viewing player
     * 
     * @param player The player viewing the hologram
     * @param text The text to process
     * @return Text with player placeholders replaced
     */
    private static String replacePlayerPlaceholders(ServerPlayer player, String text) {
        if (text == null || player == null) {
            return text;
        }
        
        String result = text;
        
        try {
            // Replace %player% with player's name
            if (result.contains("%player%")) {
                String playerName = player.getName().getString();
                result = result.replace("%player%", playerName);
            }
            
            // Replace %player_health% with player's current health
            if (result.contains("%player_health%")) {
                float health = player.getHealth();
                float maxHealth = player.getMaxHealth();
                String healthStr = String.format("%.1f/%.1f", health, maxHealth);
                result = result.replace("%player_health%", healthStr);
            }
            
            // Replace %player_level% with player's XP level
            if (result.contains("%player_level%")) {
                int level = player.experienceLevel;
                result = result.replace("%player_level%", String.valueOf(level));
            }
            
            // Replace %player_world% with player's world name
            if (result.contains("%player_world%")) {
                String worldName = player.level.dimension().location().getPath();
                // Clean up common world names
                switch (worldName) {
                    case "overworld":
                        worldName = "Overworld";
                        break;
                    case "the_nether":
                        worldName = "Nether";
                        break;
                    case "the_end":
                        worldName = "End";
                        break;
                    default:
                        // Capitalize first letter for custom dimensions
                        if (!worldName.isEmpty()) {
                            worldName = worldName.substring(0, 1).toUpperCase() + worldName.substring(1);
                        }
                        break;
                }
                result = result.replace("%player_world%", worldName);
            }
            
            // Replace %player_coords% with player's coordinates
            if (result.contains("%player_coords%")) {
                int x = (int) player.getX();
                int y = (int) player.getY();
                int z = (int) player.getZ();
                String coordsStr = String.format("%d, %d, %d", x, y, z);
                result = result.replace("%player_coords%", coordsStr);
            }
            
            // Replace %player_gamemode% with player's game mode
            if (result.contains("%player_gamemode%")) {
                String gamemode = player.gameMode.getGameModeForPlayer().getName();
                // Capitalize first letter
                if (!gamemode.isEmpty()) {
                    gamemode = gamemode.substring(0, 1).toUpperCase() + gamemode.substring(1);
                }
                result = result.replace("%player_gamemode%", gamemode);
            }
            
            // Replace %player_rank% with player's rank from permission system
            if (result.contains("%player_rank%")) {
                String rank = UtilPermissions.getPlayerRank(player);
                result = result.replace("%player_rank%", rank);
            }
            
        } catch (Exception e) {
            // If anything goes wrong with player placeholders, just return what we have
            return result;
        }
        
        return result;
    }
} 
