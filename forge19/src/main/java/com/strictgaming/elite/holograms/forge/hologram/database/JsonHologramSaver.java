package com.strictgaming.elite.holograms.forge.hologram.database;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.manager.database.HologramSaver;
import com.strictgaming.elite.holograms.forge.hologram.ForgeHologram;
import com.strictgaming.elite.holograms.forge.hologram.ForgeHologramTypeAdapter;
import com.strictgaming.elite.holograms.forge.hologram.ScoreboardHologram;
import com.strictgaming.elite.holograms.forge.util.UtilFiles;
import com.strictgaming.elite.holograms.forge.util.UtilWorld;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 *
 * Json implementation of the {@link HologramSaver} interface
 *
 */
public class JsonHologramSaver implements HologramSaver {

    private static final Logger LOGGER = LogManager.getLogger("EliteHolograms");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .registerTypeHierarchyAdapter(ForgeHologram.class, new ForgeHologramTypeAdapter())
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
            .create();

    private final Path file;

    /**
     * Entries in the file that could not be turned into a hologram, kept exactly as they were
     * read and written back on every save.
     *
     * <p>The usual cause is a hologram in a dimension that is not loaded, such as a modded dimension
     * whose mod was removed or has not registered yet. Such a hologram cannot be rebuilt, but it
     * must not be deleted either: dropping it here would erase it from disk on the very next
     * save, and it would not come back when the dimension did.
     */
    private final List<JsonElement> unresolved = new ArrayList<>();

    /** False until the file has been read, so nothing can be written over data not yet seen. */
    private boolean loaded = false;

    /** The file's modification time as of our last read or write, to spot hand edits. */
    private FileTime lastKnownModified;

    /** Whether the last load found a readable file, as opposed to none or a damaged one. */
    private boolean foundData = false;

    public JsonHologramSaver(String filePath) {
        // Make sure we use a file, not a directory
        if (!filePath.endsWith(".json")) {
            // If the path is a directory, append "holograms.json" to it
            if (filePath.endsWith("/") || filePath.endsWith("\\")) {
                filePath = filePath + "holograms.json";
            } else {
                filePath = filePath + "/holograms.json";
            }
        }

        LOGGER.info("Initializing hologram saver with file: {}", filePath);
        this.file = Paths.get(filePath);

        File parent = this.file.toAbsolutePath().getParent().toFile();

        if (!parent.exists() && !parent.mkdirs()) {
            LOGGER.error("Could not create the hologram storage directory {}", parent);
        }
    }

    @Override
    public synchronized Map<String, Hologram> load() {
        Map<String, Hologram> holograms = Maps.newHashMap();
        this.unresolved.clear();
        this.foundData = false;

        try {
            JsonElement root = UtilFiles.readJson(this.file, "holograms");

            if (root == null) {
                LOGGER.info("No saved holograms found");
                return holograms;
            }

            if (!root.isJsonArray()) {
                LOGGER.error("{} does not contain a list of holograms", this.file.getFileName());
                UtilFiles.quarantine(this.file, "holograms");
                return holograms;
            }

            this.foundData = true;

            for (JsonElement element : root.getAsJsonArray()) {
                ForgeHologram hologram = null;
                String failure = null;

                try {
                    hologram = GSON.fromJson(element, ForgeHologram.class);
                } catch (Exception e) {
                    failure = e.getMessage();
                }

                if (hologram == null || hologram.getId() == null) {
                    this.unresolved.add(element);
                    LOGGER.warn("Keeping hologram '{}' in {} without loading it: {}",
                            idOf(element), this.file.getFileName(), describeFailure(element, failure));
                    continue;
                }

                String key = hologram.getId().toLowerCase();

                if (holograms.containsKey(key)) {
                    LOGGER.warn("Ignoring a second hologram with the ID '{}'", hologram.getId());
                    continue;
                }

                // Return only. HologramManager.load() owns map mutation after clear
                holograms.put(key, hologram);
            }

            LOGGER.info("Loaded {} holograms{}", holograms.size(), this.unresolved.isEmpty()
                    ? "" : " (" + this.unresolved.size() + " kept on disk but not loaded)");
        } finally {
            this.loaded = true;
            this.lastKnownModified = UtilFiles.lastModified(this.file);
        }

        return holograms;
    }

    @Override
    public synchronized void save(List<Hologram> holograms) {
        if (holograms == null) {
            LOGGER.warn("Refusing to save null hologram list");
            return;
        }

        if (!this.loaded) {
            LOGGER.warn("Not saving holograms: {} has not been read yet", this.file.getFileName());
            return;
        }

        JsonArray out = new JsonArray();
        Set<String> written = new HashSet<>();

        for (Hologram hologram : holograms) {
            // Scoreboard holograms are persisted separately
            if (!(hologram instanceof ForgeHologram) || hologram instanceof ScoreboardHologram) {
                continue;
            }

            out.add(GSON.toJsonTree(hologram, ForgeHologram.class));
            written.add(hologram.getId().toLowerCase());
        }

        for (JsonElement kept : this.unresolved) {
            String id = idOf(kept);

            // A hologram created since, under the same ID, replaces the one we could not load.
            if (id != null && written.contains(id.toLowerCase())) {
                continue;
            }

            out.add(kept);
        }

        try {
            UtilFiles.writeAtomically(this.file, GSON.toJson(out));
            this.lastKnownModified = UtilFiles.lastModified(this.file);
            LOGGER.debug("Saved {} holograms to {}", written.size(), this.file.getFileName());
        } catch (IOException e) {
            LOGGER.error("Could not save holograms to {}", this.file.toAbsolutePath(), e);
        }
    }

    /**
     * Whether the last {@link #load()} found a readable file. False when there was no file, or
     * only a damaged one, as opposed to a file that validly lists no holograms.
     *
     * @return true if the file was read successfully
     */
    public synchronized boolean foundData() {
        return this.foundData;
    }

    /**
     * Whether the file has been changed by something other than this mod since it was last read
     * or written (in practice, edited by hand).
     *
     * @return true if the file on disk is not the one we last saw
     */
    public synchronized boolean changedOnDisk() {
        return !Objects.equals(UtilFiles.lastModified(this.file), this.lastKnownModified);
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
     * Explains, in terms a server owner can act on, why an entry was not loaded.
     */
    private static String describeFailure(JsonElement element, String failure) {
        if (element != null && element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();

            if (object.has("loc") && object.get("loc").isJsonObject()) {
                JsonObject loc = object.getAsJsonObject("loc");

                if (loc.has("world")) {
                    String world = loc.get("world").getAsString();

                    if (UtilWorld.findWorld(world) == null) {
                        return "its world '" + world + "' is not loaded. It will load again once that world is available";
                    }
                }
            }
        }

        return failure != null ? "it could not be read (" + failure + ")" : "it could not be read";
    }
}
