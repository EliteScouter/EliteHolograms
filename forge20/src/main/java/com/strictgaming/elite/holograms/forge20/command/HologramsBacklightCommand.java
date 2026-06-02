package com.strictgaming.elite.holograms.forge20.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge20.hologram.ForgeHologram;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge20.util.UtilBacklight;
import com.strictgaming.elite.holograms.forge20.util.UtilPermissions;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Toggles an invisible {@code minecraft:light} block at a hologram's location
 * to light up the surrounding area, similar to WorldEdit's {@code //set light}.
 *
 * Usage:
 *   /eh backlight &lt;id&gt; on [height]
 *   /eh backlight &lt;id&gt; off
 *   /eh backlight &lt;id&gt; toggle
 *
 * <p>{@code height} is the number of light blocks in the vertical column
 * ({@code 1}-{@code 10}), rising from the ground behind the hologram.</p>
 */
public class HologramsBacklightCommand implements Command<CommandSourceStack> {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }

    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        CommandSourceStack source = context.getSource();

        if (!UtilPermissions.hasPermission(source, UtilPermissions.BACKLIGHT)) {
            source.sendSystemMessage(Component.literal("§c§l(!) §cYou don't have permission to use the backlight command."));
            return 0;
        }

        if (args.length < 2) {
            source.sendSystemMessage(Component.literal("§c§l(!) §cUsage: /eh backlight <id> <on|off|toggle> [height 1-10]"));
            return 0;
        }

        String id = args[0];
        String mode = args[1].toLowerCase();

        Hologram hologram = HologramManager.getById(id);
        if (hologram == null) {
            source.sendSystemMessage(Component.literal("§c§l(!) §cHologram '§f" + id + "§c' not found."));
            return 0;
        }
        if (!(hologram instanceof ForgeHologram fh)) {
            source.sendSystemMessage(Component.literal("§c§l(!) §cThis hologram type does not support backlights."));
            return 0;
        }

        int requestedLevel = UtilBacklight.DEFAULT_LEVEL;
        if (args.length >= 3) {
            try {
                requestedLevel = UtilBacklight.clampLevel(Integer.parseInt(args[2]));
            } catch (NumberFormatException e) {
                source.sendSystemMessage(Component.literal("§c§l(!) §cHeight must be a number 1-10."));
                return 0;
            }
        }

        boolean enable;
        int targetLevel;
        switch (mode) {
            case "on":
                enable = true;
                targetLevel = requestedLevel;
                break;
            case "off":
                enable = false;
                targetLevel = fh.getBacklightLevel();
                break;
            case "toggle":
                enable = !fh.isBacklightEnabled();
                targetLevel = enable ? requestedLevel : fh.getBacklightLevel();
                break;
            default:
                source.sendSystemMessage(Component.literal("§c§l(!) §cUnknown mode: §f" + mode + "§c. Use on/off/toggle."));
                return 0;
        }

        try {
            fh.setBacklight(enable, targetLevel);
        } catch (Exception e) {
            LOGGER.error("Failed to set backlight for hologram {}", id, e);
            source.sendSystemMessage(Component.literal("§c§l(!) §cError applying backlight: §f" + e.getMessage()));
            return 0;
        }

        if (enable) {
            source.sendSystemMessage(Component.literal(
                    "§a§l(!) §aBacklight enabled for '§f" + id + "§a' - column height §f" + fh.getBacklightLevel() + "§a block(s)."));
        } else {
            source.sendSystemMessage(Component.literal(
                    "§a§l(!) §aBacklight disabled for '§f" + id + "§a'."));
        }
        return Command.SINGLE_SUCCESS;
    }
}
