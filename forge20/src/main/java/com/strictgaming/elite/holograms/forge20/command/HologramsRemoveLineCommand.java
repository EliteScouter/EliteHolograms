package com.strictgaming.elite.holograms.forge20.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command to remove a specific line from a hologram
 */
public class HologramsRemoveLineCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        if (args.length < 2) {
            context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cUsage: /eh removeline <id> <line>"));
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = args[0];
        
        int lineNumber;
        try {
            lineNumber = Integer.parseInt(args[1]);
            if (lineNumber < 0) {
                player.sendSystemMessage(Component.literal("§c§l(!) §cLine number must be a positive integer!"));
                return 0;
            }
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§c§l(!) §cInvalid line number: §f" + args[1]));
            return 0;
        }
        
        Hologram hologram = HologramManager.getById(id);
        
        if (hologram == null) {
            player.sendSystemMessage(Component.literal("§c§l(!) §cHologram with ID '§f" + id + "§c' not found!"));
            return 0;
        }
        
        try {
            hologram.removeLine(lineNumber);
            player.sendSystemMessage(Component.literal("§a§l(!) §aRemoved line §f" + lineNumber + "§a from hologram '§f" + id + "§a'!"));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c§l(!) §cError removing line: §f" + e.getMessage()));
            return 0;
        }
    }
} 