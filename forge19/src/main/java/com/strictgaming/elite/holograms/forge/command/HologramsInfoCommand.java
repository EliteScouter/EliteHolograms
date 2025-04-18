package com.strictgaming.elite.holograms.forge.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.hologram.ForgeHologram;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge.hologram.entity.HologramLine;
import com.strictgaming.elite.holograms.forge.util.UtilChatColour;
import com.strictgaming.elite.holograms.forge.util.UtilWorld;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.DecimalFormat;
import java.util.List;

/**
 * Command to display detailed information about a hologram
 */
public class HologramsInfoCommand implements Command<CommandSourceStack> {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    private static final DecimalFormat COORDINATE_FORMAT = new DecimalFormat("0.##");

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        LOGGER.debug("Executing info command with args: {}", String.join(", ", args));
        
        CommandSourceStack source = context.getSource();
        
        if (args.length < 1) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cUsage: /eh info <id>"));
            return 0;
        }
        
        String id = args[0];
        Hologram hologram = HologramManager.getById(id);
        
        if (hologram == null) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cHologram with ID '&f" + id + "&c' not found!"));
            return 0;
        }
        
        if (!(hologram instanceof ForgeHologram)) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cHologram with ID '&f" + id + "&c' is not a valid hologram!"));
            return 0;
        }
        
        ForgeHologram forgeHologram = (ForgeHologram) hologram;
        Vec3 position = forgeHologram.getPosition();
        
        // Display hologram information with updated styling - without right borders
        source.sendSystemMessage(UtilChatColour.parse("&3&l┌─ &b&lHologram Information: &f" + id + " &3&l──────────┐"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &bWorld: &f" + UtilWorld.getName(forgeHologram.getWorld())));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &bPosition: &f" + 
                COORDINATE_FORMAT.format(position.x) + ", " + 
                COORDINATE_FORMAT.format(position.y) + ", " + 
                COORDINATE_FORMAT.format(position.z)));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &bRange: &f" + forgeHologram.getRange() + " blocks"));
        source.sendSystemMessage(UtilChatColour.parse("&3│ &bLines: &f" + forgeHologram.getLines().size()));
        
        // Display all lines with updated styling
        source.sendSystemMessage(UtilChatColour.parse("&3&l├─ &b&lLines &3&l───────────────────────┤"));
        List<HologramLine> lines = forgeHologram.getLines();
        for (int i = 0; i < lines.size(); i++) {
            HologramLine line = lines.get(i);
            source.sendSystemMessage(UtilChatColour.parse("&3│ &bLine " + (i + 1) + ": &r" + line.getText()));
        }
        source.sendSystemMessage(UtilChatColour.parse("&3&l└────────────────────────────┘"));
        
        return Command.SINGLE_SUCCESS;
    }
} 
