package com.strictgaming.elite.holograms.forge20.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Command to list holograms near the player's location
 */
public class HologramsNearCommand implements Command<CommandSourceStack> {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    private static final int HOLOGRAMS_PER_PAGE = 8;
    private static final double DEFAULT_SEARCH_RADIUS = 50.0;

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        
        // Parse page number if provided
        int page = 1;
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) {
                    page = 1;
                }
            } catch (NumberFormatException e) {
                player.sendSystemMessage(Component.literal("§c§l(!) §cInvalid page number: §f" + args[0]));
                return 0;
            }
        }
        
        try {
            // Get player location
            String worldName = player.level().dimension().location().toString();
            double playerX = player.getX();
            double playerY = player.getY();
            double playerZ = player.getZ();
            
            // Find nearby holograms
            List<HologramDistance> nearbyHolograms = new ArrayList<>();
            
            for (Hologram hologram : HologramManager.getHolograms()) {
                // Skip holograms in different worlds
                if (!hologram.getWorldName().equals(worldName)) {
                    continue;
                }
                
                // Calculate distance
                double[] loc = hologram.getLocation();
                double distance = calculateDistance(playerX, playerY, playerZ, loc[0], loc[1], loc[2]);
                
                // Add to list if within range
                if (distance <= DEFAULT_SEARCH_RADIUS) {
                    nearbyHolograms.add(new HologramDistance(hologram, distance));
                }
            }
            
            // Sort by distance
            nearbyHolograms.sort(Comparator.comparingDouble(HologramDistance::getDistance));
            
            // Calculate pagination
            int totalPages = (int) Math.ceil((double) nearbyHolograms.size() / HOLOGRAMS_PER_PAGE);
            if (page > totalPages && totalPages > 0) {
                page = totalPages;
            }
            
            int startIndex = (page - 1) * HOLOGRAMS_PER_PAGE;
            int endIndex = Math.min(startIndex + HOLOGRAMS_PER_PAGE, nearbyHolograms.size());
            
            // Display results
            player.sendSystemMessage(Component.literal("§3§l┌─§b§lNearby Holograms (Page §f" + page + "§b/§f" + totalPages + "§b) §3§l──────┐"));
            
            if (nearbyHolograms.isEmpty()) {
                player.sendSystemMessage(Component.literal("§3│ §7No holograms found within " + DEFAULT_SEARCH_RADIUS + " blocks"));
            } else {
                for (int i = startIndex; i < endIndex; i++) {
                    HologramDistance hd = nearbyHolograms.get(i);
                    player.sendSystemMessage(Component.literal("§3│ §f" + hd.getHologram().getId() + " §7- §b" + 
                        String.format("%.1f", hd.getDistance()) + " §7blocks away"));
                }
                
                // Pagination info
                if (totalPages > 1) {
                    player.sendSystemMessage(Component.literal("§3│"));
                    
                    StringBuilder pageInfo = new StringBuilder("§3│ §7");
                    if (page > 1) {
                        pageInfo.append("§b/eh near ").append(page - 1).append(" §7for previous page");
                    } else {
                        pageInfo.append("This is the first page");
                    }
                    
                    pageInfo.append(" | ");
                    
                    if (page < totalPages) {
                        pageInfo.append("§b/eh near ").append(page + 1).append(" §7for next page");
                    } else {
                        pageInfo.append("This is the last page");
                    }
                    
                    player.sendSystemMessage(Component.literal(pageInfo.toString()));
                }
            }
            
            player.sendSystemMessage(Component.literal("§3§l└────────────────────────┘"));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            LOGGER.error("Error listing nearby holograms", e);
            player.sendSystemMessage(Component.literal("§c§l(!) §cError listing nearby holograms: §f" + e.getMessage()));
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