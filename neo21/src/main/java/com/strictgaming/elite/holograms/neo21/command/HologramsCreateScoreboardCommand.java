package com.strictgaming.elite.holograms.neo21.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo21.hologram.ScoreboardHologram;
import com.strictgaming.elite.holograms.neo21.util.UtilPermissions;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * /eh createscoreboard <id> <objective> [topCount 1-10] [interval 5-300]
 */
public class HologramsCreateScoreboardCommand implements HologramsCommand.SubCommand, Command<CommandSourceStack> {

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("eliteholograms")
                .then(Commands.literal("createscoreboard")
                    .requires(source -> UtilPermissions.canCreate(source))
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("objective", StringArgumentType.word())
                            .executes(this::run)
                            .then(Commands.argument("topCount", IntegerArgumentType.integer(1, 10))
                                .executes(this::run)
                                .then(Commands.argument("updateInterval", IntegerArgumentType.integer(5, 300))
                                    .executes(this::run)
                                )
                            )
                        )
                    )
                )
        );

        dispatcher.register(
            Commands.literal("eh")
                .then(Commands.literal("createscoreboard")
                    .requires(source -> UtilPermissions.canCreate(source))
                    .then(Commands.argument("id", StringArgumentType.word())
                        .then(Commands.argument("objective", StringArgumentType.word())
                            .executes(this::run)
                            .then(Commands.argument("topCount", IntegerArgumentType.integer(1, 10))
                                .executes(this::run)
                                .then(Commands.argument("updateInterval", IntegerArgumentType.integer(5, 300))
                                    .executes(this::run)
                                )
                            )
                        )
                    )
                )
        );
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!UtilPermissions.canCreate(source)) {
            source.sendFailure(Component.literal("§cYou don't have permission to create holograms!"));
            return 0;
        }
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cThis command can only be used by players!"));
            return 0;
        }

        try {
            String id = StringArgumentType.getString(context, "id");
            String objective = StringArgumentType.getString(context, "objective");

            int topCount = 5;
            int updateInterval = 30;
            try { topCount = IntegerArgumentType.getInteger(context, "topCount"); } catch (IllegalArgumentException ignored) {}
            try { updateInterval = IntegerArgumentType.getInteger(context, "updateInterval"); } catch (IllegalArgumentException ignored) {}

            if (id.trim().isEmpty()) {
                source.sendFailure(Component.literal("§cHologram ID cannot be empty!"));
                return 0;
            }
            if (objective.trim().isEmpty()) {
                source.sendFailure(Component.literal("§cObjective name cannot be empty!"));
                return 0;
            }
            if (HologramManager.getHologram(id).isPresent()) {
                source.sendFailure(Component.literal("§cA hologram with ID '" + id + "' already exists!"));
                return 0;
            }

            if (topCount < 1 || topCount > 10) {
                source.sendFailure(Component.literal("§cTop count must be between 1 and 10!"));
                return 0;
            }
            if (updateInterval < 5 || updateInterval > 300) {
                source.sendFailure(Component.literal("§cUpdate interval must be between 5 and 300 seconds!"));
                return 0;
            }

            String worldName = player.level().dimension().location().toString();
            double x = player.getX();
            double y = player.getY();
            double z = player.getZ();

            ScoreboardHologram holo = new ScoreboardHologram(
                    id,
                    worldName,
                    x, y, z,
                    32,
                    objective,
                    topCount,
                    updateInterval,
                    null,
                    null,
                    null
            );
            holo.spawn();
            holo.forceUpdate();

            HologramManager.save();

            final int fc = topCount;
            final int fi = updateInterval;
            source.sendSuccess(() -> Component.literal(
                "§aCreated scoreboard hologram '" + id + "' for objective '" + objective +
                "' showing top " + fc + " players (updates every " + fi + "s)"
            ), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError creating scoreboard hologram: " + e.getMessage()));
            return 0;
        }
    }

    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        return run(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("createscoreboard")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("objective", StringArgumentType.word())
                        .then(Commands.argument("topCount", IntegerArgumentType.integer(1, 10))
                            .then(Commands.argument("updateInterval", IntegerArgumentType.integer(5, 300))))));
    }
}


