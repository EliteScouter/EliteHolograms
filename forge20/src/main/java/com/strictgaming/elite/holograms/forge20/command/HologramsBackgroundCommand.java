package com.strictgaming.elite.holograms.forge20.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge20.hologram.ForgeHologram;
import com.strictgaming.elite.holograms.forge20.hologram.HologramDisplayType;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge20.util.UtilPermissions;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Controls the panel drawn behind a fixed hologram's text.
 *
 * <pre>
 *   /eh background &lt;id&gt; colour &lt;colour&gt;
 *   /eh background &lt;id&gt; opacity &lt;0-100&gt;
 *   /eh background &lt;id&gt; none
 *   /eh background &lt;id&gt; reset
 * </pre>
 *
 * <p>{@code colour} takes any of the sixteen Minecraft colour names or a hex value such as
 * {@code #1E90FF}. {@code opacity} runs from 0 (invisible) to 100 (solid), matching the
 * wording of Minecraft's own Chat Background Opacity setting, which is the same idea applied
 * to nameplates.
 *
 * <p>This only changes what players see on a fixed hologram. A player-facing hologram's text is
 * an armor stand nameplate, and the client draws that background using the viewer's own chat
 * background opacity, which no server-side mod can override.
 */
public class HologramsBackgroundCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }

    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        CommandSourceStack source = context.getSource();

        if (!UtilPermissions.canEdit(source)) {
            source.sendSystemMessage(Component.literal(
                    "§c§l(!) §cYou don't have permission to use the background command."));
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
                    "§c§l(!) §cThis hologram type does not support backgrounds."));
            return 0;
        }

        int colour = fh.getBackgroundColour();
        int opacity = fh.getBackgroundOpacity();

        switch (mode) {
            case "colour": {
                if (args.length < 3) {
                    source.sendSystemMessage(Component.literal(
                            "§c§l(!) §cUsage: /eh background <id> colour <name or #RRGGBB>"));
                    return 0;
                }

                Integer parsed = parseColour(args[2]);

                if (parsed == null) {
                    source.sendSystemMessage(Component.literal("§c§l(!) §cUnknown colour '§f" + args[2]
                            + "§c'. Use a colour name or a hex value like §f#1E90FF§c."));
                    return 0;
                }

                colour = parsed;
                break;
            }
            case "opacity": {
                Integer parsed = parsePercent(args, 2);

                if (parsed == null) {
                    source.sendSystemMessage(Component.literal(
                            "§c§l(!) §cUsage: /eh background <id> opacity <0-100>"));
                    return 0;
                }

                opacity = parsed;
                break;
            }
            case "none": {
                opacity = 0;
                break;
            }
            case "reset": {
                colour = ForgeHologram.DEFAULT_BACKGROUND_COLOUR;
                opacity = ForgeHologram.DEFAULT_BACKGROUND_OPACITY;
                break;
            }
            default: {
                sendUsage(source);
                return 0;
            }
        }

        fh.setBackground(colour, opacity);

        final int shownColour = fh.getBackgroundColour();
        final int shownOpacity = fh.getBackgroundOpacity();

        if (shownOpacity == 0) {
            source.sendSystemMessage(Component.literal(
                    "§a§l(!) §aBackground hidden for '§f" + id + "§a'."));
        } else {
            source.sendSystemMessage(Component.literal("§a§l(!) §aBackground of '§f" + id
                    + "§a' set to §f" + String.format("#%06X", shownColour)
                    + "§a at §f" + shownOpacity + "%§a opacity."));
        }

        if (fh.getDisplayType() != HologramDisplayType.FIXED) {
            source.sendSystemMessage(Component.literal("§7Saved, but '§f" + id
                    + "§7' is player-facing, so nothing changes on screen. Its text is an armor stand "
                    + "nameplate and the client draws that background itself. Run §f/eh convert " + id
                    + " fixed §7to apply it."));
        }

        return Command.SINGLE_SUCCESS;
    }

    private void sendUsage(CommandSourceStack source) {
        source.sendSystemMessage(Component.literal(
                "§c§l(!) §cUsage: /eh background <id> <colour <name|#RRGGBB> | opacity <0-100> "
                        + "| none | reset>"));
    }

    /**
     * Reads a percentage from the arguments.
     *
     * @return the value, or null when it is missing or not a number in 0-100
     */
    private static Integer parsePercent(String[] args, int index) {
        if (args.length <= index) {
            return null;
        }

        try {
            int value = Integer.parseInt(args[index].trim());
            return (value < 0 || value > 100) ? null : value;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Resolves a colour name or hex string to an RGB value.
     *
     * @param input a Minecraft colour name such as {@code red}, or hex such as {@code #1E90FF}
     * @return the RGB value, or null when the input is not a colour
     */
    public static Integer parseColour(String input) {
        if (input == null) {
            return null;
        }

        String value = input.trim();

        if (value.isEmpty()) {
            return null;
        }

        // Hex, with or without the leading hash.
        String hex = value.startsWith("#") ? value.substring(1) : value;

        if (hex.length() == 6 && hex.chars().allMatch(c -> Character.digit(c, 16) >= 0)) {
            return Integer.parseInt(hex, 16);
        }

        ChatFormatting formatting = ChatFormatting.getByName(value.toLowerCase(Locale.ROOT));

        if (formatting != null && formatting.isColor() && formatting.getColor() != null) {
            return formatting.getColor();
        }

        return null;
    }

    /**
     * @return the colour names accepted by this command, for tab completion
     */
    public static List<String> colourNames() {
        List<String> names = new ArrayList<>();

        for (ChatFormatting formatting : ChatFormatting.values()) {
            if (formatting.isColor()) {
                names.add(formatting.getName());
            }
        }

        return names;
    }
}
