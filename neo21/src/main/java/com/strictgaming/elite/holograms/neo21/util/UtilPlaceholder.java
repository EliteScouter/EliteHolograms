package com.strictgaming.elite.holograms.neo21.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for handling placeholder replacement in hologram text
 */
public class UtilPlaceholder {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");
    private static long serverStartTime = System.currentTimeMillis();

    /**
     * Set the server start time for uptime calculations
     */
    public static void setServerStartTime() {
        serverStartTime = System.currentTimeMillis();
    }

    /**
     * Replace all placeholders in the given text for a specific player
     * 
     * @param text The text containing placeholders
     * @param player The player to replace player-specific placeholders for (can be null for server-only placeholders)
     * @return The text with placeholders replaced
     */
    public static String replacePlaceholders(String text, ServerPlayer player) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;

        try {
            // Server placeholders (always available)
            result = replaceServerPlaceholders(result);

            // Player-specific placeholders (only when player is provided)
            if (player != null) {
                result = replacePlayerPlaceholders(result, player);
            }
        } catch (Exception e) {
            LOGGER.debug("Error replacing placeholders in text '{}': {}", text, e.getMessage());
        }

        return result;
    }

    /**
     * Replace server-wide placeholders
     */
    private static String replaceServerPlaceholders(String text) {
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return text;
        }

        String result = text;

        try {
            // Player count placeholders
            int playerCount = ServerLifecycleHooks.getCurrentServer().getPlayerCount();
            int maxPlayers = ServerLifecycleHooks.getCurrentServer().getMaxPlayers();
            result = result.replace("%players%", String.valueOf(playerCount));
            result = result.replace("%maxplayers%", String.valueOf(maxPlayers));

            // Server TPS
            double tps = getServerTPS();
            result = result.replace("%tps%", DECIMAL_FORMAT.format(tps));

            // Server uptime
            String uptime = getServerUptime();
            result = result.replace("%uptime%", uptime);

            // Memory usage
            String memoryUsage = getMemoryUsage();
            result = result.replace("%memory%", memoryUsage);

            // Server time
            String serverTime = getCurrentTime();
            result = result.replace("%server_time%", serverTime);

        } catch (Exception e) {
            LOGGER.debug("Error replacing server placeholders: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Replace player-specific placeholders
     */
    private static String replacePlayerPlaceholders(String text, ServerPlayer player) {
        if (player == null) {
            return text;
        }

        String result = text;

        try {
            // Player name
            result = result.replace("%player%", player.getName().getString());

            // Player health
            float health = player.getHealth();
            result = result.replace("%player_health%", DECIMAL_FORMAT.format(health));

            // Player level
            int level = player.experienceLevel;
            result = result.replace("%player_level%", String.valueOf(level));

            // Player world
            String worldName = getWorldName(player);
            result = result.replace("%player_world%", worldName);

            // Player coordinates
            String coordinates = getPlayerCoordinates(player);
            result = result.replace("%player_coords%", coordinates);

            // Player gamemode
            String gamemode = getPlayerGamemode(player);
            result = result.replace("%player_gamemode%", gamemode);

            // Player rank (using permission system)
            String rank = UtilPermissions.getPlayerRank(player);
            result = result.replace("%player_rank%", rank);

        } catch (Exception e) {
            LOGGER.debug("Error replacing player placeholders for {}: {}", player.getName().getString(), e.getMessage());
        }

        return result;
    }

    /**
     * Get current server TPS
     */
    private static double getServerTPS() {
        try {
            if (ServerLifecycleHooks.getCurrentServer() != null) {
                // Use NeoForge's getAverageTickTimeNanos method
                net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
                long averageTickTimeNanos = server.getAverageTickTimeNanos();
                double averageTickTimeMs = averageTickTimeNanos / 1_000_000.0;
                return Math.min(1000.0 / averageTickTimeMs, 20.0);
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting server TPS: {}", e.getMessage());
        }
        return 20.0; // Default to 20 TPS if calculation fails
    }

    /**
     * Calculate mean of tick times - not used in Neo21 due to API differences
     */
    private static long mean(long[] values) {
        long sum = 0L;
        for (long value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    /**
     * Get server uptime as formatted string
     */
    private static String getServerUptime() {
        try {
            long uptimeMillis = System.currentTimeMillis() - serverStartTime;
            long uptimeSeconds = uptimeMillis / 1000;
            
            long days = uptimeSeconds / 86400;
            long hours = (uptimeSeconds % 86400) / 3600;
            long minutes = (uptimeSeconds % 3600) / 60;
            long seconds = uptimeSeconds % 60;
            
            if (days > 0) {
                return String.format("%dd %dh %dm", days, hours, minutes);
            } else if (hours > 0) {
                return String.format("%dh %dm %ds", hours, minutes, seconds);
            } else if (minutes > 0) {
                return String.format("%dm %ds", minutes, seconds);
            } else {
                return String.format("%ds", seconds);
            }
        } catch (Exception e) {
            LOGGER.debug("Error calculating uptime: {}", e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Get memory usage as formatted string
     */
    private static String getMemoryUsage() {
        try {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long usedMemory = totalMemory - freeMemory;
            
            double usedMB = usedMemory / (1024.0 * 1024.0);
            double maxMB = maxMemory / (1024.0 * 1024.0);
            double percentage = (usedMemory * 100.0) / maxMemory;
            
            return String.format("%.0fMB/%.0fMB (%.1f%%)", usedMB, maxMB, percentage);
        } catch (Exception e) {
            LOGGER.debug("Error getting memory usage: {}", e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Get current server time
     */
    private static String getCurrentTime() {
        try {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            return now.format(formatter);
        } catch (Exception e) {
            LOGGER.debug("Error getting current time: {}", e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Get the display name of the world the player is in
     */
    private static String getWorldName(ServerPlayer player) {
        try {
            String worldKey = player.level().dimension().location().toString();
            // Convert resource location to friendly name
            switch (worldKey) {
                case "minecraft:overworld":
                    return "Overworld";
                case "minecraft:the_nether":
                    return "Nether";
                case "minecraft:the_end":
                    return "End";
                default:
                    // For modded dimensions, try to extract a readable name
                    if (worldKey.contains(":")) {
                        String[] parts = worldKey.split(":");
                        if (parts.length > 1) {
                            String name = parts[1].replace("_", " ");
                            return capitalize(name);
                        }
                    }
                    return worldKey;
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting world name for player {}: {}", player.getName().getString(), e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Get player coordinates as formatted string
     */
    private static String getPlayerCoordinates(ServerPlayer player) {
        try {
            int x = (int) Math.floor(player.getX());
            int y = (int) Math.floor(player.getY());
            int z = (int) Math.floor(player.getZ());
            return String.format("%d, %d, %d", x, y, z);
        } catch (Exception e) {
            LOGGER.debug("Error getting coordinates for player {}: {}", player.getName().getString(), e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Get player gamemode as string
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
            LOGGER.debug("Error getting gamemode for player {}: {}", player.getName().getString(), e.getMessage());
            return "Unknown";
        }
    }

    /**
     * Capitalize the first letter of a string
     */
    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
} 