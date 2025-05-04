package com.strictgaming.elite.holograms.neo21.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Command to list holograms near the player's location
 */
public class HologramsNearCommand implements HologramsCommand.SubCommand {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int HOLOGRAMS_PER_PAGE = 8;
    private static final double DEFAULT_SEARCH_RADIUS = 50.0;
    private static final SimpleCommandExceptionType PLAYER_ONLY = 
            new SimpleCommandExceptionType(Component.literal("This command can only be used by players"));

    /**
     * Registers this command with the given dispatcher
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register with full command name
        dispatcher.register(
            Commands.literal("eliteholograms")
                .then(Commands.literal("near")
                    .requires(source -> source.hasPermission(2))
                    .executes(this::run)
                    .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> run(context, IntegerArgumentType.getInteger(context, "page")))
                    )
                )
        );
        
        // Register with short command alias
        dispatcher.register(
            Commands.literal("eh")
                .then(Commands.literal("near")
                    .requires(source -> source.hasPermission(2))
                    .executes(this::run)
                    .then(Commands.argument("page", IntegerArgumentType.integer(1))
                        .executes(context -> run(context, IntegerArgumentType.getInteger(context, "page")))
                    )
                )
        );
    }
    
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return run(context, 1); // Default to page 1
    }
    
    public int run(CommandContext<CommandSourceStack> context, int page) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        
        // Ensure command is run by a player
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            throw PLAYER_ONLY.create();
        }
        
        try {
            // Get player location
            String worldName = player.level().dimension().location().toString();
            double playerX = player.getX();
            double playerY = player.getY();
            double playerZ = player.getZ();
            
            // Find nearby holograms
            List<HologramDistance> nearbyHolograms = new ArrayList<>();
            
            for (Map.Entry<String, Hologram> entry : HologramManager.getHolograms().entrySet()) {
                Hologram hologram = entry.getValue();
                
                // Skip holograms in different worlds
                if (!hologram.getWorld().equals(worldName)) {
                    continue;
                }
                
                // Calculate distance
                double distance = calculateDistance(playerX, playerY, playerZ, 
                                                   hologram.getX(), hologram.getY(), hologram.getZ());
                
                // Add to list if within range
                if (distance <= DEFAULT_SEARCH_RADIUS) {
                    nearbyHolograms.add(new HologramDistance(hologram, distance));
                }
            }
            
            // Sort by distance
            nearbyHolograms.sort(Comparator.comparingDouble(HologramDistance::getDistance));
            
            // Calculate pagination
            int totalPages = (int) Math.ceil((double) nearbyHolograms.size() / HOLOGRAMS_PER_PAGE);
            if (totalPages == 0) totalPages = 1;
            
            if (page > totalPages) {
                page = totalPages;
            }
            
            int startIndex = (page - 1) * HOLOGRAMS_PER_PAGE;
            int endIndex = Math.min(startIndex + HOLOGRAMS_PER_PAGE, nearbyHolograms.size());
            
            // Create final copies for lambda expressions
            final int finalPage = page;
            final int finalTotalPages = totalPages;
            
            // Display results
            source.sendSuccess(() -> Component.literal("§3§l┌─§b§lNearby Holograms (Page §f" + finalPage + "§b/§f" + finalTotalPages + "§b)§3§l──────┐"), false);
            
            if (nearbyHolograms.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§3│ §7No holograms found within " + DEFAULT_SEARCH_RADIUS + " blocks"), false);
            } else {
                for (int i = startIndex; i < endIndex; i++) {
                    HologramDistance hd = nearbyHolograms.get(i);
                    source.sendSuccess(() -> Component.literal("§3│ §f" + hd.getHologram().getId() + " §7- §b" + 
                        String.format("%.1f", hd.getDistance()) + " §7blocks away"), false);
                }
                
                // Pagination info
                if (finalTotalPages > 1) {
                    source.sendSuccess(() -> Component.literal("§3│"), false);
                    
                    StringBuilder pageInfo = new StringBuilder("§3│ §7");
                    if (finalPage > 1) {
                        pageInfo.append("§b/eh near ").append(finalPage - 1).append(" §7for previous page");
                    } else {
                        pageInfo.append("This is the first page");
                    }
                    
                    pageInfo.append(" | ");
                    
                    if (finalPage < finalTotalPages) {
                        pageInfo.append("§b/eh near ").append(finalPage + 1).append(" §7for next page");
                    } else {
                        pageInfo.append("This is the last page");
                    }
                    
                    source.sendSuccess(() -> Component.literal(pageInfo.toString()), false);
                }
            }
            
            source.sendSuccess(() -> Component.literal("§3§l└────────────────────────┘"), false);
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            LOGGER.error("Error listing nearby holograms", e);
            source.sendFailure(Component.literal("§c§l(!) §cError listing nearby holograms: §f" + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Calculate the distance between two points in 3D space
     */
    private double calculateDistance(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        try {
            return run(context);
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }
    
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("near")
                .then(Commands.argument("page", IntegerArgumentType.integer(1)));
    }
    
    /**
     * Helper class to store a hologram and its distance from the player
     */
    private static class HologramDistance {
        private final Hologram hologram;
        private final double distance;
        
        public HologramDistance(Hologram hologram, double distance) {
            this.hologram = hologram;
            this.distance = distance;
        }
        
        public Hologram getHologram() {
            return hologram;
        }
        
        public double getDistance() {
            return distance;
        }
    }
} 