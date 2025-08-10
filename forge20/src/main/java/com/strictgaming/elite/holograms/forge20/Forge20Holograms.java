package com.strictgaming.elite.holograms.forge20;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;
import com.strictgaming.elite.holograms.api.manager.HologramFactory;
import com.strictgaming.elite.holograms.api.manager.PlatformHologramManager;
import com.strictgaming.elite.holograms.forge20.command.CommandFactory;
import com.strictgaming.elite.holograms.forge20.command.HologramsCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsCreateCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsCreateScoreboardCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsListCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsDeleteCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsReloadCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsAddLineCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsSetLineCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsRemoveLineCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsMoveHereCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsTeleportCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsInsertLineCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsCopyCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsInfoCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsNearCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsMoveVerticalCommand;
import com.strictgaming.elite.holograms.forge20.config.HologramsConfig;
import com.strictgaming.elite.holograms.forge20.hologram.ForgeHologram;
import com.strictgaming.elite.holograms.forge20.hologram.HologramManager;
import com.strictgaming.elite.holograms.forge20.hologram.manager.ForgeHologramFactory;
import com.strictgaming.elite.holograms.forge20.hologram.manager.ForgeHologramManager;
import com.strictgaming.elite.holograms.forge20.util.UtilPlaceholder;
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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.google.common.collect.Lists;

import java.io.IOException;

@Mod(Forge20Holograms.MOD_ID)
public class Forge20Holograms implements PlatformHologramManager {

    public static final String MOD_ID = "eliteholograms";
    public static final String VERSION = "1.20.1-1.0.5";
    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");

    private static Forge20Holograms instance;

    private CommandFactory commandFactory = new CommandFactory();
    private HologramsConfig config;
    private boolean placeholders;
    private ForgeHologramFactory hologramFactory;
    private ForgeHologramManager hologramManager;

    public Forge20Holograms() {
        instance = this;
        LOGGER.info("Initializing Elite Holograms mod for Minecraft 1.20.1");
        MinecraftForge.EVENT_BUS.register(this);
        this.hologramFactory = new ForgeHologramFactory();
        this.hologramManager = new ForgeHologramManager();
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server starting - initializing hologram manager");
        
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
            LOGGER.info("External Placeholder API found - placeholders enabled with external support");
        } catch (ClassNotFoundException e) {
            this.placeholders = true; // Always enable our built-in placeholders
            LOGGER.info("Using built-in placeholder system - placeholders enabled");
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("Server started - loading holograms");
        
        // Initialize placeholder server start time for uptime tracking
        UtilPlaceholder.setServerStartTime();
        
        try {
            HologramManager.load();
            LOGGER.info("Holograms loaded successfully");
            
            // Force refresh visibility for all loaded holograms to ensure they spawn properly
            for (Hologram hologram : HologramManager.getAllHolograms()) {
                if (hologram instanceof ForgeHologram) {
                    ((ForgeHologram) hologram).refreshVisibility();
                }
            }
            LOGGER.info("Refreshed visibility for all loaded holograms");
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
        LOGGER.info("Registering commands...");
        
        // Create command instances
        HologramsCommand command = new HologramsCommand();
        HologramsCreateCommand createCommand = new HologramsCreateCommand();
        HologramsCreateScoreboardCommand createScoreboardCommand = new HologramsCreateScoreboardCommand();
        HologramsListCommand listCommand = new HologramsListCommand();
        HologramsDeleteCommand deleteCommand = new HologramsDeleteCommand();
        HologramsReloadCommand reloadCommand = new HologramsReloadCommand();
        HologramsAddLineCommand addLineCommand = new HologramsAddLineCommand();
        HologramsSetLineCommand setLineCommand = new HologramsSetLineCommand();
        HologramsRemoveLineCommand removeLineCommand = new HologramsRemoveLineCommand();
        HologramsMoveHereCommand moveHereCommand = new HologramsMoveHereCommand();
        HologramsTeleportCommand teleportCommand = new HologramsTeleportCommand();
        HologramsInsertLineCommand insertLineCommand = new HologramsInsertLineCommand();
        HologramsCopyCommand copyCommand = new HologramsCopyCommand();
        HologramsInfoCommand infoCommand = new HologramsInfoCommand();
        HologramsNearCommand nearCommand = new HologramsNearCommand();
        HologramsMoveVerticalCommand moveVerticalCommand = new HologramsMoveVerticalCommand();
        
        // Register commands with main command handler
        command.registerSubCommand("create", createCommand);
        command.registerSubCommand("createscoreboard", createScoreboardCommand);
        command.registerSubCommand("list", listCommand);
        command.registerSubCommand("delete", deleteCommand);
        command.registerSubCommand("reload", reloadCommand);
        command.registerSubCommand("addline", addLineCommand);
        command.registerSubCommand("setline", setLineCommand);
        command.registerSubCommand("removeline", removeLineCommand);
        command.registerSubCommand("movehere", moveHereCommand);
        command.registerSubCommand("teleport", teleportCommand);
        command.registerSubCommand("insertline", insertLineCommand);
        command.registerSubCommand("copy", copyCommand);
        command.registerSubCommand("info", infoCommand);
        command.registerSubCommand("near", nearCommand);
        command.registerSubCommand("movevertical", moveVerticalCommand);
        
        // Register main command with command dispatcher
        command.register(event.getDispatcher());
        
        LOGGER.info("Commands registered successfully!");
    }

    public static Forge20Holograms getInstance() {
        return instance;
    }

    public HologramsConfig getConfig() {
        return this.config;
    }

    @Override
    public boolean arePlaceholdersEnabled() {
        return this.placeholders;
    }
    
    @Override
    public HologramFactory getFactory() {
        return this.hologramFactory;
    }
    
    @Override
    public void reload() throws IOException {
        this.hologramManager.reload();
    }
    
    @Override
    public void clear() {
        this.hologramManager.clear();
    }

    @Override
    public HologramBuilder builder() {
        return this.hologramFactory.builder();
    }

    @Override
    public HologramBuilder builder(String id) {
        return this.hologramFactory.builder().id(id);
    }

    @Override
    public HologramBuilder builder(String... lines) {
        return this.hologramFactory.builder().lines(lines);
    }

    @Override
    public HologramBuilder builder(String world, int x, int y, int z) {
        return this.hologramFactory.builder()
            .world(world)
            .position(x, y, z);
    }
} 