package com.strictgaming.elite.holograms.forge.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command to add a line to an existing hologram
 */
public class HologramsAddLineCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = context.getArgument("name", String.class);
        String text = context.getArgument("text", String.class);
        
        Hologram hologram = HologramManager.getById(name);
        
        if (hologram == null) {
            player.sendSystemMessage(Component.literal("§cNo hologram with that name exists!"));
            return 0;
        }
        
        hologram.addLine(text);
        player.sendSystemMessage(Component.literal("§aAdded line to hologram '" + name + "'!"));
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Execute command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        if (args.length < 2) {
            context.getSource().sendSystemMessage(Component.literal("§cUsage: /hd addline <id> <text>"));
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = args[0];
        
        StringBuilder text = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            text.append(args[i]).append(" ");
        }
        
        Hologram hologram = HologramManager.getById(name);
        
        if (hologram == null) {
            player.sendSystemMessage(Component.literal("§cNo hologram with that name exists!"));
            return 0;
        }
        
        hologram.addLine(text.toString().trim());
        player.sendSystemMessage(Component.literal("§aAdded line to hologram '" + name + "'!"));
        
        return Command.SINGLE_SUCCESS;
    }
} 
