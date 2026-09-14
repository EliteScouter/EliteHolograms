package com.strictgaming.elite.holograms.neo21.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo21.hologram.implementation.NeoForgeHologram;
import com.strictgaming.elite.holograms.neo21.util.UtilChatColour;
import com.strictgaming.elite.holograms.neo21.util.UtilPermissions;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.Optional;

/**
 * Makes a hologram run a command when a player clicks it.
 *
 * <pre>
 *   /eh click &lt;id&gt; set &lt;command...&gt;
 *   /eh click &lt;id&gt; console &lt;command...&gt;
 *   /eh click &lt;id&gt; none
 * </pre>
 *
 * <p>{@code set} runs the command as the clicking player, with their own permissions.
 * {@code console} runs it elevated, which is what you want for something like a warp that
 * players are not otherwise allowed to perform.
 *
 * <p>{@code %player%} in the command is replaced with the clicking player's name.
 *
 * <p>Clicking works by putting a real, invisible {@code minecraft:interaction} hitbox in the
 * world over the hologram's text, so it only applies to holograms that are currently spawned.
 */
public class HologramsClickCommand implements HologramsCommand.SubCommand {

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
        return Commands.literal("click")
                .requires(UtilPermissions::canEdit)
                .then(Commands.argument("id", StringArgumentType.string())
                        .suggests(SUGGEST_HOLOGRAMS)
                        .then(Commands.literal("set")
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .executes(context -> apply(context, false))))
                        .then(Commands.literal("console")
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .executes(context -> apply(context, true))))
                        .then(Commands.literal("none")
                                .executes(this::clear)));
    }

    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        return clear(context);
    }

    private int clear(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            String id = StringArgumentType.getString(context, "id");
            Optional<Hologram> hologramOpt = HologramManager.getHologram(id);

            if (hologramOpt.isEmpty()) {
                source.sendFailure(UtilChatColour.parse("&cNo hologram found with ID: &f" + id));
                return 0;
            }

            if (!(hologramOpt.get() instanceof NeoForgeHologram hologram)) {
                source.sendFailure(UtilChatColour.parse("&cThis hologram cannot be made clickable"));
                return 0;
            }

            hologram.setClickCommand(null, false);
            source.sendSuccess(() -> UtilChatColour.parse("&f" + id + " &ais no longer clickable"), true);
            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            source.sendFailure(UtilChatColour.parse("&cError clearing click command: " + e.getMessage()));
            return 0;
        }
    }

    private int apply(CommandContext<CommandSourceStack> context, boolean asConsole) {
        CommandSourceStack source = context.getSource();

        try {
            String id = StringArgumentType.getString(context, "id");
            Optional<Hologram> hologramOpt = HologramManager.getHologram(id);

            if (hologramOpt.isEmpty()) {
                source.sendFailure(UtilChatColour.parse("&cNo hologram found with ID: &f" + id));
                return 0;
            }

            if (!(hologramOpt.get() instanceof NeoForgeHologram hologram)) {
                source.sendFailure(UtilChatColour.parse("&cThis hologram cannot be made clickable"));
                return 0;
            }

            // Running elevated is a privilege escalation: anyone who can edit holograms could
            // otherwise hand themselves operator commands through a sign. Gate it on admin, not
            // on the edit permission that covers the rest of this command.
            if (asConsole && !UtilPermissions.canAdmin(source)) {
                source.sendFailure(UtilChatColour.parse("&cRunning a click command elevated needs the "
                        + "admin permission. Use &f/eh click " + id + " set ...&c to run it as the player."));
                return 0;
            }

            String command = StringArgumentType.getString(context, "command").trim();

            // Stored without the slash so the dispatcher gets what it expects either way.
            if (command.startsWith("/")) {
                command = command.substring(1);
            }

            hologram.setClickCommand(command, asConsole);

            final String shown = command;
            source.sendSuccess(() -> UtilChatColour.parse("&f" + id + " &anow runs &f/" + shown
                    + " &awhen clicked, as &f" + (asConsole ? "console" : "the player")), true);

            if (!hologram.hasClickHitbox()) {
                source.sendSuccess(() -> UtilChatColour.parse("&7Saved, but &f" + id
                        + " &7is not currently spawned, so there is no hitbox to click yet."), false);
            }

            return Command.SINGLE_SUCCESS;

        } catch (Exception e) {
            source.sendFailure(UtilChatColour.parse("&cError setting click command: " + e.getMessage()));
            return 0;
        }
    }
}
