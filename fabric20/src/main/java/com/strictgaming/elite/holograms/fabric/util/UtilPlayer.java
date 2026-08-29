package com.strictgaming.elite.holograms.fabric.util;

import net.minecraft.server.level.ServerPlayer;

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
        if (uuid == null || UtilServer.get() == null) {
            return null;
        }
        
        return UtilServer.get().getPlayerList().getPlayer(uuid);
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

        return UtilServer.get().getPlayerList().getPlayerByName(name);
    }
} 