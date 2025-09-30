package com.strictgaming.elite.holograms.neo21.hologram;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.Neo21Holograms;
import com.strictgaming.elite.holograms.neo21.config.HologramsConfig;
import com.strictgaming.elite.holograms.neo21.config.ScoreboardHologramConfig;
import com.strictgaming.elite.holograms.neo21.hologram.implementation.NeoForgeHologram;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import net.minecraft.server.level.ServerPlayer;

/**
 * Manager for handling hologram operations
 */
public class HologramManager {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Hologram> HOLOGRAMS = new HashMap<>();
    private static ScoreboardHologramConfig scoreboardConfig;
    private static boolean initialized = false;
    private static long lastScoreboardSave = 0L;
    private static final long SCOREBOARD_SAVE_COOLDOWN_MS = 5000L;
    private static final Object SAVE_LOAD_LOCK = new Object();
    
    /**
     * Initialize the hologram system
     */
    public static void preInit() {
        LOGGER.info("Pre-initializing hologram manager");
        
        // Initialize scoreboard hologram config
        if (!initialized) {
            try {
                File configDir = new File("config/eliteholograms");
                if (!configDir.exists()) {
                    configDir.mkdirs();
                }
                scoreboardConfig = new ScoreboardHologramConfig(configDir);
                initialized = true;
                LOGGER.info("Scoreboard hologram config initialized");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize scoreboard hologram config", e);
            }
        }
    }
    
    /**
     * Load holograms from config
     *
     * @throws IOException If loading fails
     */
    public static void load() throws IOException {
        synchronized (SAVE_LOAD_LOCK) {
            LOGGER.info("Loading holograms from config");
            HologramsConfig config = Neo21Holograms.getInstance().getConfig();

            if (config == null) {
                LOGGER.warn("Config is null, cannot load holograms.");
                return;
            }

            // Despawn all existing before clearing
            HOLOGRAMS.values().forEach(Hologram::despawn);
            HOLOGRAMS.clear();

            // Let config directly add to manager
            config.loadHologramsIntoManager();

            // Load scoreboard holograms separately
            loadScoreboardHolograms();
        }
    }
    
    /**
     * Add a hologram to the manager
     *
     * @param hologram The hologram to add
     */
    public static void addHologram(Hologram hologram) {
        if (hologram == null) return;
        synchronized (SAVE_LOAD_LOCK) {
            HOLOGRAMS.put(hologram.getId(), hologram);
        }
        // No save here, let config do it or explicit calls
    }
    
    /**
     * Get a hologram by ID
     * 
     * @param id The hologram ID
     * @return The hologram, if found
     */
    public static Optional<Hologram> getHologram(String id) {
        return Optional.ofNullable(HOLOGRAMS.get(id));
    }
    
    /**
     * Remove a hologram from the manager
     *
     * @param id The hologram ID
     * @return True if removed, false if not found
     */
    public static boolean removeHologram(String id) {
        synchronized (SAVE_LOAD_LOCK) {
            Hologram hologram = HOLOGRAMS.remove(id);
            if (hologram != null) {
                // Ensure it's fully despawned / marked inactive
                hologram.despawn();
                try {
                    save();
                } catch (IOException e) {
                    LOGGER.error("Failed to save after removing hologram", e);
                }
                return true;
            }
            return false;
        }
    }
    
    /**
     * Get all holograms
     *
     * @return The map of holograms (returns a copy for external use)
     */
    public static Map<String, Hologram> getHolograms() {
        return new HashMap<>(HOLOGRAMS);
    }

    /**
     * Get the internal holograms map reference (for internal operations that need to modify the map)
     *
     * @return The internal holograms map
     */
    public static Map<String, Hologram> getHologramsInternal() {
        return HOLOGRAMS;
    }
    
    /**
     * Save holograms to config
     *
     * @throws IOException If saving fails
     */
    public static void save() throws IOException {
        synchronized (SAVE_LOAD_LOCK) {
            HologramsConfig config = Neo21Holograms.getInstance().getConfig();
            if (config != null) {
                config.saveHologramsFromManager(HOLOGRAMS);
                LOGGER.info("Saved {} holograms to config.", HOLOGRAMS.size());

                // Save scoreboard holograms separately
                saveScoreboardHolograms();
            } else {
                LOGGER.warn("Config is null, cannot save holograms.");
            }
        }
    }
    
    /**
     * Helper method to save holograms and handle exceptions
     */
    private static void saveHolograms() {
        try {
            save();
        } catch (IOException e) {
            LOGGER.error("Failed to save holograms", e);
        }
    }
    
    // Player event handling
    public static void handlePlayerJoin(ServerPlayer player) {
        if (player == null) return;
        LOGGER.debug("Player {} joined, checking nearby holograms.", player.getName().getString());
        HOLOGRAMS.values().forEach(hologram -> {
            if (hologram instanceof NeoForgeHologram nfHologram && nfHologram.isSpawned() && nfHologram.isPlayerNearby(player)) {
                nfHologram.spawnForPlayer(player);
            }
        });
    }
    
    public static void handlePlayerLeave(ServerPlayer player) {
        if (player == null) return;
        LOGGER.debug("Player {} left, despawning their holograms.", player.getName().getString());
        HOLOGRAMS.values().forEach(hologram -> {
            if (hologram instanceof NeoForgeHologram nfHologram && nfHologram.isVisibleTo(player)) {
                nfHologram.despawnForPlayer(player);
            }
        });
    }
    
    public static void handlePlayerMove(ServerPlayer player) {
        if (player == null) return;
        HOLOGRAMS.values().forEach(hologram -> {
            if (hologram instanceof NeoForgeHologram nfHologram && nfHologram.isSpawned()) {
                boolean isNearby = nfHologram.isPlayerNearby(player);
                boolean isVisible = nfHologram.isVisibleTo(player);
                
                if (isNearby && !isVisible) {
                    nfHologram.spawnForPlayer(player);
                } else if (!isNearby && isVisible) {
                    nfHologram.despawnForPlayer(player);
                } else if (isNearby && isVisible) {
                    // Player is nearby and hologram is visible, check if text needs updating for this player
                    // This is a good place for periodic updates if hologram lines can change dynamically 
                    // without a global /eh update command (e.g. placeholders that update frequently)
                    // For now, just ensure text is correct based on last global update.
                    nfHologram.updateTextForPlayer(player); 
                }
            }
            
            // Update scoreboard holograms
            // scoreboard ticking is handled globally each second
        });
    }
    
    /**
     * Save scoreboard holograms to separate config file (async)
     */
    private static void saveScoreboardHolograms() {
        synchronized (SAVE_LOAD_LOCK) {
            try {
                if (scoreboardConfig == null) return;
                long now = System.currentTimeMillis();
                if (now - lastScoreboardSave < SCOREBOARD_SAVE_COOLDOWN_MS) {
                    return;
                }
                List<com.strictgaming.elite.holograms.neo21.hologram.ScoreboardHologram> list = new ArrayList<>();
                for (Hologram h : HOLOGRAMS.values()) {
                    if (h instanceof com.strictgaming.elite.holograms.neo21.hologram.ScoreboardHologram sb) {
                        list.add(sb);
                    }
                }
                if (!list.isEmpty()) {
                    scoreboardConfig.save(list);
                    lastScoreboardSave = now;
                }
            } catch (Exception e) {
                LOGGER.error("Failed to save scoreboard holograms", e);
            }
        }
    }
    
    /**
     * Save scoreboard holograms to separate config file (synchronous - for shutdown)
     */
    public static void saveScoreboardHologramsSync() {
        saveScoreboardHolograms();
    }
    
    /**
     * Load scoreboard holograms from separate config file
     */
    private static void loadScoreboardHolograms() {
        // Load scoreboard holograms from dedicated config if present
        try {
            if (scoreboardConfig == null) return;
            var list = scoreboardConfig.load();
            for (var data : list) {
                if (data == null || data.id == null || data.id.isEmpty()) continue;
                if (getHologram(data.id).isPresent()) continue;
                var holo = new com.strictgaming.elite.holograms.neo21.hologram.ScoreboardHologram(
                        data.id,
                        data.worldName,
                        data.x, data.y, data.z,
                        data.range,
                        data.objectiveName,
                        data.topCount,
                        data.updateInterval,
                        data.headerFormat,
                        data.playerFormat,
                        data.emptyFormat
                );
                holo.spawn();
                holo.forceUpdate();
                addHologram(holo);
                LOGGER.info("Recreated scoreboard hologram '{}' for objective '{}'", data.id, data.objectiveName);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load scoreboard holograms", e);
        }
    }

    /**
     * Tick all scoreboard holograms once. Intended to be called once per second.
     */
    public static void tickScoreboards() {
        try {
            for (Hologram h : HOLOGRAMS.values()) {
                if (h instanceof com.strictgaming.elite.holograms.neo21.hologram.ScoreboardHologram sb) {
                    sb.tick();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error ticking scoreboard holograms", e);
        }
    }
    
    /**
     * Find a world by name
     */
    private static Level findWorldByName(String worldName) {
        try {
            net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) {
                return null;
            }
            
            for (Level world : server.getAllLevels()) {
                if (world.dimension().location().toString().equals(worldName)) {
                    return world;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error finding world by name: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get hologram by ID (compatibility method)
     */
    public static Hologram getById(String id) {
        return HOLOGRAMS.get(id);
    }
    
    /**
     * Get all holograms as list (compatibility method)
     */
    public static List<Hologram> getAllHolograms() {
        return new ArrayList<>(HOLOGRAMS.values());
    }
} 