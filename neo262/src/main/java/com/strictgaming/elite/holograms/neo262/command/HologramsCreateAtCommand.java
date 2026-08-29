package com.strictgaming.elite.holograms.neo262.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo262.hologram.HologramDisplayType;
import com.strictgaming.elite.holograms.neo262.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo262.hologram.implementation.NeoForgeHologramBuilder;
import com.strictgaming.elite.holograms.neo262.util.UtilChatColour;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.server.permissions.Permissions;
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
        dispatcher.register(Commands.literal("eliteholograms").then(buildArguments()));
        dispatcher.register(Commands.literal("eh").then(buildArguments()));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildArguments() {
        return Commands.literal("createat")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                // Explicit display type; literals match before the bare <id> argument.
                .then(Commands.literal("facing").then(coordinateArguments(HologramDisplayType.FACING)))
                .then(Commands.literal("fixed").then(coordinateArguments(HologramDisplayType.FIXED)))
                .then(coordinateArguments(HologramDisplayType.FACING));
    }

    /**
     * Builds the {@code <id> <x> <y> <z> [text]} chain for a given display type.
     */
    private com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> coordinateArguments(
            HologramDisplayType displayType) {
        return Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("x", StringArgumentType.word())
                .then(Commands.argument("y", StringArgumentType.word())
                .then(Commands.argument("z", StringArgumentType.word())
                .executes(ctx -> run(ctx, false, false, displayType))
                .then(Commands.argument("text", StringArgumentType.greedyString())
                .executes(ctx -> run(ctx, false, true, displayType))))));
    }

    public int run(CommandContext<CommandSourceStack> context, boolean hasWorld, boolean hasText)
            throws CommandSyntaxException {
        return run(context, hasWorld, hasText, HologramDisplayType.FACING);
    }

    public int run(CommandContext<CommandSourceStack> context, boolean hasWorld, boolean hasText,
                   HologramDisplayType displayType) throws CommandSyntaxException {
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
        String worldName = source.getLevel().dimension().identifier().toString();
        
        // Get text
        String text;
        if (hasText) {
            text = StringArgumentType.getString(context, "text");
        } else {
            text = "§6Example Hologram";
        }
        
        // A fixed hologram keeps whatever rotation it is given. Face the sender when one is
        // present; from console there is nothing to face, so it defaults to south.
        float yaw = 0.0F;
        if (displayType == HologramDisplayType.FIXED
                && source.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) {
            yaw = player.getYRot() - 180.0F;
        }

        // Create hologram
        Hologram hologram = new NeoForgeHologramBuilder()
                .id(id)
                .world(worldName)
                .position(x, y, z)
                .displayType(displayType)
                .rotation(yaw, 0.0F)
                .lines(text)
                .buildAndSpawn();
        
        source.sendSuccess(() -> UtilChatColour.parse("&aCreated " + displayType.getSerializedName()
                + " hologram '&f" + id + "&a' at &f" + x + ", " + y + ", " + z), true);
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
        return buildArguments();
    }
}
