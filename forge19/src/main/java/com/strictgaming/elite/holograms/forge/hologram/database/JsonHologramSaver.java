package com.strictgaming.elite.holograms.forge.hologram.database;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.api.manager.database.HologramSaver;
import com.strictgaming.elite.holograms.forge.hologram.ForgeHologram;
import com.strictgaming.elite.holograms.forge.hologram.ForgeHologramTypeAdapter;
import com.strictgaming.elite.holograms.forge.hologram.HologramManager;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
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
            .registerTypeAdapter(ForgeHologram.class, new ForgeHologramTypeAdapter())
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
                // Add to HologramManager directly
                HologramManager.addHologram(hologram);
                // Cast to Hologram for the return map
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
        LOGGER.info("Saving {} holograms to file", holograms.size());
        
        try {
            OutputStreamWriter jsonWriter = new OutputStreamWriter(new FileOutputStream(this.file), StandardCharsets.UTF_8);
            List<ForgeHologram> savedHolograms = Lists.newArrayList();

            for (Hologram hologram : holograms) {
                if (!(hologram instanceof ForgeHologram)) {
                    continue;
                }

                savedHolograms.add((ForgeHologram) hologram);
            }

            LOGGER.info("Saving {} holograms to file", savedHolograms.size());
            GSON.toJson(savedHolograms, FORGE_HOLOGRAM_LIST_TYPE, jsonWriter);
            jsonWriter.flush();
            jsonWriter.close();
        } catch (IOException e) {
            LOGGER.error("Error saving holograms: {}", e.getMessage());
            e.printStackTrace();
        }
    }
} 
