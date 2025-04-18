package com.strictgaming.elite.holograms.forge.command;

import com.strictgaming.elite.holograms.api.exception.HologramException;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command to remove a line from an existing hologram
 */
public class HologramsRemoveLineCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = context.getArgument("name", String.class);
        int lineNumber = context.getArgument("line", Integer.class);
        
        return executeCommand(player, name, lineNumber);
    }
    
    /**
     * Execute the command with the given arguments
     * This method is called by the HologramsCommand class
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        if (args.length < 2) {
            context.getSource().sendSystemMessage(Component.literal("§cUsage: /hd removeline <id> <line>"));
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = args[0];
        int lineNumber;
        
        try {
            lineNumber = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§cInvalid line number: " + args[1]));
            return 0;
        }
        
        return executeCommand(player, name, lineNumber);
    }
    
    /**
     * Common implementation for both entry points
     */
    private int executeCommand(ServerPlayer player, String name, int lineNumber) {
        Hologram hologram = HologramManager.getById(name);
        
        if (hologram == null) {
            player.sendSystemMessage(Component.literal("§cNo hologram with that name exists!"));
            return 0;
        }
        
        try {
            hologram.removeLine(lineNumber);
            player.sendSystemMessage(Component.literal("§aRemoved line " + lineNumber + " from hologram '" + name + "'!"));
            return Command.SINGLE_SUCCESS;
        } catch (HologramException e) {
            player.sendSystemMessage(Component.literal("§c" + e.getMessage()));
            return 0;
        }
    }
} 
