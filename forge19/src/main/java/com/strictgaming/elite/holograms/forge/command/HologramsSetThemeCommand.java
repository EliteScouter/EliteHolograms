package com.strictgaming.elite.holograms.forge.command;

import com.mojang.brigadier.context.CommandContext;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.config.ScoreboardThemeManager;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge.hologram.ScoreboardHologram;
import com.strictgaming.elite.holograms.forge.util.UtilChatColour;
import net.minecraft.commands.CommandSourceStack;

/**
 * Restyles an existing scoreboard hologram with a different theme.
 * Usage: /eh settheme <id> <theme>
 */
public class HologramsSetThemeCommand {

    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        CommandSourceStack source = context.getSource();

        if (args.length < 2) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUsage: /eh settheme <id> <theme>"));
            return 0;
        }

        String id = args[0];
        String theme = args[1];

        Hologram hologram = HologramManager.getById(id);
        if (hologram == null) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cNo hologram with ID '&f" + id + "&c' exists!"));
            return 0;
        }
        if (!(hologram instanceof ScoreboardHologram scoreboard)) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cHologram '&f" + id + "&c' is not a scoreboard hologram!"));
            return 0;
        }
        if (ScoreboardThemeManager.getTheme(theme) == null) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUnknown theme '&f" + theme + "&c'. Check config/eliteholograms/scoreboard_themes.json"));
            return 0;
        }

        if (!scoreboard.applyTheme(theme)) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cFailed to apply theme '&f" + theme + "&c'."));
            return 0;
        }

        HologramManager.save();

        source.sendSystemMessage(UtilChatColour.parse("&a&l(!) &aSet theme of scoreboard hologram '&f" + id + "&a' to '&f" + theme + "&a'!"));
        return 1;
    }
}
