package com.strictgaming.elite.holograms.forge20.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Command to copy an existing hologram with a new ID
 */
public class HologramsCopyCommand implements Command<CommandSourceStack> {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        if (args.length < 2) {
            context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cUsage: /eh copy <source> <new-id>"));
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        String sourceId = args[0];
        String newId = args[1];
        
        // Check if source hologram exists
        Hologram sourceHologram = HologramManager.getById(sourceId);
        if (sourceHologram == null) {
            player.sendSystemMessage(Component.literal("§c§l(!) §cSource hologram '§f" + sourceId + "§c' not found!"));
            return 0;
        }
        
        // Check if target ID already exists
        if (HologramManager.getById(newId) != null) {
            player.sendSystemMessage(Component.literal("§c§l(!) §cA hologram with ID '§f" + newId + "§c' already exists!"));
            return 0;
        }
        
        try {
            // Get player's position
            double x = player.getX();
            double y = player.getY() - 0.5; // Position below player's eye level
            double z = player.getZ();
            
            // Create a copy of the hologram with the new ID
            Hologram newHologram = sourceHologram.copy(newId);
            
            if (newHologram == null) {
                player.sendSystemMessage(Component.literal("§c§l(!) §cFailed to copy hologram!"));
                return 0;
            }
            
            // Move the copied hologram to the player's position
            newHologram.teleport(player.level().dimension().location().toString(), x, y, z);
            
            player.sendSystemMessage(Component.literal("§a§l(!) §aHologram '§f" + sourceId + "§a' copied to '§f" + newId + "§a'!"));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            LOGGER.error("Error copying hologram", e);
            player.sendSystemMessage(Component.literal("§c§l(!) §cError copying hologram: §f" + e.getMessage()));
            return 0;
        }
    }
} 