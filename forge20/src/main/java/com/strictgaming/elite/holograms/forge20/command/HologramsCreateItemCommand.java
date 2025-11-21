package com.strictgaming.elite.holograms.forge20.command;

import com.mojang.brigadier.context.CommandContext;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge20.hologram.ItemHologram;
import com.strictgaming.elite.holograms.forge20.util.UtilChatColour;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

public class HologramsCreateItemCommand {

    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        CommandSourceStack source = context.getSource();

        if (args.length < 2) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUsage: /eh createitem <id> <item> [text...]"));
            return 0;
        }

        String id = args[0];
        String itemId = args[1];

        if (HologramManager.getById(id) != null) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cHologram with ID '&f" + id + "&c' already exists!"));
            return 0;
        }

        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cYou must be a player to run this command!"));
            return 0;
        }

        Vec3 pos = player.position();
        String[] lines = new String[0];
        
        if (args.length > 2) {
            // Reconstruct the text from remaining args
            StringBuilder textBuilder = new StringBuilder();
            for (int i = 2; i < args.length; i++) {
                textBuilder.append(args[i]).append(" ");
            }
            String fullText = textBuilder.toString().trim();
            
            // Split by | for multiple lines
            lines = fullText.split("\\|");
        }

        new ItemHologram(id, player.serverLevel(), pos, 48, itemId, lines);

        source.sendSystemMessage(UtilChatColour.parse("&a&l(!) &aSuccessfully created item hologram '&f" + id + "&a'!"));
        return 1;
    }
}

