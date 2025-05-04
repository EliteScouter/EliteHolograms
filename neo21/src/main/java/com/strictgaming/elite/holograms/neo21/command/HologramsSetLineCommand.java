package com.strictgaming.elite.holograms.neo21.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Optional;

/**
 * Command to set a specific line in a hologram
 */
public class HologramsSetLineCommand implements HologramsCommand.SubCommand {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleCommandExceptionType HOLOGRAM_NOT_FOUND = 
            new SimpleCommandExceptionType(Component.literal("Hologram not found"));
    private static final SimpleCommandExceptionType INVALID_LINE = 
            new SimpleCommandExceptionType(Component.literal("Invalid line number"));

    /**
     * Registers this command with the given dispatcher
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register with full command name
        dispatcher.register(
            Commands.literal("eliteholograms")
                .then(Commands.literal("setline")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("line", IntegerArgumentType.integer(0))
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
                .then(Commands.literal("setline")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("line", IntegerArgumentType.integer(0))
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
        int line = IntegerArgumentType.getInteger(context, "line");
        String text = StringArgumentType.getString(context, "text");
        
        // Find the hologram
        Optional<Hologram> hologramOpt = HologramManager.getHologram(id);
        if (hologramOpt.isEmpty()) {
            throw HOLOGRAM_NOT_FOUND.create();
        }
        
        Hologram hologram = hologramOpt.get();
        
        // Check line number validity
        if (line < 0 || line >= hologram.getLines().size()) {
            throw INVALID_LINE.create();
        }
        
        // Set the line
        hologram.setLine(line, text);
        
        source.sendSuccess(() -> Component.literal("Updated line " + line + " of hologram '" + id + "'"), false);
        LOGGER.info("Updated line {} of hologram '{}' by {}: {}", line, id, source.getTextName(), text);
        
        return Command.SINGLE_SUCCESS;
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
        return Commands.literal("setline")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("line", IntegerArgumentType.integer(0))
                        .then(Commands.argument("text", StringArgumentType.greedyString()))));
    }
} 