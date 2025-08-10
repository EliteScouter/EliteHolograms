package com.strictgaming.elite.holograms.forge.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge.util.UtilChatColour;
import com.strictgaming.elite.holograms.forge.util.UtilPermissions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Main command for Elite Holograms
 */
public class HologramsCommand {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    private final Map<String, Object> subCommands = new HashMap<>();
    
    // Suggestion provider for hologram IDs
    private static final SuggestionProvider<CommandSourceStack> HOLOGRAM_ID_SUGGESTIONS = (context, builder) -> {
        for (Hologram hologram : HologramManager.getAllHolograms()) {
            builder.suggest(hologram.getId());
        }
        return builder.buildFuture();
    };
    
    /**
     * Register the command with the dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Add debug statement to register
        LOGGER.debug("Registering commands...");
        
        // Print out all registered commands
        if (LOGGER.isDebugEnabled()) {
            debugCommandRegistration();
        }
        
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("eliteholograms")
                .requires(source -> UtilPermissions.hasPermission(source, UtilPermissions.LIST))
                .executes(this::onCommand);
                
        LiteralArgumentBuilder<CommandSourceStack> createCommand = Commands.literal("create")
                .requires(UtilPermissions::canCreate)
                .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("text", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id"),
                        StringArgumentType.getString(ctx, "text")
                    };
                    return executeSubCommand(ctx, "create", args);
                })));

        // createscoreboard command with optional topCount and updateInterval
        LiteralArgumentBuilder<CommandSourceStack> createScoreboardCommand = Commands.literal("createscoreboard")
                .requires(UtilPermissions::canCreate)
                .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("objective", StringArgumentType.word())
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id"),
                        StringArgumentType.getString(ctx, "objective")
                    };
                    return executeSubCommand(ctx, "createscoreboard", args);
                })
                .then(Commands.argument("topCount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 10))
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id"),
                        StringArgumentType.getString(ctx, "objective"),
                        String.valueOf(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "topCount"))
                    };
                    return executeSubCommand(ctx, "createscoreboard", args);
                })
                .then(Commands.argument("updateInterval", com.mojang.brigadier.arguments.IntegerArgumentType.integer(5, 300))
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id"),
                        StringArgumentType.getString(ctx, "objective"),
                        String.valueOf(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "topCount")),
                        String.valueOf(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "updateInterval"))
                    };
                    return executeSubCommand(ctx, "createscoreboard", args);
                }))))) ;
                
        LiteralArgumentBuilder<CommandSourceStack> deleteCommand = Commands.literal("delete")
                .requires(UtilPermissions::canDelete)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id")
                    };
                    return executeSubCommand(ctx, "delete", args);
                }));
                
        LiteralArgumentBuilder<CommandSourceStack> addLineCommand = Commands.literal("addline")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .then(Commands.argument("text", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id"),
                        StringArgumentType.getString(ctx, "text")
                    };
                    return executeSubCommand(ctx, "addline", args);
                })));
                
        LiteralArgumentBuilder<CommandSourceStack> setLineCommand = Commands.literal("setline")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .then(Commands.argument("line", StringArgumentType.word())
                .then(Commands.argument("text", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id"),
                        StringArgumentType.getString(ctx, "line"),
                        StringArgumentType.getString(ctx, "text")
                    };
                    return executeSubCommand(ctx, "setline", args);
                }))));
                
        LiteralArgumentBuilder<CommandSourceStack> removeLineCommand = Commands.literal("removeline")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .then(Commands.argument("line", StringArgumentType.word())
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id"),
                        StringArgumentType.getString(ctx, "line")
                    };
                    return executeSubCommand(ctx, "removeline", args);
                })));
                
        // Add list command - make it work directly as a subcommand too
        LiteralArgumentBuilder<CommandSourceStack> listCommand = Commands.literal("list")
                .requires(UtilPermissions::canList)
                .executes(this::listHolograms);
        
        // Add near command as a direct handler function
        LiteralArgumentBuilder<CommandSourceStack> nearCommand = Commands.literal("near")
                .requires(UtilPermissions::canNear)
                .executes(this::executeNearCommand)
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
                
        // Add teleport command as a direct handler function
        LiteralArgumentBuilder<CommandSourceStack> teleportCommand = Commands.literal("teleport")
                .requires(UtilPermissions::canTeleport)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .executes(ctx -> {
                    try {
                        String id = StringArgumentType.getString(ctx, "id");
                        return this.executeTeleportCommand(ctx, id);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return 0;
                    }
                }));
                
        // Info command
        LiteralArgumentBuilder<CommandSourceStack> infoCommand = Commands.literal("info")
                .requires(UtilPermissions::canInfo)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id")
                    };
                    return executeSubCommand(ctx, "info", args);
                }));
                
        // Copy command
        LiteralArgumentBuilder<CommandSourceStack> copyCommand = Commands.literal("copy")
                .requires(UtilPermissions::canCreate)
                .then(Commands.argument("source_id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .then(Commands.argument("new_id", StringArgumentType.word())
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "source_id"),
                        StringArgumentType.getString(ctx, "new_id")
                    };
                    return executeSubCommand(ctx, "copy", args);
                })));
                
        // MoveHere command
        LiteralArgumentBuilder<CommandSourceStack> moveHereCommand = Commands.literal("movehere")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id")
                    };
                    return executeSubCommand(ctx, "movehere", args);
                }));
                
        // InsertLine command
        LiteralArgumentBuilder<CommandSourceStack> insertLineCommand = Commands.literal("insertline")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .then(Commands.argument("line", StringArgumentType.word())
                .then(Commands.argument("text", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id"),
                        StringArgumentType.getString(ctx, "line"),
                        StringArgumentType.getString(ctx, "text")
                    };
                    return executeSubCommand(ctx, "insertline", args);
                }))));

        // MoveVertical command
        LiteralArgumentBuilder<CommandSourceStack> moveVerticalCommand = Commands.literal("movevertical")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .then(Commands.argument("amount", StringArgumentType.word())
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id"),
                        StringArgumentType.getString(ctx, "amount")
                    };
                    return executeSubCommand(ctx, "movevertical", args);
                })));
                
        // Reload command
        LiteralArgumentBuilder<CommandSourceStack> reloadCommand = Commands.literal("reload")
                .requires(UtilPermissions::canAdmin)
                .executes(ctx -> {
                    String[] args = new String[] {};
                    return executeSubCommand(ctx, "reload", args);
                });
                
        command.then(createCommand);
        command.then(deleteCommand);
        command.then(createScoreboardCommand);
        command.then(addLineCommand);
        command.then(setLineCommand);
        command.then(removeLineCommand);
        command.then(listCommand);
        command.then(nearCommand);
        command.then(teleportCommand);
        command.then(infoCommand);
        command.then(copyCommand);
        command.then(moveHereCommand);
        command.then(insertLineCommand);
        command.then(moveVerticalCommand);
        command.then(reloadCommand);
        
        dispatcher.register(command);
        
        // Register aliases with subcommands
        registerAliasWithSubcommands(dispatcher, command, "eh");
        registerAliasWithSubcommands(dispatcher, command, "hologram");
        registerAliasWithSubcommands(dispatcher, command, "eliteholograms");
    }
    
    /**
     * Handle the command execution
     */
    public int onCommand(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        source.sendSystemMessage(UtilChatColour.parse("&3&l┌─&b&lElite Holograms &3&l──────┐"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &b/eh create <id> <text>"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &b/eh createscoreboard <id> <objective> [top] [interval]"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &b/eh list"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &b/eh delete <id>"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &b/eh addline <id> <text>"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &b/eh setline <id> <line> <text>"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &b/eh removeline <id> <line>"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &b/eh movehere <id>"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &b/eh near [page]"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &b/eh reload"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &b/eh teleport <id>"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &b/eh copy <target> <id>"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &b/eh insertline <id> <line> <text>"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &b/eh info <id>"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &b/eh movevertical <id> <up|down> <amount>"));
        source.sendSystemMessage(UtilChatColour.parse("&3&l└─────────────────┘"));
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
        
        // Try with holograms prefix
        if (subCommand == null) {
            String prefixedName = "eliteholograms" + cleanName;
            if (subCommands.containsKey(prefixedName)) {
                subCommand = subCommands.get(prefixedName);
            }
        }
        
        // Try all variations for specific commands
        if (subCommand == null) {
            // Special handling for common commands
            for (String key : subCommands.keySet()) {
                String keyLower = key.toLowerCase();
                // Handle aliases and variations
                if ((cleanName.equals("near") && keyLower.contains("near")) ||
                    (cleanName.equals("tp") && keyLower.contains("teleport")) ||
                    (cleanName.equals("copy") && keyLower.contains("copy")) ||
                    (cleanName.equals("reload") && keyLower.contains("reload")) ||
                    (cleanName.equals("movehere") && keyLower.contains("movehere")) ||
                    (cleanName.equals("insertline") && keyLower.contains("insertline"))) {
                    
                    LOGGER.debug("Found match for {} -> {}", cleanName, key);
                    subCommand = subCommands.get(key);
                    break;
                }
            }
        }
        
        // Debug: Print registered subcommands
        if (subCommand == null) {
            StringBuilder availableCommands = new StringBuilder("&e&l(!) &eAvailable commands: ");
            for (String cmd : subCommands.keySet()) {
                availableCommands.append(cmd).append(", ");
            }
            context.getSource().sendSystemMessage(UtilChatColour.parse(availableCommands.toString()));
            
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUnknown command: " + name));
            return 0;
        }
        
        try {
            LOGGER.debug("Executing command: {} with class: {}", name, subCommand.getClass().getName());
            // Special handling: scoreboard creator uses Brigadier arguments directly via run(context)
            if (subCommand instanceof HologramsCreateScoreboardCommand) {
                return ((HologramsCreateScoreboardCommand) subCommand).run(context);
            }
            return (int) subCommand.getClass().getMethod("executeCommand", CommandContext.class, String[].class)
                                 .invoke(subCommand, context, args);
        } catch (Exception e) {
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cError executing command: " + e.getMessage()));
            LOGGER.error("Error executing command", e);
            return 0;
        }
    }
    
    // Register an alias that also has all the subcommands
    private void registerAliasWithSubcommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                            LiteralArgumentBuilder<CommandSourceStack> command,
                                            String alias) {
        // Create the base alias command
        LiteralArgumentBuilder<CommandSourceStack> aliasCommand = Commands.literal(alias)
                .requires(source -> UtilPermissions.hasPermission(source, UtilPermissions.LIST))
                .executes(this::onCommand);
                
        // Add list command directly to the alias
        aliasCommand.then(Commands.literal("list")
                .requires(UtilPermissions::canList)
                .executes(this::listHolograms));
                
        // Add create command to the alias
        aliasCommand.then(Commands.literal("create")
                .requires(UtilPermissions::canCreate)
                .then(Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("text", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id"),
                        StringArgumentType.getString(ctx, "text")
                    };
                    return executeSubCommand(ctx, "create", args);
                }))));

        // Add createscoreboard command to the alias
        aliasCommand.then(
            Commands.literal("createscoreboard")
                .requires(UtilPermissions::canCreate)
                .then(
                    Commands.argument("id", StringArgumentType.word())
                        .then(
                            Commands.argument("objective", StringArgumentType.word())
                                .executes(ctx -> {
                                    String[] args = new String[] {
                                        StringArgumentType.getString(ctx, "id"),
                                        StringArgumentType.getString(ctx, "objective")
                                    };
                                    return executeSubCommand(ctx, "createscoreboard", args);
                                })
                                .then(
                                    Commands.argument("topCount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 10))
                                        .executes(ctx -> {
                                            String[] args = new String[] {
                                                StringArgumentType.getString(ctx, "id"),
                                                StringArgumentType.getString(ctx, "objective"),
                                                String.valueOf(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "topCount"))
                                            };
                                            return executeSubCommand(ctx, "createscoreboard", args);
                                        })
                                        .then(
                                            Commands.argument("updateInterval", com.mojang.brigadier.arguments.IntegerArgumentType.integer(5, 300))
                                                .executes(ctx -> {
                                                    String[] args = new String[] {
                                                        StringArgumentType.getString(ctx, "id"),
                                                        StringArgumentType.getString(ctx, "objective"),
                                                        String.valueOf(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "topCount")),
                                                        String.valueOf(com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "updateInterval"))
                                                    };
                                                    return executeSubCommand(ctx, "createscoreboard", args);
                                                })
                                        )
                                )
                        )
                )
        );

        // movevertical alias with suggestions and up/down
        aliasCommand.then(Commands.literal("movevertical")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .then(Commands.argument("amount", StringArgumentType.word())
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id"),
                        StringArgumentType.getString(ctx, "amount")
                    };
                    return executeSubCommand(ctx, "movevertical", args);
                }))
                .then(Commands.literal("up")
                    .then(Commands.argument("amount", StringArgumentType.word())
                    .executes(ctx -> {
                        String[] args = new String[] {
                            StringArgumentType.getString(ctx, "id"),
                            "up",
                            StringArgumentType.getString(ctx, "amount")
                        };
                        return executeSubCommand(ctx, "movevertical", args);
                    })))
                .then(Commands.literal("down")
                    .then(Commands.argument("amount", StringArgumentType.word())
                    .executes(ctx -> {
                        String[] args = new String[] {
                            StringArgumentType.getString(ctx, "id"),
                            "down",
                            StringArgumentType.getString(ctx, "amount")
                        };
                        return executeSubCommand(ctx, "movevertical", args);
                    })))));
                
        // Add delete command
        aliasCommand.then(Commands.literal("delete")
                .requires(UtilPermissions::canDelete)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id")
                    };
                    return executeSubCommand(ctx, "delete", args);
                })));
                
        // Add line command
        aliasCommand.then(Commands.literal("addline")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .then(Commands.argument("text", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id"),
                        StringArgumentType.getString(ctx, "text")
                    };
                    return executeSubCommand(ctx, "addline", args);
                }))));
                
        // Set line command
        aliasCommand.then(Commands.literal("setline")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .then(Commands.argument("line", StringArgumentType.word())
                .then(Commands.argument("text", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id"),
                        StringArgumentType.getString(ctx, "line"),
                        StringArgumentType.getString(ctx, "text")
                    };
                    return executeSubCommand(ctx, "setline", args);
                })))));
                
        // Remove line command
        aliasCommand.then(Commands.literal("removeline")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .then(Commands.argument("line", StringArgumentType.word())
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id"),
                        StringArgumentType.getString(ctx, "line")
                    };
                    return executeSubCommand(ctx, "removeline", args);
                }))));
                
        // Move here command
        aliasCommand.then(Commands.literal("movehere")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id")
                    };
                    return executeSubCommand(ctx, "movehere", args);
                })));
                
        // Near command - use the direct handler functions
        aliasCommand.then(Commands.literal("near")
                .requires(UtilPermissions::canNear)
                .executes(this::executeNearCommand)
                .then(Commands.argument("page", StringArgumentType.word())
                .executes(ctx -> {
                    try {
                        String page = StringArgumentType.getString(ctx, "page");
                        return this.executeNearCommandWithPage(ctx, page);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return 0;
                    }
                })));
                
        // Teleport command - use the direct handler
        aliasCommand.then(Commands.literal("teleport")
                .requires(UtilPermissions::canTeleport)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .executes(ctx -> {
                    try {
                        String id = StringArgumentType.getString(ctx, "id");
                        return this.executeTeleportCommand(ctx, id);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return 0;
                    }
                })));
                
        // Copy command
        aliasCommand.then(Commands.literal("copy")
                .requires(UtilPermissions::canCreate)
                .then(Commands.argument("source", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .then(Commands.argument("newid", StringArgumentType.word())
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "source"),
                        StringArgumentType.getString(ctx, "newid")
                    };
                    return executeSubCommand(ctx, "copy", args);
                }))));
                
        // Insert line command
        aliasCommand.then(Commands.literal("insertline")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .then(Commands.argument("line", StringArgumentType.word())
                .then(Commands.argument("text", StringArgumentType.greedyString())
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id"),
                        StringArgumentType.getString(ctx, "line"),
                        StringArgumentType.getString(ctx, "text")
                    };
                    return executeSubCommand(ctx, "insertline", args);
                })))));
                
        // Reload command
        aliasCommand.then(Commands.literal("reload")
                .requires(UtilPermissions::canAdmin)
                .executes(ctx -> executeSubCommand(ctx, "reload", new String[0])));
                
        // Info command
        aliasCommand.then(Commands.literal("info")
                .requires(UtilPermissions::canInfo)
                .then(Commands.argument("id", StringArgumentType.word())
                .suggests(HOLOGRAM_ID_SUGGESTIONS)
                .executes(ctx -> {
                    String[] args = new String[] {
                        StringArgumentType.getString(ctx, "id")
                    };
                    return executeSubCommand(ctx, "info", args);
                })));
                
        dispatcher.register(aliasCommand);
    }
    
    /**
     * Special handler for list command
     */
    private int listHolograms(CommandContext<CommandSourceStack> context) {
        LOGGER.debug("Direct call to listHolograms method");
        
        try {
            CommandSourceStack source = context.getSource();
            List<Hologram> holograms = HologramManager.getAllHolograms();
            
            if (holograms.isEmpty()) {
                source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cThere are no holograms on the server!"));
                return 0;
            }
            
            source.sendSystemMessage(UtilChatColour.parse("&e&l(!) &eHolograms on the server: &f" + holograms.size()));
            
            for (Hologram hologram : holograms) {
                source.sendSystemMessage(UtilChatColour.parse("&e- &f" + hologram.getId()));
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cError listing holograms: " + e.getMessage()));
            LOGGER.error("Error listing holograms", e);
            return 0;
        }
    }
    
    // Directly implement near command handler
    private int executeNearCommand(CommandContext<CommandSourceStack> context) {
        LOGGER.debug("Direct near command execution with no page");
        
        try {
            // Find the registered near command
            Object nearCmd = null;
            for (String key : subCommands.keySet()) {
                if (key.toLowerCase().contains("near")) {
                    nearCmd = subCommands.get(key);
                    break;
                }
            }
            
            if (nearCmd == null) {
                context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cNear command not found!"));
                return 0;
            }
            
            return (int) nearCmd.getClass().getMethod("executeCommand", CommandContext.class, String[].class)
                .invoke(nearCmd, context, new String[0]);
        } catch (Exception e) {
            LOGGER.error("Error executing near command", e);
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cError executing near command: " + e.getMessage()));
            return 0;
        }
    }
    
    // Near command with page parameter
    private int executeNearCommandWithPage(CommandContext<CommandSourceStack> context, String page) {
        LOGGER.debug("Direct near command execution with page: {}", page);
        
        try {
            // Find the registered near command
            Object nearCmd = null;
            for (String key : subCommands.keySet()) {
                if (key.toLowerCase().contains("near")) {
                    nearCmd = subCommands.get(key);
                    break;
                }
            }
            
            if (nearCmd == null) {
                context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cNear command not found!"));
                return 0;
            }
            
            return (int) nearCmd.getClass().getMethod("executeCommand", CommandContext.class, String[].class)
                .invoke(nearCmd, context, new String[] { page });
        } catch (Exception e) {
            LOGGER.error("Error executing near command with page", e);
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cError executing near command: " + e.getMessage()));
            return 0;
        }
    }
    
    // Teleport command handler
    private int executeTeleportCommand(CommandContext<CommandSourceStack> context, String id) {
        LOGGER.debug("Direct teleport command execution with id: {}", id);
        
        try {
            // Find the registered teleport command
            Object teleportCmd = null;
            for (String key : subCommands.keySet()) {
                if (key.toLowerCase().contains("teleport")) {
                    teleportCmd = subCommands.get(key);
                    break;
                }
            }
            
            if (teleportCmd == null) {
                context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cTeleport command not found!"));
                return 0;
            }
            
            return (int) teleportCmd.getClass().getMethod("executeCommand", CommandContext.class, String[].class)
                .invoke(teleportCmd, context, new String[] { id });
        } catch (Exception e) {
            LOGGER.error("Error executing teleport command", e);
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cError executing teleport command: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Debug method to print all registered commands
     */
    private void debugCommandRegistration() {
        LOGGER.debug("=== HOLOGRAM COMMANDS REGISTRATION DEBUG ===");
        LOGGER.debug("Registered commands in manager: {}", this.subCommands.size());
        
        for (Map.Entry<String, Object> entry : this.subCommands.entrySet()) {
            LOGGER.debug("Command: {} -> {}", entry.getKey(), entry.getValue().getClass().getName());
        }
        
        LOGGER.debug("Looking for 'near' command: {}", this.subCommands.containsKey("near"));
        LOGGER.debug("Looking for 'teleport' command: {}", this.subCommands.containsKey("teleport"));
        LOGGER.debug("==========================================");
    }
} 
