package com.strictgaming.elite.holograms.neo21.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.Neo21Holograms;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Command to create a new hologram
 */
public class HologramsCreateCommand implements HologramsCommand.SubCommand {
    
    /**
     * Registers this command with the given dispatcher
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register with full command name
        dispatcher.register(
            Commands.literal("eliteholograms")
                .then(Commands.literal("create")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(this::execute)
                        )
                        .executes(this::execute) // Also allow creating without text
                    )
                )
        );
        
        // Register with short command alias
        dispatcher.register(
            Commands.literal("eh")
                .then(Commands.literal("create")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                            .executes(this::execute)
                        )
                        .executes(this::execute) // Also allow creating without text
                    )
                )
        );
    }
    
    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            // Get command parameters
            String id = StringArgumentType.getString(context, "id");
            String text = null;
            
            // Try to get the text parameter if provided
            try {
                text = StringArgumentType.getString(context, "text");
            } catch (IllegalArgumentException e) {
                // Text parameter is optional
            }
            
            // Check if player
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("§cThis command can only be used by players"));
                return 0;
            }
            
            // Check if ID already exists
            if (HologramManager.getHologram(id).isPresent()) {
                source.sendFailure(Component.literal("§cA hologram with this ID already exists"));
                return 0;
            }
            
            // Get player position
            Vec3 pos = player.position();
            String worldName = player.level().dimension().location().toString();
            
            // Create hologram with default lines or provided text
            Hologram hologram;
            if (text != null && !text.isEmpty()) {
                hologram = Neo21Holograms.getInstance().builder()
                        .id(id)
                        .world(worldName)
                        .position(pos.x, pos.y - 0.5, pos.z)
                        .lines(text)
                        .buildAndSpawn();
            } else {
                hologram = Neo21Holograms.getInstance().builder()
                        .id(id)
                        .world(worldName)
                        .position(pos.x, pos.y - 0.5, pos.z)
                        .lines("§6Example Hologram", "§7Line 2", "§bLine 3")
                        .buildAndSpawn();
            }
            
            source.sendSuccess(() -> Component.literal("§aCreated hologram with ID: " + id), true);
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError creating hologram: " + e.getMessage()));
            return 0;
        }
    }
    
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("create")
                .then(Commands.argument("id", StringArgumentType.word())
                     .then(Commands.argument("text", StringArgumentType.greedyString())));
    }
} 