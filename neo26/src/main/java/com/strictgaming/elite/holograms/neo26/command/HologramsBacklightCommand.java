package com.strictgaming.elite.holograms.neo26.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo26.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo26.hologram.implementation.NeoForgeHologram;
import com.strictgaming.elite.holograms.neo26.util.UtilBacklight;
import com.strictgaming.elite.holograms.neo26.util.UtilPermissions;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Toggles an invisible {@code minecraft:light} block at a hologram's location
 * to light up the surrounding area, similar to WorldEdit's {@code //set light}.
 *
 * Usage:
 *   /eh backlight &lt;id&gt; on [height]
 *   /eh backlight &lt;id&gt; off
 *   /eh backlight &lt;id&gt; toggle
 *
 * <p>{@code height} is the number of light blocks in the vertical column
 * ({@code 1}-{@code 10}), rising from the ground behind the hologram.</p>
 */
public class HologramsBacklightCommand implements HologramsCommand.SubCommand {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_HOLOGRAMS = (context, builder) ->
            SharedSuggestionProvider.suggest(HologramManager.getHolograms().keySet(), builder);

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(buildTree("eliteholograms"));
        dispatcher.register(buildTree("eh"));
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildTree(String root) {
        return Commands.literal(root)
                .then(Commands.literal("backlight")
                        .requires(source -> UtilPermissions.hasPermission(source, UtilPermissions.BACKLIGHT))
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(SUGGEST_HOLOGRAMS)
                                .then(Commands.literal("on")
                                        .executes(ctx -> run(ctx, "on", UtilBacklight.DEFAULT_LEVEL))
                                        .then(Commands.argument("height",
                                                        IntegerArgumentType.integer(UtilBacklight.MIN_LEVEL, UtilBacklight.MAX_LEVEL))
                                                .executes(ctx -> run(ctx, "on",
                                                        IntegerArgumentType.getInteger(ctx, "height"))))
                                )
                                .then(Commands.literal("off")
                                        .executes(ctx -> run(ctx, "off", 0))
                                )
                                .then(Commands.literal("toggle")
                                        .executes(ctx -> run(ctx, "toggle", UtilBacklight.DEFAULT_LEVEL))
                                )
                        )
                );
    }

    private int run(CommandContext<CommandSourceStack> context, String mode, int level) {
        CommandSourceStack source = context.getSource();

        if (!UtilPermissions.hasPermission(source, UtilPermissions.BACKLIGHT)) {
            source.sendFailure(Component.literal("§cYou don't have permission to use the backlight command."));
            return 0;
        }

        String id = StringArgumentType.getString(context, "id");
        Optional<Hologram> hologramOpt = HologramManager.getHologram(id);
        if (hologramOpt.isEmpty()) {
            source.sendFailure(Component.literal("§cHologram '" + id + "' not found."));
            return 0;
        }
        if (!(hologramOpt.get() instanceof NeoForgeHologram hologram)) {
            source.sendFailure(Component.literal("§cThis hologram type does not support backlights."));
            return 0;
        }

        boolean enable;
        int targetLevel;
        switch (mode) {
            case "on" -> {
                enable = true;
                targetLevel = level;
            }
            case "off" -> {
                enable = false;
                targetLevel = hologram.getBacklightLevel();
            }
            case "toggle" -> {
                enable = !hologram.isBacklightEnabled();
                targetLevel = enable ? level : hologram.getBacklightLevel();
            }
            default -> {
                source.sendFailure(Component.literal("§cUnknown mode: " + mode));
                return 0;
            }
        }

        hologram.setBacklight(enable, targetLevel);

        if (enable) {
            final int finalLevel = hologram.getBacklightLevel();
            source.sendSuccess(() -> Component.literal(
                    "§aBacklight enabled for '§f" + id + "§a' - column height §f" + finalLevel + "§a block(s)."), true);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "§aBacklight disabled for '§f" + id + "§a'."), true);
        }
        return Command.SINGLE_SUCCESS;
    }

    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        context.getSource().sendFailure(Component.literal(
                "§cUsage: /eh backlight <id> <on|off|toggle> [height 1-10]"));
        return 0;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("backlight")
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(SUGGEST_HOLOGRAMS)
                        .then(Commands.literal("on")
                                .then(Commands.argument("height",
                                        IntegerArgumentType.integer(UtilBacklight.MIN_LEVEL, UtilBacklight.MAX_LEVEL))))
                        .then(Commands.literal("off"))
                        .then(Commands.literal("toggle")));
    }
}
