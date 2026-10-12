package com.strictgaming.elite.holograms.neo262.hologram;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import com.strictgaming.elite.holograms.neo262.hologram.entity.HologramInteraction;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo262.Neo262Holograms;
import com.strictgaming.elite.holograms.neo262.config.HologramsConfig;
import com.strictgaming.elite.holograms.neo262.config.ScoreboardHologramConfig;
import com.strictgaming.elite.holograms.neo262.hologram.implementation.NeoForgeHologram;

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
    private static final Object SAVE_LOAD_LOCK = new Object();
    /**
     * True from the end of a load until shutdown, and saves are refused outside that window.
     * Before the load a save would overwrite holograms that have not been read yet; after
     * shutdown it would write a registry that is being torn down.
     */
    private static volatile boolean acceptingSaves = false;
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
                com.strictgaming.elite.holograms.neo262.config.ScoreboardThemeManager.init(configDir);
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
                HologramsConfig config = Neo262Holograms.getInstance().getConfig();

                if (config == null) {
                    LOGGER.warn("Config is null, cannot load holograms.");
                    return;
                }

                Map<String, Hologram> existing = new HashMap<>(HOLOGRAMS);
                HOLOGRAMS.values().forEach(Hologram::despawn);
                HOLOGRAMS.clear();

                config.loadHologramsIntoManager();

                if (HOLOGRAMS.isEmpty() && !config.foundData()) {
                    // The file is missing or unreadable, and a damaged one has already been
                    // copied aside. What was in memory is the best copy left, so keep it
                    // rather than replacing it with nothing. A file that validly lists no
                    // holograms is not this case: that is how the store is emptied on purpose.
                    int kept = 0;
                    for (Map.Entry<String, Hologram> entry : existing.entrySet()) {
                        if (!(entry.getValue() instanceof ScoreboardHologram)) {
                            HOLOGRAMS.put(entry.getKey(), entry.getValue());
                            kept++;
                        }
                    }
                    if (kept > 0) {
                        LOGGER.warn("holograms.json could not be read, keeping the {} holograms already loaded", kept);
                    }
                }

                // One at a time, so a single failure cannot stop the rest (or the scoreboard
                // holograms below) from loading.
                LOGGER.info("Spawning {} loaded holograms", HOLOGRAMS.size());
                for (Hologram h : HOLOGRAMS.values()) {
                    try {
                        if (!h.isSpawned()) {
                            h.spawn();
                        }
                    } catch (Exception e) {
                        LOGGER.error("Could not spawn hologram '{}'", h.getId(), e);
                    }
                }

                loadScoreboardHolograms();
            } finally {
                loading = false;
                acceptingSaves = true;
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
        return Optional.ofNullable(getById(id));
    }
    
    public static boolean removeHologram(String id) {
        synchronized (SAVE_LOAD_LOCK) {
            Hologram hologram = HOLOGRAMS.remove(keyOf(id));
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
            if (loading || !acceptingSaves) {
                LOGGER.debug("Skipping saveSync outside a loaded session");
                return;
            }
            try {
                HologramsConfig config = Neo262Holograms.getInstance().getConfig();
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
        if (loading || !acceptingSaves) {
            return;
        }
        // Ensure save runs async to avoid blocking server thread
        CompletableFuture.runAsync(() -> {
            synchronized (SAVE_LOAD_LOCK) {
                // Re-checked under the lock: shutdown may have closed the session while
                // this was queued.
                if (loading || !acceptingSaves) {
                    return;
                }
                try {
                    HologramsConfig config = Neo262Holograms.getInstance().getConfig();
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
        LAST_CLICK.remove(player.getUUID());
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
                if (h instanceof com.strictgaming.elite.holograms.neo262.hologram.ScoreboardHologram sb) {
                    sb.tick();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error ticking holograms", e);
        }
    }
    
    /**
     * Saves every scoreboard hologram.
     *
     * <p>Kept as its own entry point for callers that used the rate-limited version. The
     * five second cooldown that version had silently dropped a second edit to a board within
     * five seconds of the first, so it now always saves.
     */
    private static void saveScoreboardHolograms() {
        forceSaveScoreboardHolograms();
    }

    public static void saveScoreboardHologramsSync() {
        saveScoreboardHolograms();
    }

    /**
     * Saves every scoreboard hologram.
     *
     * <p>Writes even when no boards are left, which is how deleting the last one reaches the
     * disk. It used to skip an empty list, so the last board deleted came back on restart.
     * That is only safe because the file is never written before it has been read.
     */
    public static void forceSaveScoreboardHolograms() {
        synchronized (SAVE_LOAD_LOCK) {
            try {
                if (scoreboardConfig == null || !scoreboardConfig.isLoaded()) return;
                List<com.strictgaming.elite.holograms.neo262.hologram.ScoreboardHologram> list = new ArrayList<>();
                for (Hologram h : HOLOGRAMS.values()) {
                    if (h instanceof com.strictgaming.elite.holograms.neo262.hologram.ScoreboardHologram sb) {
                        list.add(sb);
                    }
                }
                scoreboardConfig.save(list);
                LOGGER.debug("Saved {} scoreboard holograms", list.size());
            } catch (Exception e) {
                LOGGER.error("Failed to force save scoreboard holograms", e);
            }
        }
    }

    /**
     * Ends the session. Call after the final shutdown save.
     *
     * <p>Saves are refused from here on, so an async save queued before shutdown cannot run
     * afterwards. The registry is emptied because a single-player game keeps this class
     * loaded between worlds, and the next world must not inherit holograms bound to this
     * one.
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
            HologramsConfig config = Neo262Holograms.getInstance().getConfig();
            return (config != null && config.changedOnDisk())
                    || (scoreboardConfig != null && scoreboardConfig.changedOnDisk());
        }
    }
    
    private static void loadScoreboardHolograms() {
        try {
            if (scoreboardConfig == null) return;
            // Refresh themes from disk first so reload picks up edits without a reboot.
            com.strictgaming.elite.holograms.neo262.config.ScoreboardThemeManager.reload();
            var list = scoreboardConfig.load();
            LOGGER.debug("Loading {} scoreboard hologram configurations from config", list.size());
            for (var data : list) {
                if (data == null || data.id == null || data.id.isEmpty()) {
                    continue;
                }
                Hologram clash = getById(data.id);
                if (clash != null) {
                    if (!(clash instanceof ScoreboardHologram)) {
                        scoreboardConfig.keepUnresolved(data,
                                "a regular hologram already uses the ID '" + data.id + "'");
                    }
                    continue;
                }
                try {
                    var holo = new com.strictgaming.elite.holograms.neo262.hologram.ScoreboardHologram(
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
                            com.strictgaming.elite.holograms.neo262.config.HologramsConfig.parseColour(data.backgroundColour,
                                    com.strictgaming.elite.holograms.neo262.hologram.implementation.NeoForgeHologram.DEFAULT_BACKGROUND_COLOUR),
                            data.backgroundOpacity == null
                                    ? com.strictgaming.elite.holograms.neo262.hologram.implementation.NeoForgeHologram.DEFAULT_BACKGROUND_OPACITY
                                    : data.backgroundOpacity);

                    holo.restorePresentationState(
                            data.scale == null
                                    ? com.strictgaming.elite.holograms.neo262.hologram.entity.HologramTextDisplay.DEFAULT_SCALE
                                    : data.scale,
                            data.brightness == null
                                    ? com.strictgaming.elite.holograms.neo262.hologram.entity.HologramTextDisplay.BRIGHTNESS_AUTO
                                    : data.brightness,
                            com.strictgaming.elite.holograms.neo262.config.HologramsConfig.parseBillboard(data.billboard));

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
                    LOGGER.error("Failed to recreate scoreboard hologram '{}'", data.id, e);

                    // The board registered itself on construction, so a failure part way
                    // leaves a half-built one behind, and saving that would overwrite the
                    // good copy on disk. Take it out, and keep the original instead of
                    // dropping it, which is what used to erase it on the next save.
                    if (HOLOGRAMS.get(data.id) instanceof ScoreboardHologram) {
                        discardPartial(data.id);
                    }
                    scoreboardConfig.keepUnresolved(data, "it could not be rebuilt (" + e.getMessage() + ")");
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
    
    /**
     * Looks a hologram up by ID, ignoring case, as the Forge and Fabric editions always have,
     * and as the documentation says. This edition used to match case exactly, so {@code /eh
     * delete Shop} missed a hologram created as {@code shop}, and both could be created side by
     * side. An exact match still wins, so two IDs saved by an older build that differ only in
     * case both stay reachable.
     *
     * <p>Not locked: the server thread is the only writer, and it is the caller here.
     *
     * @param id the ID to look up
     * @return the hologram, or null
     */
    public static Hologram getById(String id) {
        if (id == null) {
            return null;
        }
        Hologram exact = HOLOGRAMS.get(id);
        return exact != null ? exact : HOLOGRAMS.get(keyOf(id));
    }

    /**
     * The registry key for an ID: the exact key when there is one, otherwise the key that
     * matches ignoring case, otherwise the ID unchanged.
     */
    private static String keyOf(String id) {
        if (id == null || HOLOGRAMS.containsKey(id)) {
            return id;
        }
        for (String key : HOLOGRAMS.keySet()) {
            if (key.equalsIgnoreCase(id)) {
                return key;
            }
        }
        return id;
    }

    /**
     * Removes a hologram that failed part way through loading, without saving.
     *
     * <p>Holograms register themselves as they are constructed, so one that failed after
     * that point is in the registry with default settings, and saving it would overwrite
     * the good copy on disk. Exact ID only: a hologram that loaded fine under an ID that
     * differs only in case must not be touched.
     *
     * @param id the exact ID of the failed hologram
     */
    public static void discardPartial(String id) {
        synchronized (SAVE_LOAD_LOCK) {
            Hologram partial = HOLOGRAMS.remove(id);
            if (partial != null) {
                try {
                    partial.despawn();
                } catch (Exception ignored) {
                    // Best effort; it is being discarded either way.
                }
            }
        }
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
                || !entity.entityTags().contains(HologramInteraction.HOLOGRAM_TAG)) {
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
                source = source.withPermission(PermissionSet.ALL_PERMISSIONS);
            }

            source.getServer().getCommands().performPrefixedCommand(source, command);
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
                        && entity.entityTags().contains(HologramInteraction.HOLOGRAM_TAG)) {
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
