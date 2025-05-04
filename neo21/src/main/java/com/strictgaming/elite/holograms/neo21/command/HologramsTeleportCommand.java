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
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.util.Optional;

/**
 * Command to teleport to a hologram
 */
public class HologramsTeleportCommand implements HologramsCommand.SubCommand {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final SimpleCommandExceptionType HOLOGRAM_NOT_FOUND = 
            new SimpleCommandExceptionType(UtilChatColour.parse("&cHologram not found"));
    private static final SimpleCommandExceptionType PLAYER_ONLY = 
            new SimpleCommandExceptionType(UtilChatColour.parse("&cThis command can only be used by players"));
    private static final SimpleCommandExceptionType WORLD_NOT_FOUND = 
            new SimpleCommandExceptionType(UtilChatColour.parse("&cHologram world not found"));
    
    // Offset to prevent players from being teleported into the ground
    private static final double Y_OFFSET = 1.0;
    
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
                .then(Commands.literal("teleport")
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
                .then(Commands.literal("teleport")
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
        String worldName = hologram.getWorld();
        ServerLevel targetLevel = null;
        
        // Find the target world
        for (ServerLevel level : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
            String levelName = level.dimension().location().toString();
            if (levelName.equals(worldName) || level.dimension().location().getPath().equals(worldName)) {
                targetLevel = level;
                break;
            }
        }
        
        if (targetLevel == null) {
            throw WORLD_NOT_FOUND.create();
        }
        
        // Teleport the player with Y offset to prevent falling into the ground
        double x = hologram.getX();
        double y = hologram.getY() + Y_OFFSET; // Add offset
        double z = hologram.getZ();
        
        // If the player is in a different dimension, teleport them to the target dimension first
        if (player.level() != targetLevel) {
            player.teleportTo(targetLevel, x, y, z, player.getYRot(), player.getXRot());
        } else {
            player.teleportTo(x, y, z);
        }
        
        source.sendSuccess(() -> UtilChatColour.parse("&aTeleported to hologram '&f" + id + "&a'"), true);
        LOGGER.info("Player {} teleported to hologram {}", player.getScoreboardName(), id);
        
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
        return Commands.literal("teleport")
                .then(Commands.argument("id", StringArgumentType.string())
                    .suggests(SUGGEST_HOLOGRAMS));
    }
} 