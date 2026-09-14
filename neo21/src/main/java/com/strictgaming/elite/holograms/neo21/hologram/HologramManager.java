package com.strictgaming.elite.holograms.neo21.hologram;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import com.strictgaming.elite.holograms.neo21.hologram.entity.HologramInteraction;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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
    /** When true, async save() is a no-op so load-time mutations cannot queue stale writes. */
    private static volatile boolean loading = false;
    
    public static void preInit() {
        LOGGER.info("Pre-initializing hologram manager");
        
        if (!initialized) {
            try {
                File configDir = new File("config/eliteholograms");
                if (!configDir.exists()) {
                    configDir.mkdirs();
                }
                scoreboardConfig = new ScoreboardHologramConfig(configDir);
                com.strictgaming.elite.holograms.neo21.config.ScoreboardThemeManager.init(configDir);
                initialized = true;
                LOGGER.info("Scoreboard hologram config initialized");
            } catch (Exception e) {
                LOGGER.error("Failed to initialize scoreboard hologram config", e);
            }
        }
    }
    
    public static void load() throws IOException {
        synchronized (SAVE_LOAD_LOCK) {
            loading = true;
            try {
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
            } finally {
                loading = false;
            }
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
        if (loading) {
            return;
        }
        // Ensure save runs async to avoid blocking server thread
        CompletableFuture.runAsync(() -> {
            synchronized (SAVE_LOAD_LOCK) {
                if (loading) {
                    return;
                }
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
            // Refresh themes from disk first so reload picks up edits without a reboot.
            com.strictgaming.elite.holograms.neo21.config.ScoreboardThemeManager.reload();
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
                            data.theme,
                            data.headerFormat,
                            data.playerFormat,
                            data.emptyFormat,
                            HologramDisplayType.fromStringOrDefault(data.displayType, HologramDisplayType.FACING),
                            data.yaw,
                            data.pitch
                    );
                    // Restore the background before spawn/forceUpdate, both of which rebuild the
                    // line entities and read it on construction.
                    holo.restoreBackgroundState(
                            com.strictgaming.elite.holograms.neo21.config.HologramsConfig.parseColour(data.backgroundColour,
                                    com.strictgaming.elite.holograms.neo21.hologram.implementation.NeoForgeHologram.DEFAULT_BACKGROUND_COLOUR),
                            data.backgroundOpacity == null
                                    ? com.strictgaming.elite.holograms.neo21.hologram.implementation.NeoForgeHologram.DEFAULT_BACKGROUND_OPACITY
                                    : data.backgroundOpacity);

                    holo.restorePresentationState(
                            data.scale == null
                                    ? com.strictgaming.elite.holograms.neo21.hologram.entity.HologramTextDisplay.DEFAULT_SCALE
                                    : data.scale,
                            data.brightness == null
                                    ? com.strictgaming.elite.holograms.neo21.hologram.entity.HologramTextDisplay.BRIGHTNESS_AUTO
                                    : data.brightness,
                            com.strictgaming.elite.holograms.neo21.config.HologramsConfig.parseBillboard(data.billboard));

                    holo.restoreClickState(data.clickCommand, data.clickAsConsole);

                    // Restore persisted backlight state before spawn so spawn() re-applies the light column
                    if (data.backlightEnabled) {
                        holo.restoreBacklightState(true, data.backlightLevel);
                    }
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

    // Maps a clickable hitbox to the hologram that owns it. Interactions are real
    // entities, so a click arrives as a normal interaction event carrying the entity -
    // this is how that entity is turned back into a hologram without scanning them all.
    private static final Map<UUID, String> INTERACTION_OWNERS = new ConcurrentHashMap<>();

    /** Stops a single player triggering a command every tick by holding right-click. */
    private static final long CLICK_COOLDOWN_MS = 500L;
    private static final Map<UUID, Long> LAST_CLICK = new ConcurrentHashMap<>();

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
     * <p>Called from the interaction event. Does nothing when the entity is not one of
     * ours, so unrelated interactions are untouched.
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

        if (!(hologram instanceof NeoForgeHologram nf) || nf.getClickCommand() == null) {
            return true;
        }

        String command = nf.getClickCommand().replace("%player%", player.getName().getString());

        try {
            CommandSourceStack source = player.createCommandSourceStack();

            if (nf.isClickAsConsole()) {
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
     * Removes clickable hitboxes left behind by a server that did not run its shutdown
     * path.
     *
     * <p>These are real entities and are saved with the chunk, so a crash would otherwise
     * leave a duplicate behind every time the server came back up. Only entities carrying
     * our own tag are touched.
     */
    public static void sweepOrphanedInteractions() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();

        if (server == null) {
            return;
        }

        int removed = 0;

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof Interaction
                        && entity.getTags().contains(HologramInteraction.HOLOGRAM_TAG)) {
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
