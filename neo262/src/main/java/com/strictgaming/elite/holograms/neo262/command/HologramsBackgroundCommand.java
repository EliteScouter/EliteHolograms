package com.strictgaming.elite.holograms.neo262.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo262.hologram.HologramDisplayType;
import com.strictgaming.elite.holograms.neo262.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo262.hologram.implementation.NeoForgeHologram;
import com.strictgaming.elite.holograms.neo262.util.UtilChatColour;
import com.strictgaming.elite.holograms.neo262.util.UtilPermissions;

import net.minecraft.network.chat.TextColor;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Controls the panel drawn behind a fixed hologram's text.
 *
 * <pre>
 *   /eh background &lt;id&gt; colour &lt;colour&gt;
 *   /eh background &lt;id&gt; opacity &lt;0-100&gt;
 *   /eh background &lt;id&gt; none
 *   /eh background &lt;id&gt; reset
 * </pre>
 *
 * <p>{@code colour} takes any of the sixteen Minecraft colour names or a hex value such as
 * {@code #1E90FF}. {@code opacity} runs from 0 (invisible) to 100 (solid), matching the
 * wording of Minecraft's own Chat Background Opacity setting, which is the same idea applied
 * to nameplates.
 *
 * <p>This only changes what players see on a fixed hologram. A player-facing hologram's text is
 * an armor stand nameplate, and the client draws that background using the viewer's own chat
 * background opacity, which no server-side mod can override.
 */
public class HologramsBackgroundCommand implements HologramsCommand.SubCommand {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_HOLOGRAMS = (context, builder) ->
            SharedSuggestionProvider.suggest(HologramManager.getHolograms().keySet(), builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_COLOURS = (context, builder) ->
            SharedSuggestionProvider.suggest(colourNames(), builder);

    /**
     * Registers this command with the given dispatcher
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("eliteholograms").then(buildArguments()));
        dispatcher.register(Commands.literal("eh").then(buildArguments()));
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return buildArguments();
    }

    private LiteralArgumentBuilder<CommandSourceStack> buildArguments() {
        return Commands.literal("background")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(SUGGEST_HOLOGRAMS)
                        // Colour is greedy so a hex value keeps its leading "#": brigadier's
                        // word() only accepts [a-zA-Z0-9_.+-] and would reject "#1E90FF".
                        .then(Commands.literal("colour")
                                .then(Commands.argument("colour", StringArgumentType.greedyString())
                                        .suggests(SUGGEST_COLOURS)
                                        .executes(context -> apply(context, "colour"))))
                        .then(Commands.literal("opacity")
                                .then(Commands.argument("percent", IntegerArgumentType.integer(0, 100))
                                        .executes(context -> apply(context, "opacity"))))
                        .then(Commands.literal("none")
                                .executes(context -> apply(context, "none")))
                        .then(Commands.literal("reset")
                                .executes(context -> apply(context, "reset"))));
    }

    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        return apply(context, "reset");
    }

    private int apply(CommandContext<CommandSourceStack> context, String mode) {
        CommandSourceStack source = context.getSource();

        try {
            String id = StringArgumentType.getString(context, "id");
            Optional<Hologram> hologramOpt = HologramManager.getHologram(id);

            if (hologramOpt.isEmpty()) {
                source.sendFailure(UtilChatColour.parse("&cNo hologram found with ID: &f" + id));
                return 0;
            }

            if (!(hologramOpt.get() instanceof NeoForgeHologram hologram)) {
                source.sendFailure(UtilChatColour.parse("&cThis hologram does not support backgrounds"));
                return 0;
            }

            int colour = hologram.getBackgroundColour();
            int opacity = hologram.getBackgroundOpacity();

            switch (mode) {
                case "colour" -> {
                    String raw = StringArgumentType.getString(context, "colour");
                    Integer parsed = parseColour(raw);

                    if (parsed == null) {
                        source.sendFailure(UtilChatColour.parse("&cUnknown colour &f" + raw
                                + "&c. Use a colour name or a hex value like &f#1E90FF&c."));
                        return 0;
                    }

                    colour = parsed;
                }
                case "opacity" -> opacity = IntegerArgumentType.getInteger(context, "percent");
                case "none" -> opacity = 0;
                default -> {
                    colour = NeoForgeHologram.DEFAULT_BACKGROUND_COLOUR;
                    opacity = NeoForgeHologram.DEFAULT_BACKGROUND_OPACITY;
                }
            }

            hologram.setBackground(colour, opacity);

            final int shownColour = hologram.getBackgroundColour();
            final int shownOpacity = hologram.getBackgroundOpacity();

            source.sendSuccess(() -> shownOpacity == 0
                    ? UtilChatColour.parse("&aBackground hidden for &f" + id)
                    : UtilChatColour.parse("&aBackground of &f" + id + " &aset to &f"
                            + String.format("#%06X", shownColour) + " &aat &f" + shownOpacity + "%&a opacity"), true);

            if (hologram.getDisplayType() != HologramDisplayType.FIXED) {
                source.sendSuccess(() -> UtilChatColour.parse("&7Saved, but &f" + id
                        + " &7is player-facing so nothing changes on screen. Its text is an armor stand "
                        + "nameplate and the client draws that background itself. "
                        + "Run &f/eh convert " + id + " fixed &7to apply it."), false);
            }

            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            source.sendFailure(UtilChatColour.parse("&cError setting background: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Resolves a colour name or hex string to an RGB value.
     *
     * @param input a Minecraft colour name such as {@code red}, or hex such as {@code #1E90FF}
     * @return the RGB value, or null when the input is not a colour
     */
    public static Integer parseColour(String input) {
        if (input == null) {
            return null;
        }

        String value = input.trim();

        if (value.isEmpty()) {
            return null;
        }

        // 26.2 moved the colour table off ChatFormatting, which no longer carries RGB values,
        // onto TextColor. Its parser already understands both "#RRGGBB" and the sixteen names,
        // so bare hex is normalised to the "#" form it expects.
        String normalised = value.toLowerCase(Locale.ROOT);

        if (!normalised.startsWith("#") && normalised.length() == 6
                && normalised.chars().allMatch(c -> Character.digit(c, 16) >= 0)) {
            normalised = "#" + normalised;
        }

        return TextColor.parseColor(normalised)
                .result()
                .map(TextColor::getValue)
                .orElse(null);
    }

    /**
     * @return the colour names accepted by this command, for tab completion
     */
    public static List<String> colourNames() {
        // Listed rather than derived: 26.2's ChatFormatting no longer knows which of its
        // constants are colours, and TextColor keeps its name table private.
        return List.of(
                "black", "dark_blue", "dark_green", "dark_aqua",
                "dark_red", "dark_purple", "gold", "gray",
                "dark_gray", "blue", "green", "aqua",
                "red", "light_purple", "yellow", "white");
    }
}
