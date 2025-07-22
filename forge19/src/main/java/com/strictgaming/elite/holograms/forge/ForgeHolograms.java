package com.strictgaming.elite.holograms.forge;

import com.google.common.collect.Lists;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.manager.HologramFactory;
import com.strictgaming.elite.holograms.forge.command.CommandFactory;
import com.strictgaming.elite.holograms.forge.command.HologramsAddLineCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsCreateCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsCreateScoreboardCommand;
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
import com.strictgaming.elite.holograms.forge.util.UtilPlaceholder;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@Mod(ForgeHolograms.MOD_ID)
public class ForgeHolograms {

    public static final String MOD_ID = "eliteholograms";
    public static final String VERSION = "1.19.2-1.0.3";
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
        
        // Record server start time for uptime placeholder
        UtilPlaceholder.recordServerStart();
        
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
        // Always enable placeholders since we have built-in ones
        this.placeholders = true;
        
        try {
            Class.forName("com.envyful.papi.forge.ForgePlaceholderAPI");
            LOGGER.info("External Placeholder API found - external placeholders also enabled");
        } catch (ClassNotFoundException e) {
            LOGGER.info("External Placeholder API not found - using built-in placeholders only");
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
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping - shutting down hologram manager");
        
        // First shutdown the background thread to prevent interference
        HologramManager.shutdown();
        
        // Then despawn all holograms to ensure they're properly cleaned up
        for (Hologram hologram : HologramManager.getAllHolograms()) {
            try {
                LOGGER.debug("Despawning hologram {} during server shutdown", hologram.getId());
                hologram.despawn();
            } catch (Exception e) {
                LOGGER.error("Error despawning hologram {} during shutdown: {}", hologram.getId(), e.getMessage());
            }
        }
        
        // Finally save all hologram data synchronously to prevent hanging
        // Save all holograms before shutdown with timeout protection
        try {
            LOGGER.info("Saving all holograms before shutdown...");
            
            // Use a separate thread with timeout to prevent hanging during save
            Thread saveThread = new Thread(() -> {
                try {
                    HologramManager.getSaver().save(Lists.newArrayList(HologramManager.getAllHolograms()));
                    HologramManager.saveScoreboardHologramsSync();
                } catch (Exception e) {
                    LOGGER.error("Error in save thread during shutdown", e);
                }
            });
            
            saveThread.start();
            saveThread.join(5000); // Wait max 5 seconds for save to complete
            
            if (saveThread.isAlive()) {
                LOGGER.warn("Save operation timed out during shutdown - forcing thread termination");
                saveThread.interrupt();
            } else {
                LOGGER.info("All holograms saved successfully during shutdown");
            }
            
        } catch (Exception e) {
            LOGGER.error("Error saving holograms during shutdown", e);
        }
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
        
        LOGGER.info("Registering HologramsCreateScoreboardCommand");
        HologramsCreateScoreboardCommand createScoreboardCommand = new HologramsCreateScoreboardCommand();
        command.registerSubCommand("createscoreboard", createScoreboardCommand);
        
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
