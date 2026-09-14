package com.strictgaming.elite.holograms.forge20.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge20.hologram.ForgeHologram;
import com.strictgaming.elite.holograms.forge20.hologram.HologramDisplayType;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge20.hologram.entity.HologramTextDisplay;
import com.strictgaming.elite.holograms.forge20.util.UtilPermissions;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Display;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controls how a fixed hologram's text is presented.
 *
 * <pre>
 *   /eh display &lt;id&gt; scale &lt;0.1-10&gt;
 *   /eh display &lt;id&gt; brightness &lt;0-15|auto&gt;
 *   /eh display &lt;id&gt; billboard &lt;fixed|vertical|horizontal|center&gt;
 *   /eh display &lt;id&gt; reset
 * </pre>
 *
 * <p>These are grouped under one command rather than three top-level ones so the {@code /eh}
 * tab-completion list stays readable.
 *
 * <p>None of them apply to a player-facing hologram. Its text is an armor stand nameplate, which
 * the client draws at a fixed size, always turned towards the camera, and lit by the world - the
 * server has no say in any of it. The values are still stored so they take effect if the hologram
 * is later converted.
 */
public class HologramsDisplayCommand implements Command<CommandSourceStack> {

    /** Scaling below this is unreadable, above it the text dwarfs most builds. */
    private static final float MIN_SCALE = 0.1F;
    private static final float MAX_SCALE = 10.0F;

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }

    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        CommandSourceStack source = context.getSource();

        if (!UtilPermissions.canEdit(source)) {
            source.sendSystemMessage(Component.literal(
                    "§c§l(!) §cYou don't have permission to use the display command."));
            return 0;
        }

        if (args.length < 2) {
            sendUsage(source);
            return 0;
        }

        String id = args[0];
        String mode = args[1].toLowerCase(Locale.ROOT);

        Hologram hologram = HologramManager.getById(id);

        if (hologram == null) {
            source.sendSystemMessage(Component.literal("§c§l(!) §cHologram '§f" + id + "§c' not found."));
            return 0;
        }

        if (!(hologram instanceof ForgeHologram fh)) {
            source.sendSystemMessage(Component.literal(
                    "§c§l(!) §cThis hologram type does not support display settings."));
            return 0;
        }

        String applied;

        switch (mode) {
            case "scale": {
                if (args.length < 3) {
                    source.sendSystemMessage(Component.literal(
                            "§c§l(!) §cUsage: /eh display <id> scale <" + MIN_SCALE + "-" + MAX_SCALE + ">"));
                    return 0;
                }

                float scale;

                try {
                    scale = Float.parseFloat(args[2].trim());
                } catch (NumberFormatException e) {
                    source.sendSystemMessage(Component.literal(
                            "§c§l(!) §c'§f" + args[2] + "§c' is not a number."));
                    return 0;
                }

                if (scale < MIN_SCALE || scale > MAX_SCALE) {
                    source.sendSystemMessage(Component.literal("§c§l(!) §cScale must be between §f"
                            + MIN_SCALE + "§c and §f" + MAX_SCALE + "§c."));
                    return 0;
                }

                fh.setScale(scale);
                applied = "scale §f" + scale;
                break;
            }
            case "brightness": {
                if (args.length < 3) {
                    source.sendSystemMessage(Component.literal(
                            "§c§l(!) §cUsage: /eh display <id> brightness <0-15|auto>"));
                    return 0;
                }

                String raw = args[2].trim().toLowerCase(Locale.ROOT);

                if (raw.equals("auto")) {
                    fh.setBrightness(HologramTextDisplay.BRIGHTNESS_AUTO);
                    applied = "brightness §fauto §a(lit by the world)";
                    break;
                }

                int level;

                try {
                    level = Integer.parseInt(raw);
                } catch (NumberFormatException e) {
                    source.sendSystemMessage(Component.literal(
                            "§c§l(!) §cBrightness must be §f0§c-§f15§c or §fauto§c."));
                    return 0;
                }

                if (level < 0 || level > 15) {
                    source.sendSystemMessage(Component.literal(
                            "§c§l(!) §cBrightness must be §f0§c-§f15§c or §fauto§c."));
                    return 0;
                }

                fh.setBrightness(level);
                applied = "brightness §f" + level;
                break;
            }
            case "billboard": {
                if (args.length < 3) {
                    source.sendSystemMessage(Component.literal(
                            "§c§l(!) §cUsage: /eh display <id> billboard <" + String.join("|", billboardNames()) + ">"));
                    return 0;
                }

                Display.BillboardConstraints billboard = parseBillboard(args[2]);

                if (billboard == null) {
                    source.sendSystemMessage(Component.literal("§c§l(!) §cUnknown billboard '§f" + args[2]
                            + "§c'. Use one of: §f" + String.join(", ", billboardNames()) + "§c."));
                    return 0;
                }

                fh.setBillboard(billboard);
                applied = "billboard §f" + billboard.getSerializedName();
                break;
            }
            case "reset": {
                fh.setScale(HologramTextDisplay.DEFAULT_SCALE);
                fh.setBrightness(HologramTextDisplay.BRIGHTNESS_AUTO);
                fh.setBillboard(Display.BillboardConstraints.FIXED);
                applied = "§fdefault display settings";
                break;
            }
            default: {
                sendUsage(source);
                return 0;
            }
        }

        final String message = applied;
        source.sendSystemMessage(Component.literal(
                "§a§l(!) §aSet " + message + "§a for '§f" + id + "§a'."));

        if (fh.getDisplayType() != HologramDisplayType.FIXED) {
            source.sendSystemMessage(Component.literal("§7Saved, but '§f" + id
                    + "§7' is player-facing, so nothing changes on screen. Its text is an armor stand "
                    + "nameplate, which the client sizes, turns and lights itself. Run §f/eh convert "
                    + id + " fixed §7to apply it."));
        }

        return Command.SINGLE_SUCCESS;
    }

    private void sendUsage(CommandSourceStack source) {
        source.sendSystemMessage(Component.literal(
                "§c§l(!) §cUsage: /eh display <id> <scale <" + MIN_SCALE + "-" + MAX_SCALE
                        + "> | brightness <0-15|auto> | billboard <"
                        + String.join("|", billboardNames()) + "> | reset>"));
    }

    /**
     * Resolves a billboard name.
     *
     * @param input the raw name
     * @return the matching constraint, or null when the name is not one
     */
    public static Display.BillboardConstraints parseBillboard(String input) {
        if (input == null) {
            return null;
        }

        String value = input.trim();

        for (Display.BillboardConstraints candidate : Display.BillboardConstraints.values()) {
            if (candidate.getSerializedName().equalsIgnoreCase(value)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * @return the billboard names accepted by this command, for tab completion
     */
    public static List<String> billboardNames() {
        List<String> names = new ArrayList<>();

        for (Display.BillboardConstraints candidate : Display.BillboardConstraints.values()) {
            names.add(candidate.getSerializedName());
        }

        return names;
    }
}
