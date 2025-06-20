package com.strictgaming.elite.holograms.neo21.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.Neo21Holograms;
import com.strictgaming.elite.holograms.neo21.hologram.HologramManager;
import com.strictgaming.elite.holograms.neo21.hologram.implementation.NeoForgeHologram;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
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
    
    /**
     * Creates a new config instance
     * 
     * @throws IOException If directories cannot be created
     */
    public HologramsConfig() throws IOException {
        setupDirectories();
    }
    
    /**
     * Sets up the configuration directories
     * 
     * @throws IOException If directories cannot be created
     */
    private void setupDirectories() throws IOException {
        Path serverDirectory = ServerLifecycleHooks.getCurrentServer().getServerDirectory();
        configDir = serverDirectory.resolve("holograms");
        
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }
        
        configFile = configDir.resolve("holograms.json");
        if (!Files.exists(configFile)) {
            Files.createFile(configFile);
            // Write default config
            Files.writeString(configFile, "{}");
        }
    }
    
    /**
     * Loads the configuration from disk
     * 
     * @throws IOException If loading fails
     */
    public void load() throws IOException {
        LOGGER.info("Loading configuration from " + configFile.toAbsolutePath());
        
        try (Reader reader = Files.newBufferedReader(configFile)) {
            Type type = new TypeToken<Map<String, HologramData>>() {}.getType();
            Map<String, HologramData> hologramDataMap = GSON.fromJson(reader, type);
            
            if (hologramDataMap == null) {
                LOGGER.warn("No holograms found in config");
                return;
            }
            
            for (Map.Entry<String, HologramData> entry : hologramDataMap.entrySet()) {
                String id = entry.getKey();
                HologramData data = entry.getValue();
                
                // Create the hologram from saved data
                Hologram hologram = Neo21Holograms.getInstance().getFactory().builder()
                        .id(id)
                        .world(data.world)
                        .position(data.x, data.y, data.z)
                        .lines(data.lines)
                        .build();
                
                // Spawn the hologram
                hologram.spawn();
                LOGGER.info("Loaded hologram: " + id);
            }
        } catch (Exception e) {
            LOGGER.error("Error loading holograms from config", e);
            // Create empty file if it's corrupted
            Files.writeString(configFile, "{}");
        }
    }
    
    /**
     * Loads holograms from config directly into the HologramManager.
     * This is called by HologramManager.load()
     * 
     * @throws IOException If loading fails
     */
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
                
                // Create the NeoForgeHologram instance, which adds itself to the manager
                NeoForgeHologram hologram = new NeoForgeHologram(
                    id,
                    data.world,
                    data.x,
                    data.y,
                    data.z,
                    data.lines
                );
                // Hologram is added to manager via its constructor.
                // Spawning will be handled by HologramManager or other logic if needed.
                hologram.spawn(); // Spawn after adding to manager
                LOGGER.debug("Loaded hologram into manager: " + id);
            }
        } catch (Exception e) {
            LOGGER.error("Error loading holograms into manager from config", e);
            Files.writeString(configFile, "{}"); // Attempt to reset if corrupt
        }
    }
    
    /**
     * Saves the configuration to disk
     * 
     * @throws IOException If saving fails
     */
    public void save() throws IOException {
        LOGGER.info("Saving configuration to " + configFile.toAbsolutePath());
        
        Map<String, HologramData> hologramDataMap = new HashMap<>();
        
        // Convert holograms to serializable data
        for (Map.Entry<String, Hologram> entry : HologramManager.getHolograms().entrySet()) {
            String id = entry.getKey();
            Hologram hologram = entry.getValue();
            
            HologramData data = new HologramData();
            data.world = hologram.getWorld();
            data.x = hologram.getX();
            data.y = hologram.getY();
            data.z = hologram.getZ();
            data.lines = hologram.getLines();
            
            hologramDataMap.put(id, data);
        }
        
        // Write to file
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(hologramDataMap, writer);
        }
    }
    
    /**
     * Saves holograms from the HologramManager to the config file.
     * This is called by HologramManager.save()
     * 
     * @param holograms The map of holograms from the HologramManager
     * @throws IOException If saving fails
     */
    public void saveHologramsFromManager(Map<String, Hologram> holograms) throws IOException {
        LOGGER.info("Saving {} holograms from manager to {}", holograms.size(), configFile.toAbsolutePath());
        
        Map<String, HologramData> hologramDataMap = new HashMap<>();
        
        for (Map.Entry<String, Hologram> entry : holograms.entrySet()) {
            String id = entry.getKey();
            Hologram hologram = entry.getValue();
            
            HologramData data = new HologramData();
            data.world = hologram.getWorld();
            data.x = hologram.getX();
            data.y = hologram.getY();
            data.z = hologram.getZ();
            data.lines = hologram.getLines();
            
            hologramDataMap.put(id, data);
        }
        
        try (Writer writer = Files.newBufferedWriter(configFile)) {
            GSON.toJson(hologramDataMap, writer);
            LOGGER.debug("Successfully saved holograms from manager.");
        } catch (IOException e) {
            LOGGER.error("Failed to save holograms from manager to config", e);
            throw e; // Re-throw to allow caller to handle
        }
    }
    
    /**
     * Data class for serializing hologram data
     */
    private static class HologramData {
        String world;
        double x, y, z;
        List<String> lines;
    }
} 