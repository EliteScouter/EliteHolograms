package com.strictgaming.elite.holograms.neo21.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
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
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Optional;

/**
 * Command to switch a hologram between display types.
 *
 * <p>{@code /eh convert <id> fixed|face}. Converting to fixed will orient the hologram towards
 * the player running the command unless it already has a rotation set, so a converted hologram
 * is readable straight away.
 */
public class HologramsConvertCommand implements HologramsCommand.SubCommand {

    private static final List<String> TYPE_SUGGESTIONS = List.of("fixed", "face", "facing");

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_HOLOGRAMS = (context, builder) ->
            SharedSuggestionProvider.suggest(HologramManager.getHolograms().keySet(), builder);

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_TYPES = (context, builder) ->
            SharedSuggestionProvider.suggest(TYPE_SUGGESTIONS, builder);

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
        return Commands.literal("convert")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(SUGGEST_HOLOGRAMS)
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests(SUGGEST_TYPES)
                                .executes(this::execute)));
    }

    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            String id = StringArgumentType.getString(context, "id");
            String rawType = StringArgumentType.getString(context, "type");

            HologramDisplayType displayType = HologramDisplayType.fromString(rawType);

            if (displayType == null) {
                source.sendFailure(UtilChatColour.parse("&cUnknown display type &f" + rawType
                        + "&c. Use &ffixed &cor &fface&c."));
                return 0;
            }

            Optional<Hologram> hologramOpt = HologramManager.getHologram(id);

            if (hologramOpt.isEmpty()) {
                source.sendFailure(UtilChatColour.parse("&cNo hologram found with ID: &f" + id));
                return 0;
            }

            if (!(hologramOpt.get() instanceof NeoForgeHologram hologram)) {
                source.sendFailure(UtilChatColour.parse("&cThis hologram cannot be converted"));
                return 0;
            }

            if (hologram.getDisplayType() == displayType) {
                source.sendFailure(UtilChatColour.parse("&f" + id + " &cis already "
                        + displayType.getSerializedName()));
                return 0;
            }

            // Point a newly fixed hologram at whoever converted it, but never overwrite a
            // rotation that was set deliberately.
            boolean orientedToPlayer = false;

            if (displayType == HologramDisplayType.FIXED
                    && hologram.getYaw() == 0.0F
                    && hologram.getPitch() == 0.0F
                    && source.getEntity() instanceof ServerPlayer player) {
                hologram.setRotation(player.getYRot() - 180.0F, 0.0F);
                orientedToPlayer = true;
            }

            hologram.setDisplayType(displayType);

            final boolean wasOriented = orientedToPlayer;
            final float appliedYaw = hologram.getYaw();

            source.sendSuccess(() -> {
                if (displayType == HologramDisplayType.FIXED) {
                    return UtilChatColour.parse("&aConverted &f" + id + " &ato a fixed hologram &7(yaw "
                            + Math.round(appliedYaw) + (wasOriented ? ", facing you" : "") + ")");
                }

                return UtilChatColour.parse("&aConverted &f" + id + " &ato a player-facing hologram");
            }, true);

            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            source.sendFailure(UtilChatColour.parse("&cError converting hologram: " + e.getMessage()));
            return 0;
        }
    }
}
