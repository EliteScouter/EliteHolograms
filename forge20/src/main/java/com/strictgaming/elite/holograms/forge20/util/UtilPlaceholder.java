package com.strictgaming.elite.holograms.forge20.util;

import net.minecraft.server.level.ServerPlayer;

/**
 *
 * Utility for placeholder replacement
 *
 */
public class UtilPlaceholder {

    /**
     *
     * Replace placeholders in a string
     *
     * @param player The player
     * @param text The text containing placeholders
     * @return The text with placeholders replaced
     */
    public static String replacePlaceholders(ServerPlayer player, String text) {
        if (text == null) {
            return "";
        }
        
        // In a simple implementation, we'll just return the original text
        // In a real implementation, this would hook into a placeholder API
        return text;
    }
} 