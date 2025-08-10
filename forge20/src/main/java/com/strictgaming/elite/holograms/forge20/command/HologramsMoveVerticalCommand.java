package com.strictgaming.elite.holograms.forge20.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

/**
 * Moves a hologram vertically by a delta in blocks
 * Usage: /eh movevertical <id> <amount>
 */
public class HologramsMoveVerticalCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }

    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        if (args.length < 2) {
            context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cUsage: /eh movevertical <id> <up|down> <amount> or <amount> (positive up, negative down)"));
            return 0;
        }

        String id = args[0];
        String dirOrAmount = args[1];
        String amountStr = args.length >= 3 ? args[2] : args[1];

        Hologram hologram = HologramManager.getById(id);
        if (hologram == null) {
            context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cHologram with ID '§f" + id + "§c' not found!"));
            return 0;
        }

        double delta;
        try {
            delta = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cAmount must be a number (e.g., 1.5)"));
            return 0;
        }

        // Interpret direction keyword if present
        if (args.length >= 3) {
            if ("down".equalsIgnoreCase(dirOrAmount)) delta = -Math.abs(delta);
            else if ("up".equalsIgnoreCase(dirOrAmount)) delta = Math.abs(delta);
        }

        try {
            double[] loc = hologram.getLocation();
            hologram.move(loc[0], loc[1] + delta, loc[2]);
            context.getSource().sendSystemMessage(Component.literal("§a§l(!) §aMoved hologram '§f" + id + "§a' by " + delta + " blocks vertically."));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendSystemMessage(Component.literal("§c§l(!) §cError moving hologram: §f" + e.getMessage()));
            return 0;
        }
    }
}


