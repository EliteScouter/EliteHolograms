package com.strictgaming.elite.holograms.neo21.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.strictgaming.elite.holograms.neo21.Neo21Holograms;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * Command to list all holograms
 */
public class HologramsListCommand implements HologramsCommand.SubCommand {
    
    /**
     * Registers this command with the given dispatcher
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register with full command name
        dispatcher.register(
            Commands.literal("eliteholograms")
                .then(Commands.literal("list")
                    .requires(source -> source.hasPermission(2))
                    .executes(this::execute)
                )
        );
        
        // Register with short command alias
        dispatcher.register(
            Commands.literal("eh")
                .then(Commands.literal("list")
                    .requires(source -> source.hasPermission(2))
                    .executes(this::execute)
                )
        );
    }
    
    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        // List of hologram IDs
        var holograms = HologramManager.getHolograms();
        
        if (holograms.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§c§l(!) §cThere are no holograms on the server!"), false);
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§e§l(!) §eHolograms on the server: §f" + holograms.size()), false);
        
        // Sort holograms alphabetically
        var sortedKeys = holograms.keySet().stream().sorted(String::compareToIgnoreCase).toList();
        
        for (String id : sortedKeys) {
            var hologram = holograms.get(id);
            String worldName = hologram.getWorld();
            
            source.sendSuccess(() -> Component.literal(
                String.format("§e- §f%s §7(%s)", hologram.getId(), worldName)
            ), false);
        }
        
        source.sendSuccess(() -> Component.literal("§e§l(!) §eUse §f/eh create <id> <text> §eto create a new hologram"), false);
        
        return 1;
    }
    
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("list");
    }
} 