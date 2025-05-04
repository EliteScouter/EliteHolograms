package com.strictgaming.elite.holograms.neo21.hologram;

import com.strictgaming.elite.holograms.api.hologram.Hologram;
import com.strictgaming.elite.holograms.neo21.Neo21Holograms;
import com.strictgaming.elite.holograms.neo21.config.HologramsConfig;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Manager for handling hologram operations
 */
public class HologramManager {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, Hologram> HOLOGRAMS = new HashMap<>();
    
    /**
     * Initialize the hologram system
     */
    public static void preInit() {
        LOGGER.info("Pre-initializing hologram manager");
    }
    
    /**
     * Load holograms from config
     * 
     * @throws IOException If loading fails
     */
    public static void load() throws IOException {
        LOGGER.info("Loading holograms from config");
        HologramsConfig config = Neo21Holograms.getInstance().getConfig();
        
        if (config == null) {
            LOGGER.warn("Config is null, can't load holograms");
            return;
        }
        
        // Clear existing holograms
        HOLOGRAMS.values().forEach(Hologram::despawn);
        HOLOGRAMS.clear();
        
        // Let the config class handle loading
        config.load();
    }
    
    /**
     * Add a hologram to the manager
     * 
     * @param hologram The hologram to add
     */
    public static void addHologram(Hologram hologram) {
        HOLOGRAMS.put(hologram.getId(), hologram);
        saveHolograms();
    }
    
    /**
     * Get a hologram by ID
     * 
     * @param id The hologram ID
     * @return The hologram, if found
     */
    public static Optional<Hologram> getHologram(String id) {
        return Optional.ofNullable(HOLOGRAMS.get(id));
    }
    
    /**
     * Remove a hologram from the manager
     * 
     * @param id The hologram ID
     * @return True if removed, false if not found
     */
    public static boolean removeHologram(String id) {
        Hologram hologram = HOLOGRAMS.remove(id);
        if (hologram != null) {
            // Ensure the hologram is despawned before removing
            if (hologram.isSpawned()) {
                LOGGER.debug("Despawning hologram {} during removal", id);
                hologram.despawn();
            }
            saveHolograms();
            return true;
        }
        return false;
    }
    
    /**
     * Get all holograms
     * 
     * @return The map of holograms
     */
    public static Map<String, Hologram> getHolograms() {
        return HOLOGRAMS;
    }
    
    /**
     * Save holograms to config
     * 
     * @throws IOException If saving fails
     */
    public static void save() throws IOException {
        // Save to config
        HologramsConfig config = Neo21Holograms.getInstance().getConfig();
        if (config != null) {
            config.save();
            LOGGER.info("Saved " + HOLOGRAMS.size() + " holograms to config");
        } else {
            LOGGER.warn("Config is null, can't save holograms");
        }
    }
    
    /**
     * Helper method to save holograms and handle exceptions
     */
    private static void saveHolograms() {
        try {
            save();
        } catch (IOException e) {
            LOGGER.error("Failed to save holograms", e);
        }
    }
} 