package com.strictgaming.elite.holograms.forge.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.hologram.ForgeHologram;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge.util.UtilChatColour;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Command to move a hologram to the player's location
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
        System.out.println("Executing movehere command with args: " + String.join(", ", args));
        
        if (args.length < 1) {
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUsage: /hd movehere <id>"));
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = args[0];
        
        Hologram hologram = HologramManager.getById(id);
        
        if (hologram == null) {
            player.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cHologram with ID '&f" + id + "&c' not found!"));
            return 0;
        }
        
        Vec3 pos = player.position();
        
        // Get the world name and coordinates
        String worldName = player.level.dimension().location().toString();
        
        // Move the hologram
        hologram.teleport(worldName, pos.x, pos.y, pos.z);
        
        player.sendSystemMessage(UtilChatColour.parse("&a&l(!) &aHologram '&f" + id + "&a' moved to your location!"));
        
        return Command.SINGLE_SUCCESS;
    }
} 
