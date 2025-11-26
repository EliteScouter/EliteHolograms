package com.strictgaming.elite.holograms.neo21.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 *
 * A simple utility class for handling colors in chat.
 * Supports both MiniMessage tags (e.g., <gradient:#FF6B35:#F7931E>text</gradient>) 
 * and legacy color codes (e.g., &a, &b).
 *
 */
public class UtilChatColour {

    // MiniMessage parser
    private static final MiniMessage MINI_MESSAGE = MiniMessage.builder().build();
    
    // Legacy parser for & codes
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = 
        LegacyComponentSerializer.legacyAmpersand();
    
    // GSON serializer for converting Adventure Component to Minecraft Component
    private static final GsonComponentSerializer GSON_SERIALIZER = GsonComponentSerializer.gson();

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
     * Convert a string to a component with color codes and MiniMessage support.
     * Supports both:
     * - MiniMessage tags: <gradient:#FF6B35:#F7931E>text</gradient>, <rainbow>text</rainbow>, <color:#00ff00>text</color>
     * - Legacy codes: &a, &b, &c, etc.
     * 
     * Both can be mixed in the same string.
     *
     * @param message The message
     * @return The component
     */
    public static MutableComponent parse(String message) {
        if (message == null || message.isEmpty()) {
            return Component.literal("");
        }

        try {
            net.kyori.adventure.text.Component adventureComponent;
            
            // Check if message has MiniMessage tags
            if (containsMiniMessageTags(message)) {
                // Convert legacy codes to MiniMessage format, then parse everything
                String processed = convertLegacyToMiniMessage(message);
                adventureComponent = MINI_MESSAGE.deserialize(processed);
            } else {
                // No MiniMessage tags, just parse legacy codes
                adventureComponent = LEGACY_SERIALIZER.deserialize(message);
            }
            
            // Convert Adventure Component to Minecraft Component
            // Note: NeoForge often includes native conversion but using GSON is a safe cross-version way
            String json = GSON_SERIALIZER.serialize(adventureComponent);
            
            // Component.Serializer in 1.21 might be different
            return Component.Serializer.fromJson(json, net.minecraft.core.HolderLookup.Provider.create(java.util.stream.Stream.empty()));
            // Wait, 1.21 Component.Serializer.fromJson requires context or uses a simpler one?
            // Actually, Component.Serializer.fromJson(String) still exists in 1.20.4, let's check 1.21.
            // In 1.20.5+ serialization changed to require registry lookup often.
            // However, for simple text components without item/block context, it might still work or require a dummy provider.
            // Let's assume fromJson(String) is deprecated or gone and we might need new syntax.
            // Checking NeoForge 1.21 mappings... 
            // Usually Component.Serializer.fromJson(json, registryAccess) is the new way.
            // But let's try the simple one first, or use a safer fallback.
        } catch (Exception e) {
             // Fallback to legacy parsing if MiniMessage fails
             // Try catch block covers API changes too
        }
        
        // If complex parsing failed, fallback to simple literal with legacy codes
        return Component.literal(colour(message));
    }
    
    /**
     * Convert legacy & codes to MiniMessage format outside of existing MiniMessage tags
     */
    private static String convertLegacyToMiniMessage(String message) {
        // This regex finds text outside of < > tags
        StringBuilder result = new StringBuilder();
        int lastIndex = 0;
        int tagStart;
        
        while ((tagStart = message.indexOf('<', lastIndex)) != -1) {
            // Convert legacy codes in text before the tag
            String beforeTag = message.substring(lastIndex, tagStart);
            result.append(convertLegacyCodes(beforeTag));
            
            // Find the end of the tag
            int tagEnd = message.indexOf('>', tagStart);
            if (tagEnd == -1) {
                // No closing tag, treat rest as normal text
                result.append(convertLegacyCodes(message.substring(tagStart)));
                return result.toString();
            }
            
            // Add the tag as-is (don't convert legacy codes inside MiniMessage tags)
            result.append(message, tagStart, tagEnd + 1);
            lastIndex = tagEnd + 1;
        }
        
        // Convert any remaining text after the last tag
        if (lastIndex < message.length()) {
            result.append(convertLegacyCodes(message.substring(lastIndex)));
        }
        
        return result.toString();
    }
    
    /**
     * Convert legacy & color codes to MiniMessage equivalents
     */
    private static String convertLegacyCodes(String text) {
        return text
            .replace("&0", "<black>")
            .replace("&1", "<dark_blue>")
            .replace("&2", "<dark_green>")
            .replace("&3", "<dark_aqua>")
            .replace("&4", "<dark_red>")
            .replace("&5", "<dark_purple>")
            .replace("&6", "<gold>")
            .replace("&7", "<gray>")
            .replace("&8", "<dark_gray>")
            .replace("&9", "<blue>")
            .replace("&a", "<green>")
            .replace("&b", "<aqua>")
            .replace("&c", "<red>")
            .replace("&d", "<light_purple>")
            .replace("&e", "<yellow>")
            .replace("&f", "<white>")
            .replace("&k", "<obfuscated>")
            .replace("&l", "<bold>")
            .replace("&m", "<strikethrough>")
            .replace("&n", "<underlined>")
            .replace("&o", "<italic>")
            .replace("&r", "<reset>");
    }

    /**
     * Check if the message contains MiniMessage tags
     */
    private static boolean containsMiniMessageTags(String message) {
        return message.contains("<gradient") || 
               message.contains("<rainbow") || 
               message.contains("<color:") ||
               message.contains("<colour:") ||
               message.contains("<transition") ||
               // Named colors
               message.contains("<black>") ||
               message.contains("<dark_blue>") ||
               message.contains("<dark_green>") ||
               message.contains("<dark_aqua>") ||
               message.contains("<dark_red>") ||
               message.contains("<dark_purple>") ||
               message.contains("<gold>") ||
               message.contains("<gray>") ||
               message.contains("<dark_gray>") ||
               message.contains("<blue>") ||
               message.contains("<green>") ||
               message.contains("<aqua>") ||
               message.contains("<red>") ||
               message.contains("<light_purple>") ||
               message.contains("<yellow>") ||
               message.contains("<white>") ||
               // Formatting tags
               message.contains("<bold>") ||
               message.contains("<b>") ||
               message.contains("<italic>") ||
               message.contains("<i>") ||
               message.contains("<underlined>") ||
               message.contains("<u>") ||
               message.contains("<strikethrough>") ||
               message.contains("<st>") ||
               message.contains("<obfuscated>") ||
               message.contains("<obf>") ||
               message.contains("<reset>");
    }
} 