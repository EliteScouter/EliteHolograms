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
 * Command to teleport the player to a hologram's location
 */
public class HologramsTeleportCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        System.out.println("Executing teleport command with args: " + String.join(", ", args));
        
        if (args.length < 1) {
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUsage: /hd teleport <id>"));
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = args[0];
        
        Hologram hologram = HologramManager.getById(id);
        
        if (hologram == null) {
            player.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cHologram with ID '&f" + id + "&c' not found!"));
            return 0;
        }
        
        if (!(hologram instanceof ForgeHologram)) {
            player.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cCannot teleport to this type of hologram!"));
            return 0;
        }
        
        ForgeHologram forgeHologram = (ForgeHologram) hologram;
        
        // Check if hologram is in the same world
        if (!forgeHologram.getWorld().equals(player.level)) {
            player.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cHologram is in a different world!"));
            return 0;
        }
        
        // Teleport the player to the hologram
        Vec3 pos = forgeHologram.getPosition();
        player.teleportTo(pos.x, pos.y, pos.z);
        
        player.sendSystemMessage(UtilChatColour.parse("&a&l(!) &aTeleported to hologram '&f" + id + "&a'!"));
        
        return Command.SINGLE_SUCCESS;
    }
} 
