package com.strictgaming.elite.holograms.fabric.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.fabric.hologram.FabricHologram;
import com.strictgaming.elite.holograms.fabric.hologram.HologramDisplayType;
import com.strictgaming.elite.holograms.fabric.hologram.HologramManager;
import com.strictgaming.elite.holograms.fabric.util.UtilChatColour;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Command to switch a hologram between display types.
 *
 * <p>{@code /eh convert <id> fixed|face}. Converting to fixed will orient the hologram towards
 * the player running the command unless it already has a rotation set, so a converted hologram
 * is readable straight away.
 */
public class HologramsConvertCommand {

    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        CommandSourceStack source = context.getSource();

        if (args.length < 2) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUsage: /eh convert <id> fixed|face"));
            return 0;
        }

        String id = args[0];
        String rawType = args[1];

        HologramDisplayType displayType = HologramDisplayType.fromString(rawType);

        if (displayType == null) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUnknown display type &f" + rawType
                    + "&c. Use &ffixed &cor &fface&c."));
            return 0;
        }

        Hologram hologram = HologramManager.getById(id);

        if (hologram == null) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cNo hologram found with ID: &f" + id));
            return 0;
        }

        if (!(hologram instanceof FabricHologram)) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cThis hologram cannot be converted"));
            return 0;
        }

        FabricHologram forgeHologram = (FabricHologram) hologram;

        if (forgeHologram.getDisplayType() == displayType) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &f" + id + " &cis already "
                    + displayType.getSerializedName()));
            return 0;
        }

        // Point a newly fixed hologram at whoever converted it, but never overwrite a rotation
        // that was set deliberately.
        boolean orientedToPlayer = false;

        if (displayType == HologramDisplayType.FIXED
                && forgeHologram.getYaw() == 0.0F
                && forgeHologram.getPitch() == 0.0F
                && source.getEntity() instanceof ServerPlayer player) {
            forgeHologram.setRotation(player.getYRot() - 180.0F, 0.0F);
            orientedToPlayer = true;
        }

        forgeHologram.setDisplayType(displayType);

        if (displayType == HologramDisplayType.FIXED) {
            source.sendSystemMessage(UtilChatColour.parse("&a&l(!) &aConverted &f" + id
                    + " &ato a fixed hologram &7(yaw " + Math.round(forgeHologram.getYaw())
                    + (orientedToPlayer ? ", facing you" : "") + ")"));
        } else {
            source.sendSystemMessage(UtilChatColour.parse("&a&l(!) &aConverted &f" + id
                    + " &ato a player-facing hologram"));
        }

        return 1;
    }
}
