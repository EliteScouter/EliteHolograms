package com.strictgaming.elite.holograms.forge20.command;

import com.mojang.brigadier.context.CommandContext;
import com.strictgaming.elite.holograms.forge20.hologram.HologramDisplayType;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge20.hologram.ItemHologram;
import com.strictgaming.elite.holograms.forge20.util.UtilChatColour;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.Arrays;

public class HologramsCreateItemCommand {

    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        return executeCommand(context, args, HologramDisplayType.FACING);
    }

    /**
     * Execute the command with an explicit display type. Only the text lines honour it - the
     * floating item is an armor stand head slot and renders the same either way.
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args,
                              HologramDisplayType displayType) {
        CommandSourceStack source = context.getSource();

        if (args.length < 2) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUsage: /eh createitem [fixed|facing] <id> <item> [text...]"));
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

        // A fixed hologram keeps whatever rotation it is given, so start it facing the creator.
        float yaw = displayType == HologramDisplayType.FIXED ? player.getYRot() - 180.0F : 0.0F;

        new ItemHologram(id, player.serverLevel(), pos, 48, itemId, displayType, yaw, 0.0F, lines);

        source.sendSystemMessage(UtilChatColour.parse("&a&l(!) &aSuccessfully created "
                + displayType.getSerializedName() + " item hologram '&f" + id + "&a'!"));
        return 1;
    }
}

