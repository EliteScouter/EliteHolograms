package com.strictgaming.elite.holograms.forge.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge.util.UtilChatColour;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;

import java.util.List;

/**
 * Command to reload holograms from storage
 */
public class HologramsReloadCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        System.out.println("Executing reload command with args: " + String.join(", ", args));
        
        CommandSourceStack source = context.getSource();
        
        source.sendSystemMessage(UtilChatColour.parse("&e&l(!) &eReloading holograms..."));
        
        try {
            // Save the current count for reporting
            int oldCount = HologramManager.getAllHolograms().size();
            
            // Despawn all holograms 
            for (Hologram hologram : HologramManager.getAllHolograms()) {
                if (hologram != null) {
                    hologram.despawn();
                }
            }
            
            // Clear all existing holograms
            HologramManager.clear();
            
            // Load holograms from file
            HologramManager.load();
            
            // Get the new count
            int newCount = HologramManager.getAllHolograms().size();
            
            source.sendSystemMessage(UtilChatColour.parse("&a&l(!) &aHolograms reloaded! (&f" + oldCount + " &a→ &f" + newCount + "&a)"));
        } catch (Exception e) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cError reloading holograms: " + e.getMessage()));
            e.printStackTrace();
        }
        
        return Command.SINGLE_SUCCESS;
    }
} 
