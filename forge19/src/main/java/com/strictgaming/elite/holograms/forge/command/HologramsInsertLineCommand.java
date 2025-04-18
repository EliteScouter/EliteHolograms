package com.strictgaming.elite.holograms.forge.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge.util.UtilChatColour;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command to insert a line into a hologram at a specific position
 */
public class HologramsInsertLineCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        System.out.println("Executing insertline command with args: " + String.join(", ", args));
        
        if (args.length < 3) {
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUsage: /hd insertline <id> <line> <text>"));
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = args[0];
        
        int lineNumber;
        try {
            lineNumber = Integer.parseInt(args[1]);
            if (lineNumber < 0) {
                player.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cLine number must be a positive integer!"));
                return 0;
            }
        } catch (NumberFormatException e) {
            player.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cInvalid line number: &f" + args[1]));
            return 0;
        }
        
        StringBuilder text = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            text.append(args[i]).append(" ");
        }
        String lineText = text.toString().trim();
        
        Hologram hologram = HologramManager.getById(id);
        
        if (hologram == null) {
            player.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cHologram with ID '&f" + id + "&c' not found!"));
            return 0;
        }
        
        try {
            hologram.insertLine(lineNumber, lineText);
            player.sendSystemMessage(UtilChatColour.parse("&a&l(!) &aInserted line at position &f" + lineNumber + "&a in hologram '&f" + id + "&a'!"));
        } catch (Exception e) {
            player.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cError inserting line: &f" + e.getMessage()));
            return 0;
        }
        
        return Command.SINGLE_SUCCESS;
    }
} 
