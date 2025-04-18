package com.strictgaming.elite.holograms.forge;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.manager.HologramFactory;
import com.strictgaming.elite.holograms.forge.command.CommandFactory;
import com.strictgaming.elite.holograms.forge.command.HologramsAddLineCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsCreateCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsDeleteCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsListCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsMoveHereCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsNearCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsRemoveLineCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsSetLineCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsTeleportCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsCopyCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsInsertLineCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsReloadCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsInfoCommand;
import com.strictgaming.elite.holograms.forge.config.HologramsConfig;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge.hologram.manager.ForgeHologramManager;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@Mod(ForgeHolograms.MOD_ID)
public class ForgeHolograms {

    public static final String MOD_ID = "eliteholograms";
    public static final String VERSION = "1.0.0";
    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");

    private static ForgeHolograms instance;

    private CommandFactory commandFactory = new CommandFactory();
    private HologramsConfig config;
    private boolean placeholders;

    public ForgeHolograms() {
        instance = this;
        LOGGER.info("Initializing Elite Holograms mod");
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server starting - initializing hologram manager");
        HologramFactory.setHologramManager(new ForgeHologramManager());

        try {
            this.config = new HologramsConfig();
            this.config.load();
            LOGGER.info("Config loaded successfully");
        } catch (IOException e) {
            LOGGER.error("Error loading config", e);
        }
        
        HologramManager.preInit();
    }

    private void checkForPlaceholders() {
        try {
            Class.forName("com.envyful.papi.forge.ForgePlaceholderAPI");
            this.placeholders = true;
            LOGGER.info("Placeholder API found - placeholders enabled");
        } catch (ClassNotFoundException e) {
            this.placeholders = false;
            LOGGER.info("Placeholder API not found - placeholders disabled");
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("Server started - loading holograms");
        try {
            HologramManager.load();
            LOGGER.info("Holograms loaded successfully");
        } catch (Exception e) {
            LOGGER.error("Error loading holograms", e);
        }
        this.checkForPlaceholders();
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        LOGGER.info("Registering commands");
        this.commandFactory.registerInjector(Hologram.class, (sender, args) -> {
            Hologram byId = HologramManager.getById(args[0]);

            if (byId == null) {
                sender.sendSystemMessage(Component.literal("§4Cannot find a hologram with that id"));
                return null;
            }

            return byId;
        });

        // Register the main command
        HologramsCommand command = new HologramsCommand();
        
        // Register list command first and explicitly
        LOGGER.info("Registering HologramsListCommand");
        HologramsListCommand listCommand = new HologramsListCommand();
        command.registerSubCommand("list", listCommand);
        
        // Register the near command explicitly
        LOGGER.info("Registering HologramsNearCommand");
        HologramsNearCommand nearCommand = new HologramsNearCommand();
        command.registerSubCommand("near", nearCommand);  // Register with clean name
        
        // Debug which commands we're registering
        LOGGER.info("Registering HologramsCreateCommand");
        this.commandFactory.registerChildCommand(command, new HologramsCreateCommand());
        
        LOGGER.info("Registering HologramsDeleteCommand");
        this.commandFactory.registerChildCommand(command, new HologramsDeleteCommand());
        
        LOGGER.info("Registering HologramsAddLineCommand");
        this.commandFactory.registerChildCommand(command, new HologramsAddLineCommand());
        
        LOGGER.info("Registering HologramsSetLineCommand");
        this.commandFactory.registerChildCommand(command, new HologramsSetLineCommand());
        
        LOGGER.info("Registering HologramsRemoveLineCommand");
        this.commandFactory.registerChildCommand(command, new HologramsRemoveLineCommand());
        
        // Register our new commands
        LOGGER.info("Registering HologramsMoveHereCommand");
        command.registerSubCommand("movehere", new HologramsMoveHereCommand());
        
        LOGGER.info("Registering HologramsTeleportCommand");
        command.registerSubCommand("teleport", new HologramsTeleportCommand());
        
        LOGGER.info("Registering HologramsCopyCommand");
        command.registerSubCommand("copy", new HologramsCopyCommand());
        
        LOGGER.info("Registering HologramsInsertLineCommand");
        command.registerSubCommand("insertline", new HologramsInsertLineCommand());
        
        LOGGER.info("Registering HologramsReloadCommand");
        command.registerSubCommand("reload", new HologramsReloadCommand());
        
        LOGGER.info("Registering HologramsInfoCommand");
        command.registerSubCommand("info", new HologramsInfoCommand());
        
        LOGGER.info("Registering main command dispatcher");
        this.commandFactory.registerCommand(event.getDispatcher(), command);
        LOGGER.info("Commands registered successfully");
    }

    public static ForgeHolograms getInstance() {
        return instance;
    }

    public HologramsConfig getConfig() {
        return this.config;
    }

    public boolean arePlaceholdersEnabled() {
        return this.placeholders;
    }
} 
