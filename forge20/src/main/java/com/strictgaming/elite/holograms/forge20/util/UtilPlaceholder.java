package com.strictgaming.elite.holograms.forge20.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 *
 * Utility for placeholder replacement
 *
 */
public class UtilPlaceholder {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.#");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    // Track server start time for uptime calculation
    private static long serverStartTime = System.currentTimeMillis();
    
    /**
     * Set the server start time (called when server starts)
     */
    public static void setServerStartTime() {
        serverStartTime = System.currentTimeMillis();
        LOGGER.debug("Server start time set for uptime tracking");
    }

    /**
     * Replace placeholders in a string
     *
     * @param player The player viewing the hologram (null for server-only placeholders)
     * @param text The text containing placeholders
     * @return The text with placeholders replaced
     */
    public static String replacePlaceholders(ServerPlayer player, String text) {
        if (text == null) {
            return "";
        }
        
        String result = text;
        
        try {
            // Server placeholders (available to all players)
            result = replaceServerPlaceholders(result);
            
            // Player-specific placeholders (only if player is provided)
            if (player != null) {
                result = replacePlayerPlaceholders(player, result);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error replacing placeholders in text: " + text, e);
            // Return original text if placeholder replacement fails
            return text;
        }
        
        return result;
    }
    
    /**
     * Replace server-wide placeholders
     */
    private static String replaceServerPlaceholders(String text) {
        try {
            // Player count placeholders
            if (text.contains("%players%")) {
                int playerCount = getOnlinePlayerCount();
                text = text.replace("%players%", String.valueOf(playerCount));
            }
            
            if (text.contains("%maxplayers%")) {
                int maxPlayers = getMaxPlayerCount();
                text = text.replace("%maxplayers%", String.valueOf(maxPlayers));
            }
            
            // Server performance placeholders
            if (text.contains("%tps%")) {
                String tps = getServerTPS();
                text = text.replace("%tps%", tps);
            }
            
            // Server uptime
            if (text.contains("%uptime%")) {
                String uptime = getServerUptime();
                text = text.replace("%uptime%", uptime);
            }
            
            // Memory usage
            if (text.contains("%memory%")) {
                String memory = getMemoryUsage();
                text = text.replace("%memory%", memory);
            }
            
            // Server time
            if (text.contains("%server_time%")) {
                String time = getCurrentTime();
                text = text.replace("%server_time%", time);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error replacing server placeholders", e);
        }
        
        return text;
    }
    
    /**
     * Replace player-specific placeholders
     */
    private static String replacePlayerPlaceholders(ServerPlayer player, String text) {
        try {
            // Player name
            if (text.contains("%player%")) {
                String playerName = player.getName().getString();
                text = text.replace("%player%", playerName);
            }
            
            // Player health
            if (text.contains("%player_health%")) {
                String health = getPlayerHealth(player);
                text = text.replace("%player_health%", health);
            }
            
            // Player level
            if (text.contains("%player_level%")) {
                int level = player.experienceLevel;
                text = text.replace("%player_level%", String.valueOf(level));
            }
            
            // Player world
            if (text.contains("%player_world%")) {
                String world = getPlayerWorld(player);
                text = text.replace("%player_world%", world);
            }
            
            // Player coordinates
            if (text.contains("%player_coords%")) {
                String coords = getPlayerCoordinates(player);
                text = text.replace("%player_coords%", coords);
            }
            
            // Player gamemode
            if (text.contains("%player_gamemode%")) {
                String gamemode = getPlayerGamemode(player);
                text = text.replace("%player_gamemode%", gamemode);
            }
            
            // Player rank
            if (text.contains("%player_rank%")) {
                String rank = UtilPermissions.getPlayerRank(player);
                text = text.replace("%player_rank%", rank);
            }
            
        } catch (Exception e) {
            LOGGER.error("Error replacing player placeholders for player: " + player.getName().getString(), e);
        }
        
        return text;
    }
    
    /**
     * Get current online player count
     */
    private static int getOnlinePlayerCount() {
        try {
            if (ServerLifecycleHooks.getCurrentServer() != null) {
                return ServerLifecycleHooks.getCurrentServer().getPlayerCount();
            }
        } catch (Exception e) {
            LOGGER.error("Error getting online player count", e);
        }
        return 0;
    }
    
    /**
     * Get maximum player count
     */
    private static int getMaxPlayerCount() {
        try {
            if (ServerLifecycleHooks.getCurrentServer() != null) {
                return ServerLifecycleHooks.getCurrentServer().getMaxPlayers();
            }
        } catch (Exception e) {
            LOGGER.error("Error getting max player count", e);
        }
        return 20; // Default fallback
    }
    
    /**
     * Get server TPS (simplified)
     */
    private static String getServerTPS() {
        try {
            // In a real implementation, you'd calculate actual TPS
            // For now, return a default value
            return "20.0";
        } catch (Exception e) {
            LOGGER.error("Error getting server TPS", e);
            return "20.0";
        }
    }
    
    /**
     * Get server uptime
     */
    private static String getServerUptime() {
        try {
            long uptimeMs = System.currentTimeMillis() - serverStartTime;
            long uptimeSeconds = uptimeMs / 1000;
            
            long hours = uptimeSeconds / 3600;
            long minutes = (uptimeSeconds % 3600) / 60;
            long seconds = uptimeSeconds % 60;
            
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } catch (Exception e) {
            LOGGER.error("Error getting server uptime", e);
            return "00:00:00";
        }
    }
    
    /**
     * Get memory usage
     */
    private static String getMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory() / 1024 / 1024; // MB
            long totalMemory = runtime.totalMemory() / 1024 / 1024; // MB
            long freeMemory = runtime.freeMemory() / 1024 / 1024; // MB
            long usedMemory = totalMemory - freeMemory;
            
            return usedMemory + "/" + maxMemory + " MB";
        } catch (Exception e) {
            LOGGER.error("Error getting memory usage", e);
            return "Unknown";
        }
    }
    
    /**
     * Get current time
     */
    private static String getCurrentTime() {
        try {
            return LocalTime.now().format(TIME_FORMAT);
        } catch (Exception e) {
            LOGGER.error("Error getting current time", e);
            return "00:00:00";
        }
    }
    
    /**
     * Get player health
     */
    private static String getPlayerHealth(ServerPlayer player) {
        try {
            float health = player.getHealth();
            float maxHealth = player.getMaxHealth();
            return DECIMAL_FORMAT.format(health) + "/" + DECIMAL_FORMAT.format(maxHealth);
        } catch (Exception e) {
            LOGGER.error("Error getting player health", e);
            return "20.0/20.0";
        }
    }
    
    /**
     * Get player world name
     */
    private static String getPlayerWorld(ServerPlayer player) {
        try {
            String worldKey = player.level().dimension().location().toString();
            
            // Convert technical names to friendly names
            switch (worldKey) {
                case "minecraft:overworld":
                    return "Overworld";
                case "minecraft:the_nether":
                    return "Nether";
                case "minecraft:the_end":
                    return "End";
                default:
                    // For custom dimensions, return the namespace:path format but cleaned up
                    if (worldKey.contains(":")) {
                        String[] parts = worldKey.split(":");
                        if (parts.length == 2) {
                            return parts[1].substring(0, 1).toUpperCase() + parts[1].substring(1);
                        }
                    }
                    return worldKey;
            }
        } catch (Exception e) {
            LOGGER.error("Error getting player world", e);
            return "Unknown";
        }
    }
    
    /**
     * Get player coordinates
     */
    private static String getPlayerCoordinates(ServerPlayer player) {
        try {
            int x = (int) player.getX();
            int y = (int) player.getY();
            int z = (int) player.getZ();
            return x + ", " + y + ", " + z;
        } catch (Exception e) {
            LOGGER.error("Error getting player coordinates", e);
            return "0, 0, 0";
        }
    }
    
    /**
     * Get player gamemode
     */
    private static String getPlayerGamemode(ServerPlayer player) {
        try {
            GameType gameType = player.gameMode.getGameModeForPlayer();
            switch (gameType) {
                case SURVIVAL:
                    return "Survival";
                case CREATIVE:
                    return "Creative";
                case ADVENTURE:
                    return "Adventure";
                case SPECTATOR:
                    return "Spectator";
                default:
                    return "Unknown";
            }
        } catch (Exception e) {
            LOGGER.error("Error getting player gamemode", e);
            return "Unknown";
        }
    }
} 