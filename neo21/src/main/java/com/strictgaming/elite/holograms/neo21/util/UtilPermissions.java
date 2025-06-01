package com.strictgaming.elite.holograms.neo21.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    public static boolean canCreate(CommandSourceStack source) {
        return hasPermission(source, CREATE);
    }

    public static boolean canDelete(CommandSourceStack source) {
        return hasPermission(source, DELETE);
    }

    public static boolean canEdit(CommandSourceStack source) {
        return hasPermission(source, EDIT);
    }

    public static boolean canList(CommandSourceStack source) {
        return hasPermission(source, LIST);
    }

    public static boolean canTeleport(CommandSourceStack source) {
        return hasPermission(source, TELEPORT);
    }

    public static boolean canAdmin(CommandSourceStack source) {
        return hasPermission(source, ADMIN);
    }

    public static boolean canInfo(CommandSourceStack source) {
        return hasPermission(source, INFO);
    }

    public static boolean canNear(CommandSourceStack source) {
        return hasPermission(source, NEAR);
    }

    private static boolean hasPermissionSystem() {
        if (!permissionSystemChecked) {
            detectPermissionSystem();
        }
        return permissionSystemDetected;
    }

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

    private static boolean isLuckPermsActuallyLoaded() {
        try {
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
            Object api = luckPermsClass.getMethod("getApi").invoke(null);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

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

    private static boolean checkLuckPerms(ServerPlayer player, String permission) {
        try {
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
            Object api = luckPermsClass.getMethod("getApi").invoke(null);
            
            Class<?> userManagerClass = Class.forName("net.luckperms.api.model.user.UserManager");
            Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
            
            Object user = userManagerClass.getMethod("getUser", java.util.UUID.class).invoke(userManager, player.getUUID());
            if (user == null) {
                return false;
            }
            
            Object permissionData = user.getClass().getMethod("getCachedData").invoke(user);
            Object checkResult = permissionData.getClass().getMethod("checkPermission", String.class).invoke(permissionData, permission);
            
            return "TRUE".equals(checkResult.toString());
        } catch (Exception e) {
            return false;
        }
    }

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

    private static String getLuckPermsRank(ServerPlayer player) {
        try {
            Class<?> luckPermsClass = Class.forName("net.luckperms.api.LuckPerms");
            Object api = luckPermsClass.getMethod("getApi").invoke(null);
            
            Class<?> userManagerClass = Class.forName("net.luckperms.api.model.user.UserManager");
            Object userManager = api.getClass().getMethod("getUserManager").invoke(api);
            
            Object user = userManagerClass.getMethod("getUser", java.util.UUID.class).invoke(userManager, player.getUUID());
            if (user == null) {
                return null;
            }
            
            String primaryGroup = (String) user.getClass().getMethod("getPrimaryGroup").invoke(user);
            
            if (primaryGroup != null && !primaryGroup.isEmpty()) {
                return primaryGroup.substring(0, 1).toUpperCase() + primaryGroup.substring(1).toLowerCase();
            }
            
            return null;
        } catch (Exception e) {
            return null;
        }
    }

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