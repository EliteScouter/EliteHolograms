package com.strictgaming.elite.holograms.forge20.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge20.hologram.ForgeHologram;
import com.strictgaming.elite.holograms.forge20.hologram.HologramDisplayType;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge20.util.UtilChatColour;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;

/**
 * Command to set the orientation of a fixed hologram.
 *
 * <p>{@code /eh setrotation <id> <yaw> [pitch]}. Yaw follows the in-game convention, so 0 faces
 * south, 90 west, 180 north and 270 east. The rotation is stored even for holograms that are
 * currently player-facing, so it applies as soon as they are converted to fixed.
 */
public class HologramsSetRotationCommand {

    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        CommandSourceStack source = context.getSource();

        if (args.length < 2) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUsage: /eh setrotation <id> <yaw> [pitch]"));
            return 0;
        }

        String id = args[0];
        Hologram hologram = HologramManager.getById(id);

        if (hologram == null) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cNo hologram found with ID: &f" + id));
            return 0;
        }

        if (!(hologram instanceof ForgeHologram)) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cThis hologram does not support rotation"));
            return 0;
        }

        float yaw;
        float pitch = 0.0F;

        try {
            yaw = Float.parseFloat(args[1]);

            if (args.length > 2) {
                pitch = Float.parseFloat(args[2]);
            }
        } catch (NumberFormatException e) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cYaw and pitch must be numbers"));
            return 0;
        }

        ForgeHologram forgeHologram = (ForgeHologram) hologram;
        forgeHologram.setRotation(yaw, pitch);

        source.sendSystemMessage(UtilChatColour.parse("&a&l(!) &aSet rotation of &f" + id
                + " &ato yaw &f" + forgeHologram.getYaw() + " &apitch &f" + forgeHologram.getPitch()));

        if (forgeHologram.getDisplayType() != HologramDisplayType.FIXED) {
            source.sendSystemMessage(UtilChatColour.parse("&7Saved, but &f" + id
                    + " &7is player-facing so nothing changes on screen. Run &f/eh convert "
                    + id + " fixed &7to apply it."));
        }

        return 1;
    }
}
