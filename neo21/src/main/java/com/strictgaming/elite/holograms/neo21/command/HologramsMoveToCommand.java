package com.strictgaming.elite.holograms.neo21.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo21.util.UtilChatColour;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Optional;

/**
 * Command to move a hologram to specific coordinates
 * Usage: /eh moveto <id> <x> <y> <z> [world]
 */
public class HologramsMoveToCommand implements HologramsCommand.SubCommand {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleCommandExceptionType HOLOGRAM_NOT_FOUND = 
            new SimpleCommandExceptionType(UtilChatColour.parse("&cHologram not found"));
    private static final SimpleCommandExceptionType INVALID_COORDS = 
            new SimpleCommandExceptionType(UtilChatColour.parse("&cInvalid coordinates! Must be numbers."));
            
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_HOLOGRAMS = (context, builder) -> 
            SharedSuggestionProvider.suggest(HologramManager.getHolograms().keySet(), builder);

    /**
     * Registers this command with the given dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register with full command name
        dispatcher.register(
            Commands.literal("eliteholograms")
                .then(Commands.literal("moveto")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(SUGGEST_HOLOGRAMS)
                        .then(Commands.argument("x", StringArgumentType.word())
                        .then(Commands.argument("y", StringArgumentType.word())
                        .then(Commands.argument("z", StringArgumentType.word())
                        .executes(ctx -> run(ctx, false))
                        .then(Commands.argument("world", StringArgumentType.greedyString())
                        .executes(ctx -> run(ctx, true))))))))
        );
        
        // Register with short command alias
        dispatcher.register(
            Commands.literal("eh")
                .then(Commands.literal("moveto")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(SUGGEST_HOLOGRAMS)
                        .then(Commands.argument("x", StringArgumentType.word())
                        .then(Commands.argument("y", StringArgumentType.word())
                        .then(Commands.argument("z", StringArgumentType.word())
                        .executes(ctx -> run(ctx, false))
                        .then(Commands.argument("world", StringArgumentType.greedyString())
                        .executes(ctx -> run(ctx, true))))))))
        );
    }

    public int run(CommandContext<CommandSourceStack> context, boolean hasWorld) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        
        // Parse coordinates
        double x, y, z;
        try {
            x = Double.parseDouble(StringArgumentType.getString(context, "x"));
            y = Double.parseDouble(StringArgumentType.getString(context, "y"));
            z = Double.parseDouble(StringArgumentType.getString(context, "z"));
        } catch (NumberFormatException e) {
            throw INVALID_COORDS.create();
        }
        
        // Get world name
        String worldName;
        if (hasWorld) {
            worldName = StringArgumentType.getString(context, "world");
        } else {
            worldName = source.getLevel().dimension().location().toString();
        }
        
        // Find the hologram
        Optional<Hologram> hologramOpt = HologramManager.getHologram(id);
        if (hologramOpt.isEmpty()) {
            throw HOLOGRAM_NOT_FOUND.create();
        }
        
        Hologram hologram = hologramOpt.get();
        
        // Move the hologram
        hologram.setPosition(worldName, x, y, z);
        
        source.sendSuccess(() -> UtilChatColour.parse("&aMoved hologram '&f" + id + "&a' to &f" + x + ", " + y + ", " + z + "&a in world &f" + worldName), false);
        LOGGER.info("Moved hologram '{}' to {}, {}, {} in world {}", id, x, y, z, worldName);
        
        return Command.SINGLE_SUCCESS;
    }
    
    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        try {
            return run(context, false);
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(UtilChatColour.parse(e.getMessage()));
            return 0;
        }
    }
    
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("moveto")
                .then(Commands.argument("id", StringArgumentType.string())
                    .suggests(SUGGEST_HOLOGRAMS)
                    .then(Commands.argument("x", StringArgumentType.word())
                    .then(Commands.argument("y", StringArgumentType.word())
                    .then(Commands.argument("z", StringArgumentType.word())
                    .then(Commands.argument("world", StringArgumentType.greedyString()))))));
    }
}
