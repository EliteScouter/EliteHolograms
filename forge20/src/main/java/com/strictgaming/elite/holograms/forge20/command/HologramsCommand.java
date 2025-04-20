package com.strictgaming.elite.holograms.forge20.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge20.util.UtilChatColour;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Main command for Elite Holograms
 */
public class HologramsCommand {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    private final Map<String, Object> subCommands = new HashMap<>();
    
    /**
     * Register the command with the dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Add debug statement to register
        LOGGER.debug("Registering commands...");
        
        // Register the main command
        registerCommandWithAliases(dispatcher, "eliteholograms");
        registerCommandWithAliases(dispatcher, "eh");
        registerCommandWithAliases(dispatcher, "hologram");
    }
    
    /**
     * Register a command with its aliases and subcommands
     */
    private void registerCommandWithAliases(CommandDispatcher<CommandSourceStack> dispatcher, String alias) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal(alias)
                .executes(this::onCommand);
        
        // Register subcommands
        for (Map.Entry<String, Object> entry : subCommands.entrySet()) {
            String name = entry.getKey();
            
            LiteralArgumentBuilder<CommandSourceStack> subCommand = Commands.literal(name);
            
            // Add appropriate arguments based on command name
            if (name.equals("create")) {
                subCommand
                    .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String[] args = new String[] {
                            StringArgumentType.getString(ctx, "id"),
                            StringArgumentType.getString(ctx, "text")
                        };
                        return executeSubCommand(ctx, "create", args);
                    })));
            } else if (name.equals("delete") || name.equals("info") || name.equals("movehere") || name.equals("teleport")) {
                subCommand
                    .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> {
                        String[] args = new String[] {
                            StringArgumentType.getString(ctx, "id")
                        };
                        return executeSubCommand(ctx, name, args);
                    }));
            } else if (name.equals("addline")) {
                subCommand
                    .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String[] args = new String[] {
                            StringArgumentType.getString(ctx, "id"),
                            StringArgumentType.getString(ctx, "text")
                        };
                        return executeSubCommand(ctx, name, args);
                    })));
            } else if (name.equals("setline") || name.equals("insertline")) {
                subCommand
                    .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("line", StringArgumentType.word())
                    .then(Commands.argument("text", StringArgumentType.greedyString())
                    .executes(ctx -> {
                        String[] args = new String[] {
                            StringArgumentType.getString(ctx, "id"),
                            StringArgumentType.getString(ctx, "line"),
                            StringArgumentType.getString(ctx, "text")
                        };
                        return executeSubCommand(ctx, name, args);
                    }))));
            } else if (name.equals("removeline")) {
                subCommand
                    .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("line", StringArgumentType.word())
                    .executes(ctx -> {
                        String[] args = new String[] {
                            StringArgumentType.getString(ctx, "id"),
                            StringArgumentType.getString(ctx, "line")
                        };
                        return executeSubCommand(ctx, name, args);
                    })));
            } else if (name.equals("list")) {
                subCommand.executes(this::listHolograms);
            } else if (name.equals("near")) {
                subCommand.executes(this::executeNearCommand)
                    .then(Commands.argument("page", StringArgumentType.word())
                    .executes(ctx -> {
                        try {
                            String page = StringArgumentType.getString(ctx, "page");
                            return this.executeNearCommandWithPage(ctx, page);
                        } catch (Exception e) {
                            e.printStackTrace();
                            return 0;
                        }
                    }));
            } else if (name.equals("copy")) {
                subCommand
                    .then(Commands.argument("target", StringArgumentType.word())
                    .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> {
                        String[] args = new String[] {
                            StringArgumentType.getString(ctx, "target"),
                            StringArgumentType.getString(ctx, "id")
                        };
                        return executeSubCommand(ctx, name, args);
                    })));
            } else if (name.equals("reload")) {
                subCommand.executes(ctx -> executeSubCommand(ctx, name, new String[0]));
            }
            
            command.then(subCommand);
        }
        
        dispatcher.register(command);
    }
    
    /**
     * Handle the command execution
     */
    public int onCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSystemMessage(Component.literal("§3§l┌─§b§lElite Holograms §3§l──────┐"));
        source.sendSystemMessage(Component.literal("§3│ §b/eh create <id> <text>"));
        source.sendSystemMessage(Component.literal("§3│ §b/eh list"));
        source.sendSystemMessage(Component.literal("§3│ §b/eh delete <id>"));
        source.sendSystemMessage(Component.literal("§3│ §b/eh addline <id> <text>"));
        source.sendSystemMessage(Component.literal("§3│ §b/eh setline <id> <line> <text>"));
        source.sendSystemMessage(Component.literal("§3│ §b/eh removeline <id> <line>"));
        source.sendSystemMessage(Component.literal("§3│ §b/eh movehere <id>"));
        source.sendSystemMessage(Component.literal("§3│ §b/eh near [page]"));
        source.sendSystemMessage(Component.literal("§3│ §b/eh reload"));
        source.sendSystemMessage(Component.literal("§3│ §b/eh teleport <id>"));
        source.sendSystemMessage(Component.literal("§3│ §b/eh copy <target> <id>"));
        source.sendSystemMessage(Component.literal("§3│ §b/eh insertline <id> <line> <text>"));
        source.sendSystemMessage(Component.literal("§3│ §b/eh info <id>"));
        source.sendSystemMessage(Component.literal("§3§l└─────────────────┘"));
        return 1;
    }
    
    /**
     * Register a subcommand
     */
    public void registerSubCommand(String name, Object subCommand) {
        this.subCommands.put(name.toLowerCase(), subCommand);
    }
    
    /**
     * Execute a subcommand
     */
    private int executeSubCommand(CommandContext<CommandSourceStack> context, String name, String[] args) {
        LOGGER.debug("Looking for command: {}", name);
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Available commands: {}", String.join(", ", subCommands.keySet()));
        }
        
        // Clean the name - standard formatting for our commands
        String cleanName = name.toLowerCase().trim();
        
        // Try to find the command with different name formats
        Object subCommand = null;
        
        // Try exact match first
        if (subCommands.containsKey(cleanName)) {
            subCommand = subCommands.get(cleanName);
        }
        
        // Debug: Print registered subcommands
        if (subCommand == null) {
            StringBuilder availableCommands = new StringBuilder("§e§l(!) §eAvailable commands: ");
            for (String cmd : subCommands.keySet()) {
                availableCommands.append(cmd).append(", ");
            }
            context.getSource().sendSystemMessage(Component.literal(availableCommands.toString()));
            
            context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cUnknown command: " + name));
            return 0;
        }
        
        try {
            // Call the executeCommand method on the subcommand
            if (subCommand instanceof HologramsCreateCommand) {
                return ((HologramsCreateCommand) subCommand).executeCommand(context, args);
            } else if (subCommand instanceof HologramsDeleteCommand) {
                return ((HologramsDeleteCommand) subCommand).executeCommand(context, args);
            } else if (subCommand instanceof HologramsListCommand) {
                return ((HologramsListCommand) subCommand).executeCommand(context, args);
            } else if (subCommand instanceof HologramsReloadCommand) {
                return ((HologramsReloadCommand) subCommand).executeCommand(context, args);
            } else if (subCommand instanceof HologramsInsertLineCommand) {
                return ((HologramsInsertLineCommand) subCommand).executeCommand(context, args);
            } else if (subCommand instanceof HologramsAddLineCommand) {
                return ((HologramsAddLineCommand) subCommand).executeCommand(context, args);
            } else if (subCommand instanceof HologramsSetLineCommand) {
                return ((HologramsSetLineCommand) subCommand).executeCommand(context, args);
            } else if (subCommand instanceof HologramsRemoveLineCommand) {
                return ((HologramsRemoveLineCommand) subCommand).executeCommand(context, args);
            } else if (subCommand instanceof HologramsMoveHereCommand) {
                return ((HologramsMoveHereCommand) subCommand).executeCommand(context, args);
            } else if (subCommand instanceof HologramsTeleportCommand) {
                return ((HologramsTeleportCommand) subCommand).executeCommand(context, args);
            } else if (subCommand instanceof HologramsCopyCommand) {
                return ((HologramsCopyCommand) subCommand).executeCommand(context, args);
            } else if (subCommand instanceof HologramsInfoCommand) {
                return ((HologramsInfoCommand) subCommand).executeCommand(context, args);
            } else if (subCommand instanceof HologramsNearCommand) {
                return ((HologramsNearCommand) subCommand).executeCommand(context, args);
            }
            
            // If we don't have a handler for this command, show an error
            context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cCommand not fully implemented: " + name));
            return 0;
        } catch (Exception e) {
            LOGGER.error("Error executing command: {}", name, e);
            context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cAn error occurred while executing the command."));
            return 0;
        }
    }
    
    /**
     * List all holograms
     */
    private int listHolograms(CommandContext<CommandSourceStack> context) {
        Object listCmd = subCommands.get("list");
        if (listCmd instanceof HologramsListCommand) {
            try {
                return ((HologramsListCommand) listCmd).executeCommand(context, new String[0]);
            } catch (Exception e) {
                LOGGER.error("Error executing list command", e);
                context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cAn error occurred while listing holograms."));
                return 0;
            }
        }
        
        // Fallback if the list command is not registered
        context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cList command not registered."));
        return 0;
    }
    
    /**
     * Execute the near command
     */
    private int executeNearCommand(CommandContext<CommandSourceStack> context) {
        Object nearCmd = subCommands.get("near");
        if (nearCmd instanceof HologramsNearCommand) {
            try {
                return ((HologramsNearCommand) nearCmd).executeCommand(context, new String[0]);
            } catch (Exception e) {
                LOGGER.error("Error executing near command", e);
                context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cAn error occurred while listing nearby holograms."));
                return 0;
            }
        }
        
        // Fallback if the near command is not registered
        context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cNear command not registered."));
        return 0;
    }
    
    /**
     * Execute the near command with a page
     */
    private int executeNearCommandWithPage(CommandContext<CommandSourceStack> context, String page) {
        Object nearCmd = subCommands.get("near");
        if (nearCmd instanceof HologramsNearCommand) {
            try {
                return ((HologramsNearCommand) nearCmd).executeCommand(context, new String[] { page });
            } catch (Exception e) {
                LOGGER.error("Error executing near command with page {}", page, e);
                context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cAn error occurred while listing nearby holograms."));
                return 0;
            }
        }
        
        // Fallback if the near command is not registered
        context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cNear command not registered."));
        return 0;
    }
    
    /**
     * Debug command registration
     */
    private void debugCommandRegistration() {
        StringBuilder registeredCommands = new StringBuilder();
        for (String cmd : subCommands.keySet()) {
            registeredCommands.append(cmd).append(", ");
        }
        LOGGER.debug("Registered commands: {}", registeredCommands.toString());
    }
} 