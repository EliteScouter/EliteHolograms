package com.strictgaming.elite.holograms.neo21.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo21.hologram.implementation.NeoForgeHologram;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.Optional;

public class HologramsAnimateLineCommand implements HologramsCommand.SubCommand {

    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        try {
            String id = StringArgumentType.getString(context, "id");
            int lineIndex = IntegerArgumentType.getInteger(context, "line") - 1;
            int interval = IntegerArgumentType.getInteger(context, "interval");
            String text = StringArgumentType.getString(context, "text");

            Optional<Hologram> hologramOpt = HologramManager.getHologram(id);
            if (hologramOpt.isEmpty()) {
                source.sendFailure(Component.literal("§cHologram not found"));
                return 0;
            }

            Hologram hologram = hologramOpt.get();
            if (!(hologram instanceof NeoForgeHologram)) {
                source.sendFailure(Component.literal("§cThis hologram type does not support animations"));
                return 0;
            }

            NeoForgeHologram forgeHologram = (NeoForgeHologram) hologram;
            
            if (lineIndex == forgeHologram.getLines().size()) {
                 forgeHologram.addAnimatedLine(Arrays.asList(text.split("\\|")), interval);
            } else if (lineIndex >= 0 && lineIndex < forgeHologram.getLines().size()) {
                 forgeHologram.setLineAnimated(lineIndex, Arrays.asList(text.split("\\|")), interval);
            } else {
                 source.sendFailure(Component.literal("§cInvalid line index"));
                 return 0;
            }

            source.sendSuccess(() -> Component.literal("§aAnimated line updated"), true);
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("§cError animating line: " + e.getMessage()));
            return 0;
        }
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("animateline")
                .then(Commands.argument("id", StringArgumentType.word())
                    .then(Commands.argument("line", IntegerArgumentType.integer(1))
                        .then(Commands.argument("interval", IntegerArgumentType.integer(1))
                            .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(this::execute)))));
    }
}

