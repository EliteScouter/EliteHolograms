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
 * Switches a hologram between display types.
 *
 * <p>Usage: {@code /eh convert <id> fixed|face}
 *
 * <p>On Forge 1.19.2 every hologram is already player-facing, so converting to {@code face} is
 * a no-op that reports success, and converting to {@code fixed} explains why it cannot be done.
 * Fixed holograms are {@code text_display} entities, which Minecraft added in 1.19.4. See
 * {@link HologramDisplayType}.
 */
public class HologramsConvertCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }

    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        CommandSourceStack source = context.getSource();

        if (args.length < 2) {
            source.sendSystemMessage(UtilChatColour.parse(
                    "&c&l(!) &cUsage: /eh convert <id> <fixed|face>"));
            return 0;
        }

        String id = args[0];
        String rawType = args[1];

        HologramDisplayType displayType = HologramDisplayType.fromString(rawType);
        if (displayType == null) {
            source.sendSystemMessage(UtilChatColour.parse(
                    "&c&l(!) &cUnknown display type '&f" + rawType + "&c'. Use &ffixed &cor &fface&c."));
            return 0;
        }

        Hologram hologram = HologramManager.getById(id);
        if (hologram == null) {
            source.sendSystemMessage(UtilChatColour.parse(
                    "&c&l(!) &cHologram with ID '&f" + id + "&c' not found!"));
            return 0;
        }

        if (!displayType.isSupported()) {
            source.sendSystemMessage(UtilChatColour.parse(HologramDisplayType.UNSUPPORTED_MESSAGE));
            source.sendSystemMessage(UtilChatColour.parse(HologramDisplayType.UNSUPPORTED_HINT));
            return 0;
        }

        // FACING is the only mode this edition renders, so the hologram is already there.
        source.sendSystemMessage(UtilChatColour.parse(
                "&e&l(!) &f" + id + " &eis already a player-facing hologram."));
        source.sendSystemMessage(UtilChatColour.parse(
                "&7On Forge &f1.19.2&7 every hologram is player-facing, so there is nothing to convert."));
        return Command.SINGLE_SUCCESS;
    }
}
