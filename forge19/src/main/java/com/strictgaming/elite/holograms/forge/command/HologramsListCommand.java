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

import java.util.List;

/**
 * Command to list all holograms on the server
 */
public class HologramsListCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        System.out.println("Executing list command with args: " + String.join(", ", args));
        
        CommandSourceStack source = context.getSource();
        List<Hologram> holograms = HologramManager.getAllHolograms();
        
        if (holograms.isEmpty()) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cThere are no holograms on the server!"));
            return 0;
        }
        
        source.sendSystemMessage(UtilChatColour.parse("&e&l(!) &eHolograms on the server: &f" + holograms.size()));
        
        // Sort holograms alphabetically
        holograms.sort((h1, h2) -> h1.getId().compareToIgnoreCase(h2.getId()));
        
        for (Hologram hologram : holograms) {
            // If we have access to ForgeHologram, show more details
            if (hologram instanceof ForgeHologram) {
                ForgeHologram fh = (ForgeHologram) hologram;
                String worldName = fh.getWorld().dimension().location().toString();
                source.sendSystemMessage(UtilChatColour.parse(
                        String.format("&e- &f%s &7(%s)", 
                        hologram.getId(),
                        worldName)
                ));
            } else {
                source.sendSystemMessage(UtilChatColour.parse("&e- &f" + hologram.getId()));
            }
        }
        
        source.sendSystemMessage(UtilChatColour.parse("&e&l(!) &eUse &f/hd create <id> <text> &eto create a new hologram"));
        
        return Command.SINGLE_SUCCESS;
    }
} 
