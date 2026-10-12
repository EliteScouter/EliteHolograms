package com.strictgaming.elite.holograms.neo26.config;
import net.minecraft.world.entity.Display;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo26.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo26.hologram.ItemHologram;
import com.strictgaming.elite.holograms.neo26.hologram.implementation.NeoForgeHologram;
import com.strictgaming.elite.holograms.neo26.hologram.ScoreboardHologram;
import com.strictgaming.elite.holograms.neo26.util.UtilFiles;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Configuration for holograms
 */
public class HologramsConfig {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private Path configFile;
    private Path configDir;

    /**
     * Entries in the file that could not be turned into a hologram, kept exactly as they were
     * read and written back on every save.
     *
     * <p>Before this, one entry that failed to load stopped the loop, so every hologram after
     * it in the file was skipped too, and the next save, written from memory, erased them all.
     */
    private final Map<String, JsonElement> unresolved = new LinkedHashMap<>();

    /** False until the file has been read, so nothing can be written over data not yet seen. */
    private boolean loaded = false;

    /** Whether the last load found a readable file, as opposed to none or a damaged one. */
    private boolean foundData = false;

    /** The file's modification time as of our last read or write, to spot hand edits. */
    private FileTime lastKnownModified;

    public HologramsConfig() throws IOException {
        setupDirectories();
    }

    private void setupDirectories() throws IOException {
        Path serverDirectory = ServerLifecycleHooks.getCurrentServer().getServerDirectory();
        configDir = serverDirectory.resolve("config").resolve("eliteholograms");

        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }

        // Deliberately not created here: an empty placeholder written now would shadow the
        // backup that loading falls back to if the real file has gone missing.
        configFile = configDir.resolve("holograms.json");
    }

    public void load() throws IOException {
        loadHologramsIntoManager();
    }

    public synchronized void loadHologramsIntoManager() {
        LOGGER.info("Loading holograms into manager from " + configFile.toAbsolutePath());
        this.unresolved.clear();
        this.foundData = false;

        try {
            JsonElement root = UtilFiles.readJson(configFile, "holograms");

            if (root == null) {
                LOGGER.info("No saved holograms found");
                return;
            }

            if (!root.isJsonObject()) {
                LOGGER.error("{} does not contain a map of holograms", configFile.getFileName());
                UtilFiles.quarantine(configFile, "holograms");
                return;
            }

            this.foundData = true;

            for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
                String id = entry.getKey();

                try {
                    HologramData data = GSON.fromJson(entry.getValue(), HologramData.class);

                    if (data == null) {
                        throw new IllegalStateException("the entry is empty");
                    }

                    loadHologram(id, data);
                } catch (Exception e) {
                    // Each hologram registers itself as it is constructed, so one that failed
                    // part way may already be in the manager with default settings. Saving that
                    // would overwrite the good copy on disk; take it out and keep the original.
                    HologramManager.discardPartial(id);
                    this.unresolved.put(id, entry.getValue());
                    LOGGER.error("Keeping hologram '{}' in {} without loading it: it could not be read",
                            id, configFile.getFileName(), e);
                }
            }
        } finally {
            this.loaded = true;
            this.lastKnownModified = UtilFiles.lastModified(configFile);
        }
    }

    /**
     * Builds one hologram from its saved form and registers it with the manager.
     */
    private void loadHologram(String id, HologramData data) {
        List<Object> linesContent = new ArrayList<>();
        if (data.lines != null) {
            for (JsonElement element : data.lines) {
                if (element.isJsonObject()) {
                    // Animated line
                    JsonObject obj = element.getAsJsonObject();
                    if (obj.has("type") && "animated".equals(obj.get("type").getAsString())) {
                        int interval = obj.has("interval") ? obj.get("interval").getAsInt() : 1;
                        JsonArray framesArray = obj.getAsJsonArray("frames");
                        List<String> frames = new ArrayList<>();
                        for (JsonElement frame : framesArray) {
                            frames.add(frame.getAsString());
                        }
                        linesContent.add(new NeoForgeHologram.AnimatedLineData(frames, interval));
                    } else {
                        linesContent.add(element.toString()); // Fallback
                    }
                } else {
                    linesContent.add(element.getAsString());
                }
            }
        }

        // Convert linesContent to String list for constructor, then set content
        List<String> stringLines = new ArrayList<>();
        for (Object obj : linesContent) {
             if (obj instanceof NeoForgeHologram.AnimatedLineData) {
                 List<String> f = ((NeoForgeHologram.AnimatedLineData) obj).frames;
                 stringLines.add(f.isEmpty() ? "" : f.get(0));
             } else {
                 stringLines.add(obj.toString());
             }
        }

        NeoForgeHologram hologram;
        if (data.item != null && !data.item.isEmpty()) {
            LOGGER.info("Loading hologram {} as ItemHologram with item: {}", id, data.item);
            hologram = new ItemHologram(
                id, data.world, data.x, data.y, data.z, data.item, stringLines
            );
        } else {
            LOGGER.info("Loading hologram {} as standard NeoForgeHologram (no item data found)", id);
            hologram = new NeoForgeHologram(
                id, data.world, data.x, data.y, data.z, stringLines
            );
        }

        // Restore the display type before the content, since setLinesContent rebuilds
        // the line entities and needs to know which kind to build.
        hologram.restoreDisplayState(
                com.strictgaming.elite.holograms.neo26.hologram.HologramDisplayType.fromStringOrDefault(
                        data.displayType,
                        com.strictgaming.elite.holograms.neo26.hologram.HologramDisplayType.FACING),
                data.yaw, data.pitch);

        hologram.restoreBackgroundState(
                parseColour(data.backgroundColour, com.strictgaming.elite.holograms.neo26.hologram.implementation.NeoForgeHologram.DEFAULT_BACKGROUND_COLOUR),
                data.backgroundOpacity == null
                        ? com.strictgaming.elite.holograms.neo26.hologram.implementation.NeoForgeHologram.DEFAULT_BACKGROUND_OPACITY
                        : data.backgroundOpacity);

        hologram.restorePresentationState(
                data.scale == null
                        ? com.strictgaming.elite.holograms.neo26.hologram.entity.HologramTextDisplay.DEFAULT_SCALE
                        : data.scale,
                data.brightness == null
                        ? com.strictgaming.elite.holograms.neo26.hologram.entity.HologramTextDisplay.BRIGHTNESS_AUTO
                        : data.brightness,
                parseBillboard(data.billboard));

        hologram.restoreClickState(data.clickCommand, data.clickAsConsole);

        // Restore complex content
        hologram.setLinesContent(linesContent);

        // Restore backlight state (does not save)
        hologram.restoreBacklightState(data.backlightEnabled,
                data.backlightLevel <= 0 ? com.strictgaming.elite.holograms.neo26.util.UtilBacklight.DEFAULT_LEVEL : data.backlightLevel);

        // Don't spawn yet. The HologramManager handles it
        LOGGER.debug("Loaded hologram into manager: " + id);
        HologramManager.addHologram(hologram);
    }

    public void save() throws IOException {
        saveHologramsFromManager(HologramManager.getHolograms());
    }

    public synchronized void saveHologramsFromManager(Map<String, Hologram> holograms) throws IOException {
        if (!this.loaded) {
            LOGGER.warn("Not saving holograms: {} has not been read yet", configFile.getFileName());
            return;
        }

        LOGGER.info("Saving {} holograms from manager to {}", holograms.size(), configFile.toAbsolutePath());

        JsonObject out = new JsonObject();
        Set<String> written = new HashSet<>();

        for (Hologram hologram : holograms.values()) {
            if (hologram instanceof ScoreboardHologram) {
                continue;
            }

            // The hologram's own ID rather than its registry key, so the case it was created
            // with is what gets stored.
            String id = hologram.getId();

            HologramData data = new HologramData();
            data.world = hologram.getWorld();
            data.x = hologram.getX();
            data.y = hologram.getY();
            data.z = hologram.getZ();

            if (hologram instanceof ItemHologram) {
                data.item = ((ItemHologram) hologram).getItemId();
                LOGGER.debug("Saving hologram {} as ItemHologram with item: {}", id, data.item);
            } else {
                LOGGER.debug("Saving hologram {} as standard NeoForgeHologram", id);
            }

            if (hologram instanceof NeoForgeHologram nf) {
                data.backlightEnabled = nf.isBacklightEnabled();
                data.backlightLevel = nf.getBacklightLevel();
                data.displayType = nf.getDisplayType().getSerializedName();
                data.yaw = nf.getYaw();
                data.pitch = nf.getPitch();
                data.backgroundColour = String.format("#%06X", nf.getBackgroundColour());
                data.backgroundOpacity = nf.getBackgroundOpacity();
                data.scale = nf.getScale();
                data.brightness = nf.getBrightness();
                data.billboard = nf.getBillboard().getSerializedName();
                data.clickCommand = nf.getClickCommand();
                data.clickAsConsole = nf.isClickAsConsole();
            }

            data.lines = new ArrayList<>();
            if (hologram instanceof NeoForgeHologram) {
                List<Object> content = ((NeoForgeHologram) hologram).getLinesContent();
                for (Object lineObj : content) {
                    if (lineObj instanceof NeoForgeHologram.AnimatedLineData) {
                        NeoForgeHologram.AnimatedLineData anim = (NeoForgeHologram.AnimatedLineData) lineObj;
                        JsonObject json = new JsonObject();
                        json.addProperty("type", "animated");
                        json.addProperty("interval", anim.interval);
                        JsonArray frames = new JsonArray();
                        for (String f : anim.frames) frames.add(f);
                        json.add("frames", frames);
                        data.lines.add(json);
                    } else {
                        // Use Gson to create JsonPrimitive (String)
                        data.lines.add(new com.google.gson.JsonPrimitive(lineObj.toString()));
                    }
                }
            } else {
                // Fallback for other implementations
                for (String line : hologram.getLines()) {
                    data.lines.add(new com.google.gson.JsonPrimitive(line));
                }
            }

            out.add(id, GSON.toJsonTree(data));
            written.add(id.toLowerCase(Locale.ROOT));
        }

        for (Map.Entry<String, JsonElement> kept : this.unresolved.entrySet()) {
            // A hologram created since, under the same ID, replaces the one we could not load.
            if (written.contains(kept.getKey().toLowerCase(Locale.ROOT))) {
                continue;
            }

            out.add(kept.getKey(), kept.getValue());
        }

        try {
            UtilFiles.writeAtomically(configFile, GSON.toJson(out));
            this.lastKnownModified = UtilFiles.lastModified(configFile);
            LOGGER.debug("Successfully saved holograms from manager.");
        } catch (IOException e) {
            LOGGER.error("Failed to save holograms from manager to config", e);
            throw e;
        }
    }

    /**
     * @return whether the last load found a readable file: false when there was no file, or
     *         only a damaged one, as opposed to a file that validly lists no holograms
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
        return !Objects.equals(UtilFiles.lastModified(configFile), this.lastKnownModified);
    }

    /**
     * Reads a billboard name from the config.
     *
     * @param value the stored name, may be null on older configs
     * @return the matching constraint, or fixed when missing or unrecognised
     */
    public static Display.BillboardConstraints parseBillboard(String value) {
        if (value != null) {
            for (Display.BillboardConstraints candidate : Display.BillboardConstraints.values()) {
                if (candidate.getSerializedName().equalsIgnoreCase(value.trim())) {
                    return candidate;
                }
            }
        }

        return Display.BillboardConstraints.FIXED;
    }

    /**
     * Reads a "#RRGGBB" colour from the config.
     *
     * @param value        the stored value, may be null on configs written before backgrounds existed
     * @param defaultValue the colour to use when the value is missing or malformed
     * @return the RGB colour
     */
    public static int parseColour(String value, int defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.replace("#", "").trim(), 16);
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid backgroundColour '{}' in config, using the default", value);
            return defaultValue;
        }
    }

    private static class HologramData {
        String world;
        double x, y, z;
        List<JsonElement> lines;
        String item;
        boolean backlightEnabled = false;
        int backlightLevel = com.strictgaming.elite.holograms.neo26.util.UtilBacklight.DEFAULT_LEVEL;
        /**
         * Absent for holograms saved before fixed holograms existed, which is why it falls back
         * to facing on load.
         */
        String displayType;
        float yaw = 0.0F;
        float pitch = 0.0F;
        /**
         * Background behind a fixed hologram's text, as "#RRGGBB" plus an opacity
         * percentage. Absent for holograms saved before backgrounds were configurable,
         * which is why they fall back to vanilla's 25% black.
         */
        String backgroundColour;
        Integer backgroundOpacity;
        /**
         * Presentation and click settings. All absent for holograms saved before they
         * existed, which read back as the look and behaviour those holograms already had.
         */
        Float scale;
        Integer brightness;
        String billboard;
        String clickCommand;
        boolean clickAsConsole = false;
    }
}
