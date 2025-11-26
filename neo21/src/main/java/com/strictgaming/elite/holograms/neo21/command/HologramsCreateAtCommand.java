package com.strictgaming.elite.holograms.neo21.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.Neo21Holograms;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo21.util.UtilChatColour;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
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
 * Command to create a hologram at specific coordinates
 * Usage: /eh createat <id> <x> <y> <z> [world] <text>
 */
public class HologramsCreateAtCommand implements HologramsCommand.SubCommand {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleCommandExceptionType HOLOGRAM_EXISTS = 
            new SimpleCommandExceptionType(UtilChatColour.parse("&cA hologram with this ID already exists"));
    private static final SimpleCommandExceptionType INVALID_COORDS = 
            new SimpleCommandExceptionType(UtilChatColour.parse("&cInvalid coordinates! Must be numbers."));

    /**
     * Registers this command with the given dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register with full command name
        dispatcher.register(
            Commands.literal("eliteholograms")
                .then(Commands.literal("createat")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("x", StringArgumentType.word())
                        .then(Commands.argument("y", StringArgumentType.word())
                        .then(Commands.argument("z", StringArgumentType.word())
                        .executes(ctx -> run(ctx, false, false))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> run(ctx, false, true))))))))
        );
        
        // Register with short command alias
        dispatcher.register(
            Commands.literal("eh")
                .then(Commands.literal("createat")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("x", StringArgumentType.word())
                        .then(Commands.argument("y", StringArgumentType.word())
                        .then(Commands.argument("z", StringArgumentType.word())
                        .executes(ctx -> run(ctx, false, false))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> run(ctx, false, true))))))))
        );
    }

    public int run(CommandContext<CommandSourceStack> context, boolean hasWorld, boolean hasText) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        
        // Check if ID already exists
        if (HologramManager.getHologram(id).isPresent()) {
            throw HOLOGRAM_EXISTS.create();
        }
        
        // Parse coordinates
        double x, y, z;
        try {
            x = Double.parseDouble(StringArgumentType.getString(context, "x"));
            y = Double.parseDouble(StringArgumentType.getString(context, "y"));
            z = Double.parseDouble(StringArgumentType.getString(context, "z"));
        } catch (NumberFormatException e) {
            throw INVALID_COORDS.create();
        }
        
        // Get world name (for now, use current world - world parameter can be added later)
        String worldName = source.getLevel().dimension().location().toString();
        
        // Get text
        String text;
        if (hasText) {
            text = StringArgumentType.getString(context, "text");
        } else {
            text = "§6Example Hologram";
        }
        
        // Create hologram
        Hologram hologram = Neo21Holograms.getInstance().builder()
                .id(id)
                .world(worldName)
                .position(x, y, z)
                .lines(text)
                .buildAndSpawn();
        
        source.sendSuccess(() -> UtilChatColour.parse("&aCreated hologram '&f" + id + "&a' at &f" + x + ", " + y + ", " + z), true);
        LOGGER.info("Created hologram '{}' at {}, {}, {} in world {}", id, x, y, z, worldName);
        
        return Command.SINGLE_SUCCESS;
    }
    
    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        try {
            return run(context, false, false);
        } catch (CommandSyntaxException e) {
            context.getSource().sendFailure(UtilChatColour.parse(e.getMessage()));
            return 0;
        }
    }
    
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("createat")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("x", StringArgumentType.word())
                    .then(Commands.argument("y", StringArgumentType.word())
                    .then(Commands.argument("z", StringArgumentType.word())
                    .then(Commands.argument("text", StringArgumentType.greedyString()))))));
    }
}
