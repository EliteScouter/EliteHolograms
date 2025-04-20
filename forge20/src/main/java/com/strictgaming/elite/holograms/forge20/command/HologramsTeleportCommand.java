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
 * Command to teleport a player to a hologram's location
 */
public class HologramsTeleportCommand implements Command<CommandSourceStack> {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        if (args.length < 1) {
            context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cUsage: /eh teleport <id>"));
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
            // Get hologram location
            double[] location = hologram.getLocation();
            String worldName = hologram.getWorldName();
            
            // Check if player is in the same world
            if (!player.level().dimension().location().toString().equals(worldName)) {
                player.sendSystemMessage(Component.literal("§c§l(!) §cCannot teleport to hologram in different world!"));
                return 0;
            }
            
            // Teleport the player to the hologram's location
            player.teleportTo(location[0], location[1], location[2]);
            
            player.sendSystemMessage(Component.literal("§a§l(!) §aTeleported to hologram '§f" + id + "§a'!"));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            LOGGER.error("Error teleporting to hologram", e);
            player.sendSystemMessage(Component.literal("§c§l(!) §cError teleporting to hologram: §f" + e.getMessage()));
            return 0;
        }
    }
} 