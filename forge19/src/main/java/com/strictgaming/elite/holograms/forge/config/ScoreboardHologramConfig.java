package com.strictgaming.elite.holograms.forge.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.strictgaming.elite.holograms.forge.hologram.ScoreboardHologram;
import com.strictgaming.elite.holograms.forge.util.UtilFiles;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration manager for scoreboard holograms
 * Saves only the metadata needed to recreate them, not the complex objects
 */
public class ScoreboardHologramConfig {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path configFile;

    /**
     * Boards in the file that could not be rebuilt, kept exactly as they were read and written
     * back on every save. See {@link #keepUnresolved}.
     */
    private final List<JsonElement> unresolved = new ArrayList<>();

    /** False until the file has been read, so nothing can be written over boards not yet seen. */
    private boolean loaded = false;

    /** The file's modification time as of our last read or write, to spot hand edits. */
    private FileTime lastKnownModified;

    public ScoreboardHologramConfig(File configDir) {
        this.configFile = new File(configDir, "scoreboard_holograms.json").toPath();
    }

    /**
     * Saves every scoreboard hologram, replacing the file's previous contents.
     *
     * <p>An empty list is written as an empty file on purpose: that is how deleting the last
     * board reaches the disk. Boards that could not be rebuilt at load are written back too.
     *
     * @param scoreboardHolograms every scoreboard hologram currently loaded
     */
    public synchronized void save(List<ScoreboardHologram> scoreboardHolograms) {
        if (!this.loaded) {
            LOGGER.warn("Not saving scoreboard holograms: {} has not been read yet", this.configFile.getFileName());
            return;
        }

        JsonArray out = new JsonArray();
        Set<String> written = new HashSet<>();

        for (ScoreboardHologram hologram : scoreboardHolograms) {
            ScoreboardHologramData data = new ScoreboardHologramData();
            data.id = hologram.getId();
            data.objectiveName = hologram.getObjectiveName();
            data.topCount = hologram.getTopCount();
            data.updateInterval = hologram.getUpdateInterval();
            data.range = hologram.getRange();
            data.worldName = hologram.getWorldName();

            double[] location = hologram.getLocation();
            data.x = location[0];
            data.y = location[1];
            data.z = location[2];

            // Preserve the theme name and the resolved format settings
            data.theme = hologram.getThemeName();
            data.headerFormat = hologram.getHeaderFormat();
            data.playerFormat = hologram.getPlayerFormat();
            data.emptyFormat = hologram.getEmptyFormat();

            // Preserve backlight state so it survives reload/restart
            data.backlightEnabled = hologram.isBacklightEnabled();
            data.backlightLevel = hologram.getBacklightLevel();

            out.add(GSON.toJsonTree(data));
            written.add(data.id.toLowerCase());
        }

        for (JsonElement kept : this.unresolved) {
            String id = idOf(kept);

            // A board created since, under the same ID, replaces the one we could not rebuild.
            if (id != null && written.contains(id.toLowerCase())) {
                continue;
            }

            out.add(kept);
        }

        try {
            UtilFiles.writeAtomically(this.configFile, GSON.toJson(out));
            this.lastKnownModified = UtilFiles.lastModified(this.configFile);
            LOGGER.debug("Saved {} scoreboard hologram configurations", written.size());
        } catch (IOException e) {
            LOGGER.error("Error saving scoreboard hologram config: {}", e.getMessage());
        }
    }

    /**
     * Reads every board in the file.
     *
     * <p>An entry that cannot be read is kept verbatim rather than dropped, and a damaged file
     * is preserved and recovered from its backup (see {@link UtilFiles#readJson}).
     *
     * @return the boards that could be read
     */
    public synchronized List<ScoreboardHologramData> load() {
        List<ScoreboardHologramData> configData = new ArrayList<>();
        this.unresolved.clear();

        try {
            JsonElement root = UtilFiles.readJson(this.configFile, "scoreboard holograms");

            if (root == null) {
                LOGGER.debug("No saved scoreboard holograms found");
                return configData;
            }

            if (!root.isJsonArray()) {
                LOGGER.error("{} does not contain a list of scoreboard holograms", this.configFile.getFileName());
                UtilFiles.quarantine(this.configFile, "scoreboard holograms");
                return configData;
            }

            for (JsonElement element : root.getAsJsonArray()) {
                ScoreboardHologramData data = null;

                try {
                    data = GSON.fromJson(element, ScoreboardHologramData.class);
                } catch (Exception e) {
                    LOGGER.warn("Could not read scoreboard hologram '{}': {}", idOf(element), e.getMessage());
                }

                if (data == null || data.id == null || data.id.isEmpty()) {
                    this.unresolved.add(element);
                    continue;
                }

                configData.add(data);
            }

            LOGGER.info("Loaded {} scoreboard hologram configurations", configData.size());
            return configData;
        } finally {
            this.loaded = true;
            this.lastKnownModified = UtilFiles.lastModified(this.configFile);
        }
    }

    /**
     * Keeps a board that was read but could not be rebuilt, so that saving does not delete it.
     *
     * <p>A board whose world is not loaded yet, or whose rebuild failed, is not in memory, and
     * the file is written from memory. Without this it would be erased from disk by the next
     * save and would not come back when its world did.
     *
     * @param data   the board as it was read
     * @param reason why it could not be rebuilt, for the log
     */
    public synchronized void keepUnresolved(ScoreboardHologramData data, String reason) {
        this.unresolved.add(GSON.toJsonTree(data));
        LOGGER.warn("Keeping scoreboard hologram '{}' in {} without loading it: {}",
                data.id, this.configFile.getFileName(), reason);
    }

    /**
     * Whether the file has been changed by something other than this mod since it was last read
     * or written (in practice, edited by hand).
     *
     * @return true if the file on disk is not the one we last saw
     */
    public synchronized boolean changedOnDisk() {
        return !Objects.equals(UtilFiles.lastModified(this.configFile), this.lastKnownModified);
    }

    /**
     * @return whether the file has been read at least once, and so may safely be written
     */
    public synchronized boolean isLoaded() {
        return this.loaded;
    }

    private static String idOf(JsonElement element) {
        if (element != null && element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();

            if (object.has("id") && object.get("id").isJsonPrimitive()) {
                return object.get("id").getAsString();
            }
        }

        return null;
    }

    /**
     * Data class for scoreboard hologram configuration
     */
    public static class ScoreboardHologramData {
        public String id;
        public String objectiveName;
        public int topCount;
        public int updateInterval;
        public int range;
        public String worldName;
        public double x;
        public double y;
        public double z;

        // Name of the theme this board was created with (null/empty = use stored raw formats).
        // When set, the theme is re-resolved on load so edits to scoreboard_themes.json apply.
        public String theme;

        // Optional custom formats (can be null for defaults)
        public String headerFormat;
        public String playerFormat;
        public String emptyFormat;

        // Backlight state (vertical light column) so it persists across reload/restart
        public boolean backlightEnabled;
        public int backlightLevel;
    }
}
