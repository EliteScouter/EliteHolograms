package com.strictgaming.elite.holograms.neo21.command;

import com.strictgaming.elite.holograms.neo21.Neo21Holograms;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.IOException;

/**
 * Command to reload holograms from config
 */
public class HologramsReloadCommand implements HologramsCommand.SubCommand {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * Registers this command with the given dispatcher
     *
     * @param dispatcher The command dispatcher
     */
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register with full command name
        dispatcher.register(
            Commands.literal("eliteholograms")
                .then(Commands.literal("reload")
                    .requires(source -> source.hasPermission(2))
                    .executes(this::run)
                )
        );
        
        // Register with short command alias
        dispatcher.register(
            Commands.literal("eh")
                .then(Commands.literal("reload")
                    .requires(source -> source.hasPermission(2))
                    .executes(this::run)
                )
        );
    }

    public int run(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();

        try {
            // Clear and reload all holograms
            // Need to get the actual internal map reference to properly clear it
            var hologramsMap = HologramManager.getHologramsInternal();
            hologramsMap.values().forEach(hologram -> hologram.despawn());
            hologramsMap.clear();

            // Reload from config
            HologramManager.load();

            source.sendSuccess(() -> Component.literal("Holograms reloaded successfully!"), false);
            LOGGER.info("Holograms reloaded by " + source.getTextName());
            return Command.SINGLE_SUCCESS;
        } catch (IOException e) {
            source.sendFailure(Component.literal("Failed to reload holograms: " + e.getMessage()));
            LOGGER.error("Error reloading holograms", e);
            return 0;
        }
    }
    
    @Override
    public int execute(CommandContext<CommandSourceStack> context) {
        return run(context);
    }
    
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> getArguments() {
        return Commands.literal("reload");
    }
} 