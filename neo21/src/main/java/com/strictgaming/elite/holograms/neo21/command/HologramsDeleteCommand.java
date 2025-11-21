package com.strictgaming.elite.holograms.neo21.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.strictgaming.elite.holograms.neo21.Neo21Holograms;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Command to delete a hologram
 */
public class HologramsDeleteCommand implements HologramsCommand.SubCommand {
    
    /**
     * Registers this command with the given dispatcher
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register with full command name
        dispatcher.register(
            Commands.literal("eliteholograms")
                .then(Commands.literal("delete")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            HologramManager.getHolograms().keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(this::execute)
                    )
                )
        );
        
        // Register with short command alias
        dispatcher.register(
            Commands.literal("eh")
                .then(Commands.literal("delete")
                    .requires(source -> source.hasPermission(2))
                    .then(Commands.argument("id", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            HologramManager.getHolograms().keySet().forEach(builder::suggest);
                            return builder.buildFuture();
                        })
                        .executes(this::execute)
                    )
                )
        );
    }
    
    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        try {
            String id = StringArgumentType.getString(context, "id");
            
            if (HologramManager.removeHologram(id)) {
                source.sendSuccess(() -> Component.literal("§aHologram deleted: " + id), true);
                return 1;
            } else {
                source.sendFailure(Component.literal("§cHologram not found: " + id));
                return 0;
            }
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError deleting hologram: " + e.getMessage()));
            return 0;
        }
    }
    
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("delete")
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests((context, builder) -> {
                        HologramManager.getHolograms().keySet().forEach(builder::suggest);
                        return builder.buildFuture();
                    }));
    }
}