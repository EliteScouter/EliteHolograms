package com.strictgaming.elite.holograms.forge.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.hologram.ForgeHologram;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge.util.UtilChatColour;
import com.google.common.collect.Lists;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Comparator;
import java.util.List;

/**
 * Command to list holograms near the player
 */
public class HologramsNearCommand implements Command<CommandSourceStack> {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");

    private static final int HOLOGRAMS_PER_PAGE = 10;

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        LOGGER.debug("NEAR COMMAND CALLED with args: {}", String.join(", ", args));
        
        ServerPlayer player = context.getSource().getPlayerOrException();
        int page = 1;
        
        if (args.length > 0) {
            try {
                page = Integer.parseInt(args[0]);
                if (page < 1) {
                    page = 1;
                }
            } catch (NumberFormatException e) {
                player.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cInvalid page number: " + args[0]));
                return 0;
            }
        }
        
        player.sendSystemMessage(UtilChatColour.parse("&e&l(!) &eSearching for nearby holograms..."));
        
        List<ForgeHologram> nearbyHolograms = new java.util.ArrayList<>();
        
        for (Hologram h : HologramManager.getAllHolograms()) {
            if (h instanceof ForgeHologram) {
                ForgeHologram fh = (ForgeHologram) h;
                
                // Only include holograms in the same world
                if (fh.getWorld().equals(player.level)) {
                    nearbyHolograms.add(fh);
                }
            }
        }
        
        // Sort by distance
        nearbyHolograms.sort(Comparator.comparingDouble(h -> 
                player.position().distanceToSqr(h.getPosition())
        ));
        
        if (nearbyHolograms.isEmpty()) {
            player.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cNo holograms found in this world!"));
            return 0;
        }
        
        // Split into pages
        List<List<ForgeHologram>> pages = Lists.partition(nearbyHolograms, HOLOGRAMS_PER_PAGE);
        
        if (page > pages.size()) {
            player.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cPage " + page + " does not exist! Max page: " + pages.size()));
            return 0;
        }
        
        List<ForgeHologram> currentPage = pages.get(page - 1);
        
        player.sendSystemMessage(UtilChatColour.parse("&e&l(!) &eNearby holograms (Page " + page + "/" + pages.size() + "):"));
        
        int i = (page - 1) * HOLOGRAMS_PER_PAGE + 1;
        for (ForgeHologram h : currentPage) {
            Vec3 pos = h.getPosition();
            double distance = Math.sqrt(player.position().distanceToSqr(pos));
            
            player.sendSystemMessage(UtilChatColour.parse(
                    String.format("&e%d. &f%s &7(%.1f blocks away, at %.1f, %.1f, %.1f)",
                        i++, h.getId(), distance, pos.x, pos.y, pos.z)
            ));
        }
        
        if (page < pages.size()) {
            player.sendSystemMessage(UtilChatColour.parse("&e&l(!) &eUse &f/eh near " + (page + 1) + " &eto see the next page"));
        }
        
        return Command.SINGLE_SUCCESS;
    }
} 
