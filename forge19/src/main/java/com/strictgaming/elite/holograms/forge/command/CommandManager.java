package com.strictgaming.elite.holograms.forge.command;

import com.strictgaming.elite.holograms.forge.ForgeHolograms;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manages registration of commands
 */
@EventBusSubscriber(modid = ForgeHolograms.MOD_ID)
public class CommandManager {

    private static final Logger LOGGER = LogManager.getLogger("AdvancedHolograms");

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering commands for Advanced Holograms");

        CommandFactory factory = new CommandFactory();
        HologramsCommand mainCommand = new HologramsCommand();
        
        // Register all subcommands here
        factory.registerChildCommand(mainCommand, new HologramsCreateCommand());
        factory.registerChildCommand(mainCommand, new HologramsDeleteCommand());
        factory.registerChildCommand(mainCommand, new HologramsAddLineCommand());
        factory.registerChildCommand(mainCommand, new HologramsSetLineCommand());
        factory.registerChildCommand(mainCommand, new HologramsRemoveLineCommand());
        
        // Register near and teleport commands
        factory.registerChildCommand(mainCommand, new HologramsNearCommand());
        factory.registerChildCommand(mainCommand, new HologramsTeleportCommand());
        
        // Register additional commands
        factory.registerChildCommand(mainCommand, new HologramsMoveHereCommand());
        factory.registerChildCommand(mainCommand, new HologramsCopyCommand());
        factory.registerChildCommand(mainCommand, new HologramsInsertLineCommand());
        factory.registerChildCommand(mainCommand, new HologramsReloadCommand());
        factory.registerChildCommand(mainCommand, new HologramsInfoCommand());
        
        // Register the main command with all subcommands
        factory.registerCommand(event.getDispatcher(), mainCommand);
    }
} 
