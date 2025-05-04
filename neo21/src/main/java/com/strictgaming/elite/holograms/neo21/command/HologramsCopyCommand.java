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
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.List;
import java.util.Optional;

/**
 * Command to copy a hologram
 */
public class HologramsCopyCommand implements HologramsCommand.SubCommand {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleCommandExceptionType HOLOGRAM_NOT_FOUND = 
            new SimpleCommandExceptionType(UtilChatColour.parse("&cSource hologram not found"));
    private static final SimpleCommandExceptionType HOLOGRAM_EXISTS = 
            new SimpleCommandExceptionType(UtilChatColour.parse("&cA hologram with that ID already exists"));
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
                .then(Commands.literal("copy")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("source", StringArgumentType.string())
                        .suggests(SUGGEST_HOLOGRAMS)
                        .then(Commands.argument("target", StringArgumentType.word())
                            .executes(this::run)
                        )
                    )
                )
        );
        
        // Register with short command alias
        dispatcher.register(
            Commands.literal("eh")
                .then(Commands.literal("copy")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("source", StringArgumentType.string())
                        .suggests(SUGGEST_HOLOGRAMS)
                        .then(Commands.argument("target", StringArgumentType.word())
                            .executes(this::run)
                        )
                    )
                )
        );
    }

    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        String sourceId = StringArgumentType.getString(context, "source");
        String targetId = StringArgumentType.getString(context, "target");
        
        // Ensure command is run by a player
        ServerPlayer player;
        try {
            player = source.getPlayerOrException();
        } catch (CommandSyntaxException e) {
            throw PLAYER_ONLY.create();
        }
        
        // Find the source hologram
        Optional<Hologram> sourceHologramOpt = HologramManager.getHologram(sourceId);
        if (sourceHologramOpt.isEmpty()) {
            throw HOLOGRAM_NOT_FOUND.create();
        }
        
        // Check if target ID exists
        if (HologramManager.getHologram(targetId).isPresent()) {
            throw HOLOGRAM_EXISTS.create();
        }
        
        Hologram sourceHologram = sourceHologramOpt.get();
        List<String> lines = sourceHologram.getLines();
        
        // Get player position to create the copy at
        Vec3 pos = player.position();
        String worldName = player.level().dimension().location().toString();
        
        // Create the copy at player's location
        Hologram newHologram = Neo21Holograms.getInstance().builder()
                .id(targetId)
                .world(worldName)
                .position(pos.x, pos.y - 0.5, pos.z)
                .lines(lines)
                .buildAndSpawn();
        
        source.sendSuccess(() -> UtilChatColour.parse("&aCreated copy of hologram '&f" + sourceId + "&a' with ID '&f" + targetId + "&a'"), true);
        
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
        return Commands.literal("copy")
                .then(Commands.argument("source", StringArgumentType.string())
                     .suggests(SUGGEST_HOLOGRAMS)
                     .then(Commands.argument("target", StringArgumentType.word())));
    }
} 