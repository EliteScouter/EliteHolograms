package com.strictgaming.elite.holograms.forge.command;

import com.strictgaming.elite.holograms.forge.hologram.ForgeHologram;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Command to create a hologram at specific coordinates
 * Usage: /eh createat <id> <x> <y> <z> [world] <text>
 */
public class HologramsCreateAtCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        if (args.length < 4) {
            context.getSource().sendSystemMessage(Component.literal("§cUsage: /eh createat <id> <x> <y> <z> [world] <text>"));
            return 0;
        }
        
        String name = args[0];
        
        if (HologramManager.getById(name) != null) {
            context.getSource().sendSystemMessage(Component.literal("§cA hologram with that name already exists!"));
            return 0;
        }
        
        // Parse coordinates
        double x, y, z;
        try {
            x = Double.parseDouble(args[1]);
            y = Double.parseDouble(args[2]);
            z = Double.parseDouble(args[3]);
        } catch (NumberFormatException e) {
            context.getSource().sendSystemMessage(Component.literal("§cInvalid coordinates! Must be numbers."));
            return 0;
        }
        
        // Check if args[4] is a world name or text
        String worldName;
        int textStartIndex;
        
        if (args.length >= 5 && (args[4].contains(":") || args[4].startsWith("minecraft"))) {
            worldName = args[4];
            textStartIndex = 5;
        } else {
            worldName = context.getSource().getLevel().dimension().location().toString();
            textStartIndex = 4;
        }
        
        // Build text from remaining args
        String text;
        if (args.length > textStartIndex) {
            StringBuilder textBuilder = new StringBuilder();
            for (int i = textStartIndex; i < args.length; i++) {
                textBuilder.append(args[i]).append(" ");
            }
            text = textBuilder.toString().trim();
        } else {
            text = "§eEdit this hologram with /eh addline " + name + " <text>";
        }
        
        // Create hologram at specified coordinates
        ForgeHologram hologram = new ForgeHologram(name, 
            context.getSource().getServer().getLevel(context.getSource().getLevel().dimension()), 
            new Vec3(x, y, z), 30, true, text);
        
        context.getSource().sendSystemMessage(Component.literal("§aHologram '" + name + "' created at " + x + ", " + y + ", " + z + "!"));
        
        return Command.SINGLE_SUCCESS;
    }
}
