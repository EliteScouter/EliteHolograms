package com.strictgaming.elite.holograms.neo262.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.strictgaming.elite.holograms.neo262.hologram.HologramDisplayType;
import com.strictgaming.elite.holograms.neo262.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo262.hologram.ItemHologram;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HologramsCreateItemCommand implements HologramsCommand.SubCommand {

    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        return create(context, HologramDisplayType.FACING);
    }

    private int create(CommandContext<CommandSourceStack> context, HologramDisplayType displayType) {
        CommandSourceStack source = context.getSource();
        try {
            String id = StringArgumentType.getString(context, "id");
            String itemId = StringArgumentType.getString(context, "item");
            String text = null;
            try {
                text = StringArgumentType.getString(context, "text");
            } catch (IllegalArgumentException e) {}

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

            List<String> lines = text != null ? Arrays.asList(text.split("\\|")) : Collections.emptyList();

            // A fixed hologram keeps whatever rotation it is given, so start it facing the creator.
            float yaw = displayType == HologramDisplayType.FIXED ? player.getYRot() - 180.0F : 0.0F;

            ItemHologram hologram = new ItemHologram(
                id, worldName, pos.x, pos.y, pos.z, itemId, 
                lines, displayType, yaw, 0.0F
            );
            hologram.spawn();

            source.sendSuccess(() -> Component.literal("§aCreated " + displayType.getSerializedName()
                    + " item hologram with ID: " + id), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError creating hologram: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("createitem")
                // Explicit display type; literals match before the bare <id> argument.
                .then(Commands.literal("facing").then(itemArguments(HologramDisplayType.FACING)))
                .then(Commands.literal("fixed").then(itemArguments(HologramDisplayType.FIXED)))
                .then(itemArguments(HologramDisplayType.FACING));
    }

    /**
     * Builds the {@code <id> <item> [text]} chain for a given display type.
     */
    private com.mojang.brigadier.builder.RequiredArgumentBuilder<CommandSourceStack, String> itemArguments(
            HologramDisplayType displayType) {
        return Commands.argument("id", StringArgumentType.word())
                .then(Commands.argument("item", StringArgumentType.string())
                        .executes(context -> create(context, displayType))
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(context -> create(context, displayType))));
    }
}
