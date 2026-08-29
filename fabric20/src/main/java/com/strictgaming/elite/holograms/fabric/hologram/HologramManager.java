package com.strictgaming.elite.holograms.fabric.hologram;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.manager.database.HologramSaver;
import com.strictgaming.elite.holograms.fabric.FabricHolograms;
import com.strictgaming.elite.holograms.fabric.config.ScoreboardHologramConfig;
import com.strictgaming.elite.holograms.fabric.hologram.database.JsonHologramSaver;
import com.strictgaming.elite.holograms.fabric.hologram.entity.HologramLineRenderer;
import com.strictgaming.elite.holograms.fabric.hologram.ScoreboardHologram;
import com.strictgaming.elite.holograms.fabric.util.UtilConcurrency;
import com.strictgaming.elite.holograms.fabric.util.UtilPlayer;
import com.strictgaming.elite.holograms.fabric.util.UtilServer;
import net.minecraft.world.level.Level;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.server.MinecraftServer;
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
    private static HologramSaver saver;
    private static ScoreboardHologramConfig scoreboardConfig;
    private static boolean shutdown = false;
    private static Thread managerThread;
    private static long lastScoreboardSave = 0;
    private static final long SCOREBOARD_SAVE_COOLDOWN = 5000; // 5 seconds cooldown
    /** Serializes save/load so async saves cannot overwrite the file after clear/reload. */
    private static final Object SAVE_LOAD_LOCK = new Object();
    /** When true, save()/saveSync() are no-ops so deserialize-triggered saves cannot wipe the file. */
    private static volatile boolean loading = false;

    public static void preInit() {
        // Don't start thread here anymore
        // managerThread = new Thread(new HologramManager());
        // managerThread.start();

        saver = (HologramSaver) new JsonHologramSaver(FabricHolograms.getInstance().getConfig().getStorageLocation());
        // Ensure scoreboard configs are saved alongside other config files
        File storagePath = new File(FabricHolograms.getInstance().getConfig().getStorageLocation());
        File configDir = storagePath.isDirectory() ? storagePath : storagePath.getParentFile();
        if (configDir != null && !configDir.exists()) {
            configDir.mkdirs();
        }
        scoreboardConfig = new ScoreboardHologramConfig(configDir);
        com.strictgaming.elite.holograms.fabric.config.ScoreboardThemeManager.init(configDir);
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

                // Load from the saver (may construct holograms that call save() — suppressed while loading)
                Map<String, Hologram> loadedHolograms = saver.load();

                if (loadedHolograms.isEmpty() && !existingHolograms.isEmpty()) {
                    LOGGER.info("No holograms loaded from file, but we have " + existingHolograms.size() + " in memory - preserving existing");
                    loading = false;
                    // Write existing ones synchronously under the same lock
                    if (saver != null) {
                        saver.save(Lists.newArrayList(existingHolograms.values()));
                        forceSaveScoreboardHolograms();
                    }
                    return;
                }

                // Clear current holograms to ensure a clean state matching the file
                // BUT we need to despawn existing ones first if the server is running
                if (UtilServer.get() != null) {
                    for (FabricHologram h : HOLOGRAMS.values()) {
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
                for (FabricHologram hologram : HOLOGRAMS.values()) {
                    if (hologram != null) {
                        hologram.spawn();
                    }
                }

                // Load scoreboard holograms separately
                loadScoreboardHolograms();
            } finally {
                loading = false;
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
        if (loading) {
            return;
        }
        UtilConcurrency.runAsync(() -> {
            synchronized (SAVE_LOAD_LOCK) {
                if (loading || saver == null) {
                    return;
                }
                try {
                    // Snapshot under lock so we never write a mid-clear empty map
                    saver.save(Lists.newArrayList(HOLOGRAMS.values()));
                    saveScoreboardHolograms();
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
     * Save scoreboard holograms to separate config file (rate-limited). Caller must hold SAVE_LOAD_LOCK.
     */
    private static void saveScoreboardHolograms() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastScoreboardSave < SCOREBOARD_SAVE_COOLDOWN) {
            return;
        }
        forceSaveScoreboardHolograms();
    }

    /**
     * Force-save scoreboard holograms (no cooldown). Caller must hold SAVE_LOAD_LOCK.
     */
    public static void forceSaveScoreboardHolograms() {
        if (scoreboardConfig == null) {
            return;
        }
        List<ScoreboardHologram> scoreboardHolograms = Lists.newArrayList();
        for (FabricHologram hologram : HOLOGRAMS.values()) {
            if (hologram instanceof ScoreboardHologram) {
                scoreboardHolograms.add((ScoreboardHologram) hologram);
            }
        }
        if (!scoreboardHolograms.isEmpty()) {
            scoreboardConfig.save(scoreboardHolograms);
            lastScoreboardSave = System.currentTimeMillis();
            LOGGER.debug("Saved {} scoreboard holograms", scoreboardHolograms.size());
        }
    }
    
    /**
     * Save scoreboard holograms to separate config file (synchronous - for shutdown)
     */
    public static void saveScoreboardHologramsSync() {
        synchronized (SAVE_LOAD_LOCK) {
            forceSaveScoreboardHolograms();
            LOGGER.info("Saved scoreboard holograms during shutdown");
        }
    }
    
    /**
     * Load scoreboard holograms from separate config file
     */
    private static void loadScoreboardHolograms() {
        // Refresh themes from disk first so reload picks up edits without a reboot.
        com.strictgaming.elite.holograms.fabric.config.ScoreboardThemeManager.reload();
        List<ScoreboardHologramConfig.ScoreboardHologramData> configData = scoreboardConfig.load();
        
        for (ScoreboardHologramConfig.ScoreboardHologramData data : configData) {
            try {
                // Find the world
                Level world = findWorldByName(data.worldName);
                if (world == null) {
                    LOGGER.warn("Could not find world '{}' for scoreboard hologram '{}'", data.worldName, data.id);
                    continue;
                }
                
                // Check if hologram already exists
                if (getById(data.id) != null) {
                    LOGGER.debug("Scoreboard hologram '{}' already exists, skipping", data.id);
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
                
                // Force initial update
                hologram.forceUpdate();

                // Re-apply persisted backlight (vertical light column) so it survives reload/restart
                if (data.backlightEnabled) {
                    hologram.setBacklight(true, data.backlightLevel);
                }

                LOGGER.info("Recreated scoreboard hologram '{}' for objective '{}'", data.id, data.objectiveName);
                
            } catch (Exception e) {
                LOGGER.error("Error recreating scoreboard hologram '{}': {}", data.id, e.getMessage());
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
        // fires inside that window - earlier relative to startup than Forge's equivalent - so
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
                    // Player is already nearby - update non-animated lines so placeholders
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
}
