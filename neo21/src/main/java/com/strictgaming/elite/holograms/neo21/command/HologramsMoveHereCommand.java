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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Optional;

/**
 * Command to move a hologram to the player's location
 */
public class HologramsMoveHereCommand implements HologramsCommand.SubCommand {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleCommandExceptionType HOLOGRAM_NOT_FOUND = 
            new SimpleCommandExceptionType(UtilChatColour.parse("&cHologram not found"));
    private static final SimpleCommandExceptionType PLAYER_ONLY = 
            new SimpleCommandExceptionType(UtilChatColour.parse("&cThis command can only be used by players"));
            
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
                .then(Commands.literal("movehere")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(SUGGEST_HOLOGRAMS)
                        .executes(this::run)
                    )
                )
        );
        
        // Register with short command alias
        dispatcher.register(
            Commands.literal("eh")
                .then(Commands.literal("movehere")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(SUGGEST_HOLOGRAMS)
                        .executes(this::run)
                    )
                )
        );
    }

    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String id = StringArgumentType.getString(context, "id");
        
        // Ensure command is run by a player
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            throw PLAYER_ONLY.create();
        }
        
        // Find the hologram
        Optional<Hologram> hologramOpt = HologramManager.getHologram(id);
        if (hologramOpt.isEmpty()) {
            throw HOLOGRAM_NOT_FOUND.create();
        }
        
        Hologram hologram = hologramOpt.get();
        
        // Get player position and move hologram there
        Vec3 pos = player.position();
        String worldName = player.level().dimension().location().toString();
        
        // Update hologram position
        hologram.setPosition(worldName, pos.x, pos.y - 0.5, pos.z);
        
        source.sendSuccess(() -> UtilChatColour.parse("&aMoved hologram '&f" + id + "&a' to your location"), false);
        LOGGER.info("Player {} moved hologram '{}' to their location: {}, {}, {}, world: {}", 
                player.getScoreboardName(), id, pos.x, pos.y - 0.5, pos.z, worldName);
        
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
        return Commands.literal("movehere")
                .then(Commands.argument("id", StringArgumentType.string())
                    .suggests(SUGGEST_HOLOGRAMS));
    }
} 