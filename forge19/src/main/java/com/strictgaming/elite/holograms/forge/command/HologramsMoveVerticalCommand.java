package com.strictgaming.elite.holograms.forge.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge.util.UtilChatColour;

import net.minecraft.commands.CommandSourceStack;

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
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUsage: /eh movevertical <id> <up|down> <amount> or <amount> (positive up, negative down)"));
            return 0;
        }

        String id = args[0];
        String dirOrAmount = args[1];
        String amountStr = args.length >= 3 ? args[2] : args[1];

        Hologram hologram = HologramManager.getById(id);
        if (hologram == null) {
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cHologram with ID '&f" + id + "&c' not found!"));
            return 0;
        }

        double delta;
        try {
            delta = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cAmount must be a number (e.g., 1.5)"));
            return 0;
        }

        // Interpret direction keyword if present
        if (args.length >= 3) {
            if ("down".equalsIgnoreCase(dirOrAmount)) delta = -Math.abs(delta);
            else if ("up".equalsIgnoreCase(dirOrAmount)) delta = Math.abs(delta);
        }

        try {
            if (!(hologram instanceof com.strictgaming.elite.holograms.forge.hologram.ForgeHologram)) {
                context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUnsupported hologram type for this platform"));
                return 0;
            }
            com.strictgaming.elite.holograms.forge.hologram.ForgeHologram fh = (com.strictgaming.elite.holograms.forge.hologram.ForgeHologram) hologram;
            net.minecraft.world.phys.Vec3 pos = fh.getPosition();
            String world = com.strictgaming.elite.holograms.forge.util.UtilWorld.getName(fh.getWorld());
            // Use teleport so world/position and line offsets are updated correctly (move() doesn't update base state)
            fh.teleport(world, pos.x, pos.y + delta, pos.z);
            context.getSource().sendSystemMessage(UtilChatColour.parse("&a&l(!) &aMoved hologram '&f" + id + "&a' by " + delta + " blocks vertically."));
            return Command.SINGLE_SUCCESS;
        } catch (Exception e) {
            context.getSource().sendSystemMessage(UtilChatColour.parse("&c&l(!) &cError moving hologram: &f" + e.getMessage()));
            return 0;
        }
    }
}


