package com.strictgaming.elite.holograms.fabric;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;
import com.strictgaming.elite.holograms.api.manager.HologramFactory;
import com.strictgaming.elite.holograms.api.manager.PlatformHologramManager;
import com.strictgaming.elite.holograms.fabric.command.CommandFactory;
import com.strictgaming.elite.holograms.fabric.command.HologramsAddLineCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsAnimateLineCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsBackgroundCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsBacklightCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsConvertCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsCopyCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsCreateAtCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsCreateCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsCreateItemCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsCreateScoreboardCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsDeleteCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsInfoCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsInsertLineCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsListCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsMoveHereCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsMoveToCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsMoveVerticalCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsNearCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsReloadCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsRemoveLineCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsSetLineCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsSetRotationCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsSetThemeCommand;
import com.strictgaming.elite.holograms.fabric.command.HologramsTeleportCommand;
import com.strictgaming.elite.holograms.fabric.config.HologramsConfig;
import com.strictgaming.elite.holograms.fabric.hologram.FabricHologram;
import com.strictgaming.elite.holograms.fabric.hologram.HologramManager;
import com.strictgaming.elite.holograms.fabric.hologram.manager.FabricHologramFactory;
import com.strictgaming.elite.holograms.fabric.hologram.manager.FabricHologramManager;
import com.strictgaming.elite.holograms.fabric.util.UtilPlaceholder;
import com.strictgaming.elite.holograms.fabric.util.UtilServer;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Fabric entry point for Elite Holograms.
 *
 * <p>This is the counterpart to the Forge {@code Forge20Holograms} class. The mod is entirely
 * server-side - holograms are packet-only entities the server sends to vanilla clients - so it
 * registers as a plain {@link ModInitializer} rather than a dedicated-server one, which means it
 * also works in single-player and on LAN worlds.
 */
public class FabricHolograms implements ModInitializer, PlatformHologramManager {

    public static final String MOD_ID = "eliteholograms";
    /**
     * Read from the jar manifest rather than repeated here, where it drifted out of step
     * with gradle.properties. Falls back to a label in a dev environment, where there is
     * no manifest to read.
     */
    public static final String VERSION = FabricHolograms.class.getPackage().getImplementationVersion() != null
            ? FabricHolograms.class.getPackage().getImplementationVersion()
            : "dev";
    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");

    private static FabricHolograms instance;

    private final CommandFactory commandFactory = new CommandFactory();
    private HologramsConfig config;
    private boolean placeholders;
    private FabricHologramFactory hologramFactory;
    private FabricHologramManager hologramManager;

    @Override
    public void onInitialize() {
        instance = this;
        LOGGER.info("Initializing Elite Holograms mod for Minecraft 1.20.1 (Fabric)");

        this.hologramFactory = new FabricHologramFactory();
        this.hologramManager = new FabricHologramManager();

        registerLifecycleEvents();
        registerPlayerEvents();
        registerCommands();
    }

    /**
     * Wires the three server lifecycle points the mod cares about. Forge splits these across
     * {@code ServerStartingEvent}, {@code ServerStartedEvent} and {@code ServerStoppingEvent};
     * Fabric's equivalents fire at the same points.
     */
    private void registerLifecycleEvents() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            UtilServer.set(server);
            onServerStarting();
        });

        ServerLifecycleEvents.SERVER_STARTED.register(server -> onServerStarted());

        // STOPPING, not STOPPED: despawning removes real light blocks from the world and sends
        // packets to still-connected players, and both have to happen before the world saves.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> onServerStopping());

        // Drop the reference only once the server is fully down, so nothing keeps a dead server
        // (and the world state behind it) alive across single-player sessions.
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> UtilServer.clear());

        ServerTickEvents.END_SERVER_TICK.register(server -> HologramManager.tick());
    }

    private void registerPlayerEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                HologramManager.onPlayerLogin(handler.getPlayer()));

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (handler.getPlayer() != null) {
                HologramManager.onPlayerQuit(handler.getPlayer().getUUID());
            }
        });
    }

    private void onServerStarting() {
        LOGGER.info("Server starting - initializing hologram manager");

        try {
            this.config = new HologramsConfig();
            this.config.load();
            LOGGER.info("Config loaded successfully");
        } catch (IOException e) {
            LOGGER.error("Error loading config", e);
        }

        HologramManager.preInit();
        HologramManager.start();
    }

    private void onServerStarted() {
        LOGGER.info("Server started - loading holograms");

        UtilPlaceholder.setServerStartTime();

        try {
            HologramManager.load();
            LOGGER.info("Holograms loaded successfully");

            // Force refresh visibility for all loaded holograms to ensure they spawn properly
            for (Hologram hologram : HologramManager.getAllHolograms()) {
                if (hologram instanceof FabricHologram) {
                    ((FabricHologram) hologram).refreshVisibility();
                }
            }
            LOGGER.info("Refreshed visibility for all loaded holograms");
        } catch (Exception e) {
            LOGGER.error("Error loading holograms", e);
        }

        this.checkForPlaceholders();
    }

    private void onServerStopping() {
        LOGGER.info("Server stopping - shutting down hologram manager");

        // Stop the background thread first so it cannot mutate state during save
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

    /**
     * Builds the command tree. Forge fires {@code RegisterCommandsEvent} once per server start;
     * Fabric's callback fires at the same point, so the subcommand instances are created fresh
     * inside it exactly as they were before.
     */
    private void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LOGGER.info("Registering commands...");

            HologramsCommand command = new HologramsCommand();

            command.registerSubCommand("create", new HologramsCreateCommand());
            command.registerSubCommand("createscoreboard", new HologramsCreateScoreboardCommand());
            command.registerSubCommand("settheme", new HologramsSetThemeCommand());
            command.registerSubCommand("createitem", new HologramsCreateItemCommand());
            command.registerSubCommand("list", new HologramsListCommand());
            command.registerSubCommand("delete", new HologramsDeleteCommand());
            command.registerSubCommand("reload", new HologramsReloadCommand());
            command.registerSubCommand("addline", new HologramsAddLineCommand());
            command.registerSubCommand("setline", new HologramsSetLineCommand());
            command.registerSubCommand("removeline", new HologramsRemoveLineCommand());
            command.registerSubCommand("animateline", new HologramsAnimateLineCommand());
            command.registerSubCommand("movehere", new HologramsMoveHereCommand());
            command.registerSubCommand("moveto", new HologramsMoveToCommand());
            command.registerSubCommand("createat", new HologramsCreateAtCommand());
            command.registerSubCommand("teleport", new HologramsTeleportCommand());
            command.registerSubCommand("insertline", new HologramsInsertLineCommand());
            command.registerSubCommand("copy", new HologramsCopyCommand());
            command.registerSubCommand("info", new HologramsInfoCommand());
            command.registerSubCommand("near", new HologramsNearCommand());
            command.registerSubCommand("movevertical", new HologramsMoveVerticalCommand());
            command.registerSubCommand("backlight", new HologramsBacklightCommand());
            command.registerSubCommand("background", new HologramsBackgroundCommand());
            command.registerSubCommand("setrotation", new HologramsSetRotationCommand());
            command.registerSubCommand("convert", new HologramsConvertCommand());

            command.register(dispatcher);

            LOGGER.info("Commands registered successfully!");
        });
    }

    private void checkForPlaceholders() {
        try {
            Class.forName("eu.pb4.placeholders.api.Placeholders");
            this.placeholders = true;
            LOGGER.info("External Placeholder API found - placeholders enabled with external support");
        } catch (ClassNotFoundException e) {
            this.placeholders = true; // Always enable our built-in placeholders
            LOGGER.info("Using built-in placeholder system - placeholders enabled");
        }
    }

    public static FabricHolograms getInstance() {
        return instance;
    }

    public HologramsConfig getConfig() {
        return this.config;
    }

    public CommandFactory getCommandFactory() {
        return this.commandFactory;
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
