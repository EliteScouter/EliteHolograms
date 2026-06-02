package com.strictgaming.elite.holograms.neo26.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo26.config.ScoreboardThemeManager;
import com.strictgaming.elite.holograms.neo26.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo26.hologram.ScoreboardHologram;
import com.strictgaming.elite.holograms.neo26.util.UtilPermissions;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.Optional;

/**
 * /eh settheme <id> <theme> - restyles an existing scoreboard hologram with a different theme.
 */
public class HologramsSetThemeCommand implements HologramsCommand.SubCommand, Command<CommandSourceStack> {

    /** Suggests the IDs of existing scoreboard holograms. */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_SCOREBOARD_IDS = (context, builder) -> {
        for (Hologram hologram : HologramManager.getAllHolograms()) {
            if (hologram instanceof ScoreboardHologram) {
                builder.suggest(hologram.getId());
            }
        }
        return builder.buildFuture();
    };

    /** Suggests the configured scoreboard theme names. */
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_THEMES = (context, builder) ->
            SharedSuggestionProvider.suggest(ScoreboardThemeManager.getThemeNames(), builder);

    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("eliteholograms")
                .then(Commands.literal("settheme")
                    .requires(UtilPermissions::canCreate)
                    .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(SUGGEST_SCOREBOARD_IDS)
                        .then(Commands.argument("theme", StringArgumentType.word())
                            .suggests(SUGGEST_THEMES)
                            .executes(this::run)
                        )
                    )
                )
        );

        dispatcher.register(
            Commands.literal("eh")
                .then(Commands.literal("settheme")
                    .requires(UtilPermissions::canCreate)
                    .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(SUGGEST_SCOREBOARD_IDS)
                        .then(Commands.argument("theme", StringArgumentType.word())
                            .suggests(SUGGEST_THEMES)
                            .executes(this::run)
                        )
                    )
                )
        );
    }

    @Override
    public int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!UtilPermissions.canCreate(source)) {
            source.sendFailure(Component.literal("§cYou don't have permission to edit holograms!"));
            return 0;
        }

        String id = StringArgumentType.getString(context, "id");
        String theme = StringArgumentType.getString(context, "theme");

        Optional<Hologram> holo = HologramManager.getHologram(id);
        if (holo.isEmpty()) {
            source.sendFailure(Component.literal("§cNo hologram with ID '" + id + "' exists!"));
            return 0;
        }
        if (!(holo.get() instanceof ScoreboardHologram scoreboard)) {
            source.sendFailure(Component.literal("§cHologram '" + id + "' is not a scoreboard hologram!"));
            return 0;
        }
        if (ScoreboardThemeManager.getTheme(theme) == null) {
            source.sendFailure(Component.literal("§cUnknown theme '" + theme + "'. Check config/eliteholograms/scoreboard_themes.json"));
            return 0;
        }

        if (!scoreboard.applyTheme(theme)) {
            source.sendFailure(Component.literal("§cFailed to apply theme '" + theme + "'."));
            return 0;
        }

        try {
            HologramManager.save();
        } catch (IOException e) {
            // Display already updated in-memory; surface the save error but don't fail the command.
            source.sendFailure(Component.literal("§eTheme applied, but saving failed: " + e.getMessage()));
        }

        source.sendSuccess(() -> Component.literal(
                "§aSet theme of scoreboard hologram '" + id + "' to '" + theme + "'"), false);
        return 1;
    }

    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        return run(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("settheme")
                .then(Commands.argument("id", StringArgumentType.word())
                    .suggests(SUGGEST_SCOREBOARD_IDS)
                    .then(Commands.argument("theme", StringArgumentType.word())
                        .suggests(SUGGEST_THEMES)));
    }
}
