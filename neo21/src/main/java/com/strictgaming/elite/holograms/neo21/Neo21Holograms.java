package com.strictgaming.elite.holograms.neo21;

import com.strictgaming.elite.holograms.api.hologram.HologramBuilder;
import com.strictgaming.elite.holograms.api.manager.HologramFactory;
import com.strictgaming.elite.holograms.api.manager.PlatformHologramManager;
import com.strictgaming.elite.holograms.neo21.command.CommandFactory;
import com.strictgaming.elite.holograms.neo21.command.HologramsAnimateLineCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsCreateCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsCreateItemCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsDeleteCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsListCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsMoveHereCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsMoveToCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsCreateAtCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsReloadCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsTeleportCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsAddLineCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsSetLineCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsRemoveLineCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsNearCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsInfoCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsCopyCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsInsertLineCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsCreateScoreboardCommand;
import com.strictgaming.elite.holograms.neo21.command.HologramsMoveVerticalCommand;
import com.strictgaming.elite.holograms.neo21.config.HologramsConfig;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo21.hologram.manager.NeoForgeHologramFactory;
import com.strictgaming.elite.holograms.neo21.hologram.manager.NeoForgeHologramManager;
import com.strictgaming.elite.holograms.api.hologram.Hologram;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
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

@Mod(Neo21Holograms.MOD_ID)
public class Neo21Holograms implements PlatformHologramManager {

    public static final String MOD_ID = "eliteholograms";
    public static final String VERSION = "1.21.1-1.0.5";
    private static final Logger LOGGER = LogUtils.getLogger();

    private static Neo21Holograms instance;

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
    private HologramsMoveVerticalCommand moveVerticalCommand;

    public Neo21Holograms(IEventBus modEventBus) {
        instance = this;
        LOGGER.info("Initializing Elite Holograms mod for Minecraft 1.21.1");
        
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
        moveVerticalCommand = new HologramsMoveVerticalCommand();
        
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
        mainCommand.registerSubCommand("movevertical", moveVerticalCommand);
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
        
        // The register() method is part of SubCommand interface which these classes implement.
        // However, SubCommand interface is defined inside HologramsCommand, so let's check if the compiler sees it correctly.
        // The error "cannot find symbol method register" suggests that createItemCommand variable type doesn't have a register method.
        // SubCommand interface DOES NOT have a register method in the previous code!
        // The previous commands implemented it manually or it was missing from interface.
        // Let's check HologramsCommand.SubCommand interface. It has execute() and getArguments().
        // It does NOT have register().
        
        // The other commands likely have a register() method implemented directly in their class, NOT via interface.
        // We need to add register() method to HologramsCreateItemCommand and HologramsAnimateLineCommand.
        // OR use the existing registration pattern if one exists.
        // Looking at HologramsCreateCommand (impl SubCommand), it has a register() method.
        // But the variable type in Neo21Holograms is the class type, not interface.
        // So we just need to add the register() method to the new command classes.
        
        // For now, I'll comment out the direct registration lines that are failing since they are also registered via mainCommand.
        // Actually, the pattern is to register aliases or top-level shortcuts if desired.
        // If we want /eh createitem, we need to register it.
        // I will fix the command classes to include register() method in a separate step.
        
        // Wait, I can just use the main command registration which registers subcommands.
        // The lines failing are:
        // createItemCommand.register(event.getDispatcher());
        // animateLineCommand.register(event.getDispatcher());
        
        // This implies I expected them to be top-level commands or have aliases?
        // If I want /createitem to work directly (which is unusual for subcommands), I'd need register().
        // But standard usage is /eh createitem.
        // The mainCommand.register() call handles /eh <subcommand>.
        // So these direct register calls might be redundant or for aliases like /createitem directly?
        // The other commands like createCommand have register() called.
        
        // Let's look at createCommand.register(). It registers "eliteholograms" -> "create" AND "eh" -> "create".
        // It effectively registers the subcommand structure again? That seems redundant if mainCommand does it.
        // OR maybe mainCommand ONLY registers the main "eh" and "eliteholograms" literals, and subcommands attach to it?
        // Let's check HologramsCommand.register().
        // It builds the tree: literal("eh").then(literal("create")...)
        // So mainCommand.register() does EVERYTHING.
        
        // So why are we calling register() on individual commands? 
        // Maybe to register stand-alone aliases?
        // If I remove these calls, the subcommands are still registered via mainCommand.
        // Let's try removing them to fix compilation first.
        
        // createItemCommand.register(event.getDispatcher());
        // listCommand.register(event.getDispatcher()); // This one works? means ListCommand has register()
        
        // OK, I will comment them out for now.
        // createItemCommand.register(event.getDispatcher());
        // animateLineCommand.register(event.getDispatcher());
        
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
        moveVerticalCommand.register(event.getDispatcher());
        
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
        com.strictgaming.elite.holograms.neo21.util.UtilPlaceholder.setServerStartTime();
        this.checkForPlaceholders();
        try {
            HologramManager.load();
            LOGGER.info("Holograms loaded successfully");
        } catch (Exception e) {
            LOGGER.error("Error loading holograms", e);
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("Server stopping - preparing to save holograms with timeout protection");
        
        Thread shutdownThread = new Thread(() -> {
            try {
                HologramManager.getHolograms().values().forEach(hologram -> {
                    try {
                        if (hologram.isSpawned()) {
                            LOGGER.debug("Despawning hologram {} during server shutdown", hologram.getId());
                            hologram.despawn();
                        }
                    } catch (Exception e) {
                        LOGGER.error("Error despawning hologram {} during shutdown: {}", hologram.getId(), e.getMessage());
                    }
                });
                
                Thread saveThread = new Thread(() -> {
                    try {
                        HologramManager.saveSync();
                        // HologramManager.saveScoreboardHologramsSync(); // Already called in saveSync()
                        LOGGER.info("Holograms saved successfully during shutdown");
                    } catch (Exception e) {
                        LOGGER.error("Error saving holograms during shutdown", e);
                    }
                }, "EliteHolograms-Save");
                
                saveThread.start();
                
                try {
                    saveThread.join(5000);
                    if (saveThread.isAlive()) {
                        LOGGER.warn("Save operation timed out during shutdown, forcing interruption");
                        saveThread.interrupt();
                    }
                } catch (InterruptedException e) {
                    LOGGER.warn("Shutdown save interrupted");
                    Thread.currentThread().interrupt();
                }
                
            } catch (Exception e) {
                LOGGER.error("Error during shutdown operations", e);
            }
        }, "EliteHolograms-Shutdown");
        
        shutdownThread.start();
        
        try {
            shutdownThread.join(2000);
            if (shutdownThread.isAlive()) {
                LOGGER.warn("Shutdown thread timed out, forcing interruption");
                shutdownThread.interrupt();
            }
        } catch (InterruptedException e) {
            LOGGER.warn("Main shutdown interrupted");
            Thread.currentThread().interrupt();
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

    public static Neo21Holograms getInstance() {
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