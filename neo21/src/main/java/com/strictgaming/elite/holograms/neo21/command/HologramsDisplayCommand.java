package com.strictgaming.elite.holograms.neo21.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.hologram.HologramDisplayType;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo21.hologram.entity.HologramTextDisplay;
import com.strictgaming.elite.holograms.neo21.hologram.implementation.NeoForgeHologram;
import com.strictgaming.elite.holograms.neo21.util.UtilChatColour;
import com.strictgaming.elite.holograms.neo21.util.UtilPermissions;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.world.entity.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controls how a fixed hologram's text is presented.
 *
 * <pre>
 *   /eh display &lt;id&gt; scale &lt;0.1-10&gt;
 *   /eh display &lt;id&gt; brightness &lt;0-15|auto&gt;
 *   /eh display &lt;id&gt; billboard &lt;fixed|vertical|horizontal|center&gt;
 *   /eh display &lt;id&gt; reset
 * </pre>
 *
 * <p>These are grouped under one command rather than three top-level ones so the {@code /eh}
 * tab-completion list stays readable.
 *
 * <p>None of them apply to a player-facing hologram. Its text is an armor stand nameplate, which
 * the client draws at a fixed size, always turned towards the camera, and lit by the world, so the
 * server has no say in any of it. The values are still stored so they take effect if the hologram
 * is later converted.
 */
public class HologramsDisplayCommand implements HologramsCommand.SubCommand {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_HOLOGRAMS = (context, builder) ->
            SharedSuggestionProvider.suggest(HologramManager.getHolograms().keySet(), builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_BILLBOARDS = (context, builder) ->
            SharedSuggestionProvider.suggest(billboardNames(), builder);

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
        return Commands.literal("display")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(SUGGEST_HOLOGRAMS)
                        .then(Commands.literal("scale")
                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.1F, 10.0F))
                                        .executes(context -> apply(context, "scale"))))
                        .then(Commands.literal("brightness")
                                .then(Commands.literal("auto")
                                        .executes(context -> apply(context, "brightness-auto")))
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, 15))
                                        .executes(context -> apply(context, "brightness"))))
                        .then(Commands.literal("billboard")
                                .then(Commands.argument("mode", StringArgumentType.word())
                                        .suggests(SUGGEST_BILLBOARDS)
                                        .executes(context -> apply(context, "billboard"))))
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
                source.sendFailure(UtilChatColour.parse("&cThis hologram does not support display settings"));
                return 0;
            }

            String applied;

            switch (mode) {
                case "scale" -> {
                    float scale = FloatArgumentType.getFloat(context, "value");
                    hologram.setScale(scale);
                    applied = "scale &f" + scale;
                }
                case "brightness" -> {
                    int level = IntegerArgumentType.getInteger(context, "level");
                    hologram.setBrightness(level);
                    applied = "brightness &f" + level;
                }
                case "brightness-auto" -> {
                    hologram.setBrightness(HologramTextDisplay.BRIGHTNESS_AUTO);
                    applied = "brightness &fauto &a(lit by the world)";
                }
                case "billboard" -> {
                    String raw = StringArgumentType.getString(context, "mode");
                    Display.BillboardConstraints billboard = parseBillboard(raw);

                    if (billboard == null) {
                        source.sendFailure(UtilChatColour.parse("&cUnknown billboard &f" + raw
                                + "&c. Use one of: &f" + String.join(", ", billboardNames()) + "&c."));
                        return 0;
                    }

                    hologram.setBillboard(billboard);
                    applied = "billboard &f" + billboard.getSerializedName();
                }
                default -> {
                    hologram.setScale(HologramTextDisplay.DEFAULT_SCALE);
                    hologram.setBrightness(HologramTextDisplay.BRIGHTNESS_AUTO);
                    hologram.setBillboard(Display.BillboardConstraints.FIXED);
                    applied = "&fdefault display settings";
                }
            }

            final String message = applied;
            source.sendSuccess(() -> UtilChatColour.parse("&aSet " + message + " &afor &f" + id), true);

            if (hologram.getDisplayType() != HologramDisplayType.FIXED) {
                source.sendSuccess(() -> UtilChatColour.parse("&7Saved, but &f" + id
                        + " &7is player-facing so nothing changes on screen. Its text is an armor stand "
                        + "nameplate, which the client sizes, turns and lights itself. "
                        + "Run &f/eh convert " + id + " fixed &7to apply it."), false);
            }

            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            source.sendFailure(UtilChatColour.parse("&cError setting display option: " + e.getMessage()));
            return 0;
        }
    }

    /**
     * Resolves a billboard name.
     *
     * @param input the raw name
     * @return the matching constraint, or null when the name is not one
     */
    public static Display.BillboardConstraints parseBillboard(String input) {
        if (input == null) {
            return null;
        }

        for (Display.BillboardConstraints candidate : Display.BillboardConstraints.values()) {
            if (candidate.getSerializedName().equalsIgnoreCase(input.trim())) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * @return the billboard names accepted by this command, for tab completion
     */
    public static List<String> billboardNames() {
        List<String> names = new ArrayList<>();

        for (Display.BillboardConstraints candidate : Display.BillboardConstraints.values()) {
            names.add(candidate.getSerializedName());
        }

        return names;
    }
}
