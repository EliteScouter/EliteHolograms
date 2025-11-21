package com.strictgaming.elite.holograms.neo21.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo21.hologram.ItemHologram;

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
            String worldName = player.level().dimension().location().toString();

            List<String> lines = text != null ? Arrays.asList(text.split("\\|")) : Collections.emptyList();

            ItemHologram hologram = new ItemHologram(
                id, worldName, pos.x, pos.y, pos.z, itemId, 
                lines
            );
            hologram.spawn();

            source.sendSuccess(() -> Component.literal("§aCreated item hologram with ID: " + id), true);
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
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("item", StringArgumentType.string())
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                             .executes(this::execute))
                        .executes(this::execute)));
    }
}
