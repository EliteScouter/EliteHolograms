package com.strictgaming.elite.holograms.forge.hologram;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.manager.database.HologramSaver;
import com.strictgaming.elite.holograms.forge.ForgeHolograms;
import com.strictgaming.elite.holograms.forge.config.ScoreboardHologramConfig;
import com.strictgaming.elite.holograms.forge.hologram.database.JsonHologramSaver;
import com.strictgaming.elite.holograms.forge.hologram.entity.HologramLine;
import com.strictgaming.elite.holograms.forge.util.UtilConcurrency;
import com.strictgaming.elite.holograms.forge.util.UtilPlayer;
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

    private HologramManager() {
        // Private constructor for singleton
    }

    public static void preInit() {
        managerThread = new Thread(new HologramManager());
        managerThread.start();

        new PlayerEventListener();
        saver = (HologramSaver) new JsonHologramSaver(ForgeHolograms.getInstance().getConfig().getStorageLocation());
        scoreboardConfig = new ScoreboardHologramConfig(new File(ForgeHolograms.getInstance().getConfig().getStorageLocation()));
    }

    public static void clear() {
        for (ForgeHologram value : HOLOGRAMS.values()) {
            value.despawn();
        }
        HOLOGRAMS.clear();
    }

    public static void load() throws IOException {
        LOGGER.info("Loading holograms from storage...");
        
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
        
        // Add any missing holograms to the HOLOGRAMS map
        for (Map.Entry<String, Hologram> entry : loadedHolograms.entrySet()) {
            if (entry.getValue() instanceof ForgeHologram && 
                !HOLOGRAMS.containsKey(entry.getKey().toLowerCase())) {
                
                HOLOGRAMS.put(entry.getKey().toLowerCase(), (ForgeHologram) entry.getValue());
                LOGGER.info("Added hologram from storage: " + entry.getKey());
            }
        }
        
        LOGGER.info("Successfully loaded " + HOLOGRAMS.size() + " holograms");
        
        // Load scoreboard holograms separately
        loadScoreboardHolograms();
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
    
    private void checkHolograms() {
        if (ServerLifecycleHooks.getCurrentServer() == null) {
            return;
        }
        
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            if (player == null || player.level == null) {
                continue; // Skip if player or their level is null
            }
            
            for (ForgeHologram hologram : HOLOGRAMS.values()) {
                if (hologram == null || hologram.getWorld() == null) {
                    continue; // Skip if hologram or its world is null
                }
                
                // Update scoreboard holograms
                if (hologram instanceof ScoreboardHologram) {
                    ((ScoreboardHologram) hologram).tick();
                }
                
                if (!hologram.getWorld().equals(player.level)) {
                    if (hologram.getNearbyPlayers().contains(player.getUUID())) {
                        hologram.getNearbyPlayers().remove(player.getUUID());

                        for (HologramLine line : hologram.getLines()) {
                            if (line != null) { // Check if line is not null
                                UtilConcurrency.runSync(() -> line.despawnForPlayer(player));
                            }
                        }
                    }

                    continue;
                }

                if (player.distanceToSqr(hologram.getPosition()) > (Math.pow(hologram.getRange(), 2))) {
                    if (hologram.getNearbyPlayers().contains(player.getUUID())) {
                        hologram.getNearbyPlayers().remove(player.getUUID());

                        for (HologramLine line : hologram.getLines()) {
                            if (line != null) { // Check if line is not null
                                UtilConcurrency.runSync(() -> line.despawnForPlayer(player));
                            }
                        }
                    }

                    continue;
                }

                if (!hologram.getNearbyPlayers().contains(player.getUUID())) {
                    for (HologramLine line : hologram.getLines()) {
                        if (line != null) { // Check if line is not null
                            UtilConcurrency.runSync(() -> line.spawnForPlayer(player));
                        }
                    }

                    hologram.getNearbyPlayers().add(player.getUUID());
                } else {
                    for (HologramLine line : hologram.getLines()) {
                        if (line != null) { // Check if line is not null
                            UtilConcurrency.runSync(() -> line.updateForPlayer(player));
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
            
            LOGGER.info("Refreshed hologram visibility for player: {}", event.getEntity().getName().getString());
        }
    }
} 
