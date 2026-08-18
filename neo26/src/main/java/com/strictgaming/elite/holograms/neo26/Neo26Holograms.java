package com.strictgaming.elite.holograms.neo26;

import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;
import com.strictgaming.elite.holograms.api.manager.HologramFactory;
import com.strictgaming.elite.holograms.api.manager.PlatformHologramManager;
import com.strictgaming.elite.holograms.neo26.command.CommandFactory;
import com.strictgaming.elite.holograms.neo26.command.HologramsAnimateLineCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsBacklightCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsCreateCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsCreateItemCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsDeleteCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsListCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsMoveHereCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsMoveToCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsCreateAtCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsReloadCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsTeleportCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsAddLineCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsSetLineCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsRemoveLineCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsNearCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsInfoCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsCopyCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsInsertLineCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsCreateScoreboardCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsSetThemeCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsMoveVerticalCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsSetRotationCommand;
import com.strictgaming.elite.holograms.neo26.command.HologramsConvertCommand;
import com.strictgaming.elite.holograms.neo26.config.HologramsConfig;
import com.strictgaming.elite.holograms.neo26.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo26.hologram.manager.NeoForgeHologramFactory;
import com.strictgaming.elite.holograms.neo26.hologram.manager.NeoForgeHologramManager;
import com.strictgaming.elite.holograms.api.hologram.Hologram;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;

@Mod(Neo26Holograms.MOD_ID)
public class Neo26Holograms implements PlatformHologramManager {

    public static final String MOD_ID = "eliteholograms";
    public static final String VERSION = "26.1.2-1.1.1";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static Neo26Holograms instance;

    private CommandFactory commandFactory = new CommandFactory();
    private HologramsConfig config;
    private boolean placeholders;
    private NeoForgeHologramFactory hologramFactory;
    private NeoForgeHologramManager hologramManager;
    
    // Command instances
    private HologramsCommand mainCommand;
    private HologramsCreateCommand createCommand;
    private HologramsCreateItemCommand createItemCommand;
    private HologramsListCommand listCommand;
    private HologramsDeleteCommand deleteCommand;
    private HologramsReloadCommand reloadCommand;
    private HologramsTeleportCommand teleportCommand;
    private HologramsAddLineCommand addLineCommand;
    private HologramsSetLineCommand setLineCommand;
    private HologramsRemoveLineCommand removeLineCommand;
    private HologramsAnimateLineCommand animateLineCommand;
    private HologramsMoveHereCommand moveHereCommand;
    private HologramsMoveToCommand moveToCommand;
    private HologramsCreateAtCommand createAtCommand;
    private HologramsNearCommand nearCommand;
    private HologramsInfoCommand infoCommand;
    private HologramsCopyCommand copyCommand;
    private HologramsInsertLineCommand insertLineCommand;
    private HologramsCreateScoreboardCommand createScoreboardCommand;
    private HologramsSetThemeCommand setThemeCommand;
    private HologramsMoveVerticalCommand moveVerticalCommand;
    private HologramsBacklightCommand backlightCommand;
    private HologramsSetRotationCommand setRotationCommand;
    private HologramsConvertCommand convertCommand;

    public Neo26Holograms(IEventBus modEventBus, ModContainer modContainer) {
        instance = this;
        LOGGER.info("Initializing Elite Holograms mod for Minecraft 26.1");
        
        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::commonSetup);
        
        this.hologramFactory = new NeoForgeHologramFactory();
        this.hologramManager = new NeoForgeHologramManager();
        
        // Initialize commands
        mainCommand = new HologramsCommand();
        createCommand = new HologramsCreateCommand();
        createItemCommand = new HologramsCreateItemCommand();
        listCommand = new HologramsListCommand();
        deleteCommand = new HologramsDeleteCommand();
        reloadCommand = new HologramsReloadCommand();
        teleportCommand = new HologramsTeleportCommand();
        addLineCommand = new HologramsAddLineCommand();
        setLineCommand = new HologramsSetLineCommand();
        removeLineCommand = new HologramsRemoveLineCommand();
        animateLineCommand = new HologramsAnimateLineCommand();
        moveHereCommand = new HologramsMoveHereCommand();
        moveToCommand = new HologramsMoveToCommand();
        createAtCommand = new HologramsCreateAtCommand();
        nearCommand = new HologramsNearCommand();
        infoCommand = new HologramsInfoCommand();
        copyCommand = new HologramsCopyCommand();
        insertLineCommand = new HologramsInsertLineCommand();
        createScoreboardCommand = new HologramsCreateScoreboardCommand();
        setThemeCommand = new HologramsSetThemeCommand();
        moveVerticalCommand = new HologramsMoveVerticalCommand();
        backlightCommand = new HologramsBacklightCommand();
        setRotationCommand = new HologramsSetRotationCommand();
        convertCommand = new HologramsConvertCommand();
        
        // Set up subcommands
        mainCommand.registerSubCommand("create", createCommand);
        mainCommand.registerSubCommand("createitem", createItemCommand);
        mainCommand.registerSubCommand("list", listCommand);
        mainCommand.registerSubCommand("delete", deleteCommand);
        mainCommand.registerSubCommand("reload", reloadCommand);
        mainCommand.registerSubCommand("teleport", teleportCommand);
        mainCommand.registerSubCommand("addline", addLineCommand);
        mainCommand.registerSubCommand("setline", setLineCommand);
        mainCommand.registerSubCommand("removeline", removeLineCommand);
        mainCommand.registerSubCommand("animateline", animateLineCommand);
        mainCommand.registerSubCommand("movehere", moveHereCommand);
        mainCommand.registerSubCommand("moveto", moveToCommand);
        mainCommand.registerSubCommand("createat", createAtCommand);
        mainCommand.registerSubCommand("near", nearCommand);
        mainCommand.registerSubCommand("info", infoCommand);
        mainCommand.registerSubCommand("copy", copyCommand);
        mainCommand.registerSubCommand("insertline", insertLineCommand);
        mainCommand.registerSubCommand("createscoreboard", createScoreboardCommand);
        mainCommand.registerSubCommand("settheme", setThemeCommand);
        mainCommand.registerSubCommand("movevertical", moveVerticalCommand);
        mainCommand.registerSubCommand("backlight", backlightCommand);
        mainCommand.registerSubCommand("setrotation", setRotationCommand);
        mainCommand.registerSubCommand("convert", convertCommand);
    }
    
    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Elite Holograms common setup");
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

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("Registering Elite Holograms commands");
        mainCommand.register(event.getDispatcher());
        
        createCommand.register(event.getDispatcher());
        listCommand.register(event.getDispatcher());
        deleteCommand.register(event.getDispatcher());
        reloadCommand.register(event.getDispatcher());
        teleportCommand.register(event.getDispatcher());
        addLineCommand.register(event.getDispatcher());
        setLineCommand.register(event.getDispatcher());
        removeLineCommand.register(event.getDispatcher());
        moveHereCommand.register(event.getDispatcher());
        moveToCommand.register(event.getDispatcher());
        createAtCommand.register(event.getDispatcher());
        nearCommand.register(event.getDispatcher());
        infoCommand.register(event.getDispatcher());
        copyCommand.register(event.getDispatcher());
        insertLineCommand.register(event.getDispatcher());
        createScoreboardCommand.register(event.getDispatcher());
        setThemeCommand.register(event.getDispatcher());
        moveVerticalCommand.register(event.getDispatcher());
        backlightCommand.register(event.getDispatcher());
        setRotationCommand.register(event.getDispatcher());
        convertCommand.register(event.getDispatcher());
        
        LOGGER.info("Commands registered successfully!");
    }

    private void checkForPlaceholders() {
        try {
            Class.forName("com.envyful.papi.forge.ForgePlaceholderAPI");
            this.placeholders = true;
            LOGGER.info("External Placeholder API found - placeholders enabled with external support");
        } catch (ClassNotFoundException e) {
            this.placeholders = true; 
            LOGGER.info("Using built-in placeholder system - placeholders enabled");
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("Server started - loading holograms");
        com.strictgaming.elite.holograms.neo26.util.UtilPlaceholder.setServerStartTime();
        this.checkForPlaceholders();
        try {
            HologramManager.load();
            LOGGER.info("Holograms loaded successfully");
        } catch (Exception e) {
            LOGGER.error("Error loading holograms", e);
        }
    }

    /**
     * Despawns holograms and writes the config on the way down.
     *
     * <p>This deliberately runs inline on the server thread. Despawning removes real
     * {@code minecraft:light} blocks from the world and sends packets to still-connected
     * players, neither of which is safe off the server thread, and {@link ServerStoppingEvent}
     * fires before the world is saved - so the work has to complete here or the light blocks
     * leak into the save file.
     *
     * <p>An earlier version handed this to two nested worker threads with join timeouts. Those
     * threads were non-daemon and were only ever interrupted, which does nothing to a thread
     * blocked inside a block write, so they could outlive the event and keep touching the world
     * while Minecraft was already saving and unloading chunks.
     */
    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping - despawning holograms and saving");

        for (Hologram hologram : HologramManager.getHolograms().values()) {
            try {
                if (hologram.isSpawned()) {
                    LOGGER.debug("Despawning hologram {} during server shutdown", hologram.getId());
                    hologram.despawn();
                }
            } catch (Exception e) {
                LOGGER.error("Error despawning hologram {} during shutdown: {}", hologram.getId(), e.getMessage());
            }
        }

        try {
            HologramManager.saveSync();
            LOGGER.info("Holograms saved successfully during shutdown");
        } catch (Exception e) {
            LOGGER.error("Error saving holograms during shutdown", e);
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HologramManager.handlePlayerJoin(player);
        }
    }
    
    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            HologramManager.handlePlayerLeave(player);
        }
    }
    
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        // Tick animations every tick
        HologramManager.tick();

        // Check player movement every 20 ticks (1 second) to avoid performance issues
        if (event.getServer().getTickCount() % 20 == 0) {
            for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                HologramManager.handlePlayerMove(player);
            }
        }
    }

    public static Neo26Holograms getInstance() {
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
