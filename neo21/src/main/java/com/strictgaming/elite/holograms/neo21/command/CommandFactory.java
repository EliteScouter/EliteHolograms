package com.strictgaming.elite.holograms.neo21.command;

import com.strictgaming.elite.holograms.neo21.util.UtilChatColour;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * Factory for creating and managing commands
 */
public class CommandFactory {
    
    /**
     * Creates a response component for command output
     * 
     * @param message The message to send
     * @return The formatted component
     */
    public Component createResponse(String message) {
        return UtilChatColour.parse(message);
    }
    
    /**
     * Sends a response to a command source
     * 
     * @param source The command source
     * @param message The message to send
     */
    public void sendResponse(CommandSourceStack source, String message) {
        source.sendSuccess(() -> createResponse(message), false);
    }
    
    /**
     * Sends an error response to a command source
     * 
     * @param source The command source
     * @param message The error message to send
     */
    public void sendErrorResponse(CommandSourceStack source, String message) {
        source.sendFailure(createResponse(message));
    }
} 