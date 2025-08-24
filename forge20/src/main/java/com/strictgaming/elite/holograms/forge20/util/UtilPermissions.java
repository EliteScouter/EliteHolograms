package com.strictgaming.elite.holograms.forge20.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// LuckPerms API imports (optional - will be available if LuckPerms is installed)
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.util.Tristate;

/**
 * Utility class for handling permissions with support for permission systems and OP fallback
 */
public class UtilPermissions {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    private static boolean permissionSystemDetected = false;
    private static boolean permissionSystemChecked = false;

    // Permission nodes
    public static final String CREATE = "eliteholograms.create";
    public static final String DELETE = "eliteholograms.delete";
    public static final String LIST = "eliteholograms.list";
    public static final String EDIT = "eliteholograms.edit";
    public static final String TELEPORT = "eliteholograms.teleport";
    public static final String ADMIN = "eliteholograms.admin";
    public static final String INFO = "eliteholograms.info";
    public static final String NEAR = "eliteholograms.near";

    /**
     * Check if a player has permission for a specific action
     * 
     * @param source The command source
     * @param permission The permission node to check
     * @return true if the player has permission
     */
    public static boolean hasPermission(CommandSourceStack source, String permission) {
        if (source == null) {
            return false;
        }

        // Always check OP status first for operators
        if (source.hasPermission(2)) {
            return true;
        }

        // If not OP, check permission systems
        if (hasPermissionSystem()) {
            try {
                return checkPermissionSystem(source, permission);
            } catch (Exception e) {
                LOGGER.debug("Error checking permission system, falling back to OP", e);
            }
        }

        return false;
    }

    /**
     * Check if player has permission to create holograms
     */
    public static boolean canCreate(CommandSourceStack source) {
        return hasPermission(source, CREATE);
    }

    /**
     * Check if player has permission to delete holograms
     */
    public static boolean canDelete(CommandSourceStack source) {
        return hasPermission(source, DELETE);
    }

    /**
     * Check if player has permission to edit holograms (addline, setline, etc.)
     */
    public static boolean canEdit(CommandSourceStack source) {
        return hasPermission(source, EDIT);
    }

    /**
     * Check if player has permission to list holograms
     */
    public static boolean canList(CommandSourceStack source) {
        return hasPermission(source, LIST);
    }

    /**
     * Check if player has permission to teleport to holograms
     */
    public static boolean canTeleport(CommandSourceStack source) {
        return hasPermission(source, TELEPORT);
    }

    /**
     * Check if player has admin permissions (reload, etc.)
     */
    public static boolean canAdmin(CommandSourceStack source) {
        return hasPermission(source, ADMIN);
    }

    /**
     * Check if player has permission to view hologram info
     */
    public static boolean canInfo(CommandSourceStack source) {
        return hasPermission(source, INFO);
    }

    /**
     * Check if player has permission to use near command
     */
    public static boolean canNear(CommandSourceStack source) {
        return hasPermission(source, NEAR);
    }

    /**
     * Check if a permission system is available
     */
    private static boolean hasPermissionSystem() {
        if (!permissionSystemChecked) {
            detectPermissionSystem();
        }
        return permissionSystemDetected;
    }

    /**
     * Detect if any known permission systems are available
     */
    private static void detectPermissionSystem() {
        permissionSystemChecked = true;
        
        if (isLuckPermsActuallyLoaded()) {
            permissionSystemDetected = true;
            LOGGER.info("LuckPerms detected and loaded - using permission nodes");
            return;
        }

        try {
            Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");
            permissionSystemDetected = true;
            LOGGER.info("FTB Ranks detected - using permission nodes");
            return;
        } catch (ClassNotFoundException ignored) {}
        
        LOGGER.info("No permission system detected - using OP fallback");
        permissionSystemDetected = false;
    }

    /**
     * Check if LuckPerms is actually loaded and working (not just classes present)
     */
    private static boolean isLuckPermsActuallyLoaded() {
        // First ensure the provider class exists to avoid NoClassDefFoundError when LuckPerms is not installed
        try {
            Class.forName("net.luckperms.api.LuckPermsProvider", false, UtilPermissions.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Throwable t) {
            LOGGER.debug("LuckPermsProvider class check failed", t);
            return false;
        }
        // Then try obtaining the API via reflection to avoid hard linkage issues
        try {
            Class<?> provider = Class.forName("net.luckperms.api.LuckPermsProvider");
            Object api = provider.getMethod("get").invoke(null);
            return api != null;
        } catch (Throwable t) {
            LOGGER.debug("Error checking LuckPerms availability", t);
            return false;
        }
    }

    /**
     * Check permission using detected permission system
     */
    private static boolean checkPermissionSystem(CommandSourceStack source, String permission) {
        if (!(source.getEntity() instanceof ServerPlayer)) {
            return false;
        }

        ServerPlayer player = (ServerPlayer) source.getEntity();

        if (checkLuckPerms(player, permission)) {
            return true;
        }

        if (checkFTBRanks(player, permission)) {
            return true;
        }

        return false;
    }

    /**
     * Check LuckPerms permission
     */
    private static boolean checkLuckPerms(ServerPlayer player, String permission) {
        try {
            // Additional safety check to prevent NoClassDefFoundError
            if (!isLuckPermsActuallyLoaded()) {
                return false;
            }

            LuckPerms api = LuckPermsProvider.get();
            if (api == null) {
                return false;
            }

            User user = api.getUserManager().getUser(player.getUUID());

            if (user == null) {
                return false;
            }

            Tristate result = user.getCachedData().getPermissionData().checkPermission(permission);
            return result == Tristate.TRUE;
        } catch (NoClassDefFoundError e) {
            LOGGER.debug("LuckPerms classes not available despite detection", e);
            return false;
        } catch (Exception e) {
            LOGGER.debug("Error checking LuckPerms permission for player " + player.getName().getString(), e);
            return false;
        }
    }

    /**
     * Check FTB Ranks permission
     */
    private static boolean checkFTBRanks(ServerPlayer player, String permission) {
        try {
            Class<?> ftbRanksClass = Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");
            Object api = ftbRanksClass.getMethod("getApi").invoke(null);
            
            boolean hasPermission = (boolean) api.getClass().getMethod("hasPermission", ServerPlayer.class, String.class).invoke(api, player, permission);
            return hasPermission;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get player's rank/group name from permission systems
     * 
     * @param player The player to get rank for
     * @return The player's rank name or fallback
     */
    public static String getPlayerRank(ServerPlayer player) {
        if (player == null) {
            return "Unknown";
        }

        if (hasPermissionSystem()) {
            String luckPermsRank = getLuckPermsRank(player);
            if (luckPermsRank != null && !luckPermsRank.isEmpty()) {
                return luckPermsRank;
            }
            
            String ftbRank = getFTBRank(player);
            if (ftbRank != null && !ftbRank.isEmpty()) {
                return ftbRank;
            }
        }
        
        CommandSourceStack source = player.createCommandSourceStack();
        return source.hasPermission(2) ? "OP" : "Player";
    }

    /**
     * Get player's primary group from LuckPerms
     */
    private static String getLuckPermsRank(ServerPlayer player) {
        if (!isLuckPermsActuallyLoaded()) {
            return null;
        }
        try {
            // Additional safety check to prevent NoClassDefFoundError
            LuckPerms api = LuckPermsProvider.get();
            if (api == null) {
                return null;
            }

            User user = api.getUserManager().getUser(player.getUUID());

            if (user == null) {
                return null;
            }

            String primaryGroup = user.getPrimaryGroup();

            if (primaryGroup != null && !primaryGroup.isEmpty()) {
                return primaryGroup.substring(0, 1).toUpperCase() + primaryGroup.substring(1).toLowerCase();
            }

            return null;
        } catch (NoClassDefFoundError e) {
            LOGGER.debug("LuckPerms classes not available despite detection", e);
            return null;
        } catch (Exception e) {
            LOGGER.debug("Error getting LuckPerms rank for player " + player.getName().getString(), e);
            return null;
        }
    }

    /**
     * Get player's rank from FTB Ranks
     */
    private static String getFTBRank(ServerPlayer player) {
        try {
            Class<?> ftbRanksClass = Class.forName("dev.ftb.mods.ftbranks.api.FTBRanksAPI");
            Object api = ftbRanksClass.getMethod("getApi").invoke(null);
            
            Object playerData = api.getClass().getMethod("getPlayerData", java.util.UUID.class).invoke(api, player.getUUID());
            if (playerData != null) {
                Object rank = playerData.getClass().getMethod("getRank").invoke(playerData);
                if (rank != null) {
                    String rankName = (String) rank.getClass().getMethod("getName").invoke(rank);
                    if (rankName != null && !rankName.isEmpty()) {
                        return rankName;
                    }
                }
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }
} 