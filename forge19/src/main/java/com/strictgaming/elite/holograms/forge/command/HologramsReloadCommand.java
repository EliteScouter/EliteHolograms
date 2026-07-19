package com.strictgaming.elite.holograms.forge.command;

import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge.util.UtilChatColour;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;

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
        CommandSourceStack source = context.getSource();
        
        source.sendSystemMessage(UtilChatColour.parse("&e&l(!) &eReloading holograms..."));
        
        try {
            int oldCount = HologramManager.getAllHolograms().size();

            // Save synchronously before reloading to avoid race condition
            // where async save runs after the map is cleared, writing 0 holograms
            HologramManager.saveSync();

            // load() handles despawn + clear + reload under SAVE_LOAD_LOCK
            HologramManager.load();
            
            int newCount = HologramManager.getAllHolograms().size();
            
            source.sendSystemMessage(UtilChatColour.parse("&a&l(!) &aHolograms reloaded! (&f" + oldCount + " &a→ &f" + newCount + "&a)"));
        } catch (Exception e) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cError reloading holograms: " + e.getMessage()));
            e.printStackTrace();
        }
        
        return Command.SINGLE_SUCCESS;
    }
}
