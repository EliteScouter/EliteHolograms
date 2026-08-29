package com.strictgaming.elite.holograms.forge;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.manager.HologramFactory;
import com.strictgaming.elite.holograms.forge.command.CommandFactory;
import com.strictgaming.elite.holograms.forge.command.HologramsAddLineCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsCreateCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsCreateAtCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsCreateScoreboardCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsDeleteCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsListCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsMoveHereCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsMoveToCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsNearCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsRemoveLineCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsSetLineCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsTeleportCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsCopyCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsInsertLineCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsReloadCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsInfoCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsAnimateLineCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsCreateItemCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsBacklightCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsMoveVerticalCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsSetRotationCommand;
import com.strictgaming.elite.holograms.forge.command.HologramsConvertCommand;
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

    /**
     * Read from the jar manifest, which build.gradle populates with {@code Implementation-Version}
     * from {@code project.version}. Previously this was a hardcoded literal and had drifted to
     * 1.1.1 while the jar shipped as 1.2.0. Falls back to a label in a dev environment, where
     * there is no manifest to read.
     */
    public static final String VERSION = ForgeHolograms.class.getPackage().getImplementationVersion() != null
            ? ForgeHolograms.class.getPackage().getImplementationVersion()
            : "dev";
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
    public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            HologramManager.tick();
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping - shutting down hologram manager");
        
        // First shutdown the background thread to prevent interference
        HologramManager.shutdown();
        
        // Save BEFORE despawn so we never risk writing after a clear/despawn race
        try {
            LOGGER.info("Saving all holograms before shutdown...");
            HologramManager.saveSync();
            LOGGER.info("All holograms saved successfully during shutdown");
        } catch (Exception e) {
            LOGGER.error("Error saving holograms during shutdown", e);
        }

        for (Hologram hologram : HologramManager.getAllHolograms()) {
            try {
                LOGGER.debug("Despawning hologram {} during server shutdown", hologram.getId());
                hologram.despawn();
            } catch (Exception e) {
                LOGGER.error("Error despawning hologram {} during shutdown: {}", hologram.getId(), e.getMessage());
            }
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
        
        LOGGER.info("Registering HologramsCreateAtCommand");
        command.registerSubCommand("createat", new HologramsCreateAtCommand());
        
        LOGGER.info("Registering HologramsCreateScoreboardCommand");
        HologramsCreateScoreboardCommand createScoreboardCommand = new HologramsCreateScoreboardCommand();
        command.registerSubCommand("createscoreboard", createScoreboardCommand);

        LOGGER.info("Registering HologramsSetThemeCommand");
        command.registerSubCommand("settheme", new com.strictgaming.elite.holograms.forge.command.HologramsSetThemeCommand());
        
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
        
        LOGGER.info("Registering HologramsMoveToCommand");
        command.registerSubCommand("moveto", new HologramsMoveToCommand());
        
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
        
        LOGGER.info("Registering HologramsAnimateLineCommand");
        command.registerSubCommand("animateline", new HologramsAnimateLineCommand());
        
        LOGGER.info("Registering HologramsCreateItemCommand");
        command.registerSubCommand("createitem", new HologramsCreateItemCommand());

        LOGGER.info("Registering HologramsBacklightCommand");
        command.registerSubCommand("backlight", new HologramsBacklightCommand());

        // Previously only registered by the redundant CommandManager event subscriber, which
        // meant /eh movevertical tab-completed but failed the subcommand lookup at runtime.
        LOGGER.info("Registering HologramsMoveVerticalCommand");
        command.registerSubCommand("movevertical", new HologramsMoveVerticalCommand());

        // Fixed-display commands. Forge 1.19.2 cannot render fixed holograms (text_display
        // entities arrived in 1.19.4), so these exist to report that clearly instead of
        // failing as unknown commands.
        LOGGER.info("Registering HologramsSetRotationCommand");
        command.registerSubCommand("setrotation", new HologramsSetRotationCommand());

        LOGGER.info("Registering HologramsConvertCommand");
        command.registerSubCommand("convert", new HologramsConvertCommand());

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
