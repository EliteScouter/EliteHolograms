package com.strictgaming.elite.holograms.forge20.command;

import com.mojang.brigadier.context.CommandContext;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge20.hologram.ForgeHologram;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge20.util.UtilChatColour;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

public class HologramsAnimateLineCommand {

    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        CommandSourceStack source = context.getSource();

        if (args.length < 4) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUsage: /eh animateline <id> <line> <seconds> <frames...>"));
            return 0;
        }

        String id = args[0];
        Hologram hologram = HologramManager.getById(id);

        if (hologram == null) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cHologram with ID '&f" + id + "&c' not found!"));
            return 0;
        }

        if (!(hologram instanceof ForgeHologram)) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cHologram with ID '&f" + id + "&c' is not a valid hologram!"));
            return 0;
        }

        int lineIndex;
        try {
            lineIndex = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cLine number must be a number!"));
            return 0;
        }
        
        int interval;
        try {
            interval = Integer.parseInt(args[2]);
            if (interval < 1) {
                source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cInterval must be at least 1 second!"));
                return 0;
            }
        } catch (NumberFormatException e) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cInterval must be a number!"));
            return 0;
        }
        
        // Combine remaining args into one string
        StringBuilder framesBuilder = new StringBuilder();
        for (int i = 3; i < args.length; i++) {
            framesBuilder.append(args[i]).append(" ");
        }
        String framesStr = framesBuilder.toString().trim();
        
        // Split by |
        List<String> frames = Arrays.asList(framesStr.split("\\|"));
        
        if (frames.size() < 2) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cYou must provide at least 2 frames separated by |"));
            return 0;
        }

        ForgeHologram forgeHologram = (ForgeHologram) hologram;
        
        try {
            forgeHologram.setLineAnimated(lineIndex, frames, interval);
            source.sendSystemMessage(UtilChatColour.parse("&a&l(!) &aSuccessfully animated line &f" + lineIndex + " &aof hologram &f" + id));
        } catch (Exception e) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cError animating line: " + e.getMessage()));
        }

        return 1;
    }
}

