package com.strictgaming.elite.holograms.forge.util;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.UUID;

/**
 *
 * Utility class for player operations
 *
 */
public class UtilPlayer {

    /**
     *
     * Get an online player by UUID
     *
     * @param uuid The UUID of the player
     * @return The player, or null if not found
     */
    public static ServerPlayer getOnlinePlayer(UUID uuid) {
        if (uuid == null || ServerLifecycleHooks.getCurrentServer() == null) {
            return null;
        }
        
        return ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(uuid);
    }

    /**
     *
     * Get an online player by name
     *
     * @param name The name of the player
     * @return The player, or null if not found
     */
    public static ServerPlayer getOnlinePlayer(String name) {
        if (name == null) {
            return null;
        }

        return ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(name);
    }
} 
