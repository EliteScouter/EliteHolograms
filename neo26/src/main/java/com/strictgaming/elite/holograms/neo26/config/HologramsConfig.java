package com.strictgaming.elite.holograms.neo26.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo26.Neo26Holograms;
import com.strictgaming.elite.holograms.neo26.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo26.hologram.ItemHologram;
import com.strictgaming.elite.holograms.neo26.hologram.implementation.NeoForgeHologram;
import com.strictgaming.elite.holograms.neo26.hologram.ScoreboardHologram;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    
    public HologramsConfig() throws IOException {
        setupDirectories();
    }
    
    private void setupDirectories() throws IOException {
        Path serverDirectory = ServerLifecycleHooks.getCurrentServer().getServerDirectory();
        configDir = serverDirectory.resolve("config").resolve("eliteholograms");
        
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }
        
        configFile = configDir.resolve("holograms.json");
        if (!Files.exists(configFile)) {
            Files.createFile(configFile);
            Files.writeString(configFile, "{}");
        }
    }
    
    public void load() throws IOException {
        loadHologramsIntoManager();
    }
    
    public void loadHologramsIntoManager() throws IOException {
        LOGGER.info("Loading holograms into manager from " + configFile.toAbsolutePath());
        
        try (Reader reader = Files.newBufferedReader(configFile)) {
            Type type = new TypeToken<Map<String, HologramData>>() {}.getType();
            Map<String, HologramData> hologramDataMap = GSON.fromJson(reader, type);
            
            if (hologramDataMap == null) {
                LOGGER.warn("No holograms found in config for manager loading.");
                return;
            }
            
            for (Map.Entry<String, HologramData> entry : hologramDataMap.entrySet()) {
                String id = entry.getKey();
                HologramData data = entry.getValue();
                
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

                // Restore complex content
                hologram.setLinesContent(linesContent);

                // Restore backlight state (does not save)
                hologram.restoreBacklightState(data.backlightEnabled,
                        data.backlightLevel <= 0 ? com.strictgaming.elite.holograms.neo26.util.UtilBacklight.DEFAULT_LEVEL : data.backlightLevel);

                // Don't spawn yet - let the HologramManager handle it
                LOGGER.debug("Loaded hologram into manager: " + id);
                HologramManager.addHologram(hologram);
            }
        } catch (Exception e) {
            LOGGER.error("Error loading holograms into manager from config", e);
            // Files.writeString(configFile, "{}"); // Don't auto-reset, might lose data if just a bad read
        }
    }
    
    public void save() throws IOException {
        saveHologramsFromManager(HologramManager.getHolograms());
    }
    
    public void saveHologramsFromManager(Map<String, Hologram> holograms) throws IOException {
        LOGGER.info("Saving {} holograms from manager to {}", holograms.size(), configFile.toAbsolutePath());
        
        Map<String, HologramData> hologramDataMap = new HashMap<>();
        
        for (Map.Entry<String, Hologram> entry : holograms.entrySet()) {
            String id = entry.getKey();
            Hologram hologram = entry.getValue();
            if (hologram instanceof ScoreboardHologram) {
                continue;
            }
            
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
            
            hologramDataMap.put(id, data);
        }
        
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(hologramDataMap, writer);
            LOGGER.debug("Successfully saved holograms from manager.");
        } catch (IOException e) {
            LOGGER.error("Failed to save holograms from manager to config", e);
            throw e;
        }
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
    }
}