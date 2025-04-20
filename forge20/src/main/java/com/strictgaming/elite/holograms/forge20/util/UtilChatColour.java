package com.strictgaming.elite.holograms.forge20.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 *
 * A simple utility class for handling colors in chat.
 *
 */
public class UtilChatColour {

    /**
     *
     * Translate the color codes in the given message using the given color code prefix
     *
     * @param altColorChar The color code prefix
     * @param message The message
     * @return The translated message
     */
    public static String translateColourCodes(char altColorChar, String message) {
        if (message == null) {
            return null;
        }

        char[] b = message.toCharArray();

        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
                b[i] = '\u00A7';
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }

        return new String(b);
    }

    /**
     *
     * Translates the color codes in the given message using the ampersand as the color code prefix
     *
     * @param message The message
     * @return The translated message
     */
    public static String colour(String message) {
        return translateColourCodes('&', message);
    }

    /**
     *
     * Translates the color codes in the given message to a Component
     *
     * @param message The message
     * @return The translated Component
     */
    public static Component colour(String prefix, String message) {
        return Component.literal(translateColourCodes(prefix.charAt(0), message));
    }

    /**
     *
     * Convert a string to a component with color codes
     *
     * @param message The message
     * @return The component
     */
    public static MutableComponent parse(String message) {
        return Component.literal(colour(message));
    }
} 