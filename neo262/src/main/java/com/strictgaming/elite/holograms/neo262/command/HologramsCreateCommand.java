package com.strictgaming.elite.holograms.neo262.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo262.hologram.HologramDisplayType;
import com.strictgaming.elite.holograms.neo262.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo262.hologram.implementation.NeoForgeHologram;
import com.strictgaming.elite.holograms.neo262.hologram.implementation.NeoForgeHologramBuilder;

import net.minecraft.server.permissions.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Command to create a new hologram.
 *
 * <p>Supports {@code /eh create <id> [text]}, which keeps the original behaviour of a
 * player-facing hologram, plus {@code /eh create facing|fixed <id> [text]} to pick the display
 * type up front. A fixed hologram is oriented to face whoever created it.
 */
public class HologramsCreateCommand implements HologramsCommand.SubCommand {

    private static final String[] DEFAULT_LINES = {"§6Example Hologram", "§7Line 2", "§bLine 3"};

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
        return Commands.literal("create")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                // Explicit display type: /eh create fixed|facing <id> [text].
                // Brigadier matches literals before arguments, so these take precedence over
                // the bare <id> form below.
                .then(displayTypeBranch("facing", HologramDisplayType.FACING))
                .then(displayTypeBranch("fixed", HologramDisplayType.FIXED))
                // Original form: /eh create <id> [text]
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> create(context, HologramDisplayType.FACING))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(context -> create(context, HologramDisplayType.FACING))));
    }

    private LiteralArgumentBuilder<CommandSourceStack> displayTypeBranch(String literal, HologramDisplayType displayType) {
        return Commands.literal(literal)
                .then(Commands.argument("id", StringArgumentType.word())
                        .executes(context -> create(context, displayType))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(context -> create(context, displayType))));
    }

    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        return create(context, HologramDisplayType.FACING);
    }

    private int create(CommandContext<CommandSourceStack> context, HologramDisplayType displayType) {
        CommandSourceStack source = context.getSource();

        try {
            String id = StringArgumentType.getString(context, "id");
            String text = null;

            // Text is optional
            try {
                text = StringArgumentType.getString(context, "text");
            } catch (IllegalArgumentException e) {
                // Left null, default lines are used below
            }

            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("§cThis command can only be used by players"));
                return 0;
            }

            if (HologramManager.getHologram(id).isPresent()) {
                source.sendFailure(Component.literal("§cA hologram with this ID already exists"));
                return 0;
            }

            Vec3 pos = player.position();
            String worldName = player.level().dimension().identifier().toString();

            // A fixed hologram keeps whatever rotation it is given, so start it facing the
            // creator. Pitch stays level; /eh setrotation can tilt it afterwards.
            float yaw = displayType == HologramDisplayType.FIXED ? player.getYRot() - 180.0F : 0.0F;

            NeoForgeHologramBuilder builder = new NeoForgeHologramBuilder()
                    .id(id)
                    .world(worldName)
                    .position(pos.x, pos.y - 0.5, pos.z)
                    .displayType(displayType)
                    .rotation(yaw, 0.0F);

            if (text != null && !text.isEmpty()) {
                builder.lines(text);
            } else {
                builder.lines(DEFAULT_LINES);
            }

            Hologram hologram = builder.buildAndSpawn();

            if (hologram instanceof NeoForgeHologram created && displayType == HologramDisplayType.FIXED) {
                source.sendSuccess(() -> Component.literal("§aCreated fixed hologram §f" + id
                        + "§a, oriented to face you §7(yaw " + Math.round(created.getYaw()) + ")"), true);
            } else {
                source.sendSuccess(() -> Component.literal("§aCreated hologram with ID: " + id), true);
            }

            return 1;

        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError creating hologram: " + e.getMessage()));
            return 0;
        }
    }
}
