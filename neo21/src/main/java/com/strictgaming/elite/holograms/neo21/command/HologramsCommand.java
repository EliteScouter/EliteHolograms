package com.strictgaming.elite.holograms.neo21.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.strictgaming.elite.holograms.neo21.Neo21Holograms;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Main command handler for hologram commands
 */
public class HologramsCommand {
    
    private final Map<String, SubCommand> subCommands = new HashMap<>();
    
    /**
     * Registers a subcommand
     * 
     * @param name The subcommand name
     * @param subCommand The subcommand handler
     */
    public void registerSubCommand(String name, SubCommand subCommand) {
        subCommands.put(name, subCommand);
    }
    
    /**
     * Registers this command with the command dispatcher
     * 
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register with 'eliteholograms' command
        LiteralArgumentBuilder<CommandSourceStack> fullCommandBuilder = Commands.literal("eliteholograms")
                .requires(source -> source.hasPermission(2));
        
        // Add help subcommand
        fullCommandBuilder.executes(this::showHelp);
        
        // Register with 'eh' command (short alias)
        LiteralArgumentBuilder<CommandSourceStack> shortCommandBuilder = Commands.literal("eh")
                .requires(source -> source.hasPermission(2));
        
        // Add help subcommand to short version too
        shortCommandBuilder.executes(this::showHelp);
        
        // Register all subcommands to both command variants
        for (Map.Entry<String, SubCommand> entry : subCommands.entrySet()) {
            fullCommandBuilder.then(Commands.literal(entry.getKey())
                    .executes(entry.getValue()::execute)
                    .then(entry.getValue().getArguments()));
            
            shortCommandBuilder.then(Commands.literal(entry.getKey())
                    .executes(entry.getValue()::execute)
                    .then(entry.getValue().getArguments()));
        }
        
        dispatcher.register(fullCommandBuilder);
        dispatcher.register(shortCommandBuilder);
    }
    
    /**
     * Shows help information for all commands
     * 
     * @param context The command context
     * @return Command result
     */
    private int showHelp(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSuccess(() -> Component.literal("§3§l┌─§b§lElite Holograms §3§l──────┐"), false);
        source.sendSuccess(() -> Component.literal("§3│ §b/eh create <id> <text>"), false);
        source.sendSuccess(() -> Component.literal("§3│ §b/eh list"), false);
        source.sendSuccess(() -> Component.literal("§3│ §b/eh delete <id>"), false);
        source.sendSuccess(() -> Component.literal("§3│ §b/eh addline <id> <text>"), false);
        source.sendSuccess(() -> Component.literal("§3│ §b/eh setline <id> <line> <text>"), false);
        source.sendSuccess(() -> Component.literal("§3│ §b/eh removeline <id> <line>"), false);
        source.sendSuccess(() -> Component.literal("§3│ §b/eh movehere <id>"), false);
        source.sendSuccess(() -> Component.literal("§3│ §b/eh near [page]"), false);
        source.sendSuccess(() -> Component.literal("§3│ §b/eh reload"), false);
        source.sendSuccess(() -> Component.literal("§3│ §b/eh teleport <id>"), false);
        source.sendSuccess(() -> Component.literal("§3│ §b/eh copy <target> <id>"), false);
        source.sendSuccess(() -> Component.literal("§3│ §b/eh insertline <id> <line> <text>"), false);
        source.sendSuccess(() -> Component.literal("§3│ §b/eh info <id>"), false);
        source.sendSuccess(() -> Component.literal("§3§l└─────────────────┘"), false);
        
        return 1;
    }
    
    /**
     * Interface for subcommands
     */
    public interface SubCommand {
        /**
         * Executes the subcommand
         * 
         * @param context The command context
         * @return Command result
         */
        int execute(CommandContext<CommandSourceStack> context);
        
        /**
         * Gets the argument builder for this command
         * 
         * @return The argument builder
         */
        LiteralArgumentBuilder<CommandSourceStack> getArguments();
    }
} 