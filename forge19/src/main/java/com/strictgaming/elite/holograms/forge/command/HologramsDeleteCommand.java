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
 * Command to delete an existing hologram
 */
public class HologramsDeleteCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = context.getArgument("name", String.class);
        
        Hologram hologram = HologramManager.getById(name);
        
        if (hologram == null) {
            player.sendSystemMessage(Component.literal("§cNo hologram with that name exists!"));
            return 0;
        }
        
        hologram.delete();
        player.sendSystemMessage(Component.literal("§aHologram '" + name + "' deleted successfully!"));
        
        return Command.SINGLE_SUCCESS;
    }
    
    /**
     * Execute command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        if (args.length < 1) {
            context.getSource().sendSystemMessage(Component.literal("§cUsage: /hd delete <id>"));
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        String name = args[0];
        
        Hologram hologram = HologramManager.getById(name);
        
        if (hologram == null) {
            player.sendSystemMessage(Component.literal("§cNo hologram with that name exists!"));
            return 0;
        }
        
        hologram.delete();
        player.sendSystemMessage(Component.literal("§aHologram '" + name + "' deleted successfully!"));
        
        return Command.SINGLE_SUCCESS;
    }
} 
