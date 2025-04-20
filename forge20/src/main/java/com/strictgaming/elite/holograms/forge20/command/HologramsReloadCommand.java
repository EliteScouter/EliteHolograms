package com.strictgaming.elite.holograms.forge20.command;

import com.strictgaming.elite.holograms.api.manager.PlatformHologramManager;
import com.strictgaming.elite.holograms.forge20.Forge20Holograms;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.io.IOException;

/**
 * Command to reload holograms from storage
 */
public class HologramsReloadCommand implements Command<CommandSourceStack> {

    @Override
    public int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return executeCommand(context, new String[0]);
    }
    
    /**
     * Execute the command with given arguments
     */
    public int executeCommand(CommandContext<CommandSourceStack> context, String[] args) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        PlatformHologramManager manager = Forge20Holograms.getInstance();
        
        try {
            source.sendSystemMessage(Component.literal("§e§l(!) §eReloading holograms..."));
            manager.reload();
            source.sendSystemMessage(Component.literal("§a§l(!) §aHolograms reloaded successfully."));
            return Command.SINGLE_SUCCESS;
        } catch (IOException e) {
            source.sendSystemMessage(Component.literal("§c§l(!) §cError reloading holograms: " + e.getMessage()));
            return 0;
        }
    }
} 