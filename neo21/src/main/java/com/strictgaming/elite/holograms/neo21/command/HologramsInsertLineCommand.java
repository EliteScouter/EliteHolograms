package com.strictgaming.elite.holograms.neo21.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo21.util.UtilChatColour;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.List;
import java.util.Optional;

/**
 * Command to insert a line at a specific position in a hologram
 */
public class HologramsInsertLineCommand implements HologramsCommand.SubCommand {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleCommandExceptionType HOLOGRAM_NOT_FOUND = 
            new SimpleCommandExceptionType(UtilChatColour.parse("&cHologram not found"));
    private static final SimpleCommandExceptionType INVALID_INDEX = 
            new SimpleCommandExceptionType(UtilChatColour.parse("&cInvalid line index"));
            
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_HOLOGRAMS = (context, builder) -> 
            SharedSuggestionProvider.suggest(HologramManager.getHolograms().keySet(), builder);

    /**
     * Registers this command with the given dispatcher
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register with full command name
        dispatcher.register(
            Commands.literal("eliteholograms")
                .then(Commands.literal("insertline")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(SUGGEST_HOLOGRAMS)
                        .then(Commands.argument("line", IntegerArgumentType.integer(1))
                            .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(this::run)
                            )
                        )
                    )
                )
        );
        
        // Register with short command alias
        dispatcher.register(
            Commands.literal("eh")
                .then(Commands.literal("insertline")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(SUGGEST_HOLOGRAMS)
                        .then(Commands.argument("line", IntegerArgumentType.integer(1))
                            .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(this::run)
                            )
                        )
                    )
                )
        );
    }

    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        int lineIndex = IntegerArgumentType.getInteger(context, "line");
        String text = StringArgumentType.getString(context, "text");
        
        // Find the hologram
        Optional<Hologram> hologramOpt = HologramManager.getHologram(id);
        if (hologramOpt.isEmpty()) {
            throw HOLOGRAM_NOT_FOUND.create();
        }
        
        Hologram hologram = hologramOpt.get();
        List<String> lines = hologram.getLines();
        
        // Convert 1-based index to 0-based
        int zeroBasedIndex = lineIndex - 1;
        
        // Validate the line index
        if (zeroBasedIndex < 0 || zeroBasedIndex > lines.size()) {
            throw INVALID_INDEX.create();
        }
        
        // Insert the line
        hologram.insertLine(zeroBasedIndex, text);
        
        source.sendSuccess(() -> UtilChatColour.parse(
            "&aInserted line at position &f" + lineIndex + "&a in hologram '&f" + id + "&a':&r " + text), false);
        
        return Command.SINGLE_SUCCESS;
    }
    
    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        try {
            return run(context);
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(UtilChatColour.parse(e.getMessage()));
            return 0;
        }
    }
    
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("insertline")
                .then(Commands.argument("id", StringArgumentType.string())
                     .suggests(SUGGEST_HOLOGRAMS)
                     .then(Commands.argument("line", IntegerArgumentType.integer(1))
                          .then(Commands.argument("text", StringArgumentType.greedyString()))));
    }
} 