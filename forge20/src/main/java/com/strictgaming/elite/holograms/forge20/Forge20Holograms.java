package com.strictgaming.elite.holograms.forge20;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;
import com.strictgaming.elite.holograms.api.manager.HologramFactory;
import com.strictgaming.elite.holograms.api.manager.PlatformHologramManager;
import com.strictgaming.elite.holograms.forge20.command.CommandFactory;
import com.strictgaming.elite.holograms.forge20.command.HologramsCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsCreateCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsCreateScoreboardCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsSetThemeCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsListCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsDeleteCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsReloadCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsAddLineCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsSetLineCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsRemoveLineCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsMoveHereCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsMoveToCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsCreateAtCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsTeleportCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsInsertLineCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsCopyCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsInfoCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsNearCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsMoveVerticalCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsCreateItemCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsAnimateLineCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsBacklightCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsSetRotationCommand;
import com.strictgaming.elite.holograms.forge20.command.HologramsConvertCommand;
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
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkConstants;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import java.io.IOException;

@Mod(Forge20Holograms.MOD_ID)
public class Forge20Holograms implements PlatformHologramManager {

    public static final String MOD_ID = "eliteholograms";

    /**
     * Read from the jar manifest, which build.gradle populates with {@code Implementation-Version}
     * from {@code project.version}. This was previously a hardcoded literal and had drifted to
     * 1.1.1 while the jar shipped as 1.2.0, the same defect already fixed on the Forge 1.19.2
     * edition. Falls back to a label in a dev environment, where there is no manifest to read.
     */
    public static final String VERSION = Forge20Holograms.class.getPackage().getImplementationVersion() != null
            ? Forge20Holograms.class.getPackage().getImplementationVersion()
            : "dev";
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
        registerServerSideOnlyDisplayTest();
        MinecraftForge.EVENT_BUS.register(this);
        this.hologramFactory = new ForgeHologramFactory();
        this.hologramManager = new ForgeHologramManager();
    }

    /**
     * Declares the mod as server-side-only for the multiplayer server list compatibility check.
     *
     * <p>Without this, Forge's default display test requires the mod to be present on both sides
     * with the same version, so a server running Elite Holograms shows as an "Incompatible FML
     * modded server" (red X) to clients that do not have it installed. Holograms are rendered
     * entirely with vanilla entity packets and the mod registers no network channel of its own,
     * so a client genuinely does not need it.</p>
     *
     * <p>{@code IGNORESERVERONLY} tells clients to ignore this mod when the server has it and they
     * do not. The predicate returns {@code isFromServer} so that a client which <em>does</em> have
     * it installed still accepts any version reported by a server, per Forge's own guidance in
     * {@link IExtensionPoint.DisplayTest}. Note this is a display test only: it does not change
     * whether a connection succeeds.</p>
     *
     * <p>Forge 1.19.2 and 1.20.1 have no {@code displayTest} key in {@code mods.toml} (that arrived
     * in Forge 1.21), so this has to be registered in code. The NeoForge editions need no
     * equivalent: their networking negotiates per registered payload, and this mod registers none.</p>
     */
    private void registerServerSideOnlyDisplayTest() {
        ModLoadingContext.get().registerExtensionPoint(
                IExtensionPoint.DisplayTest.class,
                () -> new IExtensionPoint.DisplayTest(
                        () -> NetworkConstants.IGNORESERVERONLY,
                        (remoteVersion, isFromServer) -> isFromServer
                )
        );
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
        
        // Initialize manager helpers if needed (this is safe to call multiple times)
        HologramManager.preInit();
        
        // Start the background thread
        HologramManager.start();
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

    @SubscribeEvent
    public void onServerTick(net.minecraftforge.event.TickEvent.ServerTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            HologramManager.tick();
        }
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        LOGGER.info("Registering commands...");
        
        // Create command instances
        HologramsCommand command = new HologramsCommand();
        HologramsCreateCommand createCommand = new HologramsCreateCommand();
        HologramsCreateScoreboardCommand createScoreboardCommand = new HologramsCreateScoreboardCommand();
        HologramsSetThemeCommand setThemeCommand = new HologramsSetThemeCommand();
        HologramsListCommand listCommand = new HologramsListCommand();
        HologramsDeleteCommand deleteCommand = new HologramsDeleteCommand();
        HologramsReloadCommand reloadCommand = new HologramsReloadCommand();
        HologramsAddLineCommand addLineCommand = new HologramsAddLineCommand();
        HologramsSetLineCommand setLineCommand = new HologramsSetLineCommand();
        HologramsRemoveLineCommand removeLineCommand = new HologramsRemoveLineCommand();
        HologramsMoveHereCommand moveHereCommand = new HologramsMoveHereCommand();
        HologramsMoveToCommand moveToCommand = new HologramsMoveToCommand();
        HologramsCreateAtCommand createAtCommand = new HologramsCreateAtCommand();
        HologramsTeleportCommand teleportCommand = new HologramsTeleportCommand();
        HologramsInsertLineCommand insertLineCommand = new HologramsInsertLineCommand();
        HologramsCopyCommand copyCommand = new HologramsCopyCommand();
        HologramsInfoCommand infoCommand = new HologramsInfoCommand();
        HologramsNearCommand nearCommand = new HologramsNearCommand();
        HologramsMoveVerticalCommand moveVerticalCommand = new HologramsMoveVerticalCommand();
        HologramsCreateItemCommand createItemCommand = new HologramsCreateItemCommand();
        HologramsAnimateLineCommand animateLineCommand = new HologramsAnimateLineCommand();
        HologramsBacklightCommand backlightCommand = new HologramsBacklightCommand();
        HologramsSetRotationCommand setRotationCommand = new HologramsSetRotationCommand();
        HologramsConvertCommand convertCommand = new HologramsConvertCommand();
        
        // Register commands with main command handler
        command.registerSubCommand("create", createCommand);
        command.registerSubCommand("createscoreboard", createScoreboardCommand);
        command.registerSubCommand("settheme", setThemeCommand);
        command.registerSubCommand("createitem", createItemCommand);
        command.registerSubCommand("list", listCommand);
        command.registerSubCommand("delete", deleteCommand);
        command.registerSubCommand("reload", reloadCommand);
        command.registerSubCommand("addline", addLineCommand);
        command.registerSubCommand("setline", setLineCommand);
        command.registerSubCommand("removeline", removeLineCommand);
        command.registerSubCommand("animateline", animateLineCommand);
        command.registerSubCommand("movehere", moveHereCommand);
        command.registerSubCommand("moveto", moveToCommand);
        command.registerSubCommand("createat", createAtCommand);
        command.registerSubCommand("teleport", teleportCommand);
        command.registerSubCommand("insertline", insertLineCommand);
        command.registerSubCommand("copy", copyCommand);
        command.registerSubCommand("info", infoCommand);
        command.registerSubCommand("near", nearCommand);
        command.registerSubCommand("movevertical", moveVerticalCommand);
        command.registerSubCommand("backlight", backlightCommand);
        command.registerSubCommand("setrotation", setRotationCommand);
        command.registerSubCommand("convert", convertCommand);
        
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