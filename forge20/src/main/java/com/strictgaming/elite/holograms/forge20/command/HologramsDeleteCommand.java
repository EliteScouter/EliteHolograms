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
 * Command to delete a hologram
 */
public class HologramsDeleteCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = context.getArgument("id", String.class);
        
        Hologram hologram = HologramManager.getById(name);
        
        if (hologram == null) {
            player.sendSystemMessage(Component.literal("§cA hologram with that name doesn't exist!"));
            return 0;
        }
        
        hologram.delete();
        player.sendSystemMessage(Component.literal("§aHologram '" + name + "' deleted successfully!"));
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        if (args.length < 1) {
            context.getSource().sendSystemMessage(Component.literal("§cUsage: /eh delete <id>"));
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = args[0];
        
        Hologram hologram = HologramManager.getById(name);
        
        if (hologram == null) {
            player.sendSystemMessage(Component.literal("§cA hologram with that name doesn't exist!"));
            return 0;
        }
        
        hologram.delete();
        player.sendSystemMessage(Component.literal("§aHologram '" + name + "' deleted successfully!"));
        
        return Command.SINGLE_SUCCESS;
    }
} 