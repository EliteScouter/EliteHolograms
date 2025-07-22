package com.strictgaming.elite.holograms.forge.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge.hologram.ScoreboardHologram;
import com.strictgaming.elite.holograms.forge.util.UtilPermissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command to create a scoreboard-based hologram
 * Usage: /holograms createscoreboard <id> <objective> [topCount] [updateInterval]
 */
public class HologramsCreateScoreboardCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // Check permissions
        if (!UtilPermissions.canCreate(source)) {
            source.sendFailure(Component.literal("§cYou don't have permission to create holograms!"));
            return 0;
        }
        
        // Must be a player
        if (!(source.getEntity() instanceof ServerPlayer)) {
            source.sendFailure(Component.literal("§cThis command can only be used by players!"));
            return 0;
        }
        
        ServerPlayer player = (ServerPlayer) source.getEntity();
        
        try {
            // Get arguments
            String id = StringArgumentType.getString(context, "id");
            String objective = StringArgumentType.getString(context, "objective");
            
            // Optional arguments with defaults
            int topCount;
            int updateInterval;
            
            try {
                topCount = IntegerArgumentType.getInteger(context, "topCount");
            } catch (IllegalArgumentException ignored) {
                topCount = 5; // Use default
            }
            
            try {
                updateInterval = IntegerArgumentType.getInteger(context, "updateInterval");
            } catch (IllegalArgumentException ignored) {
                updateInterval = 30; // Use default (30 seconds)
            }
            
            // Validate arguments
            if (id.trim().isEmpty()) {
                source.sendFailure(Component.literal("§cHologram ID cannot be empty!"));
                return 0;
            }
            
            if (objective.trim().isEmpty()) {
                source.sendFailure(Component.literal("§cObjective name cannot be empty!"));
                return 0;
            }
            
            // Check if hologram already exists
            if (HologramManager.getById(id) != null) {
                source.sendFailure(Component.literal("§cA hologram with ID '" + id + "' already exists!"));
                return 0;
            }
            
            // Validate ranges
            if (topCount < 1 || topCount > 10) {
                source.sendFailure(Component.literal("§cTop count must be between 1 and 10!"));
                return 0;
            }
            
            if (updateInterval < 5 || updateInterval > 300) {
                source.sendFailure(Component.literal("§cUpdate interval must be between 5 and 300 seconds!"));
                return 0;
            }
            
            // Create the scoreboard hologram
            ScoreboardHologram hologram = new ScoreboardHologram(
                id,
                player.level,
                player.position(),
                32, // Default range
                objective,
                topCount,
                updateInterval,
                null, // Use default header format
                null, // Use default player format
                null  // Use default empty format
            );
            
            // Force initial update
            hologram.forceUpdate();
            
            // Save
            HologramManager.save();
            
            // Create final copies for lambda
            final int finalTopCount = topCount;
            final int finalUpdateInterval = updateInterval;
            
            source.sendSuccess(Component.literal(
                "§aCreated scoreboard hologram '" + id + "' for objective '" + objective + 
                "' showing top " + finalTopCount + " players (updates every " + finalUpdateInterval + "s)"
            ), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError creating scoreboard hologram: " + e.getMessage()));
            return 0;
        }
    }
}