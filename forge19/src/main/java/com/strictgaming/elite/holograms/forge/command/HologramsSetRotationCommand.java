package com.strictgaming.elite.holograms.forge.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.hologram.HologramDisplayType;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge.util.UtilChatColour;

import net.minecraft.commands.CommandSourceStack;

/**
 * Reports that setting a hologram's orientation is not available on Forge 1.19.2.
 *
 * <p>Usage: {@code /eh setrotation <id> <yaw> [pitch]}
 *
 * <p>Rotation only has meaning for fixed holograms, which are {@code text_display} entities.
 * That entity type arrived in Minecraft 1.19.4, so on 1.19.2 every hologram is an armor stand
 * nameplate that the client turns towards the viewer and a stored yaw/pitch could never be
 * applied. See {@link HologramDisplayType} for the full explanation.
 *
 * <p>The command still exists, validates its arguments and checks the hologram, so the command
 * surface matches the newer editions and administrators get a specific explanation instead of
 * a bare "Unknown command".
 */
public class HologramsSetRotationCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }

    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        CommandSourceStack source = context.getSource();

        if (args.length < 2) {
            source.sendSystemMessage(UtilChatColour.parse(
                    "&c&l(!) &cUsage: /eh setrotation <id> <yaw> [pitch]"));
            return 0;
        }

        String id = args[0];

        Hologram hologram = HologramManager.getById(id);
        if (hologram == null) {
            source.sendSystemMessage(UtilChatColour.parse(
                    "&c&l(!) &cHologram with ID '&f" + id + "&c' not found!"));
            return 0;
        }

        // Validate the numbers so a typo is reported as a typo rather than being masked by the
        // unsupported-platform message below.
        try {
            float yaw = Float.parseFloat(args[1]);
            if (yaw < -360.0F || yaw > 360.0F) {
                source.sendSystemMessage(UtilChatColour.parse(
                        "&c&l(!) &cYaw must be between &f-360&c and &f360&c."));
                return 0;
            }
        } catch (NumberFormatException e) {
            source.sendSystemMessage(UtilChatColour.parse(
                    "&c&l(!) &cYaw must be a number (e.g. &f90&c)."));
            return 0;
        }

        if (args.length >= 3) {
            try {
                float pitch = Float.parseFloat(args[2]);
                if (pitch < -90.0F || pitch > 90.0F) {
                    source.sendSystemMessage(UtilChatColour.parse(
                            "&c&l(!) &cPitch must be between &f-90&c and &f90&c."));
                    return 0;
                }
            } catch (NumberFormatException e) {
                source.sendSystemMessage(UtilChatColour.parse(
                        "&c&l(!) &cPitch must be a number (e.g. &f0&c)."));
                return 0;
            }
        }

        source.sendSystemMessage(UtilChatColour.parse(HologramDisplayType.UNSUPPORTED_MESSAGE));
        source.sendSystemMessage(UtilChatColour.parse(HologramDisplayType.UNSUPPORTED_HINT));
        return 0;
    }
}
