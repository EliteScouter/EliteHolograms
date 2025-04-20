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
 * Command to move a hologram to the player's current location
 */
public class HologramsMoveHereCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        if (args.length < 1) {
            context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cUsage: /eh movehere <id>"));
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = args[0];
        
        Hologram hologram = HologramManager.getById(id);
        
        if (hologram == null) {
            player.sendSystemMessage(Component.literal("§c§l(!) §cHologram with ID '§f" + id + "§c' not found!"));
            return 0;
        }
        
        try {
            // Get player's position
            double x = player.getX();
            double y = player.getY() - 0.5; // Position below player's eye level
            double z = player.getZ();
            
            // Move the hologram to the player's location
            hologram.teleport(player.level().dimension().location().toString(), x, y, z);
            
            player.sendSystemMessage(Component.literal("§a§l(!) §aMoved hologram '§f" + id + "§a' to your location!"));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§c§l(!) §cError moving hologram: §f" + e.getMessage()));
            return 0;
        }
    }
} 