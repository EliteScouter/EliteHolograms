package com.strictgaming.elite.holograms.neo21.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo21.util.UtilPermissions;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Moves a hologram vertically by a delta in blocks: /eh movevertical <id> <amount>
 */
public class HologramsMoveVerticalCommand implements HologramsCommand.SubCommand {

    private static final SuggestionProvider<CommandSourceStack> HOLOGRAM_ID_SUGGESTIONS = (context, builder) -> {
        for (String id : HologramManager.getHolograms().keySet()) {
            builder.suggest(id);
        }
        return builder.buildFuture();
    };

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Full alias: eliteholograms
        dispatcher.register(
            Commands.literal("eliteholograms")
                .then(Commands.literal("movevertical")
                    .requires(UtilPermissions::canEdit)
                    .then(Commands.argument("id", StringArgumentType.word()).suggests(HOLOGRAM_ID_SUGGESTIONS)
                        .then(Commands.argument("amount", StringArgumentType.word())
                            .executes(this::execute)
                        )
                        .then(Commands.literal("up")
                            .then(Commands.argument("amount", StringArgumentType.word())
                                .executes(ctx -> executeDirectional(ctx, true))
                            )
                        )
                        .then(Commands.literal("down")
                            .then(Commands.argument("amount", StringArgumentType.word())
                                .executes(ctx -> executeDirectional(ctx, false))
                            )
                        )
                    )
                )
        );
        // Short alias: eh
        dispatcher.register(
            Commands.literal("eh")
                .then(Commands.literal("movevertical")
                    .requires(UtilPermissions::canEdit)
                    .then(Commands.argument("id", StringArgumentType.word()).suggests(HOLOGRAM_ID_SUGGESTIONS)
                        .then(Commands.argument("amount", StringArgumentType.word())
                            .executes(this::execute)
                        )
                        .then(Commands.literal("up")
                            .then(Commands.argument("amount", StringArgumentType.word())
                                .executes(ctx -> executeDirectional(ctx, true))
                            )
                        )
                        .then(Commands.literal("down")
                            .then(Commands.argument("amount", StringArgumentType.word())
                                .executes(ctx -> executeDirectional(ctx, false))
                            )
                        )
                    )
                )
        );
    }

    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!UtilPermissions.canEdit(source)) {
            source.sendFailure(Component.literal("§cYou don't have permission"));
            return 0;
        }
        try {
            String id = StringArgumentType.getString(context, "id");
            String amountStr = StringArgumentType.getString(context, "amount");

            Optional<Hologram> opt = HologramManager.getHologram(id);
            if (opt.isEmpty()) {
                source.sendFailure(Component.literal("§cHologram not found"));
                return 0;
            }
            double delta;
            try {
                delta = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                source.sendFailure(Component.literal("§cAmount must be a number (e.g., 1.5)"));
                return 0;
            }

            Hologram h = opt.get();
            double x = h.getX();
            double y = h.getY();
            double z = h.getZ();
            h.setPosition(h.getWorld(), x, y + delta, z);
            source.sendSuccess(() -> Component.literal("§aMoved hologram '" + id + "' by " + delta + " blocks vertically."), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    private int executeDirectional(CommandContext<CommandSourceStack> context, boolean isUp) {
        CommandSourceStack source = context.getSource();
        if (!UtilPermissions.canEdit(source)) return 0;
        try {
            String id = StringArgumentType.getString(context, "id");
            String amountStr = StringArgumentType.getString(context, "amount");
            Optional<Hologram> opt = HologramManager.getHologram(id);
            if (opt.isEmpty()) {
                source.sendFailure(Component.literal("§cHologram not found"));
                return 0;
            }
            double amt = Double.parseDouble(amountStr);
            double delta = isUp ? Math.abs(amt) : -Math.abs(amt);
            Hologram h = opt.get();
            h.setPosition(h.getWorld(), h.getX(), h.getY() + delta, h.getZ());
            source.sendSuccess(() -> Component.literal("§aMoved hologram '" + id + "' " + (isUp ? "up " : "down ") + Math.abs(delta) + " blocks."), false);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError: " + e.getMessage()));
            return 0;
        }
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("movevertical")
            .then(Commands.argument("id", StringArgumentType.word()).suggests(HOLOGRAM_ID_SUGGESTIONS)
                .then(Commands.argument("amount", StringArgumentType.word()))
                .then(Commands.literal("up")
                    .then(Commands.argument("amount", StringArgumentType.word())
                        .executes(ctx -> executeDirectional(ctx, true))
                    )
                )
                .then(Commands.literal("down")
                    .then(Commands.argument("amount", StringArgumentType.word())
                        .executes(ctx -> executeDirectional(ctx, false))
                    )
                )
            );
    }
}


