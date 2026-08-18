package com.strictgaming.elite.holograms.neo21.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.hologram.HologramDisplayType;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo21.hologram.implementation.NeoForgeHologram;
import com.strictgaming.elite.holograms.neo21.util.UtilChatColour;
import com.strictgaming.elite.holograms.neo21.util.UtilPermissions;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Optional;

/**
 * Command to set the orientation of a fixed hologram.
 *
 * <p>{@code /eh setrotation <id> <yaw> [pitch]}. Yaw follows the in-game convention, so 0 faces
 * south, 90 west, 180 north and 270 east. The rotation is stored even for holograms that are
 * currently player-facing, so it applies as soon as they are converted to fixed.
 */
public class HologramsSetRotationCommand implements HologramsCommand.SubCommand {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_HOLOGRAMS = (context, builder) ->
            SharedSuggestionProvider.suggest(HologramManager.getHolograms().keySet(), builder);

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
        return Commands.literal("setrotation")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(SUGGEST_HOLOGRAMS)
                        .then(Commands.argument("yaw", FloatArgumentType.floatArg(-360.0F, 360.0F))
                                .executes(this::execute)
                                .then(Commands.argument("pitch", FloatArgumentType.floatArg(-90.0F, 90.0F))
                                        .executes(this::execute))));
    }

    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            String id = StringArgumentType.getString(context, "id");
            Optional<Hologram> hologramOpt = HologramManager.getHologram(id);

            if (hologramOpt.isEmpty()) {
                source.sendFailure(UtilChatColour.parse("&cNo hologram found with ID: &f" + id));
                return 0;
            }

            if (!(hologramOpt.get() instanceof NeoForgeHologram hologram)) {
                source.sendFailure(UtilChatColour.parse("&cThis hologram does not support rotation"));
                return 0;
            }

            float yaw = FloatArgumentType.getFloat(context, "yaw");
            float pitch = 0.0F;

            try {
                pitch = FloatArgumentType.getFloat(context, "pitch");
            } catch (IllegalArgumentException e) {
                // Pitch is optional and defaults to level
            }

            hologram.setRotation(yaw, pitch);

            final float appliedYaw = hologram.getYaw();
            final float appliedPitch = hologram.getPitch();

            source.sendSuccess(() -> UtilChatColour.parse("&aSet rotation of &f" + id
                    + " &ato yaw &f" + appliedYaw + " &apitch &f" + appliedPitch), true);

            if (hologram.getDisplayType() != HologramDisplayType.FIXED) {
                source.sendSuccess(() -> UtilChatColour.parse("&7Saved, but &f" + id
                        + " &7is player-facing so nothing changes on screen. "
                        + "Run &f/eh convert " + id + " fixed &7to apply it."), false);
            }

            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            source.sendFailure(UtilChatColour.parse("&cError setting rotation: " + e.getMessage()));
            return 0;
        }
    }
}
