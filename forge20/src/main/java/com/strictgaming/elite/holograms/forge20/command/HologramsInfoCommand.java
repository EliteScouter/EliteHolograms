package com.strictgaming.elite.holograms.forge20.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge20.hologram.ForgeHologram;
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
 * Command to display information about a specific hologram
 */
public class HologramsInfoCommand implements Command<CommandSourceStack> {

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
            context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cUsage: /eh info <id>"));
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
            // Get hologram details
            String worldName = hologram.getWorldName();
            double[] location = hologram.getLocation();
            int lineCount = 0;
            
            if (hologram instanceof ForgeHologram) {
                ForgeHologram forgeHologram = (ForgeHologram) hologram;
                lineCount = forgeHologram.getLines().size();
            }
            
            // Display hologram information
            player.sendSystemMessage(Component.literal("§3§l┌─§b§lHologram Info: §f" + id + " §3§l──────┐"));
            player.sendSystemMessage(Component.literal("§3│ §bWorld: §f" + worldName));
            player.sendSystemMessage(Component.literal("§3│ §bLocation: §f" + 
                String.format("%.2f, %.2f, %.2f", location[0], location[1], location[2])));
            player.sendSystemMessage(Component.literal("§3│ §bLines: §f" + lineCount));
            
            // Display line content if available
            if (hologram instanceof ForgeHologram) {
                ForgeHologram forgeHologram = (ForgeHologram) hologram;
                
                if (!forgeHologram.getLines().isEmpty()) {
                    player.sendSystemMessage(Component.literal("§3│"));
                    player.sendSystemMessage(Component.literal("§3│ §b§lLine Content:"));
                    
                    for (int i = 0; i < forgeHologram.getLines().size(); i++) {
                        String lineText = forgeHologram.getLines().get(i).getText();
                        player.sendSystemMessage(Component.literal("§3│ §f" + i + ": §7" + lineText));
                    }
                }
            }
            
            player.sendSystemMessage(Component.literal("§3§l└─────────────────┘"));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            LOGGER.error("Error displaying hologram info", e);
            player.sendSystemMessage(Component.literal("§c§l(!) §cError displaying hologram info: §f" + e.getMessage()));
            return 0;
        }
    }
} 