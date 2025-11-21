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
import java.util.concurrent.CompletableFuture;

import net.minecraft.world.level.Level;
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
    
    public static void preInit() {
        LOGGER.info("Pre-initializing hologram manager");
        
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
    
    public static void load() throws IOException {
        synchronized (SAVE_LOAD_LOCK) {
            LOGGER.info("Loading holograms from config");
            HologramsConfig config = Neo21Holograms.getInstance().getConfig();

            if (config == null) {
                LOGGER.warn("Config is null, cannot load holograms.");
                return;
            }

            HOLOGRAMS.values().forEach(Hologram::despawn);
            HOLOGRAMS.clear();

            config.loadHologramsIntoManager();

            // Spawn all loaded holograms now that they're registered
            LOGGER.info("Spawning {} loaded holograms", HOLOGRAMS.size());
            for (Hologram h : HOLOGRAMS.values()) {
                if (!h.isSpawned()) {
                    h.spawn();
                }
            }

            loadScoreboardHolograms();
        }
    }
    
    public static void addHologram(Hologram hologram) {
        if (hologram == null) return;
        synchronized (SAVE_LOAD_LOCK) {
            HOLOGRAMS.put(hologram.getId(), hologram);
        }
    }
    
    public static Optional<Hologram> getHologram(String id) {
        return Optional.ofNullable(HOLOGRAMS.get(id));
    }
    
    public static boolean removeHologram(String id) {
        synchronized (SAVE_LOAD_LOCK) {
            Hologram hologram = HOLOGRAMS.remove(id);
            if (hologram != null) {
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
    
    public static Map<String, Hologram> getHolograms() {
        return new HashMap<>(HOLOGRAMS);
    }

    public static Map<String, Hologram> getHologramsInternal() {
        return HOLOGRAMS;
    }
    
    public static void saveSync() {
        synchronized (SAVE_LOAD_LOCK) {
            try {
                HologramsConfig config = Neo21Holograms.getInstance().getConfig();
                if (config != null) {
                    config.saveHologramsFromManager(HOLOGRAMS);
                    LOGGER.info("Saved {} holograms to config (sync).", HOLOGRAMS.size());
                    forceSaveScoreboardHolograms();
                }
            } catch (Exception e) {
                LOGGER.error("Failed to save holograms (sync)", e);
            }
        }
    }
    
    public static void save() throws IOException {
        // Ensure save runs async to avoid blocking server thread
        CompletableFuture.runAsync(() -> {
            synchronized (SAVE_LOAD_LOCK) {
                try {
                    HologramsConfig config = Neo21Holograms.getInstance().getConfig();
                    if (config != null) {
                        config.saveHologramsFromManager(HOLOGRAMS);
                        LOGGER.info("Saved {} holograms to config.", HOLOGRAMS.size());

                        forceSaveScoreboardHolograms();
                    } else {
                        LOGGER.warn("Config is null, cannot save holograms.");
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to save holograms", e);
                }
            }
        });
    }
    
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
                } 
                // No continuous update here
            }
        });
    }

    /**
     * Called every server tick to update animations and scoreboards
     */
    public static void tick() {
        try {
            for (Hologram h : HOLOGRAMS.values()) {
                if (h instanceof NeoForgeHologram) {
                    ((NeoForgeHologram) h).tick();
                }
                if (h instanceof com.strictgaming.elite.holograms.neo21.hologram.ScoreboardHologram sb) {
                    sb.tick();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error ticking holograms", e);
        }
    }
    
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
    
    public static void saveScoreboardHologramsSync() {
        saveScoreboardHolograms();
    }

    public static void forceSaveScoreboardHolograms() {
        synchronized (SAVE_LOAD_LOCK) {
            try {
                if (scoreboardConfig == null) return;
                List<com.strictgaming.elite.holograms.neo21.hologram.ScoreboardHologram> list = new ArrayList<>();
                for (Hologram h : HOLOGRAMS.values()) {
                    if (h instanceof com.strictgaming.elite.holograms.neo21.hologram.ScoreboardHologram sb) {
                        list.add(sb);
                    }
                }
                if (!list.isEmpty()) {
                    scoreboardConfig.save(list);
                    lastScoreboardSave = System.currentTimeMillis();
                    LOGGER.debug("Force saved {} scoreboard holograms", list.size());
                }
            } catch (Exception e) {
                LOGGER.error("Failed to force save scoreboard holograms", e);
            }
        }
    }
    
    private static void loadScoreboardHolograms() {
        try {
            if (scoreboardConfig == null) return;
            var list = scoreboardConfig.load();
            LOGGER.debug("Loading {} scoreboard hologram configurations from config", list.size());
            for (var data : list) {
                if (data == null || data.id == null || data.id.isEmpty()) {
                    continue;
                }
                if (getHologram(data.id).isPresent()) {
                    continue;
                }
                try {
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
                } catch (Exception e) {
                    LOGGER.error("Failed to recreate scoreboard hologram '{}': {}", data.id, e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load scoreboard holograms", e);
        }
    }

    /**
     * @deprecated Use tick() instead
     */
    @Deprecated
    public static void tickScoreboards() {
        tick();
    }
    
    public static Hologram getById(String id) {
        return HOLOGRAMS.get(id);
    }
    
    public static List<Hologram> getAllHolograms() {
        return new ArrayList<>(HOLOGRAMS.values());
    }
}