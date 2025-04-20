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
 * Command to set the text of a specific line in a hologram
 */
public class HologramsSetLineCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        if (args.length < 3) {
            context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cUsage: /eh setline <id> <line> <text>"));
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
        
        StringBuilder text = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            text.append(args[i]).append(" ");
        }
        String lineText = text.toString().trim();
        
        Hologram hologram = HologramManager.getById(id);
        
        if (hologram == null) {
            player.sendSystemMessage(Component.literal("§c§l(!) §cHologram with ID '§f" + id + "§c' not found!"));
            return 0;
        }
        
        try {
            hologram.setLine(lineNumber, lineText);
            player.sendSystemMessage(Component.literal("§a§l(!) §aSet line §f" + lineNumber + "§a in hologram '§f" + id + "§a'!"));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c§l(!) §cError setting line: §f" + e.getMessage()));
            return 0;
        }
    }
} 