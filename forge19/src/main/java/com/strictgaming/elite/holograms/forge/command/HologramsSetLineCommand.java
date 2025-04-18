package com.strictgaming.elite.holograms.forge.command;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge.util.UtilChatColour;
import com.strictgaming.elite.holograms.forge.util.UtilParse;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Command for setting a specific line in a hologram
 */
public class HologramsSetLineCommand {
    
    /**
     * Register this command with the command dispatcher
     */
    public void register(CommandFactory factory, LiteralArgumentBuilder<CommandSourceStack> parent) {
        LiteralArgumentBuilder<CommandSourceStack> command = Commands.literal("setline")
                .requires(source -> source.hasPermission(2)) // Admin level
                .executes(this::showUsage);
                
        parent.then(command);
    }
    
    /**
     * Show usage message
     */
    private int showUsage(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(UtilChatColour.parse("&c/hd setline <id> <line number> <text>"));
        return 1;
    }
    
    /**
     * Execute the command
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) {
        CommandSourceStack source = context.getSource();
        
        if (args.length < 3) {
            return showUsage(context);
        }
        
        String id = args[0];
        Hologram hologram = HologramManager.getById(id);
        
        if (hologram == null) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cCannot find hologram with ID: " + id));
            return 0;
        }
        
        Optional<Integer> lineNumber = UtilParse.parseInteger(args[1]);
        
        if (lineNumber.isEmpty()) {
            source.sendSystemMessage(UtilChatColour.parse("&c&l(!) &cInvalid line number: " + args[1]));
            return 0;
        }
        
        StringBuilder text = new StringBuilder();
        
        for (int i = 2; i < args.length; i++) {
            text.append(args[i]).append(" ");
        }
        
        hologram.setLine(lineNumber.get(), text.toString().trim());
        source.sendSystemMessage(UtilChatColour.parse("&e&l(!) &eUpdated line " + lineNumber.get() + " of hologram with ID: &b" + id));
        return 1;
    }
} 
