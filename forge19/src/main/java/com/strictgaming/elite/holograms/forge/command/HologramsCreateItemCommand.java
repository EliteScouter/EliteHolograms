package com.strictgaming.elite.holograms.forge.command;

import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge.hologram.ItemHologram;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command to create an item hologram (displays a floating item with text)
 * Usage: /eh createitem <id> <item> [text...]
 */
public class HologramsCreateItemCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return 0;
    }
    
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        if (args.length < 2) {
            context.getSource().sendSystemMessage(Component.literal("§c§lUsage: §r§c/eh createitem <id> <item> [text...]"));
            context.getSource().sendSystemMessage(Component.literal("§7Example: /eh createitem shop_sword diamond_sword &6&lLegendary Sword|&7Price: &a$500"));
            context.getSource().sendSystemMessage(Component.literal("§7Item format: minecraft:diamond_sword or just diamond_sword"));
            return 0;
        }
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        String id = args[0];
        String itemId = args[1];
        
        // Ensure item ID has namespace
        if (!itemId.contains(":")) {
            itemId = "minecraft:" + itemId;
        }
        
        // Check if hologram already exists
        if (HologramManager.getById(id) != null) {
            player.sendSystemMessage(Component.literal("§cA hologram with ID '" + id + "' already exists!"));
            return 0;
        }
        
        // Parse text lines (optional)
        String[] lines = new String[0];
        if (args.length > 2) {
            StringBuilder textBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                textBuilder.append(args[i]);
                if (i < args.length - 1) {
                    textBuilder.append(" ");
                }
            }
            // Split by | for multiple lines
            lines = textBuilder.toString().split("\\|");
        }
        
        try {
            ItemHologram hologram = new ItemHologram(
                id,
                player.level,
                player.position(),
                32, // Default range
                itemId,
                lines
            );
            
            player.sendSystemMessage(Component.literal("§aItem hologram '" + id + "' created successfully!"));
            if (lines.length > 0) {
                player.sendSystemMessage(Component.literal("§7With " + lines.length + " text line(s)"));
            }
        } catch (Exception e) {
            player.sendSystemMessage(Component.literal("§cError creating item hologram: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
        
        return Command.SINGLE_SUCCESS;
    }
}



