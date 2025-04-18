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
 * Command to copy a hologram to a new location
 */
public class HologramsCopyCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        System.out.println("Executing copy command with args: " + String.join(", ", args));
        
        if (args.length < 2) {
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUsage: /hd copy <source> <new-id>"));
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        String sourceId = args[0];
        String newId = args[1];
        
        Hologram source = HologramManager.getById(sourceId);
        
        if (source == null) {
            player.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cHologram with ID '&f" + sourceId + "&c' not found!"));
            return 0;
        }
        
        if (HologramManager.getById(newId) != null) {
            player.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cA hologram with ID '&f" + newId + "&c' already exists!"));
            return 0;
        }
        
        Vec3 pos = player.position();
        String worldName = player.level.dimension().location().toString();
        
        // Create the copy at the player's location
        Hologram copy = source.copy(newId, worldName, pos.x, pos.y, pos.z);
        
        player.sendSystemMessage(UtilChatColour.parse("&a&l(!) &aHologram '&f" + sourceId + "&a' copied to '&f" + newId + "&a' at your location!"));
        
        return Command.SINGLE_SUCCESS;
    }
} 
