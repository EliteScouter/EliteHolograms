package com.strictgaming.elite.holograms.forge.hologram.database;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.manager.database.HologramSaver;
import com.strictgaming.elite.holograms.forge.hologram.ForgeHologram;
import com.strictgaming.elite.holograms.forge.hologram.ForgeHologramTypeAdapter;
import com.strictgaming.elite.holograms.forge.hologram.ScoreboardHologram;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            
    private static final Type FORGE_HOLOGRAM_LIST_TYPE = new TypeToken<ArrayList<ForgeHologram>>(){}.getType();

    private File file;

    public JsonHologramSaver(String file) {
        this.file = Paths.get(file).toFile();

        try {
            if (!this.file.exists()) {
                if (!this.file.getParentFile().exists()) {
                    this.file.getParentFile().mkdirs();
                }

                this.file.createNewFile();
                
                // Initialize with empty array
                FileWriter writer = new FileWriter(this.file);
                writer.write("[]");
                writer.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Hologram> load() {
        Map<String, Hologram> holograms = Maps.newHashMap();

        try {
            if (this.file.length() == 0) {
                LOGGER.info("Hologram file is empty, writing empty array");
                try (FileWriter writer = new FileWriter(this.file)) {
                    writer.write("[]");
                }
                return holograms;
            }
            
            // Load from file
            InputStreamReader jsonReader = new InputStreamReader(new FileInputStream(this.file), StandardCharsets.UTF_8);
            List<ForgeHologram> forgeHolograms = GSON.fromJson(jsonReader, FORGE_HOLOGRAM_LIST_TYPE);
            jsonReader.close();

            if (forgeHolograms == null) {
                LOGGER.info("No holograms found in file (null list)");
                return holograms;
            }

            LOGGER.info("Successfully deserialized {} holograms from file", forgeHolograms.size());
            for (ForgeHologram hologram : forgeHolograms) {
                if (hologram == null) {
                    LOGGER.info("Skipping null hologram");
                    continue;
                }
                
                if (holograms.containsKey(hologram.getId().toLowerCase())) {
                    LOGGER.info("Duplicate hologram ID: {}", hologram.getId());
                    continue;
                }
                
                LOGGER.info("Loading hologram: {}", hologram.getId());
                // Return only — HologramManager.load() owns map mutation after clear
                holograms.put(hologram.getId().toLowerCase(), hologram);
            }
        } catch (Exception e) {
            LOGGER.error("Error loading holograms: {}", e.getMessage());
            e.printStackTrace();
        }

        return holograms;
    }

    @Override
    public void save(List<Hologram> holograms) {
        if (holograms == null) {
            LOGGER.warn("Refusing to save null hologram list");
            return;
        }

        LOGGER.info("Saving {} holograms to file {}", holograms.size(), this.file.getAbsolutePath());

        try {
            List<ForgeHologram> savedHolograms = Lists.newArrayList();

            for (Hologram hologram : holograms) {
                if (!(hologram instanceof ForgeHologram)) {
                    continue;
                }

                // Skip ScoreboardHologram instances - they cannot be serialized safely
                if (hologram instanceof ScoreboardHologram) {
                    LOGGER.debug("Skipping ScoreboardHologram '{}' from save (not serializable)", hologram.getId());
                    continue;
                }

                savedHolograms.add((ForgeHologram) hologram);
            }

            // Write to a temp file then replace, so a crash mid-write cannot leave []
            File tempFile = new File(this.file.getAbsolutePath() + ".tmp");
            try (OutputStreamWriter jsonWriter = new OutputStreamWriter(
                    new FileOutputStream(tempFile), StandardCharsets.UTF_8)) {
                GSON.toJson(savedHolograms, FORGE_HOLOGRAM_LIST_TYPE, jsonWriter);
                jsonWriter.flush();
            }

            if (this.file.exists() && !this.file.delete()) {
                LOGGER.warn("Could not delete old hologram file before replace; writing in place");
                try (OutputStreamWriter jsonWriter = new OutputStreamWriter(
                        new FileOutputStream(this.file), StandardCharsets.UTF_8)) {
                    GSON.toJson(savedHolograms, FORGE_HOLOGRAM_LIST_TYPE, jsonWriter);
                    jsonWriter.flush();
                }
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            } else if (!tempFile.renameTo(this.file)) {
                Files.copy(tempFile.toPath(), this.file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                //noinspection ResultOfMethodCallIgnored
                tempFile.delete();
            }

            LOGGER.info("Saved {} holograms to file", savedHolograms.size());
        } catch (IOException e) {
            LOGGER.error("Error saving holograms: {}", e.getMessage());
            e.printStackTrace();
        }
    }
} 
