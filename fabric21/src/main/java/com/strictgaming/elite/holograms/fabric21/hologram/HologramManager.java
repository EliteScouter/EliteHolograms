package com.strictgaming.elite.holograms.fabric21.hologram;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.manager.database.HologramSaver;
import com.strictgaming.elite.holograms.fabric21.FabricHolograms;
import com.strictgaming.elite.holograms.fabric21.config.ScoreboardHologramConfig;
import com.strictgaming.elite.holograms.fabric21.hologram.database.JsonHologramSaver;
import com.strictgaming.elite.holograms.fabric21.hologram.entity.HologramInteraction;
import com.strictgaming.elite.holograms.fabric21.hologram.entity.HologramLineRenderer;
import com.strictgaming.elite.holograms.fabric21.hologram.ScoreboardHologram;
import com.strictgaming.elite.holograms.fabric21.util.UtilConcurrency;
import com.strictgaming.elite.holograms.fabric21.util.UtilPlayer;
import com.strictgaming.elite.holograms.fabric21.util.UtilServer;
import net.minecraft.world.level.Level;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.server.level.ServerPlayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 *
 * Static factory manager for all {@link Hologram}s on the server
 *
 */
public class HologramManager implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    
    private static final Map<String, FabricHologram> HOLOGRAMS = Maps.newConcurrentMap();
    private static JsonHologramSaver saver;
    private static ScoreboardHologramConfig scoreboardConfig;
    private static volatile boolean shutdown = false;
    private static Thread managerThread;
    /**
     * True from the end of a load until shutdown, and saves are refused outside that window.
     * Before the load a save would overwrite holograms that have not been read yet; after
     * shutdown it would write a registry that is being torn down.
     */
    private static volatile boolean acceptingSaves = false;
    /** Serializes save/load so async saves cannot overwrite the file after clear/reload. */
    private static final Object SAVE_LOAD_LOCK = new Object();
    /** When true, save()/saveSync() are no-ops so deserialize-triggered saves cannot wipe the file. */
    private static volatile boolean loading = false;

    public static void preInit() {
        // Don't start thread here anymore
        // managerThread = new Thread(new HologramManager());
        // managerThread.start();

        saver = new JsonHologramSaver(FabricHolograms.getInstance().getConfig().getStorageLocation());
        // Ensure scoreboard configs are saved alongside other config files
        File storagePath = new File(FabricHolograms.getInstance().getConfig().getStorageLocation());
        File configDir = storagePath.isDirectory() ? storagePath : storagePath.getParentFile();
        if (configDir != null && !configDir.exists()) {
            configDir.mkdirs();
        }
        scoreboardConfig = new ScoreboardHologramConfig(configDir);
        com.strictgaming.elite.holograms.fabric21.config.ScoreboardThemeManager.init(configDir);
    }
    
    /**
     * Starts the background manager thread. Should be called when server starts.
     */
    public static void start() {
        if (managerThread != null && managerThread.isAlive()) {
            return;
        }
        
        shutdown = false;
        managerThread = new Thread(new HologramManager());
        managerThread.setName("HologramManager-Thread");
        managerThread.setDaemon(true); // Ensure it dies if JVM exits
        managerThread.start();
        LOGGER.info("Hologram manager thread started");
    }

    private HologramManager() {
        // Private constructor for singleton
    }

    public static void clear() {
        synchronized (SAVE_LOAD_LOCK) {
            for (FabricHologram value : HOLOGRAMS.values()) {
                value.despawn();
            }
            HOLOGRAMS.clear();
        }
    }

    public static void load() throws IOException {
        synchronized (SAVE_LOAD_LOCK) {
            loading = true;
            LOGGER.info("Loading holograms from storage...");

            // Use the correct context classloader for Gson deserialization
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(FabricHolograms.class.getClassLoader());

            try {
                // Save a snapshot of the currently loaded holograms
                Map<String, FabricHologram> existingHolograms = new HashMap<>(HOLOGRAMS);

                // Load from the saver (may construct holograms that call save(), which is suppressed while loading)
                Map<String, Hologram> loadedHolograms = saver.load();

                if (loadedHolograms.isEmpty() && !existingHolograms.isEmpty() && !saver.foundData()) {
                    // The file is missing or unreadable, and a damaged one has already been copied
                    // aside. What is in memory is the best copy left, so keep it and write it
                    // back rather than replacing it with nothing. A file that validly lists no
                    // holograms is not this case: that is how the store is emptied on purpose.
                    LOGGER.warn("holograms.json could not be read, keeping the {} holograms already loaded",
                            existingHolograms.size());
                    loading = false;
                    acceptingSaves = true;
                    saver.save(Lists.newArrayList(existingHolograms.values()));
                    forceSaveScoreboardHolograms();
                    return;
                }

                // Clear current holograms to ensure a clean state matching the file
                // BUT we need to despawn existing ones first if the server is running
                if (UtilServer.get() != null) {
                    // Reading the file constructed replacements, and each one registered itself
                    // over the live hologram with its ID. Despawning the registry here would
                    // therefore despawn the new, never-spawned objects and strand the old ones:
                    // still drawn for players, still holding their backlight blocks and their
                    // click hitbox entities. Despawn what was actually on screen.
                    for (FabricHologram h : existingHolograms.values()) {
                        h.despawn();
                    }
                }
                HOLOGRAMS.clear();

                // Add loaded holograms
                for (Map.Entry<String, Hologram> entry : loadedHolograms.entrySet()) {
                    if (entry.getValue() instanceof FabricHologram) {
                        HOLOGRAMS.put(entry.getKey().toLowerCase(), (FabricHologram) entry.getValue());
                        LOGGER.info("Added hologram from storage: " + entry.getKey());
                    }
                }

                LOGGER.info("Successfully loaded " + HOLOGRAMS.size() + " holograms");

                // Spawn all loaded holograms now that they're added to the manager
                // This ensures ItemHolograms initialize their item stands
                // One at a time, so a single failure cannot stop the rest (or the scoreboard
                // holograms below) from loading.
                for (FabricHologram hologram : HOLOGRAMS.values()) {
                    try {
                        hologram.spawn();
                    } catch (Exception e) {
                        LOGGER.error("Could not spawn hologram '{}'", hologram.getId(), e);
                    }
                }

                // Load scoreboard holograms separately
                loadScoreboardHolograms();
            } finally {
                loading = false;
                acceptingSaves = true;
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
        }
    }

    /**
     * Synchronous save under the save/load lock. Use before reload/shutdown so pending
     * async saves cannot race with clear() and write an empty/partial file.
     */
    public static void saveSync() {
        synchronized (SAVE_LOAD_LOCK) {
            if (loading) {
                LOGGER.debug("Skipping saveSync while holograms are loading");
                return;
            }
            if (!acceptingSaves) {
                LOGGER.debug("Skipping saveSync outside a loaded session");
                return;
            }
            if (saver == null) {
                LOGGER.warn("Saver is null, cannot save holograms");
                return;
            }
            try {
                List<Hologram> snapshot = Lists.newArrayList(HOLOGRAMS.values());
                LOGGER.info("Saving {} holograms to storage (sync)", snapshot.size());
                saver.save(snapshot);
                forceSaveScoreboardHolograms();
            } catch (Exception e) {
                LOGGER.error("Failed to save holograms (sync)", e);
            }
        }
    }

    public static void save() {
        if (loading || !acceptingSaves) {
            return;
        }
        UtilConcurrency.runAsync(() -> {
            synchronized (SAVE_LOAD_LOCK) {
                // Re-checked under the lock: shutdown may have closed the session while
                // this was queued.
                if (loading || !acceptingSaves || saver == null) {
                    return;
                }
                try {
                    // Snapshot under lock so we never write a mid-clear empty map
                    saver.save(Lists.newArrayList(HOLOGRAMS.values()));
                    forceSaveScoreboardHolograms();
                } catch (Exception e) {
                    LOGGER.error("Failed to save holograms", e);
                }
            }
        });
    }

    public static void addHologram(Hologram hologram) {
        if (!(hologram instanceof FabricHologram)) {
            return;
        }

        addHologram((FabricHologram) hologram);
    }

    public static void addHologram(FabricHologram hologram) {
        HOLOGRAMS.put(hologram.getId().toLowerCase(), hologram);
    }

    public static void removeHologram(Hologram hologram) {
        if (!(hologram instanceof FabricHologram)) {
            return;
        }

        removeHologram((FabricHologram) hologram);
    }

    public static void removeHologram(FabricHologram hologram) {
        HOLOGRAMS.remove(hologram.getId().toLowerCase());
    }

    public static Hologram getById(String id) {
        return HOLOGRAMS.get(id.toLowerCase());
    }

    public static List<Hologram> getAllHolograms() {
        return Collections.unmodifiableList(Lists.newArrayList(HOLOGRAMS.values()));
    }

    /**
     * Get all holograms on the server
     * 
     * @return A list of all holograms
     */
    public static List<Hologram> getHolograms() {
        return getAllHolograms();
    }

    /**
     * Get the hologram saver used by this manager
     * 
     * @return The hologram saver instance
     */
    public static HologramSaver getSaver() {
        return saver;
    }

    /**
     * Saves every scoreboard hologram. Caller must hold SAVE_LOAD_LOCK.
     *
     * <p>There used to be a five second cooldown here, which silently dropped a second edit to
     * a board made within five seconds of the first. It only reached the disk at shutdown, so
     * a crash lost it. The cooldown existed to absorb board refreshes, which no longer save.
     *
     * <p>Writes even when no boards are left, which is how deleting the last one reaches the
     * disk. That is only safe because the file is never written before it has been read.
     */
    public static void forceSaveScoreboardHolograms() {
        if (scoreboardConfig == null || !scoreboardConfig.isLoaded()) {
            return;
        }
        List<ScoreboardHologram> scoreboardHolograms = Lists.newArrayList();
        for (FabricHologram hologram : HOLOGRAMS.values()) {
            if (hologram instanceof ScoreboardHologram) {
                scoreboardHolograms.add((ScoreboardHologram) hologram);
            }
        }
        scoreboardConfig.save(scoreboardHolograms);
        LOGGER.debug("Saved {} scoreboard holograms", scoreboardHolograms.size());
    }
    
    /**
     * Save scoreboard holograms to separate config file (synchronous, for shutdown)
     */
    public static void saveScoreboardHologramsSync() {
        synchronized (SAVE_LOAD_LOCK) {
            forceSaveScoreboardHolograms();
            LOGGER.info("Saved scoreboard holograms during shutdown");
        }
    }

    /**
     * Ends the session. Call after the final shutdown save.
     *
     * <p>Saves are refused from here on, so an async save queued before shutdown cannot run
     * afterwards. The registry is emptied because a single-player game keeps this class
     * loaded between worlds, and the next world must not inherit holograms bound to this
     * one's levels.
     */
    public static void close() {
        synchronized (SAVE_LOAD_LOCK) {
            acceptingSaves = false;
            HOLOGRAMS.clear();
            INTERACTION_OWNERS.clear();
            LAST_CLICK.clear();
        }
    }

    /**
     * Whether either store was edited by hand since the mod last read or wrote it.
     *
     * <p>{@code /eh reload} uses this to decide whether to write memory to disk first. It
     * used to do so unconditionally, which meant a hand edit was overwritten by the very
     * command meant to load it.
     *
     * @return true if either file differs from the one the mod last saw
     */
    public static boolean storesChangedOnDisk() {
        synchronized (SAVE_LOAD_LOCK) {
            return (saver != null && saver.changedOnDisk())
                    || (scoreboardConfig != null && scoreboardConfig.changedOnDisk());
        }
    }
    
    /**
     * Load scoreboard holograms from separate config file
     */
    /**
     * Reads a "#RRGGBB" colour from the scoreboard config.
     *
     * @param value        the stored value, null on configs written before backgrounds existed
     * @param defaultValue the colour to use when it is missing or malformed
     * @return the RGB colour
     */
    private static int parseColour(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.replace("#", "").trim(), 16);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid backgroundColour '{}' for a scoreboard hologram, using the default", value);
            return defaultValue;
        }
    }

    /**
     * Reads a billboard name from the scoreboard config.
     *
     * @param value the stored name, may be null on older configs
     * @return the matching constraint, or fixed when missing or unrecognised
     */
    private static Display.BillboardConstraints parseBillboard(String value) {
        Display.BillboardConstraints parsed =
                com.strictgaming.elite.holograms.fabric21.command.HologramsDisplayCommand.parseBillboard(value);
        return parsed == null ? Display.BillboardConstraints.FIXED : parsed;
    }

    private static void loadScoreboardHolograms() {
        // Refresh themes from disk first so reload picks up edits without a reboot.
        com.strictgaming.elite.holograms.fabric21.config.ScoreboardThemeManager.reload();
        List<ScoreboardHologramConfig.ScoreboardHologramData> configData = scoreboardConfig.load();
        
        for (ScoreboardHologramConfig.ScoreboardHologramData data : configData) {
            try {
                // Find the world
                Level world = findWorldByName(data.worldName);
                if (world == null) {
                    // Skipping it outright would erase it from disk on the next save.
                    scoreboardConfig.keepUnresolved(data, "its world '" + data.worldName
                            + "' is not loaded. It will load again once that world is available");
                    continue;
                }
                
                // Check if hologram already exists
                if (getById(data.id) != null) {
                    if (!(getById(data.id) instanceof ScoreboardHologram)) {
                        scoreboardConfig.keepUnresolved(data,
                                "a regular hologram already uses the ID '" + data.id + "'");
                    }
                    continue;
                }
                
                // Create the scoreboard hologram
                ScoreboardHologram hologram = new ScoreboardHologram(
                    data.id,
                    world,
                    new net.minecraft.world.phys.Vec3(data.x, data.y, data.z),
                    data.range,
                    data.objectiveName,
                    data.topCount,
                    data.updateInterval,
                    data.theme,
                    data.headerFormat,
                    data.playerFormat,
                    data.emptyFormat,
                    HologramDisplayType.fromStringOrDefault(data.displayType, HologramDisplayType.FACING),
                    data.yaw,
                    data.pitch
                );
                
                // Restore presentation and click settings before the first render, which
                // rebuilds the line entities and reads them on construction.
                hologram.restoreBackgroundState(
                        parseColour(data.backgroundColour, FabricHologram.DEFAULT_BACKGROUND_COLOUR),
                        data.backgroundOpacity == null
                                ? FabricHologram.DEFAULT_BACKGROUND_OPACITY
                                : data.backgroundOpacity);

                hologram.restorePresentationState(
                        data.scale == null
                                ? com.strictgaming.elite.holograms.fabric21.hologram.entity.HologramTextDisplay.DEFAULT_SCALE
                                : data.scale,
                        data.brightness == null
                                ? com.strictgaming.elite.holograms.fabric21.hologram.entity.HologramTextDisplay.BRIGHTNESS_AUTO
                                : data.brightness,
                        parseBillboard(data.billboard));

                hologram.restoreClickState(data.clickCommand, data.clickAsConsole);

                // Force initial update
                hologram.forceUpdate();

                // Re-apply persisted backlight (vertical light column) so it survives reload/restart
                if (data.backlightEnabled) {
                    hologram.setBacklight(true, data.backlightLevel);
                }

                LOGGER.info("Recreated scoreboard hologram '{}' for objective '{}'", data.id, data.objectiveName);
                
            } catch (Exception e) {
                LOGGER.error("Error recreating scoreboard hologram '{}'", data.id, e);

                // The constructor registers the board before it is fully configured, so a
                // failure part way leaves a half-built one behind, and saving that would
                // overwrite the good copy on disk with default settings. Take it back out and
                // keep the original instead.
                Hologram partial = getById(data.id);
                if (partial instanceof ScoreboardHologram board) {
                    try {
                        board.despawn();
                    } catch (Exception ignored) {
                        // Best effort; it is being discarded either way.
                    }
                    removeHologram(board);
                }
                scoreboardConfig.keepUnresolved(data, "it could not be rebuilt (" + e.getMessage() + ")");
            }
        }
    }
    
    /**
     * Find a world by name
     */
    private static Level findWorldByName(String worldName) {
        if (UtilServer.get() == null) {
            return null;
        }
        
        for (Level world : UtilServer.get().getAllLevels()) {
            if (world.dimension().location().toString().equals(worldName)) {
                return world;
            }
        }
        
        return null;
    }

    /**
     * Signals the manager to shutdown and stops the background thread
     */
    public static void shutdown() {
        LOGGER.info("Shutting down hologram manager...");
        shutdown = true;
        
        if (managerThread != null) {
            managerThread.interrupt();
            
            // Wait for thread to finish with timeout to prevent hanging
            try {
                managerThread.join(2000); // Wait max 2 seconds
                if (managerThread.isAlive()) {
                    LOGGER.warn("Hologram manager thread did not shutdown gracefully within timeout");
                } else {
                    LOGGER.info("Hologram manager thread shutdown successfully");
                }
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted while waiting for hologram manager thread to shutdown");
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public void run() {
        while (!shutdown) {
            try {
                Thread.sleep(500); // Run every half second
                checkHolograms();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Called every server tick to update animations and scoreboards
     * This should be called from the main server thread
     */
    public static void tick() {
        if (UtilServer.get() == null) {
            return;
        }

        for (FabricHologram hologram : HOLOGRAMS.values()) {
            if (hologram == null || hologram.getWorld() == null) {
                continue; 
            }
            
            // Tick all holograms (for animations)
            hologram.tick();
            
            // Additional scoreboard-specific ticking
            if (hologram instanceof ScoreboardHologram) {
                ((ScoreboardHologram) hologram).tick();
            }
        }
    }

    private void checkHolograms() {
        MinecraftServer server = UtilServer.get();

        // The player list is built after the server object exists. Fabric's SERVER_STARTING
        // fires inside that window (earlier relative to startup than Forge's equivalent), so
        // this thread can get a live server whose player list is still null.
        if (server == null || server.getPlayerList() == null) {
            return;
        }
        
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player == null || player.level() == null) {
                continue; // Skip if player or their level is null
            }
            
            for (FabricHologram hologram : HOLOGRAMS.values()) {
                if (hologram == null || hologram.getWorld() == null) {
                    continue; // Skip if hologram or its world is null
                }
                
                boolean isNearby = hologram.getNearbyPlayers().contains(player.getUUID());
                
                if (!hologram.getWorld().equals(player.level())) {
                    if (isNearby) {
                        hologram.getNearbyPlayers().remove(player.getUUID());

                        // Despawn item if this is an ItemHologram
                        if (hologram instanceof ItemHologram) {
                            UtilConcurrency.runSync(() -> ((ItemHologram) hologram).despawnItemFor(player));
                        }
                        
                        for (HologramLineRenderer line : hologram.getLines()) {
                            if (line != null) { // Check if line is not null
                                UtilConcurrency.runSync(() -> line.despawnForPlayer(player));
                            }
                        }
                    }

                    continue;
                }

                if (player.distanceToSqr(hologram.getPosition()) > (Math.pow(hologram.getRange(), 2))) {
                    if (isNearby) {
                        hologram.getNearbyPlayers().remove(player.getUUID());

                        // Despawn item if this is an ItemHologram
                        if (hologram instanceof ItemHologram) {
                            UtilConcurrency.runSync(() -> ((ItemHologram) hologram).despawnItemFor(player));
                        }
                        
                        for (HologramLineRenderer line : hologram.getLines()) {
                            if (line != null) { // Check if line is not null
                                UtilConcurrency.runSync(() -> line.despawnForPlayer(player));
                            }
                        }
                    }

                    continue;
                }

                if (!hologram.getNearbyPlayers().contains(player.getUUID())) {
                    // A player being in range means this chunk is loaded, which is the only
                    // point at which a click hitbox can actually be added to the world.
                    UtilConcurrency.runSync(hologram::ensureClickHitbox);

                    // Spawn item first if this is an ItemHologram
                    if (hologram instanceof ItemHologram) {
                        UtilConcurrency.runSync(() -> ((ItemHologram) hologram).spawnItemFor(player));
                    }
                    
                    for (HologramLineRenderer line : hologram.getLines()) {
                        if (line != null) { // Check if line is not null
                            UtilConcurrency.runSync(() -> {
                                line.spawnForPlayer(player);
                            });
                        }
                    }

                    hologram.getNearbyPlayers().add(player.getUUID());
                } else {
                    // Player is already nearby: update non-animated lines so placeholders
                    // like %players% and %maxplayers% stay current
                    for (HologramLineRenderer line : hologram.getLines()) {
                        if (line != null && !line.isAnimated()) {
                            UtilConcurrency.runSync(() -> line.updateForPlayer(player));
                        }
                    }
                }
            }
        }
    }

    /**
     * Drops a departing player from every hologram's viewer list.
     *
     * <p>Forge delivered this through {@code PlayerEvent.PlayerLoggedOutEvent}. On Fabric the
     * mod initialiser calls it from {@code ServerPlayConnectionEvents.DISCONNECT}.
     *
     * @param playerUUID the departing player
     */
    public static void onPlayerQuit(UUID playerUUID) {
        if (playerUUID == null) {
            return;
        }

        LAST_CLICK.remove(playerUUID);

        for (FabricHologram value : HologramManager.HOLOGRAMS.values()) {
            if (value != null && value.getNearbyPlayers() != null) {
                value.getNearbyPlayers().remove(playerUUID);
            }
        }
    }

    /**
     * Re-checks every hologram's visibility for a joining player.
     *
     * <p>Forge delivered this through {@code PlayerEvent.PlayerLoggedInEvent}. On Fabric the
     * mod initialiser calls it from {@code ServerPlayConnectionEvents.JOIN}.
     *
     * @param player the joining player
     */
    public static void onPlayerLogin(ServerPlayer player) {
        if (player == null) {
            return;
        }

        for (FabricHologram hologram : HologramManager.HOLOGRAMS.values()) {
            if (hologram != null) {
                hologram.refreshVisibility();
            }
        }

        LOGGER.debug("Refreshed hologram visibility for player: {}", player.getName().getString());
    }
    // Maps a clickable hitbox to the hologram that owns it. Interactions are real entities,
    // so a click arrives as a normal interaction event carrying the entity. This is how that
    // entity is turned back into a hologram without scanning every one of them.
    private static final Map<UUID, String> INTERACTION_OWNERS = Maps.newConcurrentMap();

    /** Stops a single player triggering a command every tick by holding right-click. */
    private static final long CLICK_COOLDOWN_MS = 500L;
    private static final Map<UUID, Long> LAST_CLICK = Maps.newConcurrentMap();

    /**
     * Records that a clickable hitbox belongs to a hologram.
     *
     * @param entityUUID the hitbox entity
     * @param hologramId the owning hologram
     */
    public static void registerInteraction(UUID entityUUID, String hologramId) {
        INTERACTION_OWNERS.put(entityUUID, hologramId);
    }

    /**
     * Forgets a clickable hitbox, called when it is removed from the world.
     *
     * @param entityUUID the hitbox entity
     */
    public static void unregisterInteraction(UUID entityUUID) {
        INTERACTION_OWNERS.remove(entityUUID);
    }

    /**
     * Runs the click command of whichever hologram owns the given hitbox.
     *
     * <p>Called from each loader's interaction event. Does nothing, and reports nothing, when
     * the entity is not one of ours, so unrelated interactions are untouched.
     *
     * @param player     the player who clicked
     * @param entity     the entity they clicked
     * @return true when this was one of our hitboxes and the click was consumed
     */
    public static boolean handleInteractionClick(ServerPlayer player, Entity entity) {
        if (player == null || !(entity instanceof Interaction)
                || !entity.getTags().contains(HologramInteraction.HOLOGRAM_TAG)) {
            return false;
        }

        String hologramId = INTERACTION_OWNERS.get(entity.getUUID());

        if (hologramId == null) {
            // Ours by tag but not by registry, so it is a stale copy saved into the chunk
            // by a build before hitboxes stopped persisting. Clear it out as we meet it,
            // which repairs an existing world without a chunk-load sweep.
            LOGGER.debug("Discarding an orphaned hologram click hitbox at {}", entity.blockPosition());
            entity.discard();
            return false;
        }

        // Consume the click from here on: it was ours even if we end up running nothing.
        long now = System.currentTimeMillis();
        Long last = LAST_CLICK.get(player.getUUID());

        if (last != null && (now - last) < CLICK_COOLDOWN_MS) {
            return true;
        }

        LAST_CLICK.put(player.getUUID(), now);

        Hologram hologram = getById(hologramId);

        if (!(hologram instanceof FabricHologram fh) || fh.getClickCommand() == null) {
            return true;
        }

        String command = fh.getClickCommand().replace("%player%", player.getName().getString());

        try {
            CommandSourceStack source = player.createCommandSourceStack();

            if (fh.isClickAsConsole()) {
                // Same source, elevated, so @s and the execution position still mean the
                // player who clicked. Setting this mode is gated on the admin permission.
                source = source.withPermission(4);
            }

            player.getServer().getCommands().performPrefixedCommand(source, command);
        } catch (Exception e) {
            LOGGER.error("Error running click command for hologram {}: {}", hologramId, e.getMessage());
        }

        return true;
    }

    /**
     * Removes clickable hitboxes left behind by a server that did not run its shutdown path.
     *
     * <p>These are real entities and are saved with the chunk, so a crash would otherwise leave
     * a duplicate behind every time the server came back up. Only entities carrying our own tag
     * are touched.
     */
    public static void sweepOrphanedInteractions() {
        if (UtilServer.get() == null) {
            return;
        }

        int removed = 0;

        for (ServerLevel level : UtilServer.get().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Interaction && entity.getTags().contains(HologramInteraction.HOLOGRAM_TAG)) {
                    entity.discard();
                    removed++;
                }
            }
        }

        if (removed > 0) {
            LOGGER.info("Removed {} orphaned hologram click hitboxes left by a previous run", removed);
        }
    }
}
