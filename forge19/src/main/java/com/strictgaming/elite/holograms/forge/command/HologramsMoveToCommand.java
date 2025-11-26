package com.strictgaming.elite.holograms.forge.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge.util.UtilChatColour;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;

/**
 * Command to move a hologram to specific coordinates
 * Usage: /eh moveto <id> <x> <y> <z> [world]
 */
public class HologramsMoveToCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        if (args.length < 4) {
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUsage: /eh moveto <id> <x> <y> <z> [world]"));
            return 0;
        }
        
        String id = args[0];
        
        // Parse coordinates
        double x, y, z;
        try {
            x = Double.parseDouble(args[1]);
            y = Double.parseDouble(args[2]);
            z = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cInvalid coordinates! Must be numbers."));
            return 0;
        }
        
        // Get world name (optional, defaults to command source's world)
        String worldName;
        if (args.length >= 5) {
            worldName = args[4];
        } else {
            worldName = context.getSource().getLevel().dimension().location().toString();
        }
        
        // Find the hologram
        Hologram hologram = HologramManager.getById(id);
        
        if (hologram == null) {
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cHologram with ID '&f" + id + "&c' not found!"));
            return 0;
        }
        
        // Move the hologram
        hologram.teleport(worldName, x, y, z);
        
        context.getSource().sendSystemMessage(UtilChatColour.parse("&a&l(!) &aHologram '&f" + id + "&a' moved to &f" + x + ", " + y + ", " + z + "&a in world &f" + worldName));
        
        return Command.SINGLE_SUCCESS;
    }
}
