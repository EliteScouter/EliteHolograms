package com.envyful.papi.api.util;

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
    public static String replaceIdentifiers(ServerPlayer player, String text) {
        if (text == null) {
            return "";
        }
        
        // In a real implementation, this would use the PlaceholderAPI
        // Since we're just mocking this class, we'll return the original text
        return text;
    }
} 