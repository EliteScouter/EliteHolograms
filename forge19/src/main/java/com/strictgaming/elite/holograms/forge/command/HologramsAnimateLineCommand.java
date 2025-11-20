package com.strictgaming.elite.holograms.forge.command;

import com.strictgaming.elite.holograms.forge.hologram.ForgeHologram;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;

/**
 * Command to make a hologram line animated (cycle through multiple text frames)
 * Usage: /eh animateline <id> <line> <interval> <text1>|<text2>|<text3>
 */
public class HologramsAnimateLineCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        // Not used - we use executeCommand instead
        return 0;
    }
    
    /**
     * Execute command with given arguments
     * Format: /eh animateline <id> <line> <interval> <frames>
     * Frames are separated by | character
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        if (args.length < 4) {
            context.getSource().sendSystemMessage(Component.literal("§c§lUsage: §r§c/eh animateline <id> <line> <interval> <frame1>|<frame2>|<frame3>"));
            context.getSource().sendSystemMessage(Component.literal("§7Example: /eh animateline welcome 1 3 &aHello!|&bWelcome!|&cHi there!"));
            context.getSource().sendSystemMessage(Component.literal("§7Interval is in seconds between frames"));
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = args[0];
        
        // Parse line number
        int lineNumber;
        try {
            lineNumber = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§cLine number must be a valid number!"));
            return 0;
        }
        
        // Parse interval
        int interval;
        try {
            interval = Integer.parseInt(args[2]);
            if (interval < 1) {
                player.sendSystemMessage(Component.literal("§cInterval must be at least 1 second!"));
                return 0;
            }
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§cInterval must be a valid number (seconds)!"));
            return 0;
        }
        
        // Get hologram
        ForgeHologram hologram = (ForgeHologram) HologramManager.getById(id);
        if (hologram == null) {
            player.sendSystemMessage(Component.literal("§cNo hologram with ID '" + id + "' exists!"));
            return 0;
        }
        
        // Parse frames (rest of args joined and split by |)
        StringBuilder framesText = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            framesText.append(args[i]);
            if (i < args.length - 1) {
                framesText.append(" ");
            }
        }
        
        String[] frameArray = framesText.toString().split("\\|");
        if (frameArray.length < 2) {
            player.sendSystemMessage(Component.literal("§cYou must provide at least 2 frames separated by | character!"));
            player.sendSystemMessage(Component.literal("§7Example: &aFrame 1|&bFrame 2|&cFrame 3"));
            return 0;
        }
        
        List<String> frames = Arrays.asList(frameArray);
        
        try {
            hologram.setLineAnimated(lineNumber, frames, interval);
            player.sendSystemMessage(Component.literal("§aLine " + lineNumber + " of hologram '" + id + "' is now animated!"));
            player.sendSystemMessage(Component.literal("§7" + frames.size() + " frames, " + interval + " second interval"));
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
        
        return Command.SINGLE_SUCCESS;
    }
}

