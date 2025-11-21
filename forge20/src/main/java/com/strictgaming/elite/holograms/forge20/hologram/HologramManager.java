package com.strictgaming.elite.holograms.forge20.hologram;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.manager.database.HologramSaver;
import com.strictgaming.elite.holograms.forge20.Forge20Holograms;
import com.strictgaming.elite.holograms.forge20.config.ScoreboardHologramConfig;
import com.strictgaming.elite.holograms.forge20.hologram.database.JsonHologramSaver;
import com.strictgaming.elite.holograms.forge20.hologram.entity.HologramLine;
import com.strictgaming.elite.holograms.forge20.hologram.entity.AnimatedHologramLine;
import com.strictgaming.elite.holograms.forge20.hologram.ScoreboardHologram;
import com.strictgaming.elite.holograms.forge20.util.UtilConcurrency;
import com.strictgaming.elite.holograms.forge20.util.UtilPlayer;
import net.minecraft.world.level.Level;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.server.ServerLifecycleHooks;
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
    
    private static final Map<String, ForgeHologram> HOLOGRAMS = Maps.newConcurrentMap();
    private static HologramSaver saver;
    private static ScoreboardHologramConfig scoreboardConfig;
    private static boolean shutdown = false;
    private static Thread managerThread;
    private static long lastScoreboardSave = 0;
    private static final long SCOREBOARD_SAVE_COOLDOWN = 5000; // 5 seconds cooldown

    public static void preInit() {
        // Don't start thread here anymore
        // managerThread = new Thread(new HologramManager());
        // managerThread.start();

        new PlayerEventListener();
        saver = (HologramSaver) new JsonHologramSaver(Forge20Holograms.getInstance().getConfig().getStorageLocation());
        // Ensure scoreboard configs are saved alongside other config files
        File storagePath = new File(Forge20Holograms.getInstance().getConfig().getStorageLocation());
        File configDir = storagePath.isDirectory() ? storagePath : storagePath.getParentFile();
        if (configDir != null && !configDir.exists()) {
            configDir.mkdirs();
        }
        scoreboardConfig = new ScoreboardHologramConfig(configDir);
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
        for (ForgeHologram value : HOLOGRAMS.values()) {
            value.despawn();
        }
        HOLOGRAMS.clear();
    }

    public static void load() throws IOException {
        LOGGER.info("Loading holograms from storage...");
        
        // Use the correct context classloader for Gson deserialization
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(Forge20Holograms.class.getClassLoader());
        
        try {
            // Save a snapshot of the currently loaded holograms
            Map<String, ForgeHologram> existingHolograms = new HashMap<>(HOLOGRAMS);
            
            // Load from the saver
            Map<String, Hologram> loadedHolograms = saver.load();
            
            if (loadedHolograms.isEmpty() && !existingHolograms.isEmpty()) {
                LOGGER.info("No holograms loaded from file, but we have " + existingHolograms.size() + " in memory - preserving existing");
                // If we have holograms in memory but none loaded from file, likely an error in loading
                // Just write the existing ones to file again
                save();
                return;
            }
            
            // Clear current holograms to ensure a clean state matching the file
            // BUT we need to despawn existing ones first if the server is running
            if (ServerLifecycleHooks.getCurrentServer() != null) {
                 for (ForgeHologram h : HOLOGRAMS.values()) {
                     h.despawn();
                 }
            }
            HOLOGRAMS.clear();

            // Add loaded holograms
            for (Map.Entry<String, Hologram> entry : loadedHolograms.entrySet()) {
                if (entry.getValue() instanceof ForgeHologram) {
                    HOLOGRAMS.put(entry.getKey().toLowerCase(), (ForgeHologram) entry.getValue());
                    LOGGER.info("Added hologram from storage: " + entry.getKey());
                }
            }
            
            LOGGER.info("Successfully loaded " + HOLOGRAMS.size() + " holograms");
            
            // Spawn all loaded holograms now that they're added to the manager
            // This ensures ItemHolograms initialize their item stands
            for (ForgeHologram hologram : HOLOGRAMS.values()) {
                if (hologram != null) {
                    hologram.spawn();
                }
            }
            
            // Load scoreboard holograms separately
            loadScoreboardHolograms();
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public static void save() {
        UtilConcurrency.runAsync(() -> {
            // Save regular holograms
            saver.save(Lists.newArrayList(HOLOGRAMS.values()));
            
            // Save scoreboard holograms separately
            saveScoreboardHolograms();
        });
    }

    public static void addHologram(Hologram hologram) {
        if (!(hologram instanceof ForgeHologram)) {
            return;
        }

        addHologram((ForgeHologram) hologram);
    }

    public static void addHologram(ForgeHologram hologram) {
        HOLOGRAMS.put(hologram.getId().toLowerCase(), hologram);
    }

    public static void removeHologram(Hologram hologram) {
        if (!(hologram instanceof ForgeHologram)) {
            return;
        }

        removeHologram((ForgeHologram) hologram);
    }

    public static void removeHologram(ForgeHologram hologram) {
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
     * Save scoreboard holograms to separate config file (async)
     */
    private static void saveScoreboardHolograms() {
        // Rate limit scoreboard saves to prevent spam
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastScoreboardSave < SCOREBOARD_SAVE_COOLDOWN) {
            return; // Skip save if too recent
        }
        
        List<ScoreboardHologram> scoreboardHolograms = Lists.newArrayList();
        
        for (ForgeHologram hologram : HOLOGRAMS.values()) {
            if (hologram instanceof ScoreboardHologram) {
                scoreboardHolograms.add((ScoreboardHologram) hologram);
            }
        }
        
        if (!scoreboardHolograms.isEmpty()) {
            scoreboardConfig.save(scoreboardHolograms);
            lastScoreboardSave = currentTime;
        }
    }
    
    /**
     * Save scoreboard holograms to separate config file (synchronous - for shutdown)
     */
    public static void saveScoreboardHologramsSync() {
        List<ScoreboardHologram> scoreboardHolograms = Lists.newArrayList();
        
        for (ForgeHologram hologram : HOLOGRAMS.values()) {
            if (hologram instanceof ScoreboardHologram) {
                scoreboardHolograms.add((ScoreboardHologram) hologram);
            }
        }
        
        if (!scoreboardHolograms.isEmpty()) {
            scoreboardConfig.save(scoreboardHolograms);
            LOGGER.info("Saved {} scoreboard holograms during shutdown", scoreboardHolograms.size());
        }
    }
    
    /**
     * Load scoreboard holograms from separate config file
     */
    private static void loadScoreboardHolograms() {
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
                    data.headerFormat,
                    data.playerFormat,
                    data.emptyFormat
                );
                
                // Force initial update
                hologram.forceUpdate();
                
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
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return null;
        }
        
        for (Level world : ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
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
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return;
        }

        for (ForgeHologram hologram : HOLOGRAMS.values()) {
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
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return;
        }
        
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (player == null || player.level() == null) {
                continue; // Skip if player or their level is null
            }
            
            for (ForgeHologram hologram : HOLOGRAMS.values()) {
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
                        
                        for (HologramLine line : hologram.getLines()) {
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
                        
                        for (HologramLine line : hologram.getLines()) {
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
                    
                    for (HologramLine line : hologram.getLines()) {
                        if (line != null) { // Check if line is not null
                            UtilConcurrency.runSync(() -> {
                                line.spawnForPlayer(player);
                            });
                        }
                    }

                    hologram.getNearbyPlayers().add(player.getUUID());
                } else {
                     // This else block was refreshing every tick, which is wasteful and might be causing flicker or issues
                     // Forge19 does NOT update every tick unless it's an animated line.
                     // We should only update if it's animated or if we need to force an update.
                     
                    for (HologramLine line : hologram.getLines()) {
                        if (line != null && line instanceof AnimatedHologramLine) {
                             // Only update animated lines here. 
                             // Normal lines don't need constant packet spam.
                             // The tick() method handles the actual animation logic.
                             // But wait, the tick() method updates for all nearby players.
                             // So we don't need to do ANYTHING here for already-nearby players
                             // unless we are handling general periodic refreshes.
                        }
                    }
                }
            }
        }
    }

    private static class PlayerEventListener {
        private long nextRun = 0L;
        
        public PlayerEventListener() {
            // Register this instance to the Forge event bus
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(this);
        }
        
        @SubscribeEvent
        public void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
            if (event == null || event.getEntity() == null) {
                return;
            }
            
            UUID playerUUID = event.getEntity().getUUID();
            if (playerUUID == null) {
                return;
            }
            
            for (ForgeHologram value : HologramManager.HOLOGRAMS.values()) {
                if (value != null && value.getNearbyPlayers() != null) {
                    value.getNearbyPlayers().remove(playerUUID);
                }
            }
        }
        
        @SubscribeEvent
        public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
            if (event == null || event.getEntity() == null || !(event.getEntity() instanceof ServerPlayer)) {
                return;
            }
            
            // When a player logs in, refresh all holograms to make them check visibility
            for (ForgeHologram hologram : HologramManager.HOLOGRAMS.values()) {
                if (hologram != null) {
                    hologram.refreshVisibility();
                }
            }
            
            LOGGER.debug("Refreshed hologram visibility for player: {}", event.getEntity().getName().getString());
        }
    }
}
